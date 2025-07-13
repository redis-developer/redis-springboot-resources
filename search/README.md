# Redis Query Engine (former RediSearch)
the Redis Query Engine is a powerful search engine built into Redis that provides advanced search capabilities for Redis data structures. It enables real-time indexing and querying of textual, numeric, and geospatial data stored in Redis, allowing for complex search operations without the need for a separate search engine.

## What is the Redis Query Engine?

Redis Search extends Redis with full-text search capabilities, secondary indexing, and query language support. It allows you to:

- Create indexes on Redis data
- Perform full-text searches with complex queries
- Filter by exact match, ranges, and geospatial queries
- Implement autocomplete functionality
- Perform vector similarity search (KNN) for semantic search applications

The Redis Query Engine is fully integrated with Redis, providing high performance, scalability, and real-time indexing with minimal latency.

## Applicability

Redis Search is particularly useful for:

1. **Full-text search applications**: Search through text fields with support for stemming, fuzzy matching, and complex queries.
2. **Autocomplete systems**: Implement type-ahead suggestions with prefix matching.
3. **Filtering and faceted search**: Filter data based on multiple criteria and aggregate results.
4. **E-commerce search**: Provide fast and relevant product search with filtering by categories, price ranges, etc.
5. **Content discovery**: Help users discover relevant content based on their interests.
6. **Vector similarity search**: Implement semantic search, recommendation systems, and other AI-powered search applications using vector embeddings.
7. **Real-time analytics**: Combine search with aggregations for real-time analytics on your data.

## Use Cases

This module contains several examples demonstrating different Redis Search capabilities:

| Use Case                           | Description                                                                                                                     | Link                                                                     |
|------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| Full-text Search and Autocomplete  | Demonstrates how to implement full-text search and autocomplete functionality using the Redis Query Engine with Redis OM Spring | [full-text-search-and-autocomplete](./full-text-search-and-autocomplete) |
| Vector Search with Redis OM Spring | Shows how to implement vector similarity search (KNN) for semantic search applications with Redis OM Spring                     | [vector-search](./vector-search)                                         |
| Vector Search with Spring AI       | Demonstrates integration of Redis Vector Search with Spring AI                                                                  | [vector-search-spring-ai](./vector-search-spring-ai)                     |

Each submodule contains a complete Spring Boot application that demonstrates the respective Redis Query Engine capabilities.