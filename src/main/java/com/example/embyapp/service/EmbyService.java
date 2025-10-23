package com.example.embyapp.service;

import com.example.emby.EmbyClient.ApiClient;
import com.example.emby.EmbyClient.ApiException;
import com.example.emby.EmbyClient.Java.ItemsServiceApi;
import com.example.emby.EmbyClient.Java.SystemServiceApi;
import com.example.emby.EmbyClient.Java.UserServiceApi;
import com.example.emby.modelEmby.AuthenticationAuthenticationResult;
import com.example.emby.modelEmby.SystemInfo;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
// Import OkHttp 2.x classes
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.OkHttpClient;


import java.io.IOException;
import java.util.UUID;
import java.util.prefs.Preferences;


public class EmbyService {

    private static EmbyService instance;
    private final ApiClient apiClient;
    private AuthenticationAuthenticationResult currentAuthResult;
    private String clientAuthHeader; // Field to store the X-Emby-Authorization header value
    private String currentAccessToken; // Field to store the access token


    // Preferences node for storing session data
    private static final String PREF_NODE_PATH = "/com/example/embyapp";
    private static final String KEY_SERVER_URL = "serverUrl";
    private static final String KEY_ACCESS_TOKEN = "accessToken";
    private static final String KEY_CLIENT_AUTH_HEADER = "clientAuthHeader"; // Key to save the header
    private final Preferences prefs;


    // Cached API service instances
    private UserServiceApi userServiceApi;
    private ItemsServiceApi itemsServiceApi;
    private SystemServiceApi systemServiceApi;


    private final BooleanProperty loggedIn = new SimpleBooleanProperty(false);


    // --- Interceptor to add BOTH X-Emby-Authorization and X-Emby-Token ---
    private class AuthHeaderInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request.Builder builder = originalRequest.newBuilder();


            // 1. Add X-Emby-Authorization header IF we have it
            String localClientAuthHeader = EmbyService.this.clientAuthHeader; // Use local copy
            if (localClientAuthHeader != null && !localClientAuthHeader.isEmpty()) {
                builder.header("X-Emby-Authorization", localClientAuthHeader);
            }

            // 2. Add X-Emby-Token header IF we have an access token stored in EmbyService
            String localAccessToken = EmbyService.this.currentAccessToken; // Use local copy from outer class
            if (localAccessToken != null && !localAccessToken.isEmpty()) {
                // Remove existing X-Emby-Token if ApiKeyAuth interceptor added it (safer)
                builder.removeHeader("X-Emby-Token");
                // Add our own X-Emby-Token header
                builder.header("X-Emby-Token", localAccessToken);
            }


