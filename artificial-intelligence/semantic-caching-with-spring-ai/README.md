### Semantic Caching with Spring AI Demo

Semantic Caching is a technique that enhances Large Language Model (LLM) applications by caching responses based on the semantic meaning of queries rather than exact matches. This demo showcases how to implement Semantic Caching using Spring AI and Redis Vector Store to improve performance and reduce costs in a beer recommendation system.

#### Key Features

1. **Spring AI Integration**: Use Spring AI's abstractions for working with LLMs and vector embeddings
2. **Transformers Embedding Model**: Generate embeddings using the Transformers library
3. **Redis Vector Store**: Store and search vector embeddings in Redis
4. **Semantic Similarity Matching**: Find cached responses for semantically similar queries
5. **Configurable Similarity Threshold**: Control the precision of cache hits
6. **Performance Metrics**: Track embedding, search, and LLM processing times
7. **Integrated with RAG**: Combine semantic caching with Retrieval-Augmented Generation

#### How It Works

The application uses Spring AI's `RedisVectorStore` to store and search vector embeddings of queries and their responses, and Spring AI's `ChatModel` to generate responses when cache misses occur.

##### Configuring the Semantic Cache

```kotlin
@Bean
fun semanticCachingVectorStore(
    embeddingModel: TransformersEmbeddingModel,
    jedisPooled: JedisPooled
): RedisVectorStore {
    return RedisVectorStore.builder(jedisPooled, embeddingModel)
        .indexName("semanticCachingIdx")
        .contentFieldName("content")
        .embeddingFieldName("embedding")
        .metadataFields(
            RedisVectorStore.MetadataField("answer", Schema.FieldType.TEXT),
            )
        .prefix("semantic-caching:")
        .initializeSchema(true)
        .vectorAlgorithm(RedisVectorStore.Algorithm.HSNW)
        .build()
}
```

Let's break this down:

- **Index Name**: `semanticCachingIdx` - Redis will create an index with this name for searching cached queries
- **Content Field**: `content` - The raw query text that will be embedded
- **Embedding Field**: `embedding` - The field that will store the resulting vector embedding
- **Metadata Fields**: `answer` - TEXT field for storing the cached response
- **Prefix**: `semantic-caching:` - All vector documents in Redis will be stored with keys starting with "semantic-caching:" to namespace them properly
- **Initialize Schema**: `true` - Automatically create the index and schema on startup if it doesn't already exist
- **Vector Algorithm**: `HSNW` - Use the Hierarchical Navigable Small World algorithm for approximate nearest neighbor search

##### Storing and Retrieving from Cache

The semantic caching service provides two main methods:

```kotlin
fun storeInCache(prompt: String, answer: String) {
    semanticCachingVectorStore.add(listOf(Document(
        prompt,
        mapOf(
            "answer" to answer
        )
    )))
}

fun getFromCache(prompt: String, similarityThreshold: Double): String? {
    val results = semanticCachingVectorStore.similaritySearch(
        SearchRequest.builder()
            .query(prompt)
            .topK(1)
            .build()
    )

    if (results?.isNotEmpty() == true) {
        if (similarityThreshold < (results[0].score ?: 0.0)) {
            logger.info("Returning cached answer. Similarity score: ${results[0].score}")
            return results[0].metadata["answer"] as String
        }
    }

    return null
}
```

The `storeInCache` method stores a query and its corresponding answer in the Redis vector store. The `getFromCache` method retrieves a cached answer for a semantically similar query if the similarity score exceeds the specified threshold.

##### Integrating with RAG

The RAG service integrates semantic caching with the retrieval-augmented generation process:

```kotlin
fun retrieve(message: String): RagResult {
    val startCachingTime = System.currentTimeMillis()
    val cachedAnswer = semanticCachingService.getFromCache(message, 0.8)
    val cachingTimeMs = System.currentTimeMillis() - startCachingTime

    if (cachedAnswer != null) {
        return RagResult(
            generation = Generation(AssistantMessage(cachedAnswer)),
            metrics = RagMetrics(
                embeddingTimeMs = 0,
                searchTimeMs = 0,
                llmTimeMs = 0
            )
        )
    }

    // Regular RAG process if no cache hit
    // ...

    semanticCachingService.storeInCache(message, response.result.output.text.toString())

    return RagResult(
        generation = response.result,
        metrics = RagMetrics(
            embeddingTimeMs = embeddingTimeMs,
            searchTimeMs = searchTimeMs,
            llmTimeMs = llmTimeMs
        )
    )
}
```

The service first checks if a semantically similar query exists in the cache. If found, it returns the cached answer immediately. If not, it performs the regular RAG process and stores the new query and response in the cache for future use.

#### Running the Demo

1. Start Redis with the Redis Open Source image:

```shell
docker run -p 6379:6379 redis
```

2. Run the application:

```shell
./gradlew :artificial-intelligence:semantic-caching-with-spring-ai:bootRun
```

3. Open your browser to http://localhost:8080

4. Start asking questions about beers, such as:
   - "What's the ABV of Sierra Nevada Pale Ale?"
   - "Can you recommend a beer with high IBU?"
   - "What's a good beer for someone who likes fruity flavors?"

Notice how subsequent similar questions are answered much faster as they're retrieved from the semantic cache.

#### Benefits of Semantic Caching

1. **Improved Performance**: Cached responses are returned immediately without the need for LLM processing.
2. **Reduced Costs**: Fewer LLM API calls means lower costs, especially for high-volume applications.
3. **Consistent Responses**: Users receive the same answer for semantically similar questions.
4. **Configurable Precision**: Adjust the similarity threshold to control the balance between cache hits and precision.
5. **Seamless Integration**: Works with existing RAG systems with minimal changes.

#### Redis Insight

RedisInsight is a graphical tool developed by Redis to help developers and administrators interact with and manage Redis databases more efficiently. It provides a visual interface for exploring keys, running commands, analyzing memory usage, and monitoring performance metrics in real-time. RedisInsight supports features like full-text search, time series, streams, and vector data structures, making it especially useful for working with more advanced Redis use cases. With its intuitive UI, it simplifies debugging, optimizing queries, and understanding data patterns without requiring deep familiarity with the Redis CLI.

Video: [Redis Insight Deep Dive](https://www.youtube.com/watch?v=dINUz_XOZ0M)

[Get Redis Insight](https://redis.io/insight/)