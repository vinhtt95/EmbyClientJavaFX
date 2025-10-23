package com.example.embyapp.viewmodel;

import com.example.emby.EmbyClient.ApiException;
import com.example.emby.EmbyClient.Java.UserServiceApi;
import com.example.emby.modelEmby.AuthenticateUserByName;
import com.example.emby.modelEmby.AuthenticationAuthenticationResult;
import com.example.embyapp.service.EmbyService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;

import java.io.IOException;
import java.util.UUID;


public class LoginViewModel {

    private final StringProperty serverUrl = new SimpleStringProperty("");
    private final StringProperty username = new SimpleStringProperty("");
    private final StringProperty password = new SimpleStringProperty("");
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final BooleanProperty loginInProgress = new SimpleBooleanProperty(false);
    private final BooleanProperty loginSuccess = new SimpleBooleanProperty(false); // Added for signalling success


    private final EmbyService embyService;

    // Constructor accepting EmbyService
    public LoginViewModel(EmbyService embyService) {
        this.embyService = embyService;
    }


    // --- Property Getters ---
    public StringProperty serverUrlProperty() { return serverUrl; }
    public StringProperty usernameProperty() { return username; }
    public StringProperty passwordProperty() { return password; }
    public StringProperty statusMessageProperty() { return statusMessage; }
    public BooleanProperty loginInProgressProperty() { return loginInProgress; }
    public BooleanProperty loginSuccessProperty() { return loginSuccess; } // Getter for success signal


    // --- Login Action ---
    public void login() {
        if (loginInProgress.get()) {
            return; // Prevent multiple login attempts
        }

        String url = serverUrl.get().trim();
        String user = username.get().trim();
        String pass = password.get(); // Password typically doesn't need trimming


        // Basic validation
        if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            statusMessage.set("Please enter Server URL, Username, and Password.");
            return;
        }

        // --- Prepare for background task ---
        loginInProgress.set(true);
        statusMessage.set("Logging in...");
        loginSuccess.set(false); // Reset success flag


        // --- Create and run background task ---
        Task<AuthenticationAuthenticationResult> loginTask = new Task<>() {
            @Override
            protected AuthenticationAuthenticationResult call() throws Exception {
                // Configure EmbyService base path (only if different)
                if (!url.equals(embyService.getApiClient().getBasePath())) {
                    embyService.getApiClient().setBasePath(url);
                }

                // Prepare authentication request
                AuthenticateUserByName authRequest = new AuthenticateUserByName();
                authRequest.setUsername(user);
                authRequest.setPw(pass);


                // Prepare authentication header (X-Emby-Authorization)
                // Use more specific/dynamic values if possible
                String clientName = "EmbyClientJavaFX";
                String deviceName = "MyMacDesktop"; // Or get dynamically System.getProperty("os.name")
                String deviceId = UUID.randomUUID().toString(); // Generate unique ID
                String appVersion = "1.0.0"; // Your app version

                String clientAuthHeader = String.format(
                        "Emby Client=\"%s\", Device=\"%s\", DeviceId=\"%s\", Version=\"%s\"",
                        clientName, deviceName, deviceId, appVersion
                );

                // Get UserServiceApi instance from EmbyService
                UserServiceApi userService = embyService.getUserServiceApi();


                // Call the API
                return userService.postUsersAuthenticatebyname(authRequest, clientAuthHeader);
            }
        };

        // --- Handle Task Completion (on JavaFX Application Thread) ---
        loginTask.setOnSucceeded(event -> {
            AuthenticationAuthenticationResult authResult = loginTask.getValue();
            if (authResult != null && authResult.getAccessToken() != null) {
                statusMessage.set("Login Successful!");
                // *** FIX: Pass serverUrl as the second argument ***
                embyService.setCurrentAuthResult(authResult, url); // Store result and URL in service
                loginSuccess.set(true); // Signal success
            } else {
                // Should not happen if API call succeeded without exception but handle defensively
                statusMessage.set("Login failed: Invalid response from server.");
                embyService.setCurrentAuthResult(null, null); // Clear any previous auth state
            }
            loginInProgress.set(false);
        });

        loginTask.setOnFailed(event -> {
            Throwable exception = loginTask.getException();
            String errorMessage = "Login failed: ";
            if (exception instanceof ApiException) {
                ApiException apiEx = (ApiException) exception;
                errorMessage += "API Error " + apiEx.getCode();
                System.err.println("API Exception during login: " + apiEx.getCode());
                System.err.println("Response body: " + apiEx.getResponseBody());
                apiEx.printStackTrace();
                // Provide more specific user feedback based on code
                if (apiEx.getCode() == 401 || apiEx.getCode() == 403) {
                    errorMessage = "Login failed: Invalid username or password.";
                } else if (apiEx.getCode() == 400) {
                    errorMessage = "Login failed: Bad request (check server URL or client header).";
                } else if (apiEx.getCode() == 0) {
                    errorMessage = "Login failed: Could not connect to the server (check URL and network).";
                } else {
                    errorMessage = "Login failed: Server returned error " + apiEx.getCode() + ".";
                }


            } else if (exception instanceof IOException) {
                errorMessage += "Network error. Check server URL and connection.";
                exception.printStackTrace();
            }
            else {
                errorMessage += "An unexpected error occurred.";
                exception.printStackTrace();
            }
            statusMessage.set(errorMessage);
            embyService.setCurrentAuthResult(null, null); // Clear any previous auth state
            loginInProgress.set(false);
        });

        // Start the background task
        new Thread(loginTask).start();
    }
}