            Request newRequest = builder.build();
            return chain.proceed(newRequest);
        }
    }


    // Private constructor for Singleton
    private EmbyService() {
        // 1. Initialize ApiClient with default constructor
        apiClient = new ApiClient();

        // 2. Get the default OkHttpClient from ApiClient
        OkHttpClient defaultClient = apiClient.getHttpClient();

        // 3. Add our custom interceptor (which handles both headers)
        defaultClient.interceptors().add(new AuthHeaderInterceptor());


        // Configure other ApiClient settings if needed
        // apiClient.setConnectTimeout(30_000);


        prefs = Preferences.userRoot().node(PREF_NODE_PATH);
        System.out.println("Using Preferences node: " + prefs.absolutePath());
    }


    // Get singleton instance
    public static synchronized EmbyService getInstance() {
        if (instance == null) {
            instance = new EmbyService();
        }
        return instance;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public AuthenticationAuthenticationResult getCurrentAuthResult() {
        return currentAuthResult;
    }

    // --- Session Management ---

    // Helper to generate the client auth header string
    private String generateClientAuthHeader() {
        String appName = "EmbyClientJavaFX";
        String appVersion = "1.0.0";
        String deviceName = "MyMacDesktop";
        String deviceId = prefs.get("deviceId", UUID.randomUUID().toString());
        prefs.put("deviceId", deviceId);

        return String.format("Emby Client=\"%s\", Device=\"%s\", DeviceId=\"%s\", Version=\"%s\"",
                appName, deviceName, deviceId, appVersion);
    }


    public void setCurrentAuthResult(AuthenticationAuthenticationResult authResult, String serverUrl) {
        this.currentAuthResult = authResult;
        if (authResult != null && authResult.getAccessToken() != null && serverUrl != null && !serverUrl.isEmpty()) {
            this.clientAuthHeader = generateClientAuthHeader(); // Generate and store header
            this.currentAccessToken = authResult.getAccessToken(); // Store token locally
            apiClient.setBasePath(serverUrl);
            apiClient.setAccessToken(this.currentAccessToken); // Still set it for ApiClient state (if it uses it internally)
            loggedIn.set(true);
            saveSession(serverUrl, this.currentAccessToken, this.clientAuthHeader); // Save header too
        } else {
            apiClient.setAccessToken(null); // Clear token in ApiClient state
            this.currentAccessToken = null; // Clear local token
            loggedIn.set(false);
            this.clientAuthHeader = null; // Clear header value
        }
        // Clear cached API instances as authentication state changed
        this.userServiceApi = null;
        this.itemsServiceApi = null;
        this.systemServiceApi = null;
    }


    public ReadOnlyBooleanProperty loggedInProperty() {
        return loggedIn;
    }


    public boolean isLoggedIn() {
        return loggedIn.get();
    }


    // Method to save session info (including client auth header)
    private void saveSession(String serverUrl, String accessToken, String clientHeader) {
        if (serverUrl != null && !serverUrl.isEmpty() && accessToken != null && !accessToken.isEmpty() && clientHeader != null && !clientHeader.isEmpty()) {
            try {
                System.out.println("Saving session: URL=" + serverUrl + ", Token=" + accessToken.substring(0, Math.min(accessToken.length(), 10)) + "..., Header=" + clientHeader);
                prefs.put(KEY_SERVER_URL, serverUrl);
                prefs.put(KEY_ACCESS_TOKEN, accessToken);
                prefs.put(KEY_CLIENT_AUTH_HEADER, clientHeader);
                prefs.flush();
                System.out.println("Session saved successfully.");
            } catch (Exception e) {
                System.err.println("Error saving session preferences: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Attempted to save session with null or empty URL/Token/Header.");
        }
    }


    // Method to load session info (returns array [url, token, clientHeader] or null)
    private String[] loadSession() {
        String serverUrl = prefs.get(KEY_SERVER_URL, null);
        String accessToken = prefs.get(KEY_ACCESS_TOKEN, null);
        String clientHeader = prefs.get(KEY_CLIENT_AUTH_HEADER, null);


        if (serverUrl != null && !serverUrl.isEmpty() && accessToken != null && !accessToken.isEmpty() && clientHeader != null && !clientHeader.isEmpty()) {
            System.out.println("Loaded session: URL=" + serverUrl + ", Token=" + accessToken.substring(0, Math.min(accessToken.length(), 10)) + "..., Header=" + clientHeader);
            return new String[]{serverUrl, accessToken, clientHeader};
        } else {
            System.out.println("No saved session found or header missing.");
            return null;
        }
    }

    // Method to clear saved session info
    public void clearSession() {
        try {
            System.out.println("Clearing saved session...");
            prefs.remove(KEY_SERVER_URL);
            prefs.remove(KEY_ACCESS_TOKEN);
            prefs.remove(KEY_CLIENT_AUTH_HEADER);
            prefs.flush();
            System.out.println("Session cleared.");
        } catch (Exception e) {
            System.err.println("Error clearing session preferences: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // Method to attempt restoring session on startup
    public boolean tryRestoreSession() {
        String[] sessionData = loadSession();
        if (sessionData != null) {
            String serverUrl = sessionData[0];
            String accessToken = sessionData[1];
            String loadedClientHeader = sessionData[2];

            try {
                System.out.println("Attempting to restore session...");
                // Configure ApiClient state for the check AND for the interceptor
                apiClient.setBasePath(serverUrl);
                apiClient.setAccessToken(accessToken); // Set token state in ApiClient (in case it's used internally)
                this.currentAccessToken = accessToken; // Set local token state for interceptor
                this.clientAuthHeader = loadedClientHeader; // Set header state for interceptor

                // Make a simple API call - interceptor will add both headers now
                SystemInfo systemInfo = getSystemServiceApi().getSystemInfo();


                if (systemInfo != null) {
                    System.out.println("Session restored successfully. Server Version: " + systemInfo.getVersion());
                    // Keep ApiClient state as it is (token and header already set)
                    AuthenticationAuthenticationResult restoredAuth = new AuthenticationAuthenticationResult();
                    restoredAuth.setAccessToken(accessToken); // Minimal auth result for internal state
                    this.currentAuthResult = restoredAuth;
                    loggedIn.set(true);
                    // Clear cached service APIs
                    this.userServiceApi = null;
                    this.itemsServiceApi = null;
                    this.systemServiceApi = null;
                    return true;
                } else {
                    System.err.println("Session restore check returned null SystemInfo.");
                    clearSession();
                }

            } catch (ApiException e) {
                System.err.println("API Exception during session restore (Code: " + e.getCode() + "): " + e.getMessage() + " Body: " + e.getResponseBody());
                if (e.getCode() != 0) {
                    clearSession();
                }
            } catch (Exception e) {
                System.err.println("Non-API Exception during session restore: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (!loggedIn.get()) {
                    // Clear ApiClient state if restore failed
                    apiClient.setAccessToken(null);
                    this.currentAccessToken = null; // Clear local token
                    this.currentAuthResult = null;
                    this.clientAuthHeader = null;
                    System.out.println("Cleared client state after failed restore attempt.");
                }
            }
        }

        System.out.println("Session restore failed or no session found.");
        loggedIn.set(false);
        return false;
    }


    public void logout() {
        System.out.println("Logging out...");
        setCurrentAuthResult(null, null); // Clears current auth, tokens, and clientAuthHeader in ApiClient and service
        clearSession(); // Clears saved session data
    }


    // --- Getters for specific API services (cached) ---

    // Getters remain the same
    public synchronized UserServiceApi getUserServiceApi() {
        if (userServiceApi == null) {
            System.out.println("Creating UserServiceApi");
            userServiceApi = new UserServiceApi(apiClient);
        }
        return userServiceApi;
    }

    public synchronized ItemsServiceApi getItemsServiceApi() {
        if (!isLoggedIn()) {
            System.err.println("Attempted to get ItemsServiceApi while not logged in.");
            return null;
        }
        if (itemsServiceApi == null) {
            System.out.println("Creating ItemsServiceApi");
            itemsServiceApi = new ItemsServiceApi(apiClient);
        }
        return itemsServiceApi;
    }


    public synchronized SystemServiceApi getSystemServiceApi() {
        if (systemServiceApi == null) {
            System.out.println("Creating SystemServiceApi");
            systemServiceApi = new SystemServiceApi(apiClient);
        }
        return systemServiceApi;
    }
}

