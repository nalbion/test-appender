package io.github.nalbion;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

/**
 * Allows logging output to be tested.
 * Assumes that the application uses {@link Logger}, which is the default provided by Spring Boot.
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
 * <p>Copied from <a href="https://github.com/nalbion/test-appender">Test Appender</a></p>
 */
public class TestAppender extends AppenderBase<ILoggingEvent> {
    private final List<ILoggingEvent> events = new ArrayList<>();
    private final boolean detachOthers;
    private Level level;
    private int stackDepth = 4;

    public static Predicate<ILoggingEvent> atLogLevel(Level level) {
        return e -> e.getLevel().isGreaterOrEqual(level);
    }

    /**
     * Create TestAppender.
     *
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

    /**
     * Can be used to adjust the level at which logs are captured.
     *
     * @param level - INFO, WARN etc
     */
    public void setLevel(Level level) {
        this.level = level;
    }

    /**
     * Defaults to 4.
     */
    public void setStackDepth(int stackDepth) {
        this.stackDepth = stackDepth;
    }

    /**
     * A short-cut for <code>setLevel(level); start();</code>.
     *
     * @param level - Can be used to adjust the level at which logs are captured
     */
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
     * Asserts that the captured logs are as expected.
     *
     * @param expected - can be a multi-line string with <code>\n</code> or <code>\r\n</code> terminations.
     */
    public void assertLogs(String expected) {
        assertLogsInternal(expected, getLoggedLines());
    }

    /**
     * Can be used to assert what logging output <i>would</i> look like if captured/viewed at that level or higher.
     *
     * @param level - INFO, WARN etc
     * @param expected - can be a multi-line string with <code>\n</code> or <code>\r\n</code> terminations.
     */
    public void assertLogs(Level level, String expected) {
        assertLogsInternal(expected, getLoggedLines(level));
    }

    public void assertLogs(Level level, Function<String, String> mapper, String expected) {
        String[] mappedLogLines = events.stream()
                .filter(atLogLevel(level))
                .map(this::extractFormattedMessage)
                .map(mapper)
                .toArray(String[]::new);
        assertLogsInternal(expected, mappedLogLines);
    }

    public void assertLogs(Function<String, String> mapper, String expected) {
        String[] mappedLogLines = events.stream()
                .map(this::extractFormattedMessage)
                .map(mapper)
                .toArray(String[]::new);
        assertLogsInternal(expected, mappedLogLines);
    }

    public void assertAnyLog(Predicate<ILoggingEvent> p) {
        if (!events.stream().anyMatch(p)) {
            Assertions.assertEquals("<Predicate>",
                    String.join(System.lineSeparator(), getLoggedLines()),
                    MessageFormatter.format("None of the {} log lines matched", events.size()).getMessage());
        }
    }

    public void assertNoLog(Predicate<ILoggingEvent> p) {
        String[] matches = events.stream().filter(p).map(this::extractFormattedMessage).toArray(String[]::new);
        if (matches.length != 0) {
            Assertions.assertEquals("<No match for Predicate>",
                    String.join(System.lineSeparator(), matches),
                    MessageFormatter.format("Found {} matching log line(s)", matches.length).getMessage());
        }
    }

    public void reset() {
        this.events.clear();
    }

    @Override
    protected void append(ILoggingEvent loggingEvent) {
        events.add(loggingEvent);
    }

    private String extractFormattedMessage(ILoggingEvent e) {
        String msg = e.getFormattedMessage();

        if (0 != stackDepth && null != e.getThrowableProxy()) {
            IThrowableProxy ex = e.getThrowableProxy();
            msg += "\n"
                    + ex.getClassName() + ": " + ex.getMessage() + "\n"
                    + Arrays.stream(ex.getStackTraceElementProxyArray())
                    .limit(stackDepth)
                    .map(el -> "    " + el.getSTEAsString())
                    .collect(Collectors.joining("\n"));
        }

        return msg;
    }

    private void assertLogsInternal(String expected, String[] actual) {
        if (!System.lineSeparator().equals("\n")) {
            expected = expected.replace(System.lineSeparator(), "\n");
        }

        Assertions.assertEquals(expected, String.join("\n", actual), "Log assertion failed");
    }

    private String[] getLoggedLines(Level level) {
        return events.stream()
                .filter(atLogLevel(level))
                .map(this::extractFormattedMessage)
                .toArray(String[]::new);
    }

    private String[] getLoggedLines() {
        return events.stream().map(this::extractFormattedMessage).toArray(String[]::new);
    }
}
