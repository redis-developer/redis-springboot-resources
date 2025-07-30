<img src="readme-assets/redis-logo.webp" style="width: 130px" alt="Redis Logo">

# Redis Spring Boot Recipes

Redis Spring Boot Recipes is a comprehensive collection of practical examples and patterns demonstrating how to leverage Redis with Spring Boot for various common application needs. This repository serves as a reference implementation for developers looking to integrate Redis into their Spring Boot applications.

## Use Cases

This repository contains several modules demonstrating different Redis capabilities with Spring Boot:

### Artificial Intelligence

| Module                             | Description                                                                               | Link                                                                                         |
|------------------------------------|-------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| Agent Memory with Spring AI        | Demonstrates how to implement AI agent memory using Redis and Spring AI                   | [agent-memory-with-spring-ai](./artificial-intelligence/agent-memory-with-spring-ai)         |
| RAG with Spring AI                 | Shows how to implement Retrieval-Augmented Generation (RAG) using Redis and Spring AI     | [rag-with-spring-ai](./artificial-intelligence/rag-with-spring-ai)                           |
| Semantic Caching with Spring AI    | Illustrates how to implement semantic caching for LLM responses using Redis and Spring AI | [semantic-caching-with-spring-ai](./artificial-intelligence/semantic-caching-with-spring-ai) |
| Vector Search with Redis OM Spring | Shows how to implement vector similarity search using Redis OM Spring                     | [vector-search](./search/vector-search)                                                      |
| Vector Search with Spring AI       | Illustrates how to implement vector similarity search using Spring AI and Redis           | [vector-search-spring-ai](./search/vector-search-spring-ai)                                  |

### Search

| Module                                                 | Description                                                                           | Link                                                                            |
|--------------------------------------------------------|---------------------------------------------------------------------------------------|---------------------------------------------------------------------------------|
| Full-Text Search and Autocomplete with Redis OM Spring | Demonstrates how to implement full-text search and autocomplete using Redis OM Spring | [full-text-search-and-autocomplete](./search/full-text-search-and-autocomplete) |
| Vector Search with Redis OM Spring                     | Shows how to implement vector similarity search using Redis OM Spring                 | [vector-search](./search/vector-search)                                         |
| Vector Search with Spring AI                           | Illustrates how to implement vector similarity search using Spring AI and Redis       | [vector-search-spring-ai](./search/vector-search-spring-ai)                     |

### Security

| Module    | Description                                                                                                                                          | Link                              |
|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------|
| Multi-ACL | Demonstrates how to configure and manage multiple Redis ACL users in Spring Boot, separating read and write responsibilities across different users. | [multi-acl](./security/multi-acl) |                                                                                                                                                      |      |


### Session Management

| Module                         | Description                                                                             | Link                                                                                  |
|--------------------------------|-----------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| Distributed Session Management | Demonstrates how to implement distributed session management in Spring Boot using Redis | [distributed-session-management](./session-management/distributed-session-management) |

Each module contains one or more complete Spring Boot applications that demonstrate the respective Redis capabilities.

## Getting Started

Each recipe is contained in its own directory with specific instructions on how to run and use the example.

## Contributing

Contributions are welcome! If you have a recipe or improvement you'd like to share, please feel free to submit a pull request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
