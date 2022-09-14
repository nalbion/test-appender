# Test Appender
![Release Pipeline Status](https://github.com/nalbion/test-appender/actions/workflows/maven-publish.yml/badge.svg)
[![Sonartype Lift status](https://lift.sonatype.com/api/badge/github.com/nalbion/test-appender)](https://lift.sonatype.com/results/github.com/nalbion/test-appender)

By taking TDD one step further, and specifying what the logging output should look like before writing the code 
we can expect that the logs will give maintainers clear and useful logs.

Based on Jaroslaw Michalik's article [Don't mock static: test SLF4J Logger with appenders](https://kotlintesting.com/mock-slf4j/).

For JavaScript applications, refer to [log-dd](https://github.com/nalbion/log-dd).

`TestAppender` assumes that the application uses `ch.qos.logback.classic.Logger`, which is the default provided by Spring Boot.

## Installation

This library is available from [Maven Central](https://s01.oss.sonatype.org/) or 
[Git Hub Packages](https://github.com/nalbion/test-appender/packages)

### Gradle
```groovy
dependencies {
    ...
    testRuntimeOnly 'io.github.nalbion:test-appender:1.0.1'
}
```

### Maven
```xml
  <dependencies>
    ...
    <dependency>
      <groupId>io.github.nalbion</groupId>
      <artifactId>test-appender</artifactId>
      <version>1.0.1</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
```

## Usage

```java
...
import io.github.nalbion.TestAppender;
import org.junit.jupiter.api.Test;

class MyTest {
    private final TestAppender testAppender = new TestAppender(true);

    @BeforeEach
    void setup() {
        testAppender.start();
    }

    @Test
    void myTest() {
        // When
        new MyApplication().doSomething();

        // Then
        // Check all logs match a multi-line string
        testAppender.assertLogs("""
                Hello World!
                My application calls log.info() twice.""");

        // Check that only some lines are logged at WARN
        testAppender.assertLogs(Level.WARN, "My application calls log.info() twice.");

        // Check that at least one of the lines matches a Predicate
        testAppender.assertAnyLogs(
                TestAppender.atLogLevel(Level.INFO)
                        .and(e -> e.getFormattedMessage().matches("Hello .*!"))
        );
    }

    @Test
    void dynamicValues() {
        // When
        logger.info("The time is {}", new SimpleDateFormat("HH:mm").format(new Date()));
        
        // Then
        // Handle dynamic values - date/time, random values
        testAppender.assertLogs(line -> line.replaceFirst("\\b\\d{1,2}:\\d{2}\\b", "hh:mm"),
                "The time is hh:mm");     
    }
}
```
