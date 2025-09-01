### Context-Enabled Semantic Caching with Spring AI Demo

Semantic Caching is a technique that enhances Large Language Model (LLM) applications by caching responses based on the semantic meaning of queries rather than exact matches. 

Even though Semantic Caching can help us save costs and time, it may come with downsides depending on the business on which its applied.

Sometimes, prompts may be similar, but refer to different contexts. For example: `What kind of beer goes well with meat?` and `What kind of beer goes well with Pizza?`

These two prompts are semantically similar, but refer to two different context: `Pizza` and `Meat` - This is where Context Enabled Semantic Caching may help. 

Instead of relying solely on the semantic caching, we can serve the cached response to a less capable, cheaper, and faster model with the new provided information so that it can generate a response that satisfies the prompt with information, tone, or other characteristics that came from the more capable model.

This demo showcases how to implement Context-Enabled Semantic Caching using Spring AI and Redis Vector Store to improve performance and reduce costs in a beer recommendation system.

## Learning resources:

- Video: [What is semantic caching?](https://www.youtube.com/watch?v=AtVTT_s8AGc)
- Video: [What is an embedding model?](https://youtu.be/0U1S0WSsPuE)
- Video: [Exact vs Approximate Nearest Neighbors - What's the difference?](https://youtu.be/9NvO-VdjY80)
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
cd redis-springboot-recipes/artificial-intelligence/semantic-caching-with-spring-ai
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
- semantic-caching-app: the Spring Boot app that implements the RAG application

## Using the demo

When all of your services are up and running. Go to `localhost:8080` to access the demo.

![Screenshot of a web app titled “Semantic Caching with Spring AI.” It features a Beer Knowledge Assistant chat interface with a welcome message, input box, and “Start New Chat” and “Clear Chat” buttons. The footer displays “Powered by Redis.”](readme-assets/1_home.png)

If you click on `Start Chat`, it may be that the embeddings are still being created, and you get a message asking for this operation to complete. This is the operation where the documents we'll search through will be turned into vectors and then stored in the database. It is done only the first time the app starts up and is required regardless of the vector database you use.

![Popup message stating that embeddings are still being created (14,472 of 20,000 completed), with an estimated duration of three minutes and a “Close” button.](readme-assets/2_embeddings_being_created.png)

Once all the embeddings have been created, you can start asking your chatbot questions. It will semantically search through the documents we have stored, try to find the best answer for your questions, and cache the responses semantically in Redis:

![Animated screen recording of a user typing “What kind of beer goes well with smoked meat?” into the Beer Knowledge Assistant in the Semantic Caching with Spring AI demo. The interface shows the question being sent, demonstrating semantic search in action.](readme-assets/3_asking_a_question.gif)

If you ask something similar to a question had already been asked, your chatbot will retrieve it from the cache instead of sending the query to the LLM. Retrieving an answer much faster now.

![Animated screen recording showing a user asking a similar follow-up question, “What type of beer is a good combination with smoked beef?” The assistant instantly retrieves a cached answer from Redis, demonstrating faster response through semantic caching.](readme-assets/4_retrieving_from_cache.gif)

## How It Is Implemented

The application uses Spring AI's `RedisVectorStore` to store and retrieve responses from a semantic cache.

### Configuring the Chat Models

```kotlin
@Bean
fun openAiExpensiveChatModel(): OpenAiChatModel {
  val modelName = "gpt-5-2025-08-07"
  return openAiChatModel(modelName)
}

@Bean
fun openAiCheapChatModel(): OpenAiChatModel {
  val modelName = "gpt-5-nano-2025-08-07"
  return openAiChatModel(modelName)
}

private fun openAiChatModel(modelName: String): OpenAiChatModel {
  val openAiApi = OpenAiApi.builder()
      .apiKey(System.getenv("OPENAI_API_KEY"))
      .build()
  val openAiChatOptions = OpenAiChatOptions.builder()
      .model(modelName)
      .temperature(0.4)
      .build()

  return OpenAiChatModel.builder()
      .openAiApi(openAiApi)
      .defaultOptions(openAiChatOptions)
      .build()
}
```

### Configuring the Semantic Cache

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

- **Index Name**: `semanticCachingIdx` - Redis will create an index with this name for searching cached responses
- **Content Field**: `content` - The raw prompt that will be embedded
- **Embedding Field**: `embedding` - The field that will store the resulting vector embedding
- **Metadata Fields**: `answer` - A TEXT field to store the LLM's response
- **Prefix**: `semantic-caching:` - All keys in Redis will be prefixed with this to organize the data
- **Vector Algorithm**: `HSNW` - Hierarchical Navigable Small World algorithm for efficient approximate nearest neighbor search

### Storing Responses in the Semantic Cache

When a user asks a question and the system generates a response, it stores the prompt and response in the semantic cache:

```kotlin
fun storeInCache(prompt: String, answer: String) {
    semanticCachingVectorStore.add(listOf(Document(
        prompt,
        mapOf(
            "answer" to answer
        )
    )))
}
```

This method:
1. Creates a `Document` with the prompt as the content
2. Adds the answer as metadata
3. Stores the document in the vector store, which automatically generates and stores the embedding

### Retrieving Responses from the Semantic Cache

When a user asks a question, the system first checks if there's a semantically similar question in the cache:

```kotlin
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

This method:
1. Performs a vector similarity search for the most similar prompt in the cache
2. Checks if the similarity score is above the threshold (typically 0.8)
3. If a match is found, the system uses the cheaper model to compute the new response based on the new knowledge and the previously generated response.

### Integrating with the RAG System

The RAG service integrates the semantic cache with the RAG system:

```kotlin
// Regular prompt and prompt suffix in case of cache hit

 private val systemBeerPrompt = """
        You're assisting with questions about products in a beer catalog.
        Use the information from the DOCUMENTS section to provide accurate answers.
        The answer involves referring to the ABV or IBU of the beer, include the beer name in the response.
        If unsure, simply state that you don't know.

        DOCUMENTS:
        {documents}
    """.trimIndent()

private val semanticCachedAnswerPromptSuffix = """
        A similar prompt has been processed before. Use it as the base for your response with the new document selection and new prompt:
        
        SIMILAR PROMPT ALREADY PROCESSED:
            SIMILAR PROMPT:
            {similarPrompt}
            
            SIMILAR ANSWER:
            {similarAnswer}
    """.trimIndent()


 fun retrieve(message: String): RagResult {
    // Get documents
    val docs = getDocuments(message)

    // Get potential cached answer
    val (cachedQuestion, cachedAnswer) = semanticCachingService.getFromCache(message, 0.8)

    // Generate System Prompt
    val systemMessage = if (cachedQuestion != null && cachedAnswer != null) {
       getSystemMessage(docs, cachedQuestion, cachedAnswer)
    } else {
       getSystemMessage(docs)
    }

    val userMessage = UserMessage(message)

    val prompt = Prompt(listOf(systemMessage, userMessage))

    // Call the expensive or cheap model accordingly
    val response: ChatResponse  = if (cachedQuestion != null && cachedAnswer != null) {
       openAiCheapChatModel.call(prompt)
    } else {
       openAiExpensiveChatModel.call(prompt)
    }

    // Store in semantic caching
    semanticCachingService.storeInCache(message, response.result.output.text.toString())

    return RagResult(
       generation = response.result
    )
 }
```

This orchestrates the entire process:
1. Check if there's a semantically similar prompt in the cache
2. If found, return the cached answer immediately
3. If not found, perform the standard RAG process:
   - Retrieve relevant documents using vector similarity search
   - Generate a response using the LLM
   - Store the prompt and response in the semantic cache for future use

This approach significantly improves performance and reduces costs by avoiding unnecessary LLM calls for semantically similar queries, while still providing accurate and contextually relevant responses.
