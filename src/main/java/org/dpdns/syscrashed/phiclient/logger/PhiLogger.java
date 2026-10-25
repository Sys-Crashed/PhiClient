package org.dpdns.syscrashed.phiclient.logger;

/**
 * Unified logging facade for PhiClient (package {@code org.dpdns.syscrashed.phiclient.logger}).
 *
 * <p>This is a zero-dependency implementation backed by the standard output/error streams.
 * It can later be swapped for a full implementation (e.g. Log4j) without touching any call
 * site, as long as the public method signatures stay intact.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #info(String)} - logs an informational message to stdout.</li>
 *   <li>{@link #warn(String)} - logs a warning (prefixed with {@code [WARN]}) to stdout.</li>
 *   <li>{@link #error(String)} - logs an error (prefixed with {@code [ERROR]}) to stderr.</li>
 *   <li>{@link #error(String, Throwable)} - logs an error together with a stack trace to stderr.</li>
 * </ul>
 */
public final class PhiLogger {

    private static final String PREFIX = "[PhiClient] ";

    private PhiLogger() {
    }

    /**
     * Logs an informational message.
     *
     * @param message the message to log; printed to stdout with the {@code [PhiClient]} prefix.
     */
    public static void info(String message) {
        System.out.println(PREFIX + message);
    }

    /**
     * Logs a warning message.
     *
     * @param message the message to log; prefixed with {@code [WARN]} and printed to stdout.
     */
    public static void warn(String message) {
        System.out.println(PREFIX + "[WARN] " + message);
    }

    /**
     * Logs an error message.
     *
     * @param message the message to log; prefixed with {@code [ERROR]} and printed to stderr.
     */
    public static void error(String message) {
        System.err.println(PREFIX + "[ERROR] " + message);
    }

    /**
     * Logs an error message together with an exception stack trace.
     *
     * @param message   the message describing the error.
     * @param throwable the exception/error whose stack trace should be printed; may be {@code null}.
     */
    public static void error(String message, Throwable throwable) {
        System.err.println(PREFIX + "[ERROR] " + message);
        if (throwable != null) {
            throwable.printStackTrace(System.err);
        }
    }
}
