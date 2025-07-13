### Redis Agent Memory Demo

Redis Agent Memory provides a robust solution for implementing short-term and long-term memory capabilities for AI agents. It enables agents to store, retrieve, and use different types of memories, enhancing their ability to maintain context and provide personalized responses.

#### Key Features

1. **Long-term memory—Episodic Memory Storage**: Store personal experiences and user preferences
2. **Long-term memory—Semantic Memory Storage**: Store general knowledge and facts
3. **Vector Similarity Search**: Retrieve relevant memories based on semantic similarity
4. **Short-term memory**: Maintain context across multiple interactions
5. **Memory Deduplication**: Check for similar existing memories to avoid duplication
6. **Conversation History**: Store and retrieve conversation history
7. **Memory Summarization**: Automatically summarize long conversations

#### How It Works

The application uses Spring AI's `RedisVectorStore` to store and search vector embeddings of memories.

##### Configuring the Vector Store

```kotlin
@Bean
fun memoryVectorStore(
    embeddingModel: EmbeddingModel,
    jedisPooled: JedisPooled
): RedisVectorStore {
    return RedisVectorStore.builder(jedisPooled, embeddingModel)
        .indexName("memoryIdx")
        .contentFieldName("content")
        .embeddingFieldName("embedding")
        .metadataFields(
            RedisVectorStore.MetadataField("memoryType", Schema.FieldType.TAG),
            RedisVectorStore.MetadataField("metadata", Schema.FieldType.TEXT),
            RedisVectorStore.MetadataField("userId", Schema.FieldType.TAG),
            RedisVectorStore.MetadataField("createdAt", Schema.FieldType.TEXT)
        )
        .prefix("memory:")
        .initializeSchema(true)
        .vectorAlgorithm(RedisVectorStore.Algorithm.HSNW)
        .build()
}
```

Let's break this down:

- **Index Name**: `memoryIdx` - Redis will create an index with this name for searching memories
- **Content Field**: `content` - The raw memory content that will be embedded
- **Embedding Field**: `embedding` - The field that will store the resulting vector embedding
- **Metadata Fields**:
  - `memoryType`: TAG field for filtering by memory type (EPISODIC or SEMANTIC)
  - `metadata`: TEXT field for storing additional context about the memory
  - `userId`: TAG field for filtering by user ID
  - `createdAt`: TEXT field for storing the creation timestamp

##### Storing Memories

Memories are stored as Spring AI `Document` objects with metadata:

```kotlin
val memory = Memory(
    content = content,
    memoryType = memoryType,
    userId = userId ?: systemUserId,
    metadata = validatedMetadata,
    createdAt = LocalDateTime.now()
)

val document = Document(
    content,
    mapOf(
        "memoryType" to memoryType.name,
        "metadata" to validatedMetadata,
        "userId" to (userId ?: systemUserId),
        "createdAt" to memory.createdAt.toString()
    )
)

memoryVectorStore.add(listOf(document))
```

##### Retrieving Memories

The memory service uses Spring AI's `SearchRequest` and `FilterExpressionBuilder` to perform vector similarity search with filters:

```kotlin
val b = FilterExpressionBuilder()
val filterList = mutableListOf<FilterExpressionBuilder.Op>()

// Add user filter
val effectiveUserId = userId ?: systemUserId
filterList.add(b.eq("userId", effectiveUserId))

// Add memory type filter if specified
if (memoryType != null) {
    filterList.add(b.eq("memoryType", memoryType.name))
}

// Combine filters
val filterExpression = when (filterList.size) {
    0 -> null
    1 -> filterList[0]
    else -> filterList.reduce { acc, expr -> b.and(acc, expr) }
}?.build()

// Execute search
val searchResults = memoryVectorStore.similaritySearch(
    SearchRequest.builder()
        .query(query)
        .topK(limit)
        .filterExpression(filterExpression)
        .build()
)
```

This performs a vector similarity search using:
- A semantic query that is embedded into a vector
- A topK setting to limit how many nearest matches to return
- A Redis filter expression to narrow down by memory type, user ID, and thread ID

##### Agent System Prompt

The agent is configured with a system prompt that explains its capabilities and access to different types of memory:

```kotlin
@Bean
fun travelAgentSystemPrompt(): Message {
    val promptText = """
        You are a travel assistant helping users plan their trips. You remember user preferences
        and provide personalized recommendations based on past interactions.

        You have access to the following types of memory:
        1. Short-term memory: The current conversation thread
        2. Long-term memory:
           - Episodic: User preferences and past trip experiences (e.g., "User prefers window seats")
           - Semantic: General knowledge about travel destinations and requirements

        Always be helpful, personal, and context-aware in your responses.

        Always answer in text format. No markdown or special formatting.
    """.trimIndent()

    return SystemMessage(promptText)
}
```

#### Running the Demo

1. Start Redis with the Redis Open Source image:

```shell
docker run -p 6379:6379 redis
```

2. Run the application:

```shell
./gradlew :artificial-intelligence:agent-memory:bootRun
```

3. Open your browser to http://localhost:8080

4. Interact with the travel assistant by sending messages. The assistant will remember your preferences and provide personalized responses based on your conversation history.

#### API Endpoints

- `GET /api/memory/retrieve&userId=`: Retrieve memories from a user
- `POST /api/chat/send?message=&userId=`: Send a message to the AI agent and get a response
- `GET /api/chat/history?userId=`: Get the conversation history for a thread
- `DELETE /api/chat/history?userId=`: Clear the conversation history for a thread

#### Redis Insight

RedisInsight is a graphical tool developed by Redis to help developers and administrators interact with and manage Redis databases more efficiently. It provides a visual interface for exploring keys, running commands, analyzing memory usage, and monitoring performance metrics in real-time. RedisInsight supports features like full-text search, time series, streams, and vector data structures, making it especially useful for working with more advanced Redis use cases. With its intuitive UI, it simplifies debugging, optimizing queries, and understanding data patterns without requiring deep familiarity with the Redis CLI.

Video: [Redis Insight Deep Dive](https://www.youtube.com/watch?v=dINUz_XOZ0M)

[Get Redis Insight](https://redis.io/insight/)
