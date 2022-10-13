package io.github.nalbion;

import ch.qos.logback.classic.Level;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Function;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TestAppenderTest {
    public static final Logger logger = LoggerFactory.getLogger(TestAppenderTest.class);
    private final TestAppender testAppender = new TestAppender(true);

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
    void shouldAssertLogsWithPredicate() {
        // When
        logger.info("Hello {}!", "World");
        logger.warn("My application calls log.info() twice.");

        // Then
        testAppender.assertAnyLog(e -> e.getFormattedMessage().matches("Hello .*!"));

        testAppender.assertAnyLog(
                TestAppender.atLogLevel(Level.INFO)
                .and(e -> e.getFormattedMessage().matches("Hello .*!"))
        );

        Throwable ex = Assertions.assertThrows(Throwable.class, () -> {
            testAppender.assertAnyLog(
                    TestAppender.atLogLevel(Level.INFO)
                            .and(e -> e.getFormattedMessage().matches("Not logged"))
            );
        });

        Assertions.assertEquals("None of the 2 log lines matched ==> expected: <<Predicate>> "
                        + "but was: <Hello World!\n"
                        + "My application calls log.info() twice.>",
                ex.getMessage().replace(System.lineSeparator(), "\n"));
    }

    @Test
    void shouldAssertLogsWithNegativePredicate() {
        // When
        logger.info("Hello {}!", "World");
        logger.warn("My application calls log.info() twice.");

        // Then
        testAppender.assertNoLog(e -> e.getFormattedMessage().matches("Hi .*!"));

        testAppender.assertNoLog(
                TestAppender.atLogLevel(Level.INFO)
                        .and(e -> e.getFormattedMessage().matches("Hi .*!"))
        );

        Throwable ex = Assertions.assertThrows(Throwable.class, () -> {
            testAppender.assertNoLog(
                    TestAppender.atLogLevel(Level.INFO)
                            .and(e -> e.getFormattedMessage().matches("Hello .*!"))
            );
        });

        Assertions.assertEquals("Found 1 matching log line(s) ==> expected: <<No match for Predicate>> "
                        + "but was: <Hello World!>", ex.getMessage());
    }

    @Test
    void shouldAssertLogsWithStringMapper() {
        // When
        logger.info("The time is {}", new SimpleDateFormat("HH:mm").format(new Date()));
        logger.warn("User name: {} {}", "John", "Smith");

        Function<String, String> mapper = line ->
                line.replaceFirst("\\b\\d{1,2}:\\d{2}\\b", "hh:mm")
                        .replaceFirst("User name: .*", "User name: <USER NAME>");

        // Then
        testAppender.assertLogs(mapper,
                "The time is hh:mm\n"
                        + "User name: <USER NAME>");

        testAppender.assertLogs(Level.WARN, mapper,
                "User name: <USER NAME>");
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
