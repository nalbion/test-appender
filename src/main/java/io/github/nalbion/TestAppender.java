package io.github.nalbion;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Assertions;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

/**
 * Allows logging output to be tested. Assumes that the application uses {@link Logger},
 * which is the default provided by Spring Boot.
 *
 * <pre>
 * private TestAppender testAppender = new TestAppender(true);
 *
 * &#64;BeforeEach
 * void setup() {
 *     testAppender.start();
 * }
 *
 * &#64;Test
 * void myTest() {
 *     // When
 *     new MyApplication().doSomething();
 *
 *     // Then
 *     testAppender.assertLogs("""
 *             Hello World!
 *             My application call log.info() twice.""");
 * }
 * </pre>
 *
 * Based on <a href="https://kotlintesting.com/mock-slf4j/">Jaroslaw Michalik's article</a>
 */
public class TestAppender extends AppenderBase<ILoggingEvent> {
    private final List<ILoggingEvent> events = new ArrayList<>();
    private boolean detachOthers;
    private Level level;

    public static Predicate<ILoggingEvent> atLogLevel(Level level) {
        return (e) -> e.getLevel().isGreaterOrEqual(level);
    }

    /**
     * @param detachOthers If true, will disable all other loggers for the duration of the test.
     *                     This can make the test logs a lot less noisy.
     */
    public TestAppender(boolean detachOthers) {
        this.detachOthers = detachOthers;
    }

    public TestAppender(boolean detachOthers, Level level) {
        this.detachOthers = detachOthers;
        this.level = level;
    }

    /** Can be used to adjust the level at which logs are captured */
    public void setLevel(Level level) {
        this.level = level;
    }

    /** A short-cut for <code>setLevel(level); start();</code> */
    public void start(Level level) {
        setLevel(level);
        start();
    }

    @Override
    public void start() {
        if (!started) {
            Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            if (detachOthers) {
                logger.detachAndStopAllAppenders();
            }
            logger.addAppender(this);
            if (null != level) {
                logger.setLevel(level);
            }
        }
        reset();
        super.start();
    }

    /**
     * @param expected - can be a multi-line string with <code>\n</code> or <code>\r\n</code> terminations.
     */
    public void assertLogs(String expected) {
        assertLogs(expected, getLoggedLines());
    }

    /**
     * @param level - can be used to assert what logging output <i>would</i> look like if captured/viewed at that level or higher
     */
    public void assertLogs(Level level, String expected) {
        assertLogs(expected, getLoggedLines(level));
    }

    public void assertLogs(Predicate<ILoggingEvent> p) {
        if (!events.stream().anyMatch(p)) {
            Assertions.assertEquals("<Predicate>",
                    String.join(System.lineSeparator(), getLoggedLines()),
                    MessageFormatter.format("None of the {} log lines matched", events.size()).getMessage());
        }
    }

    public void reset() {
        this.events.clear();
    }

    @Override
    protected void append(ILoggingEvent loggingEvent) {
        loggingEvent.getLevel();
        loggingEvent.getFormattedMessage();
        events.add(loggingEvent);
    }

    private void assertLogs(String expected, String[] actual) {
        if (!System.lineSeparator().equals("\n")) {
            expected = expected.replace(System.lineSeparator(), "\n");
        }

        Assertions.assertEquals(expected, String.join("\n", actual), "Log assertion failed");
    }

    private String[] getLoggedLines(Level level) {
        return events.stream()
                .filter(atLogLevel(level))
                .map(e -> e.getFormattedMessage())
                .toArray(String[]::new);
    }

    private String[] getLoggedLines() {
        return events.stream().map(e -> e.getFormattedMessage()).toArray(String[]::new);
    }
}
