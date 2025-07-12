### Vector Search with Spring AI Demo

Vector similarity search (semantic search) allows you to find items based on their semantic meaning rather than exact keyword matches. Spring AI provides a standardized way to work with AI models and vector embeddings across different providers. This demo showcases how to integrate Redis Vector Search with Spring AI to implement semantic search applications.

#### Key Features

1. **Spring AI Integration**: Use Spring AI's abstractions for working with vector embeddings
2. **Transformers Embedding Model**: Generate embeddings using the Transformers library
3. **Redis Vector Store**: Store and search vector embeddings in Redis
4. **Pre-filtered Search**: Combine vector search with traditional filtering (title, cast, year, genres)

#### How It Works

The application uses Spring AI's `RedisVectorStore` to store and search vector embeddings.

RedisVectorStore is a Spring AI abstraction for storing and querying vector-embedded documents (in this case, movie data) using Redis (via the Redis Query Engine). It’s commonly used for semantic search, recommendations, or similarity-based queries.

```kotlin
@Bean
fun movieVectorStore(
    embeddingModel: EmbeddingModel,
    jedisPooled: JedisPooled
): RedisVectorStore {
    return RedisVectorStore.builder(jedisPooled, embeddingModel)
        .indexName("movieIdx")
        .contentFieldName("extract")
        .embeddingFieldName("extractEmbedding")
        .metadataFields(
            RedisVectorStore.MetadataField("title", Schema.FieldType.TEXT),
            RedisVectorStore.MetadataField("year", Schema.FieldType.NUMERIC),
            RedisVectorStore.MetadataField("cast", Schema.FieldType.TAG),
            RedisVectorStore.MetadataField("genres", Schema.FieldType.TAG),
            RedisVectorStore.MetadataField("thumbnail", Schema.FieldType.TEXT),
        )
        .prefix("movies:")
        .initializeSchema(true)
        .vectorAlgorithm(RedisVectorStore.Algorithm.HSNW)
        .build()
}
```

Let's break this down:

##### A vector store must be defined for each index as a Spring Bean:

```kotlin
@Bean
fun movieVectorStore(
    embeddingModel: EmbeddingModel,
    jedisPooled: JedisPooled
): RedisVectorStore
```

