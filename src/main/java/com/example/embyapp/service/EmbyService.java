package com.example.embyapp.service;

import com.example.emby.EmbyClient.ApiClient;
import com.example.emby.EmbyClient.ApiException;
import com.example.emby.EmbyClient.Java.ItemsServiceApi;
import com.example.emby.EmbyClient.Java.SystemServiceApi;
import com.example.emby.EmbyClient.Java.UserServiceApi;
import com.example.emby.modelEmby.AuthenticationAuthenticationResult;
import com.example.emby.modelEmby.SystemInfo;
import com.example.emby.modelEmby.UserDto;
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
    private static final String KEY_CLIENT_AUTH_HEADER = "clientAuthHeader";
    private static final String KEY_USER_ID = "userId"; // Key to save the User ID
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
                builder.removeHeader("X-Emby-Token"); // Remove existing (safer)
                builder.header("X-Emby-Token", localAccessToken); // Add our own
            }


            Request newRequest = builder.build();
            return chain.proceed(newRequest);
        }
    }


    // Private constructor for Singleton
    private EmbyService() {
        apiClient = new ApiClient();
        OkHttpClient defaultClient = apiClient.getHttpClient();
        defaultClient.interceptors().add(new AuthHeaderInterceptor());
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

    // Lấy UserID hiện tại (nếu đã đăng nhập)
    public String getCurrentUserId() {
        if (currentAuthResult != null && currentAuthResult.getUser() != null) {
            return currentAuthResult.getUser().getId();
        }
        return null;
    }


    // --- Session Management ---

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
        String userIdToSave = null; // Prepare userId for saving

        if (authResult != null && authResult.getAccessToken() != null && serverUrl != null && !serverUrl.isEmpty()) {
            this.clientAuthHeader = generateClientAuthHeader();
            this.currentAccessToken = authResult.getAccessToken();
            apiClient.setBasePath(serverUrl);
            loggedIn.set(true);

            if (authResult.getUser() != null && authResult.getUser().getId() != null) {
                userIdToSave = authResult.getUser().getId(); // Get userId if available
            } else {
                System.err.println("Warning: UserDto or UserId is null in AuthenticationResult during login.");
            }

            saveSession(serverUrl, this.currentAccessToken, this.clientAuthHeader, userIdToSave); // Save userId too

            // Clear cached API instances AFTER setting auth state
            this.userServiceApi = null;
            this.itemsServiceApi = null;
            this.systemServiceApi = null;
        } else {
            // (Khi logout hoặc login fail)
            this.currentAccessToken = null;
            loggedIn.set(false);
            this.clientAuthHeader = null;
            // Clear cached API instances
            this.userServiceApi = null;
            this.itemsServiceApi = null;
            this.systemServiceApi = null;
        }
    }


    public ReadOnlyBooleanProperty loggedInProperty() {
        return loggedIn;
    }


    public boolean isLoggedIn() {
        return loggedIn.get();
    }


    // Method to save session info (including client auth header and userId)
    private void saveSession(String serverUrl, String accessToken, String clientHeader, String userId) {
        if (serverUrl != null && !serverUrl.isEmpty() && accessToken != null && !accessToken.isEmpty() && clientHeader != null && !clientHeader.isEmpty() && userId != null && !userId.isEmpty()) {
            try {
                System.out.println("Saving session: URL=" + serverUrl + ", Token=" + accessToken.substring(0, Math.min(accessToken.length(), 10)) + "..., Header=" + clientHeader + ", UserID=" + userId);
                prefs.put(KEY_SERVER_URL, serverUrl);
                prefs.put(KEY_ACCESS_TOKEN, accessToken);
                prefs.put(KEY_CLIENT_AUTH_HEADER, clientHeader);
                prefs.put(KEY_USER_ID, userId); // Save User ID
                prefs.flush();
                System.out.println("Session saved successfully.");
            } catch (Exception e) {
                System.err.println("Error saving session preferences: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Attempted to save session with null or empty URL/Token/Header/UserID.");
        }
    }


    // Method to load session info (returns array [url, token, clientHeader, userId] or null)
    private String[] loadSession() {
        String serverUrl = prefs.get(KEY_SERVER_URL, null);
        String accessToken = prefs.get(KEY_ACCESS_TOKEN, null);
        String clientHeader = prefs.get(KEY_CLIENT_AUTH_HEADER, null);
        String userId = prefs.get(KEY_USER_ID, null); // Load User ID


        if (serverUrl != null && !serverUrl.isEmpty() && accessToken != null && !accessToken.isEmpty() && clientHeader != null && !clientHeader.isEmpty() && userId != null && !userId.isEmpty()) {
            System.out.println("Loaded session: URL=" + serverUrl + ", Token=" + accessToken.substring(0, Math.min(accessToken.length(), 10)) + "..., Header=" + clientHeader + ", UserID=" + userId);
            return new String[]{serverUrl, accessToken, clientHeader, userId};
        } else {
            System.out.println("No saved session found or essential info missing (URL/Token/Header/UserID).");
            return null;
        }
    }

    /**
     * SỬA LỖI (Vá lỗi của sếp):
     * Chỉ xóa Token, Header, UserID.
     * KHÔNG XÓA KEY_SERVER_URL, để LoginViewModel có thể load lại.
     */
    public void clearSession() {
        try {
            System.out.println("Clearing session (keeping server URL)...");
            // KHÔNG XÓA SERVER URL: prefs.remove(KEY_SERVER_URL);
            prefs.remove(KEY_ACCESS_TOKEN);
            prefs.remove(KEY_CLIENT_AUTH_HEADER);
            prefs.remove(KEY_USER_ID); // Clear User ID
            prefs.flush();
            System.out.println("Session tokens cleared.");
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
            String loadedUserId = sessionData[3]; // Get the loaded User ID

            // --- Start optimistic setup ---
            apiClient.setBasePath(serverUrl);
            this.currentAccessToken = accessToken;
            this.clientAuthHeader = loadedClientHeader;
            // --- End optimistic setup ---

            try {
                System.out.println("Attempting to restore session (Step 1: System Info)...");
                SystemInfo systemInfo = getSystemServiceApi().getSystemInfo(); // Verify token viability

                if (systemInfo != null) {
                    System.out.println("System Info check OK. (Step 2: Get User Info using ID: " + loadedUserId + ")...");

                    // LƯU Ý: Vẫn dùng logic gốc của project (gọi thẳng UserServiceApi)
                    // Nếu sau này muốn chuyển sang UserRepository thì sửa ở đây.
                    UserDto currentUser = getUserServiceApi().getUsersById(loadedUserId);

                    if (currentUser != null) {
                        System.out.println("User Info check OK. Session restored successfully for User: " + currentUser.getName());

                        // Create the FULL AuthenticationResult
                        AuthenticationAuthenticationResult restoredAuth = new AuthenticationAuthenticationResult();
                        restoredAuth.setAccessToken(accessToken);
                        restoredAuth.setUser(currentUser); // Store the fetched User DTO

                        this.currentAuthResult = restoredAuth;
                        loggedIn.set(true);

                        // Clear cached service APIs AFTER successful restore
                        this.userServiceApi = null;
                        this.itemsServiceApi = null;
                        this.systemServiceApi = null;
                        return true; // SUCCESS!

                    } else {
                        System.err.println("Session restore failed: Could not retrieve User Info (getUsersById returned null for ID: " + loadedUserId + ").");
                        clearSession(); // Clear session if user info fails
                    }
                } else {
                    System.err.println("Session restore failed: Could not retrieve System Info (getSystemInfo returned null).");
                    clearSession(); // Clear session if system info fails
                }

            } catch (ApiException e) {
                System.err.println("API Exception during session restore (Code: " + e.getCode() + "): " + e.getMessage()); // Bỏ e.getResponseBody()
                if (e.getCode() != 0) { // Nếu lỗi không phải là do mất kết nối
                    clearSession(); // Xóa token hỏng/hết hạn
                }
            } catch (Exception e) {
                System.err.println("Non-API Exception during session restore: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (!loggedIn.get()) {
                    // Clear client state ONLY IF restore ultimately failed
                    this.currentAccessToken = null;
                    this.currentAuthResult = null;
                    this.clientAuthHeader = null;
                    System.out.println("Cleared client state after failed restore attempt.");
                }
            }
        }

        System.out.println("Session restore failed or no session found.");
        loggedIn.set(false);
        this.currentAccessToken = null;
        this.currentAuthResult = null;
        this.clientAuthHeader = null;
        return false;
    }


    public void logout() {
        System.out.println("Logging out...");
        setCurrentAuthResult(null, null); // Xóa state
        clearSession(); // Xóa token đã lưu (nhưng giữ lại URL)
    }


    // --- Getters for specific API services (cached) ---

    /**
     * SỬA LỖI (Vá lỗi NPE khi login):
     * Xóa check isLoggedIn()
     * UserServiceApi PHẢI dùng được khi chưa login.
     */
    public synchronized UserServiceApi getUserServiceApi() {
        // if (!isLoggedIn()) { // <<<--- DÒNG NÀY SAI
        //     System.err.println("Attempted to get UserServiceApi while not logged in.");
        //     return null; // <<<--- DÒNG NÀY GÂY LỖI NPE
        // }
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

