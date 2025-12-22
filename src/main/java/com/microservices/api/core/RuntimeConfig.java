package main.java.com.microservices.api.core;

public class RuntimeConfig {
    private static String baseUri;
    private static String apiKey;
    private static int timeoutSeconds;

    public static String getBaseUri() {
        return baseUri;
    }

    public static void setBaseUri(String baseUri) {
        RuntimeConfig.baseUri = baseUri;
    }

    public static String getApiKey() {
        return apiKey;
    }

    public static void setApiKey(String apiKey) {
        RuntimeConfig.apiKey = apiKey;
    }

    public static int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public static void setTimeoutSeconds(int timeoutSeconds) {
        RuntimeConfig.timeoutSeconds = timeoutSeconds;
    }
}
