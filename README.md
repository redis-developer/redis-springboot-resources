# Redis Spring Boot Recipes

This repository contains a collection of recipes and examples demonstrating how to use Redis with Spring Boot for various common use cases.

## Overview

Redis is a versatile in-memory data structure database that can be used as a cache, message broker, search store, vector store, and more. When combined with Spring Boot, it provides powerful solutions for many common application requirements.

This repository aims to provide practical, ready-to-use examples of Redis integration with Spring Boot applications.

## Recipes

### Session Management

The [session-management](./session-management) directory contains examples of how to implement session management in Spring Boot applications:

- **In-App Session Management**: Demonstrates the default Spring Boot session management behavior, where sessions are stored in the application's memory.
  
- **Distributed Session Management**: Shows how to implement distributed session management using Redis, allowing sessions to be shared across multiple application instances.

For more details, see the [Session Management README](./session-management/README.md).

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