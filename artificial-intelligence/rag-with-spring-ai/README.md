### RAG with Spring AI Demo

Retrieval-Augmented Generation (RAG) is a technique that enhances Large Language Models (LLMs) by providing them with relevant information retrieved from a knowledge base. This demo showcases how to implement RAG using Spring AI and Redis Vector Store to create a beer recommendation system.

#### Key Features

1. **Spring AI Integration**: Use Spring AI's abstractions for working with LLMs and vector embeddings
2. **Transformers Embedding Model**: Generate embeddings using the Transformers library
3. **Redis Vector Store**: Store and search vector embeddings in Redis
4. **Retrieval-Augmented Generation**: Enhance LLM responses with relevant information from a knowledge base
5. **Performance Metrics**: Track embedding, search, and LLM processing times

#### How It Works

The application uses Spring AI's `RedisVectorStore` to store and search vector embeddings of beer data, and Spring AI's `ChatModel` to generate responses.

##### Configuring the Vector Store

```kotlin
@Bean
fun memoryVectorStore(
    embeddingModel: TransformersEmbeddingModel,
    jedisPooled: JedisPooled
): RedisVectorStore {
    return RedisVectorStore.builder(jedisPooled, embeddingModel)
        .indexName("beerIdx")
        .contentFieldName("content")
        .embeddingFieldName("embedding")
        .prefix("beer:")
        .initializeSchema(true)
        .vectorAlgorithm(RedisVectorStore.Algorithm.HSNW)
        .build()
}
```

Let's break this down:

- **Index Name**: `beerIdx` - Redis will create an index with this name for searching beer data
- **Content Field**: `content` - The raw beer data that will be embedded
- **Embedding Field**: `embedding` - The field that will store the resulting vector embedding
- **Prefix**: `beer:` - All vector documents in Redis will be stored with keys starting with "beer:" to namespace them properly
- **Initialize Schema**: `true` - Automatically create the index and schema on startup if it doesn't already exist
- **Vector Algorithm**: `HSNW` - Use the Hierarchical Navigable Small World algorithm for approximate nearest neighbor search

##### Loading Beer Data

The application loads beer data from a JSON file and stores it in the vector store:

```kotlin
val loader = JsonReader(file, *KEYS)
val documents = loader.get()
val batchSize = 500

documents.chunked(batchSize).forEachIndexed { index, batch ->
    vectorStore.add(batch)
    logger.info("Inserted batch ${index + 1} with ${batch.size} documents")
}
```

The data is processed in batches of 500 documents to avoid overwhelming the system.

##### Implementing RAG

The RAG service retrieves relevant beer information based on the user's question and augments the LLM prompt with this information:

```kotlin
fun retrieve(message: String): RagResult {
    // Get documents with timing metrics
    val (embTime, searchTime, docs) = measureAugmentTime(message)
    
    val systemMessage = getSystemMessage(docs)
    val userMessage = UserMessage(message)
    val prompt = Prompt(listOf(systemMessage, userMessage))
    
    // Measure time for LLM processing
    val startLlmTime = System.currentTimeMillis()
    val response: ChatResponse = chatModel.call(prompt)
    val llmTimeMs = System.currentTimeMillis() - startLlmTime
    
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

The system prompt is augmented with the retrieved documents:

```kotlin
private val systemBeerPrompt = """
    You're assisting with questions about products in a beer catalog.
    Use the information from the DOCUMENTS section to provide accurate answers.
    The answer involves referring to the ABV or IBU of the beer, include the beer name in the response.
    If unsure, simply state that you don't know.

    DOCUMENTS:
    {documents}
""".trimIndent()
```

##### API Endpoints

The application provides two API endpoints:

1. `POST /chat/startChat`: Start a new chat session and get a chat ID
2. `POST /chat/{chatId}`: Send a message to a specific chat session and get a response

#### Running the Demo

1. Start Redis with the Redis Open Source image:

```shell
docker run -p 6379:6379 redis
```

2. Run the application:

```shell
./gradlew :artificial-intelligence:rag-with-spring-ai:bootRun
```

3. Open your browser to http://localhost:8080

4. Start a chat session and ask questions about beers, such as:
   - "What's the ABV of Sierra Nevada Pale Ale?"
   - "Can you recommend a beer with high IBU?"
   - "What's a good beer for someone who likes fruity flavors?"

#### Benefits of RAG

1. **Enhanced Accuracy**: By providing the LLM with relevant information, RAG improves the accuracy of responses, especially for domain-specific questions.
2. **Reduced Hallucinations**: The LLM is less likely to generate incorrect information when it has access to factual data.
3. **Up-to-date Information**: The knowledge base can be updated independently of the LLM, ensuring that responses reflect the latest information.
4. **Customization**: RAG allows you to tailor the LLM's responses to your specific domain or use case.

#### Redis Insight

RedisInsight is a graphical tool developed by Redis to help developers and administrators interact with and manage Redis databases more efficiently. It provides a visual interface for exploring keys, running commands, analyzing memory usage, and monitoring performance metrics in real-time. RedisInsight supports features like full-text search, time series, streams, and vector data structures, making it especially useful for working with more advanced Redis use cases. With its intuitive UI, it simplifies debugging, optimizing queries, and understanding data patterns without requiring deep familiarity with the Redis CLI.

Video: [Redis Insight Deep Dive](https://www.youtube.com/watch?v=dINUz_XOZ0M)

[Get Redis Insight](https://redis.io/insight/)