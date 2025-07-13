# Redis Spring Boot Recipes

Redis Spring Boot Recipes is a comprehensive collection of practical examples and patterns demonstrating how to leverage Redis with Spring Boot for various common application needs. This repository serves as a reference implementation for developers looking to integrate Redis into their Spring Boot applications.

## What is Redis Spring Boot Recipes?

Redis Spring Boot Recipes provides ready-to-use examples of Redis integration with Spring Boot applications. It allows you to:

- Implement distributed session management for scalable applications
- Use Redis Query Engine for full-text search and autocomplete functionality
- Leverage vector similarity search for AI-powered applications
- Explore various Redis data structures and capabilities within a Spring Boot context
- Learn best practices for Redis integration with Spring Boot

The examples are designed to be practical, well-documented, and easy to understand, making them ideal for both learning and production use.

## Applicability

Redis Spring Boot Recipes is particularly useful for:

1. **Web applications**: Implement session management, caching, and search functionality.
2. **Microservices architectures**: Enable stateless application design while preserving user context.
3. **AI-powered applications**: Implement semantic search, recommendation systems, and other AI features.
4. **E-commerce platforms**: Provide fast and relevant product search with filtering capabilities.
5. **Real-time applications**: Leverage Redis's high performance for real-time data processing.
6. **Learning and exploration**: Understand Redis capabilities and integration patterns with Spring Boot.
7. **Production-ready implementations**: Use as a reference for implementing Redis features in production applications.

## Use Cases

This repository contains several modules demonstrating different Redis capabilities with Spring Boot:

| Module                  | Description                                                                                              | Link                                                 |
|-------------------------|----------------------------------------------------------------------------------------------------------|------------------------------------------------------|
| Session Management      | Demonstrates how to implement session management in Spring Boot applications using Redis                 | [session-management](./session-management)           |
| Search                  | Shows how to use the Redis Query Engine for full-text search, autocomplete, and vector similarity search | [search](./search)                                   |
| Artificial Intelligence | Demonstrates how to implement AI features using Redis, including agent memory, context awareness, and retrieval-augmented generation | [artificial-intelligence](./artificial-intelligence) |

Each module contains one or more complete Spring Boot applications that demonstrate the respective Redis capabilities.

## Getting Started

Each recipe is contained in its own directory with specific instructions on how to run and use the example.

### Prerequisites

- Java 17 or higher
- Gradle
- Docker (for running Redis)

### Running Redis

Most examples require a Redis instance. You can start one using Docker:

```bash
docker run -p 6379:6379 redis
```

## Contributing

Contributions are welcome! If you have a recipe or improvement you'd like to share, please feel free to submit a pull request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
