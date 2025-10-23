package com.example.embyapp.service;

import com.example.emby.EmbyClient.ApiClient;
import com.example.emby.EmbyClient.Java.ItemsServiceApi; // Import ItemsServiceApi
import com.example.emby.EmbyClient.Java.UserServiceApi;
import com.example.emby.modelEmby.AuthenticationAuthenticationResult;

/**
 * Singleton service to manage the Emby API client and authentication state.
 */
public class EmbyService {

    private static EmbyService instance;
    private final ApiClient apiClient;
    private AuthenticationAuthenticationResult currentAuthResult;
    private UserServiceApi userServiceApi; // Cache the UserServiceApi instance
    private ItemsServiceApi itemsServiceApi; // Cache the ItemsServiceApi instance

    private EmbyService() {
        apiClient = new ApiClient();
        // Configure ApiClient defaults if needed (e.g., timeouts)
        // apiClient.setConnectTimeout(60_000); // Example: 60 seconds
    }

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

    public void setCurrentAuthResult(AuthenticationAuthenticationResult authResult) {
        this.currentAuthResult = authResult;
        // Optionally, update ApiClient with authentication token if needed for subsequent requests
        if (authResult != null && authResult.getAccessToken() != null) {
            // NOTE: The SDK might not have a direct setAccessToken method.
            // Authentication might need to be handled via interceptors or headers.
            // For now, we just store the result.
            // You might need to add an interceptor to ApiClient to add the
            // "X-Emby-Token" header for authenticated requests.
            System.out.println("Authentication successful. Token: " + authResult.getAccessToken());
            System.out.println("User ID: " + authResult.getUser().getId());
        } else {
            System.out.println("Authentication result cleared or invalid.");
        }
        // Clear cached Api instances if authentication changes
        this.userServiceApi = null;
        this.itemsServiceApi = null; // Clear items service cache
        // Clear other cached Api instances here...
    }

    // --- Getter for UserServiceApi ---
    /**
     * Gets or creates an instance of UserServiceApi.
     * @return The UserServiceApi instance.
     */
    public UserServiceApi getUserServiceApi() {
        if (this.userServiceApi == null) {
            this.userServiceApi = new UserServiceApi(this.apiClient);
        }
        return this.userServiceApi;
    }

    // --- Added Getter for ItemsServiceApi ---
    /**
     * Gets or creates an instance of ItemsServiceApi.
     * @return The ItemsServiceApi instance.
     */
    public ItemsServiceApi getItemsServiceApi() {
        if (this.itemsServiceApi == null) {
            this.itemsServiceApi = new ItemsServiceApi(this.apiClient);
        }
        return this.itemsServiceApi;
    }


    // --- Add similar getter methods for other Api classes as needed ---
     /*
     public LibraryServiceApi getLibraryServiceApi() {
         // Cache and return LibraryServiceApi instance
     }
     */

    public void logout() {
        setCurrentAuthResult(null);
        // Add any other logout logic (e.g., clear tokens in ApiClient if applicable)
        System.out.println("User logged out.");
    }
}

