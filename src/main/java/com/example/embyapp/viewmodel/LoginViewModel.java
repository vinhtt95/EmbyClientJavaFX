package com.example.embyapp.viewmodel;

import embyclient.ApiException;
import embyclient.api.UserServiceApi;
import embyclient.model.AuthenticateUserByName;
import embyclient.model.AuthenticationAuthenticationResult;
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.I18nManager; // <-- IMPORT
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;

import java.io.IOException;
import java.util.UUID;
import java.util.prefs.Preferences;


public class LoginViewModel {

    private final StringProperty serverUrl = new SimpleStringProperty(loadLastServerUrl());

    // Sửa luôn cả 2 dòng này để xóa giá trị hardcode (nếu sếp muốn)
    private final StringProperty username = new SimpleStringProperty("admin");
    private final StringProperty password = new SimpleStringProperty("123@123a");
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final BooleanProperty loginInProgress = new SimpleBooleanProperty(false);
    private final BooleanProperty loginSuccess = new SimpleBooleanProperty(false); // Added for signalling success

    // Thêm 2 hằng số này (copy từ EmbyService, vì nó là private)
    private static final String PREF_NODE_PATH = "/com/example/embyapp";
    private static final String KEY_SERVER_URL = "serverUrl";

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
        I18nManager i18n = I18nManager.getInstance(); // <-- Get I18n


        // Basic validation
        if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            statusMessage.set(i18n.getString("loginViewModel", "errorEmptyFields")); // <-- UPDATE
            return;
        }

        // --- Prepare for background task ---
        loginInProgress.set(true);
        statusMessage.set(i18n.getString("loginViewModel", "statusLoggingIn")); // <-- UPDATE
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
                statusMessage.set(i18n.getString("loginViewModel", "statusSuccess")); // <-- UPDATE
                // *** FIX: Pass serverUrl as the second argument ***
                embyService.setCurrentAuthResult(authResult, url); // Store result and URL in service
                loginSuccess.set(true); // Signal success
            } else {
                // Should not happen if API call succeeded without exception but handle defensively
                statusMessage.set(i18n.getString("loginViewModel", "errorInvalidResponse")); // <-- UPDATE
                embyService.setCurrentAuthResult(null, null); // Clear any previous auth state
            }
            loginInProgress.set(false);
        });

        loginTask.setOnFailed(event -> {
            Throwable exception = loginTask.getException();
            String errorMessage; // <-- Remove default "Login failed: "
            if (exception instanceof ApiException) {
                ApiException apiEx = (ApiException) exception;
                errorMessage = i18n.getString("loginViewModel", "errorApi", apiEx.getCode()); // <-- UPDATE
                System.err.println("API Exception during login: " + apiEx.getCode());
                System.err.println("Response body: " + apiEx.getResponseBody());
                apiEx.printStackTrace();
                // Provide more specific user feedback based on code
                if (apiEx.getCode() == 401 || apiEx.getCode() == 403) {
                    errorMessage = i18n.getString("loginViewModel", "errorUnauthorized"); // <-- UPDATE
                } else if (apiEx.getCode() == 400) {
                    errorMessage = i18n.getString("loginViewModel", "errorBadRequest"); // <-- UPDATE
                } else if (apiEx.getCode() == 0) {
                    errorMessage = i18n.getString("loginViewModel", "errorConnection"); // <-- UPDATE
                } else {
                    errorMessage = i18n.getString("loginViewModel", "errorApiGeneric", apiEx.getCode()); // <-- UPDATE
                }


            } else if (exception instanceof IOException) {
                errorMessage = i18n.getString("loginViewModel", "errorNetwork"); // <-- UPDATE
                exception.printStackTrace();
            }
            else {
                errorMessage = i18n.getString("loginViewModel", "errorUnexpected"); // <-- UPDATE
                exception.printStackTrace();
            }
            statusMessage.set(errorMessage);
            embyService.setCurrentAuthResult(null, null); // Clear any previous auth state
            loginInProgress.set(false);
        });

        // Start the background task
        new Thread(loginTask).start();
    }

    // Thêm hàm helper này vào cuối class:
    private String loadLastServerUrl() {
        try {
            Preferences prefs = Preferences.userRoot().node(PREF_NODE_PATH);
            return prefs.get(KEY_SERVER_URL, ""); // Lấy URL đã lưu, hoặc "" nếu không có
        } catch (Exception e) {
            System.err.println("Error loading last server URL from preferences: " + e.getMessage());
            return "";
        }
    }
}