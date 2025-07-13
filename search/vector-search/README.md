### Vector Search with Redis OM Spring Demo

Vector similarity search (also known as semantic search) is a powerful technique that allows you to find items based on their semantic meaning rather than exact keyword matches. Redis Query Engine supports vector similarity search through its vector indexing capabilities, enabling you to implement semantic search applications with high performance and low latency.

This demo showcases how to implement vector similarity search using Redis OM Spring, a library that simplifies working with Redis data models and the Redis Query Engine.

#### Key Features

1. **Vector Similarity Search**: Find movies with similar descriptions using semantic similarity
2. **Automatic Embedding Generation**: Automatically generate vector embeddings from text
3. **Hybrid Search**: Combine vector search with traditional filtering (title, cast, year, genres)
4. **K-Nearest Neighbors (KNN)**: Find the most similar items using KNN algorithm

#### How It Works

The application uses Redis OM Spring annotations to define a document model with vector embedding capabilities:

##### Defining the document

```java
@Document
public class Movie {
    @Id
    private String id;

    @Searchable
    @AutoComplete
    private String title;

    @Indexed(sortable = true)
    private int year;

    @Indexed
    private List<String> cast;

    @Indexed
    private List<String> genres;

    @Vectorize(
            destination = "embeddedExtract",
            embeddingType = EmbeddingType.SENTENCE
    )
    private String extract;

    @Indexed(
            schemaFieldType = SchemaFieldType.VECTOR,
            algorithm = VectorField.VectorAlgorithm.HNSW,
            type = VectorType.FLOAT32,
            dimension = 384,
            distanceMetric = DistanceMetric.COSINE,
            initialCapacity = 10
    )
    private float[] embeddedExtract;
    
    // ...
}
```

Key annotations:
- `@Document`
  - **Purpose:** Marks the class as a Redis JSON document.
  - **Effect:** This tells Redis OM Spring to persist instances of this class in Redis as a JSON Document using a specific index and key pattern.
  - **Similar to:** @Entity in JPA.
  
- `@Id`
  - **Purpose:** Marks the field as the unique identifier for the document.
  - **Effect:** This field will be used as the Redis key suffix (e.g., movies:{id}). If not populated, Redis OM Spring will automatically populate it with a randomly generated ULID.

- `@Searchable`
  - **Purpose:** Enables full-text search on this field.
  - **Effect:** The field is indexed as TEXT and becomes searchable via `FT.SEARCH` or Redis OM queries.

- `@AutoComplete`
  - **Purpose:** Enables support for auto-completion queries.
  - **Effect:** Creates a Redis AutoComplete index, allowing prefix matching and suggestions.

- `@Indexed(sortable = true)`
  - **Purpose:** Indexes the field and allows sorting on it.
  - **Effect:** Enables range queries and ordering by this field. sortable = true is required for sorting.

- `@Indexed`
  - **Purpose:** Indexes a field for filtering or querying.
  - **Effect:** Enables efficient Redis-based filters. For List<String>, it’s typically treated as a TAG field.

- `@Vectorize`
  - **Purpose:** Tells Redis OM to generate a vector embedding from the extract field.
  - **Params:**
      - **destination** = "embeddedExtract": Output embedding will be stored in the embeddedExtract field.
      - **embeddingType** = EmbeddingType.SENTENCE: Embedding model will treat the input as a sentence for encoding.
  - **Effect:** Automatically generates and stores vector embeddings at runtime (or insert time), used for vector search.

- `@Indexed(...)` (on embeddedExtract)
  - **Purpose:** Marks the float array as a vector field for vector similarity search.
  - **Params:**
    - **schemaFieldType** = SchemaFieldType.VECTOR: Declares this as a vector.
    - **algorithm** = VectorField.VectorAlgorithm.HNSW: Uses the HNSW (Hierarchical Navigable Small World) algorithm for approximate nearest neighbor search.
    - **type** = VectorType.FLOAT32: Declares each vector component as a 32-bit float.
    - **dimension** = 384: Sets the expected dimensionality of the vector (must match the model’s output).
    - **distanceMetric** = DistanceMetric.COSINE: Uses cosine similarity for ranking vectors.
    - **initialCapacity** = 10: Preallocates capacity in the index (mainly for optimization).
  - **Effect:** Allows querying Redis with KNN or hybrid vector + filter search.

