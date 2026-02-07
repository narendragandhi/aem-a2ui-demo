package com.example.aema2ui.config;

import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.io.IOException;
import java.util.Map;

/**
 * Global exception handler for consistent error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbort(ClientAbortException ex) {
        // Client disconnected - this is expected behavior, log at debug level
        logger.debug("Client disconnected: {}", ex.getMessage());
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void handleAsyncTimeout(AsyncRequestTimeoutException ex) {
        // SSE/async request timed out - this is expected behavior for long-running streams
        logger.debug("Async request timed out (SSE connection closed): {}", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(IllegalArgumentException ex) {
        logger.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation Error",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(Exception ex) {
        // Check if this is a client disconnection
        if (isClientDisconnection(ex)) {
            logger.debug("Client disconnected: {}", ex.getMessage());
            return null; // No response needed, client is gone
        }

        logger.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal Server Error",
                "message", "An unexpected error occurred"
        ));
    }

    /**
     * Check if the exception indicates a client disconnection.
     */
    private boolean isClientDisconnection(Throwable e) {
        Throwable current = e;
        while (current != null) {
            String className = current.getClass().getName();
            String message = current.getMessage();

            if (className.contains("ClientAbortException")) {
                return true;
            }

            if (current instanceof IOException && message != null) {
                if (message.contains("Broken pipe") ||
                    message.contains("Connection reset") ||
                    message.contains("Stream closed")) {
                    return true;
                }
            }

            current = current.getCause();
        }
        return false;
    }
}
