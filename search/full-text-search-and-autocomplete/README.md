### Full-Text Search and Autocomplete with Redis OM Spring Demo

Full-text search and autocomplete are powerful features that allow users to find relevant information quickly and efficiently. Redis Query Engine provides robust capabilities for implementing these features with high performance and low latency. This demo showcases how to implement both full-text search and autocomplete functionality using Redis OM Spring, a library that simplifies working with Redis data models and the Redis Query Engine.

#### Key Features

1. **Full-Text Search**: Search through movie descriptions and titles with support for complex queries
2. **Autocomplete**: Get real-time suggestions as you type
3. **Filtering**: Filter search results by multiple criteria (title, cast, year, genres)
4. **Faceted Search**: Browse and filter by movie genres
5. **High Performance**: Leverage Redis's in-memory data structure for fast search and retrieval

#### How It Works

The application uses Redis OM Spring annotations to define a searchable document model:

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

    @Searchable
    @AutoCompletePayload("title")
    private String extract;
    
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
  - **Effect:** Enables efficient Redis-based filters. For List<String>, it's typically treated as a TAG field.

- `@AutoCompletePayload`
  - **Purpose:** Associates additional data with autocomplete suggestions.
  - **Effect:** When autocomplete suggestions are returned, they include the specified field as payload data.

##### Defining a repository

```java
public interface MovieRepository extends RedisDocumentRepository<Movie, String> {
    List<Suggestion> autoCompleteTitle(String title, AutoCompleteOptions options);

    Iterable<String> getAllGenres();
}
```

The repository interface extends RedisDocumentRepository and defines methods for autocomplete and retrieving all genres. Redis OM Spring automatically implements these methods based on the annotations in the Movie class.

##### Searching through the index

To effectively search through the schema, we use Redis OM Spring's EntityStream:

```java
SearchStream<Movie> stream = entityStream.of(Movie.class);
List<Movie> matchedMovies = stream
        .filter(Movie$.TITLE.containing(title))
        .filter(Movie$.EXTRACT.containing(extract))
        .filter(Movie$.CAST.eq(actors))
        .filter(Movie$.YEAR.eq(year))
        .filter(Movie$.GENRES.eq(genres))
        .sorted(Movie$.YEAR)
        .collect(Collectors.toList());
```

Let's break it down:

1. Create a Redis SearchStream for Movie Entities

```java
SearchStream<Movie> stream = entityStream.of(Movie.class);
```

- Initializes a SearchStream over the Redis index for Movie objects.
- This is the Redis OM abstraction for running fluent-style Redis Query Engine queries.

2. Apply Filters for Full-Text Search and Structured Metadata:

```java
.filter(Movie$.TITLE.containing(title))
.filter(Movie$.EXTRACT.containing(extract))
```

- TITLE.containing(title) – performs full-text search on the movie title
- EXTRACT.containing(extract) – performs full-text search on the movie extract/description

```java
.filter(Movie$.CAST.eq(actors))
.filter(Movie$.YEAR.eq(year))
.filter(Movie$.GENRES.eq(genres))
```

These are structured filters that further narrow down the results:
- CAST.eq(actors) – exact match on cast list
- YEAR.eq(year) – filter by exact year
- GENRES.eq(genres) – exact genre match

3. Sort and Collect Results:

```java
.sorted(Movie$.YEAR)
.collect(Collectors.toList());
```

- .sorted(Movie$.YEAR) – orders the results by year
- .collect(Collectors.toList()) – collects the results into a List

##### Implementing Autocomplete

Autocomplete functionality is implemented using the MovieRepository:

```java
List<Suggestion> suggestions = movieRepository
        .autoCompleteTitle(query, AutoCompleteOptions.get().withPayload());
```

- autoCompleteTitle – calls the repository method that leverages the @AutoComplete annotation on the title field
- AutoCompleteOptions.get().withPayload() – configures the autocomplete to include payload data (the extract field)

#### Running the Demo

1. Start Redis with the Redis Open Source image:

```shell
docker run -p 6379:6379 redis
```

2. Run the application:

```shell
./gradlew :search:full-text-search-and-autocomplete:bootRun
```

3. Open your browser to http://localhost:8080

4. Try searching for movies by typing in the search box. You'll see autocomplete suggestions appear as you type.

5. Use the filters to narrow down your search by genres, year, or cast members.

#### API Endpoints

- `GET /search/{q}`: Get autocomplete suggestions for a query
- `GET /search?title=&text=&cast=&year=&genres=`: Search movies with filters
- `GET /genres`: Get all available genres

#### Redis Insight

RedisInsight is a graphical tool developed by Redis to help developers and administrators interact with and manage Redis databases more efficiently. It provides a visual interface for exploring keys, running commands, analyzing memory usage, and monitoring performance metrics in real-time. RedisInsight supports features like full-text search, time series, streams, and vector data structures, making it especially useful for working with more advanced Redis use cases. With its intuitive UI, it simplifies debugging, optimizing queries, and understanding data patterns without requiring deep familiarity with the Redis CLI.

Video: [Redis Insight Deep Dive](https://www.youtube.com/watch?v=dINUz_XOZ0M)

[Get Redis Insight](https://redis.io/insight/)