##### Defining a repository:


##### Searching through the index:

To effectively search through the schema, we will use Redis OM Spring's EntityStream:

```java
float[] embeddedQuery = embedder
        .getTextEmbeddingsAsFloats(List.of(extract), Movie$.EXTRACT)
        .getFirst();

SearchStream<Movie> stream = entityStream.of(Movie.class);
List<Pair<Movie, Double>> matchedMovies = stream
        .filter(Movie$.EMBEDDED_EXTRACT.knn(numberOfNearestNeighbors, embeddedQuery))
        .filter(Movie$.TITLE.containing(title))
        .filter(Movie$.CAST.eq(actors))
        .filter(Movie$.YEAR.eq(year))
        .filter(Movie$.GENRES.eq(genres))
        .map(Fields.of(Movie$._THIS, Movie$._EMBEDDED_EXTRACT_SCORE))
        .collect(Collectors.toList());
```

Let's break it down:

1. Embed the Extract Field using Redis OM Spring's Embedder:

```java
float[] embeddedQuery = embedder
    .getTextEmbeddingsAsFloats(List.of(extract), Movie$.EXTRACT)
    .getFirst();
```

- This line converts the extract string into a float array (embedding vector).
- embedder.getTextEmbeddingsAsFloats(...):
  - Likely uses a sentence embedding model (like OpenAI, BERT, etc.).
  - Takes the text input (extract) and embeds it using the model.
- Movie$.EXTRACT tells the embedder which field this embedding is meant to correspond to.
- getFirst() retrieves the first (and only) embedding since only one input string was given.

2. Create a Redis SearchStream for Movie Entities

```java
SearchStream<Movie> stream = entityStream.of(Movie.class);
```

- Initializes a SearchStream over the Redis index for Movie objects.
- This is the Redis OM abstraction for running fluent-style Redis Query Engine queries.

3. Filter by Vector Similarity and Structured Metadata:

```java
List<Pair<Movie, Double>> matchedMovies = stream
    .filter(Movie$.EMBEDDED_EXTRACT.knn(numberOfNearestNeighbors, embeddedQuery))
```

- Applies a K-Nearest Neighbors (KNN) vector similarity filter on the embeddedExtract field.
- Returns the top numberOfNearestNeighbors most similar documents to the embeddedQuery.

```java
    .filter(Movie$.TITLE.containing(title))
    .filter(Movie$.CAST.eq(actors))
    .filter(Movie$.YEAR.eq(year))
    .filter(Movie$.GENRES.eq(genres))
```

These are structured filters that further narrow down the results:
- TITLE.containing(title) – partial text match on movie title.
- CAST.eq(actors) – exact match on cast list.
- YEAR.eq(year) – filter by exact year.
- GENRES.eq(genres) – exact genre match.

4. Select Fields and Collect Results: 

```java
    .map(Fields.of(Movie$._THIS, Movie$._EMBEDDED_EXTRACT_SCORE))
    .collect(Collectors.toList());
```

- .map(...) projects only the actual Movie object (_THIS) and the similarity score (_EMBEDDED_EXTRACT_SCORE).
- Returns a List<Pair<Movie, Double>>:
  - Movie = the matched object
  - Double = similarity score (higher = more similar)

```java
float[] embeddedQuery = embedder.getTextEmbeddingsAsFloats(List.of(extract), Movie$.EXTRACT).getFirst();
stream = stream.filter(Movie$.EMBEDDED_EXTRACT.knn(numberOfNearestNeighbors, embeddedQuery))
               .sorted(Movie$._EMBEDDED_EXTRACT_SCORE);
```

#### Running the Demo

1. Start Redis with the Redis Open Source Docker image:

```shell
docker run -p 6379:6379 redis
```

2. Run the application:

```shell
./gradlew :search:vector-search:bootRun
```

It will take around 2 minutes to embed all the movies

3. Open your browser to http://localhost:8080

4. Try searching for movies by entering a description in the search box. The application will find movies with semantically similar descriptions.

