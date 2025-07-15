# RAG with Spring AI Demo

Retrieval-Augmented Generation (RAG) is a technique that enhances Large Language Models (LLMs) by providing them with relevant information retrieved from a knowledge base. This demo showcases how to implement RAG using Spring AI and Redis Vector Store to create a beer recommendation system.

## Learning resources:

- Video: [What is an embedding model?](https://youtu.be/0U1S0WSsPuE)
- Video: [Exact vs Approximate Nearest Neighbors - What's the difference?](https://youtu.be/9NvO-VdjY80)
- Video: [What is RAG?](https://youtu.be/xPMQ2cVbUTI)
- Video: [What is semantic search?](https://youtu.be/o3XN4dImESE)
- Video: [What is a vector database?](https://youtu.be/Yhv19le0sBw)

## Requirements

To run this demo, you’ll need the following installed on your system:
- Docker – [Install Docker](https://docs.docker.com/get-docker/)
- Docker Compose – Included with Docker Desktop or available via CLI installation guide
- An OpenAI API Key – You can get one from [platform.openai.com](https://platform.openai.com)

## Running the demo

The easiest way to run the demo is with Docker Compose, which sets up all required services in one command.

### Step 1: Clone the repository

If you haven’t already:

```bash
git clone https://github.com/redis-developer/redis-springboot-recipes.git
cd redis-springboot-recipes/artificial-intelligence/rag-with-spring-ai
```

### Step 2: Configure your environment

You can pass your OpenAI API key in two ways:

#### Option 1: Export the key via terminal

```bash
export OPENAI_API_KEY=sk-your-api-key
```

#### Option 2: Use a .env file

Create a `.env` file in the same directory as the `docker-compose.yml` file:

```env
OPENAI_API_KEY=sk-your-api-key
```

### Step 3: Start the services

```bash
docker compose up --build
```

This will start:

- redis: for storing both vector embeddings and chat history
- redis-insight: a UI to explore the Redis data
- rag-app: the Spring Boot app that implements the RAG application

## Using the demo

When all of your services are up and running. Go to `localhost:8080` to access the demo.

![Screenshot of the RAG with Spring AI demo web interface. The page displays a chat application titled “Beer Knowledge Assistant.” The description above the chat box explains that the demo showcases a Retrieval-Augmented Generation system using Spring AI and Redis, designed to answer questions about beer products. The assistant welcomes the user and invites questions about beer characteristics like ABV and IBU. Red “Start New Chat” and “Clear Chat” buttons are visible, and a text input field is at the bottom. The UI is styled with a red theme and labeled “Powered by Redis.”](readme-assets/1_app_home.png)

If you click on `Start Chat`, it may be that the embeddings are still being created and you get a message asking for this operation to complete. This is the operation where the documents we'll search through will be turned into vectors and then stored in the database. It is done only the first time the app starts up and is required regardless of the vector database you use. 

![Popup message indicating embedding progress. The message reads: “Embeddings are still being created (2500 of 20000 already created). This operation takes around three minutes to complete. Please try again later.” A “Close” button is displayed at the bottom right. The dialog box has a gray background behind it.](readme-assets/2_embeddings_still_processing.png)

Once all the embeddings have been created, you can start asking your chatbot questions. It will semantically search through the documents we have stored and try to find the best answer for your questions: 

![User interacting with the Beer Knowledge Assistant demo. The user types the question “What kind of beer goes well with smoked meat?” into the input field and clicks the red “Send” button. The interface shows a chat window with the system’s greeting and a response from the assistant is expected. The branding at the top reads “RAG with Spring AI” and the bottom banner says “Powered by Redis.”](readme-assets/3_sending_message.gif)

### Redis Insight

RedisInsight is a graphical tool developed by Redis to help developers and administrators interact with and manage Redis databases more efficiently. It provides a visual interface for exploring keys, running commands, analyzing memory usage, and monitoring performance metrics in real-time. RedisInsight supports features like full-text search, time series, streams, and vector data structures, making it especially useful for working with more advanced Redis use cases. With its intuitive UI, it simplifies debugging, optimizing queries, and understanding data patterns without requiring deep familiarity with the Redis CLI.

The Docker Compose file will also spin up an instance of Redis Insight. We can access it by going to `localhost:5540`:

If we go to Redis Insight, we will be able to see the data stored in Redis:

![RedisInsight view of a Redis database containing beer data. The interface shows a list of JSON keys under the beer prefix, each representing an individual beer record. One key (beer:0000f112-a09f-4838-a060-bd679695615c) is selected, displaying its JSON content on the right, which includes an embedding and descriptive text about the beer “Hoe Hoe Hoe”—a Belgian Style Wheat Bier with details like ABV (5.5), IBU (10.5), ingredients, and flavor profile. The RedisInsight app is labeled “RAG with Spring AI” in the browser tab.](readme-assets/4_redis_insight.png)

And if run the command `FT.INFO 'beerIdx'`, we'll be able to see the schema that was created for indexing our documents efficiently:

![RedisInsight showing FT.INFO for beerIdx vector index. The Workbench tab displays the output of the FT.INFO 'beerIdx' command. The index is built on JSON documents with the prefix beer:. Two attributes are indexed: $.content (TEXT with weight 1) and $.embedding (VECTOR using HNSW algorithm, FLOAT32, 384 dimensions, cosine distance, M=16, EF_CONSTRUCTION=200). Metadata at the bottom shows the index contains 21,999 documents and over 1.2 million records. The interface is part of a demo labeled “RAG with Spring AI”.](readme-assets/redis_insight_index.png)

## How It Is Implemented

The application uses Spring AI's `RedisVectorStore` to store and search vector embeddings of beer descriptions.

### Configuring the Vector Store

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

- **Index Name**: `beerIdx` - Redis will create an index with this name for searching beer descriptions
- **Content Field**: `content` - The raw beer description that will be embedded
- **Embedding Field**: `embedding` - The field that will store the resulting vector embedding
- **Prefix**: `beer:` - All keys in Redis will be prefixed with this to organize the data
- **Vector Algorithm**: `HSNW` - Hierarchical Navigable Small World algorithm for efficient approximate nearest neighbor search

### Performing Vector Similarity Search

When a user asks a question, the system performs vector similarity search to find relevant beer descriptions:

```kotlin
private fun measureAugmentTime(message: String): Triple<Long, Long, List<Document>> {
    val request = SearchRequest
        .builder()
        .query(message)
        .topK(topK)
        .build()

    // Measure total search time (includes embedding)
    val startSearchTime = System.currentTimeMillis()
    val documents = store.similaritySearch(request) ?: emptyList()
    val totalSearchTime = System.currentTimeMillis() - startSearchTime

    // Estimate embedding time as a portion of search time
    val embeddingTimeMs = (totalSearchTime * 0.7).toLong()
    val searchTimeMs = totalSearchTime - embeddingTimeMs

    return Triple(embeddingTimeMs, searchTimeMs, documents)
}
```

This performs a vector similarity search using:
- The user's query, which is embedded into a vector
- A topK setting to limit how many nearest matches to return

### Stuffing the Prompt with Retrieved Information

The system then creates a prompt that includes the retrieved beer descriptions:

```kotlin
private fun getSystemMessage(docsForAugmentation: List<Document>): Message {
    val documents = docsForAugmentation.joinToString("\n") { it.text.toString() }

    val systemPromptTemplate = SystemPromptTemplate(systemBeerPrompt)
    return systemPromptTemplate.createMessage(mapOf("documents" to documents))
}
```

The system prompt template includes a placeholder for the retrieved documents:

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

### Generating the Answer

Finally, the system generates an answer using the LLM:

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

This orchestrates the entire RAG process:
1. Retrieve relevant documents using vector similarity search
2. Create a system message that includes the retrieved documents
3. Create a prompt with the system message and the user's message
4. Call the LLM to generate a response based on the prompt
5. Return the response along with performance metrics

This approach allows the system to provide accurate answers about beers by combining the knowledge of the LLM with specific information retrieved from the vector database.
