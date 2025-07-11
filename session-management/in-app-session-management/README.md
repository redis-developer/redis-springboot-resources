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