5. Use the filters to narrow down your search by genres, year, or cast members.

#### API Endpoints

- `GET /search?title=&text=&cast=&year=&genres=&numberOfNearestNeighbors=`: Search movies with vector similarity and filters
- `GET /genres`: Get all available genres

#### Screenshots

##### Autocomplete of title

![A movie search web application showing an autocomplete dropdown for the “Movie Title” input field. The user has typed “Finding”, and the system suggests titles including “Finding You”, “Finding Nemo”, “Finding Dory”, “Finding Bliss”, and “Finding Amanda”. To the right, there are additional input fields for “Movie Extract”, “Year”, “Cast”, and a dropdown for “Genre” (currently filtered by “animate”). The bottom section displays a “Search Results” header indicating 5 movies found in 5 ms, with movie cards partially visible.](readme-assets/autocomplete.png)

##### Vector Search

![A movie search web app titled “Movie Search and Autocomplete.” The user has entered the text “movie about a clownfish who searches for his son” in the “Movie Extract” input field. No filters are applied for title, cast, year, or genre. The number of nearest neighbors is set to 10.  Under “Search Results,” the app displays 10 movies found in 6 milliseconds, with the top three being: 1.	Swordfish (2001) – Similarity Score: 0.4660. Cast: Hugh Jackman, John Travolta, Halle Berry, Don Cheadle. Genres: Crime, Drama, Action, Thriller. The card includes a black-and-white image from the movie. 2.	Big Fish (2003) – Similarity Score: 0.4842. Cast includes Ewan McGregor and Albert Finney. Genres: Comedy, Drama, Fantasy. Card shows a stylized tree on a landscape. 3.	Finding Nemo (2003) – Similarity Score: 0.5050. Cast: Albert Brooks, Ellen DeGeneres. Genres: Animated, Family, Adventure, Comedy, Drama. Poster features a shark underwater.  Each movie card includes title, year, similarity score, cast, genres, and a short description.](readme-assets/vector-search.png)

##### Pre-filtered Vector Search

![A movie search web application interface titled “Movie Search and Autocomplete.” The user has entered “movie about a clownfish who searches for his son” in the “Movie Extract” field, selected “Albert Brooks” under “Cast,” and filtered the “Genre” by “animated.” The search results show 5 movies found in 5 milliseconds. Displayed movies include: 1.	Finding Nemo (2003) – with a similarity score of 0.505 and cast including Albert Brooks and Ellen DeGeneres. 2.	Finding Nemo 3D (2012) – same similarity score and expanded cast list. 3.	Finding Dory (2016) – similarity score of 0.604, featuring returning and additional cast members.  Each result includes a poster image, year, cast, genres, and a short description.](readme-assets/pre-filtered-vector-search.png)

#### Redis Insight

RedisInsight is a graphical tool developed by Redis to help developers and administrators interact with and manage Redis databases more efficiently. It provides a visual interface for exploring keys, running commands, analyzing memory usage, and monitoring performance metrics in real-time. RedisInsight supports features like full-text search, time series, streams, and vector data structures, making it especially useful for working with more advanced Redis use cases. With its intuitive UI, it simplifies debugging, optimizing queries, and understanding data patterns without requiring deep familiarity with the Redis CLI.

Video: [Redis Insight Deep Dive](https://www.youtube.com/watch?v=dINUz_XOZ0M)

[Get Redis Insight](https://redis.io/insight/)

#### View of a selected document in Redis Insight

![RedisInsight displaying a JSON document with key com.redis.vectorsearch.domain.Movie:01K026NX3HNGQ1491OV34PTBSP, representing the movie “Absence of Malice” (1981), including fields such as title, year, cast, genres, extract, embeddedExtract, and a thumbnail image URL.](readme-assets/selected-document-redis-insight.png)

#### View of Movie Index's schema

!A RedisInsight view of the MovieIdx index (com.redis.vectorsearch.domain.MovieIdx) showing schema fields like title, year, cast, genres, embeddedExtract, and id, with embeddedExtract configured as an HNSW vector field using FLOAT32 type, 384 dimensions, and cosine distance.](readme-assets/schema-redis-insight.png)