## 🔐 What is Session Management?

Session management is the process of maintaining user-specific state across multiple requests in a stateless protocol like HTTP. It enables web applications to remember who a user is, whether they're authenticated, and what actions or data are associated with them — such as login status, shopping cart contents, or user preferences.

## 💡 In Application Memory Session Management

By default, most web frameworks (including Spring Boot) store session data **in memory** on the application server. When a user logs in, a session is created and associated with a unique session ID (typically stored in a cookie). On later requests, the server uses this ID to retrieve the user’s session data from its local memory.

## ⚠️ Limitations of Sessions stored in the memory of the application

While simple and fast, in-memory session management has serious limitations:

- **Not scalable**: Sessions are tied to a specific server. If the app is scaled to multiple instances, users may lose their session when routed to another instance.
- **No failover**: If the server crashes or restarts, all sessions are lost.
- **No central visibility**: Sessions can't be shared or inspected across nodes.

These limitations make in-application-memory sessions unsuitable for modern, cloud-native, or distributed applications.

## 🌐 Distributed Session Management

Distributed session management solves these problems by storing session data in a **centralized external store** — such as Redis or a database. This allows all application instances to access the same session data, enabling:

- **Horizontal scalability** without sticky sessions
- **High availability** and failover resilience
- **Persistence** of sessions across restarts or deployments

With distributed session management, user experience remains consistent and reliable, regardless of how many servers the app runs on or where requests are routed.

## 🚀 Why Redis is a Practical Choice for Session Management

Redis is an in-memory key-value store commonly used for caching and short-lived data. It fits session management well because of its performance characteristics and data model.

- **Fast access**: Since Redis operates in memory, it allows for low-latency read and write operations, which helps maintain responsive user interactions.

- **Simple data structure**: Sessions are stored as key-value pairs, and Redis is built around that model. This makes it easy to store, retrieve, and expire session data by session ID.

- **Built-in expiration**: Redis supports key expiration natively, allowing sessions to expire automatically without manual cleanup.

- **Centralized storage**: All application instances can read and write to the same Redis store. This ensures consistent session state across multiple servers, which is essential in distributed environments.

- **Optional persistence**: Redis can persist session data to disk using snapshots or append-only logs. This helps recover session data after failures if needed.

- **Scalability**: Redis supports clustering and replication, making it suitable for applications that need to scale horizontally or maintain high availability.

Redis offers the features required for session management in distributed systems without introducing complex dependencies.

## Demo

This subproject contains two demos. The first shows how Spring Boot manages sessions in-app memory by default, and the second demonstrates how to manage sessions in a distributed setup using Redis.

### In-App Session Management Demo

When using Spring Boot with Spring Security, session management is enabled by default. This means that after a user logs in, a session is created to keep them authenticated across requests.

By default, these sessions are stored in memory only. If the server is restarted, all session data is lost, and users are effectively logged out.

Spring Boot does offer a way to persist sessions to disk by enabling a configuration flag. This allows sessions to survive application restarts, but it’s not suitable for modern distributed applications.

To enable basic session persistence to disk, you can set:

`server.servlet.session.persistent=false`

#### Running the demo

1.	Start two instances of your Spring Boot application on different ports:

```shell
./gradlew bootRun --args='--server.port=8081'
./gradlew bootRun --args='--server.port=8082'
```

2.	Open localhost:8081 in your browser and log in using the credentials:

```text
Username: user
Password: password
```

After logging in, you’ll notice a session has been created and assigned a session ID.
	
3. Now go to localhost:8082 and log in with the same credentials.

You’ll see that a new, different session ID is created — this instance doesn’t have access to the session from the first instance. Each application manages its own sessions in memory.

4.	Session persistence after restart

To observe session persistence across restarts, use two separate browsers (e.g., Chrome and Safari), and log in to each app in its own browser. This is necessary because the session cookie (JSESSIONID) is shared per domain (localhost) — if you use the same browser, one instance will overwrite the other’s cookie.

When the app shuts down, Tomcat saves session data to disk. It stores each instance’s sessions in separate folders, named using the port they’re running on. These directories look like:

```text
tomcat.8081.xxxxxxxxxxxxx/
tomcat.8082.xxxxxxxxxxxxx/
```

Inside those folders, you’ll find the serialized session data (e.g., SESSIONS.ser), which Tomcat will reload when the instance starts again.

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