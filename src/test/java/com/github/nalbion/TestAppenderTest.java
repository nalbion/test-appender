package com.github.nalbion;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TestAppenderTest {
    public static final Logger logger = LoggerFactory.getLogger(TestAppenderTest.class);
    private TestAppender testAppender = new TestAppender(true);

    @BeforeEach
    void setup() {
        testAppender.start(Level.INFO);
    }

    @Test
    void shouldAssertLogs() {
        // When
        logger.info("Hello {}!", "World");
        logger.warn("My application calls log.info() twice.");

        // Then
        testAppender.assertLogs("Hello World!\n"
                + "My application calls log.info() twice.");
        testAppender.assertLogs(Level.WARN, "My application calls log.info() twice.");
// Doc strings supported in JDK 15 and above
//        testAppender.assertLogs("""
//                Hello World!
//                My application calls log.info() twice.""");
    }

    @Test
    void shouldAssertLogsWithLambda() {
        // When
        logger.info("Hello {}!", "World");
        logger.warn("My application calls log.info() twice.");

        // Then
        testAppender.assertLogs(
                TestAppender.atLogLevel(Level.INFO)
                .and(e -> e.getFormattedMessage().matches("Hello .*!"))
        );

        Assertions.assertThrows(Throwable.class, () -> {
            testAppender.assertLogs(
                    TestAppender.atLogLevel(Level.INFO)
                            .and(e -> e.getFormattedMessage().matches("Not logged"))
            );
        });
    }

    @Test
    void shouldStartWithSpecifiedLevel() {
        testAppender.stop();
        testAppender.start(Level.WARN);

        // When
        logger.info("Hello {}!", "World");
        logger.warn("My application calls log.info() twice.");

        // Then
        testAppender.assertLogs("My application calls log.info() twice.");
        testAppender.assertLogs(Level.ERROR, "");
    }

    @Test
    void shouldCreateWithSpecifiedLevel() {
        TestAppender testAppender1 = new TestAppender(true, Level.WARN);
        testAppender1.start();

        // When
        logger.info("Hello {}!", "World");
        logger.warn("My application calls log.info() twice.");

        // Then
        testAppender1.assertLogs("My application calls log.info() twice.");
        testAppender1.assertLogs(Level.WARN, "My application calls log.info() twice.");
    }

    @Test
    void shouldClearLogsBetweenTests() {
        // Given
        logger.info("Has logged previously");

        // When
        testAppender.start();
        logger.info("Logs from another test");

        // Then
        testAppender.assertLogs("Logs from another test");
    }
}