This Spring bean creates and returns a configured RedisVectorStore. It depends on:
- embeddingModel: used to convert text (e.g. movie descriptions) into vector embeddings. (Video: [What's an embedding model?](https://www.youtube.com/watch?v=0U1S0WSsPuE))
- jedisPooled: a thread-safe pooled Redis client to interact with Redis.

##### Index Name: 

```kotlin
.indexName("movieIdx")
```

Redis will create an index named movieIdx — this is used to search the vector data efficiently.

##### Field to be embbeded:

```kotlin
.contentFieldName("extract")
.embeddingFieldName("extractEmbedding")
```

- "extract": the raw content (e.g. movie synopsis) that will be embedded.
- "extractEmbedding": the field that will store the resulting vector embedding.

##### Metadata fields:

```kotlin
.metadataFields(
    RedisVectorStore.MetadataField("title", Schema.FieldType.TEXT),
    RedisVectorStore.MetadataField("year", Schema.FieldType.NUMERIC),
    RedisVectorStore.MetadataField("cast", Schema.FieldType.TAG),
    RedisVectorStore.MetadataField("genres", Schema.FieldType.TAG),
    RedisVectorStore.MetadataField("thumbnail", Schema.FieldType.TEXT),
)
```

These fields are indexed as part of the Redis schema and can be used in filtering queries:
- TEXT: full-text searchable (e.g. title, thumbnail URL)
- NUMERIC: range queries (e.g. year)
- TAG: exact-match filtering (e.g. genre, cast)

##### Redis Document Key Prefix: 

```kotlin
.prefix("movies:")
```

All vector documents in Redis will be stored with keys starting with "movies:" to namespace them properly.

##### Schema Initialization:

```kotlin
.initializeSchema(true)
```

Tells RedisVectorStore to automatically create the index and schema on startup, if it doesn’t already exist.

##### Vector Similarity Algorithm:

```kotlin
.vectorAlgorithm(RedisVectorStore.Algorithm.HSNW)
```

Specifies the approximate nearest-neighbor algorithm to use (Video: [Exact vs Approximate Nearest Neighbor](https://www.youtube.com/watch?v=9NvO-VdjY80)). HSNW (Hierarchical Navigable Small World) is fast and well-suited for high-dimensional vector search.

Once this bean is registered, your application can:
- Store movies along with their embeddings in Redis
- Search for similar movies by embedding a query and running a vector similarity search
- Filter the results by metadata (e.g., genre, year, cast)

#### Storing movies using the MovieVectorStore

Movies are stored as Spring AI `Document` objects with metadata:

```kotlin
val documents = movies.map { movie ->
    val text = movie.extract ?: ""
    val metadata = mapOf(
        "title" to (movie.title ?: ""),
        "year" to movie.year,
        "cast" to movie.cast,
        "genres" to movie.genres,
        "thumbnail" to (movie.thumbnail ?: "")
    )
    Document(text, metadata)
}
movieVectorStore.add(documents)
```

#### Searching movies with the MovieVectorStore

The search service uses Spring AI's `SearchRequest` and `FilterExpressionBuilder` to perform vector similarity search with filters:

```kotlin
val b = FilterExpressionBuilder()

val filterList = mutableListOf<FilterExpressionBuilder.Op>()

if (title.isNotBlank()) {
    filterList.add(b.`in`("title", title))
}

// [...more filters]

val filterExpression = when (filterList.size) {
    0 -> null
    1 -> filterList[0]
    else -> filterList.reduce { acc, expr -> b.and(acc, expr) }
}?.build()

val searchResults = movieVectorStore.similaritySearch(
    SearchRequest.builder()
        .query(extract)
        .topK(numberOfNearestNeighbors)
        .filterExpression(filterExpression)
        .build()
)
```

This Kotlin snippet performs a semantic search (Video: [What is semantic search?](https://www.youtube.com/watch?v=o3XN4dImESE)) on a Redis vector store (movieVectorStore), building up optional filters dynamically based on provided parameters. Here’s a breakdown of what’s happening:

1. Filter Expression Builder Initialization

```kotlin
val b = FilterExpressionBuilder()
```

Creates a builder (b) to help construct complex Redis filter expressions. It’s used to filter documents based on metadata like title, year, cast, etc.

2. Collect Filters Based on Conditions

```kotlin
val filterList = mutableListOf<FilterExpressionBuilder.Op>()
```

This list will hold all the conditional filters — each representing a Redis-compatible metadata condition.

```kotlin
if (title.isNotBlank()) {
    filterList.add(b.`in`("title", title))
}
```

If a non-empty title is provided, a filter is added to match documents where the "title" metadata matches the input.

Similar conditional checks (not shown here) would be used to filter by cast, year, genres, etc.

3. Combine Filters into One Expression

```kotlin
val filterExpression = when (filterList.size) {
    0 -> null
    1 -> filterList[0]
    else -> filterList.reduce { acc, expr -> b.and(acc, expr) }
}?.build()
```

This logic constructs the final Redis filter expression:
- If there are no filters, the search is unfiltered.
- If there’s only one, it uses it as-is.
- If there are multiple, it chains them with logical AND using `b.and(...)`.

`.build()` converts it into the final FilterExpression string Redis expects.

4. Run the Semantic Vector Search

```kotlin
val searchResults = movieVectorStore.similaritySearch(
    SearchRequest.builder()
        .query(extract)                              // The semantic search query (text to embed)
        .topK(numberOfNearestNeighbors)              // Return top N closest matches
        .filterExpression(filterExpression)          // Apply optional metadata filtering
        .build()
)
```

This performs a vector similarity search using:
- A semantic query (extract) that is embedded into a vector,
- A topK setting to limit how many nearest matches to return,
- A Redis filter expression to narrow down by metadata.

#### Running the Demo

1. Start Redis with the Redis Open Source image:

```shell
docker run -p 6379:6379 redis
```

2. Run the application:

```shell
./gradlew :search:vector-search-spring-ai:bootRun
```

It will take around 2 minutes to embed all movies

3. Open your browser to http://localhost:8080

4. Try searching for movies by entering a description in the search box. The application will find movies with semantically similar descriptions.

5. Use the filters to narrow down your search by genres, year, or cast members.

#### Screenshots

##### View of vector search

![A web interface for “Movie Search” showing a form with input fields for Movie Title, Movie Extract, Year, Cast, Genre, and Number of Nearest Neighbors. The user has entered the extract: “a movie about a kid and a doctor who go back in t”. Below the search form, the “Search Results” section displays 10 movies found in 17 milliseconds. The results include movie cards for “Back to School” (1986), “Time Changer” (2002), and “Back to the Future” (1985), each showing a similarity score and cast details.](readme-assets/vector-search.png)

##### View of pre-filtered vector search

![A “Movie Search” web UI with search fields populated: “Back” in the Movie Title field, and “a movie about a kid and a doctor who go back in t” in the Movie Extract field. The Genre filter is set to “science fiction,” and Number of Nearest Neighbors is 10. Below the form, the “Search Results” section shows 4 movies found in 11 milliseconds. Displayed results include “Back to the Future” (1985), “Back to the Future Part III” (1990), and “Back to the Future Part II” (1989), each showing a similarity score, cast list, genres, a short synopsis, and a “Read more” link.](readme-assets/vector-search.png)

##### View of list of genres

![A web-based “Movie Search” interface showing a partially filled form. The “Movie Extract” field contains the text “a movie about a kid and a doctor who go back in t”, while other fields are blank. The “Genre” dropdown is expanded, displaying a searchable list of genres including “romance”, “independent”, “martial arts”, “superhero”, and “historical”. Below the form, a “Search Results” section indicates 10 movies were found in 17 milliseconds, with movie cards partially visible at the bottom.](readme-assets/list-of-genres.png)

##### Redis Insight

RedisInsight is a graphical tool developed by Redis to help developers and administrators interact with and manage Redis databases more efficiently. It provides a visual interface for exploring keys, running commands, analyzing memory usage, and monitoring performance metrics in real-time. RedisInsight supports features like full-text search, time series, streams, and vector data structures, making it especially useful for working with more advanced Redis use cases. With its intuitive UI, it simplifies debugging, optimizing queries, and understanding data patterns without requiring deep familiarity with the Redis CLI.

Video: [Redis Insight Deep Dive](https://www.youtube.com/watch?v=dINUz_XOZ0M)

[Get Redis Insight](https://redis.io/insight/)

##### View of all documents in Redis Insight

![RedisInsight interface. The left panel lists Redis keys under the “movies” namespace, each identified as a JSON type with approximately 11 KB size. The right panel is currently empty, prompting the user to select a key to view its details. The interface includes key filters, a refresh indicator, and memory usage stats at the top.](./readme-assets/view-of-all-documents-in-redis-insight.png)

#### View of a selected document in Redis Insight

![RedisInsight interface showing detailed view of a selected JSON key named movies:12dab771-29ae-44fe-b44d-758557fc673b. On the left panel, a list of movie JSON keys is displayed under the “movies” namespace. The right panel reveals the selected key’s content, including fields such as cast, thumbnail, extractEmbedding, extract, year, genres, and title. The extract field contains a detailed synopsis of the movie “Toy Story 4”.](./readme-assets/view-of-all-documents-in-redis-insight.png)

#### View of Movie Index's schema 

![RedisInsight interface displaying the result of the command FT.INFO 'movieIdx'. The output panel shows index metadata for a Redis Search index named movieIdx that is indexing JSON documents with prefix movies:. The table outlines indexed fields: extract, extractEmbedding, title, year, cast, genres, and thumbnail, including their types (e.g., TEXT, VECTOR, NUMERIC, TAG), weight, and vector indexing configuration (HNSW algorithm, FLOAT32 type, 384 dimensions, COSINE distance, etc.). Summary statistics are shown at the bottom, including total document and term counts.](./readme-assets/view-of-all-documents-in-redis-insight.png)

#### View of the result of `FT.TAGVALS movieIdx genres`

![RedisInsight interface showing the result of the FT.TAGVALS 'movieIdx' genres command. The screen displays a list of genre tag values indexed in the movieIdx RediSearch index, including values such as "action", "adventure", "animated", "biography", "comedy", "crime", "dance", "disaster", "documentary", "drama", "erotic", and "family". The response is timestamped and executed in 1.103 milliseconds.](./readme-assets/view-of-tagvals-in-redis-insight.png)

#### API Endpoints

- `GET /search?title=&text=&cast=&year=&genres=&numberOfNearestNeighbors=`: Search movies with vector similarity and filters
- `GET /genres`: Get all available genres

