package app.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExceptionLogger {
    private static final Logger prodLogger = LoggerFactory.getLogger("production");
    private static final Logger debugLogger = LoggerFactory.getLogger("debug");

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void logError(String errorMSG, Exception e) {
        LocalDateTime timestamp = LocalDateTime.now();
        String formatted = timestamp.format(FORMATTER);

        Throwable root = e;
        while (root.getCause() != null && root != root.getCause()) {
            root = root.getCause();
        }

        prodLogger.error("[{}] Database error: {} | Root cause: {} - {}",
                errorMSG, formatted, e.getClass().getSimpleName(), root.getMessage());

        // Fuld trace til debug
        debugLogger.debug("Full stack trace: ", e);
    }
}
