# springboot-classchecker

A tool to find incompatible classes between spring boot 1 and spring boot 2, help upgrade from spring boot 1 to spring boot 2.

```bash
mvn clean package
java -jar target/classchecker-0.0.1-SNAPSHOT.jar demo-springboot1-starter.jar
```

Output:

```
path: demo-springboot1-starter.jar
org.springframework.boot.actuate.autoconfigure.ConditionalOnEnabledHealthIndicator
org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration
org.springframework.boot.actuate.autoconfigure.HealthIndicatorAutoConfiguration
```

Args can be jar file or directory, such as:

```
java -jar target/classchecker-0.0.1-SNAPSHOT.jar target/classes aaa.jar bbb.jar
```