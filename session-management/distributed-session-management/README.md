### Distributed Session Management Demo

In modern applications, relying on in-memory or file-based session storage is not enough — especially when your app is deployed in a clustered or cloud environment with multiple instances behind a load balancer. Each instance managing its own session data results in inconsistent user experiences and broken authentication flows.

To solve this, Spring provides support for distributed session management using a shared store like Redis. Instead of storing sessions in memory or on disk, session data is kept in Redis, allowing all app instances to access and update the same session regardless of which one the request hits.

#### How to Enable Redis-backed Sessions

1. Start an instance of Redis locally using Docker:

`docker run -p 6379:6379 redis`

2. Include the following dependencies to your gradle or maven file:

```kotlin
    implementation("org.springframework.session:spring-session-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
```

3. And the following properties to your properties file:

```text
# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.session.redis.namespace=spring:session
spring.session.redis.flush-mode=immediate
spring.session.redis.repository-type=default

# Session Configuration
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=false
server.servlet.session.tracking-modes=cookie
server.servlet.session.timeout=60s
```

#### Running the demo

1.	Start two instances of your Spring Boot application on different ports:

```shell
./distributed-session-management/gradlew bootRun --args='--server.port=8081'
./distributed-session-management/gradlew bootRun --args='--server.port=8082'
```

2. Log in on one instance (e.g., localhost:8081).
3. Then visit the other instance (e.g., localhost:8082) in the same browser and without logging in again — you’ll still be authenticated, because both instances share the same session from Redis.