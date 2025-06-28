package com.iso8583.simulator.simulator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de una prueba de carga
 */
public class LoadTestResult {

    private String id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalMessages;
    private int successfulMessages;
    private int failedMessages;
    private long averageResponseTime;
    private long minResponseTime;
    private long maxResponseTime;
    private double throughput;
    private List<ErrorSummary> errors;
    private String status;

    public LoadTestResult() {
        this.errors = new ArrayList<>();
        this.status = "PENDING";
    }

    public LoadTestResult(String id) {
        this();
        this.id = id;
        this.startTime = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public int getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(int totalMessages) {
        this.totalMessages = totalMessages;
    }

    public int getSuccessfulMessages() {
        return successfulMessages;
    }

    public void setSuccessfulMessages(int successfulMessages) {
        this.successfulMessages = successfulMessages;
    }

    public int getFailedMessages() {
        return failedMessages;
    }

    public void setFailedMessages(int failedMessages) {
        this.failedMessages = failedMessages;
    }

    public long getAverageResponseTime() {
        return averageResponseTime;
    }

    public void setAverageResponseTime(long averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
    }

    public long getMinResponseTime() {
        return minResponseTime;
    }

    public void setMinResponseTime(long minResponseTime) {
        this.minResponseTime = minResponseTime;
    }

    public long getMaxResponseTime() {
        return maxResponseTime;
    }

    public void setMaxResponseTime(long maxResponseTime) {
        this.maxResponseTime = maxResponseTime;
    }

    public double getThroughput() {
        return throughput;
    }

    public void setThroughput(double throughput) {
        this.throughput = throughput;
    }

    public List<ErrorSummary> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorSummary> errors) {
        this.errors = errors;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Métodos de utilidad
    public void addError(String message, int count) {
        this.errors.add(new ErrorSummary(message, count));
    }

    public long getDurationInMillis() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return 0;
    }

    public double getSuccessRate() {
        if (totalMessages == 0) return 0.0;
        return (double) successfulMessages / totalMessages * 100.0;
    }

    public double getFailureRate() {
        if (totalMessages == 0) return 0.0;
        return (double) failedMessages / totalMessages * 100.0;
    }

    public void complete() {
        this.endTime = LocalDateTime.now();
        this.status = "COMPLETED";

        // Calcular throughput (mensajes por segundo)
        long durationMs = getDurationInMillis();
        if (durationMs > 0) {
            this.throughput = (double) totalMessages / (durationMs / 1000.0);
        }
    }

    public void fail(String reason) {
        this.endTime = LocalDateTime.now();
        this.status = "FAILED";
        addError(reason, 1);
    }

    @Override
    public String toString() {
        return "LoadTestResult{" +
                "id='" + id + '\'' +
                ", totalMessages=" + totalMessages +
                ", successfulMessages=" + successfulMessages +
                ", failedMessages=" + failedMessages +
                ", averageResponseTime=" + averageResponseTime +
                ", throughput=" + throughput +
                ", status='" + status + '\'' +
                '}';
    }

    /**
     * Clase interna para resumen de errores
     */
    public static class ErrorSummary {
        private String message;
        private int count;

        public ErrorSummary() {}

        public ErrorSummary(String message, int count) {
            this.message = message;
            this.count = count;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        @Override
        public String toString() {
            return "ErrorSummary{" +
                    "message='" + message + '\'' +
                    ", count=" + count +
                    '}';
        }
    }
}