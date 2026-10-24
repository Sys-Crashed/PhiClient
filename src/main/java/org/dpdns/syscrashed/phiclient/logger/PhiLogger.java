package org.dpdns.syscrashed.phiclient.logger;

/**
 * PhiClient 统一日志器。
 *
 * 当前为零依赖实现（基于标准输出/错误流），后续可无痛替换为 Log4j 等实现，
 * 只要保持方法签名不变，全工程调用点无需改动。
 */
public final class PhiLogger {

    private static final String PREFIX = "[PhiClient] ";

    private PhiLogger() {
    }

    public static void info(String message) {
        System.out.println(PREFIX + message);
    }

    public static void warn(String message) {
        System.out.println(PREFIX + "[WARN] " + message);
    }

    public static void error(String message) {
        System.err.println(PREFIX + "[ERROR] " + message);
    }

    public static void error(String message, Throwable throwable) {
        System.err.println(PREFIX + "[ERROR] " + message);
        if (throwable != null) {
            throwable.printStackTrace(System.err);
        }
    }
}
