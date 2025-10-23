package com.example.embyapp.viewmodel;

import com.example.emby.EmbyClient.ApiException;
import com.example.emby.EmbyClient.Java.UserServiceApi;
import com.example.emby.modelEmby.AuthenticateUserByName;
import com.example.emby.modelEmby.AuthenticationAuthenticationResult;
import com.example.embyapp.service.EmbyService;
import javafx.application.Platform;
import javafx.beans.property.*;

public class LoginViewModel {

    private final StringProperty serverUrl = new SimpleStringProperty("");
    private final StringProperty username = new SimpleStringProperty("");
    private final StringProperty password = new SimpleStringProperty("");
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final BooleanProperty loginInProgress = new SimpleBooleanProperty(false);
    private final BooleanProperty loginSuccess = new SimpleBooleanProperty(false); // Track login success

    private final EmbyService embyService; // Store the EmbyService instance

    // Constructor to receive EmbyService
    public LoginViewModel(EmbyService embyService) {
        this.embyService = embyService; // Receive and store EmbyService
        // Optional: Pre-fill server URL if you want a default from EmbyService or config
        this.serverUrl.set("http://localhost:8096"); // Example default
    }


    public StringProperty serverUrlProperty() {
        return serverUrl;
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public StringProperty passwordProperty() {
        return password;
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public BooleanProperty loginInProgressProperty() {
        return loginInProgress;
    }

    public BooleanProperty loginSuccessProperty() { return loginSuccess; }

    // Optional setter if needed, e.g., for pre-filling
    public void setServerUrl(String url) {
        this.serverUrl.set(url);
    }

    // Optional setter if needed
    public void setStatusMessage(String message) {
        Platform.runLater(() -> statusMessage.set(message));
    }


    public void login() {
        if (serverUrl.get() == null || serverUrl.get().trim().isEmpty() ||
                username.get() == null || username.get().trim().isEmpty() ||
                password.get() == null || password.get().isEmpty()) { // Password can be empty but not null
            setStatusMessage("Please enter Server URL, Username, and Password.");
            return;
        }

        loginInProgress.set(true);
        loginSuccess.set(false); // Reset success status
        setStatusMessage("Logging in...");

        // Run network operation in background thread
        new Thread(() -> {
            try {
                // Configure ApiClient base path
                embyService.getApiClient().setBasePath(serverUrl.get().trim()); // Trim whitespace

                // Prepare authentication request
                AuthenticateUserByName authRequest = new AuthenticateUserByName();
                authRequest.setUsername(username.get().trim()); // Trim whitespace
                authRequest.setPw(password.get()); // Password usually doesn't need trimming

                // Get UserServiceApi from EmbyService
                UserServiceApi userService = embyService.getUserServiceApi(); // Use the stored EmbyService

                // --- MODIFIED HEADER CONSTRUCTION ---
                // Construct the X-Emby-Authorization header string with the "Emby " prefix.
                // Format: Emby Client="AppName", Device="DeviceName", DeviceId="UniqueDeviceID", Version="AppVersion"
                // You should replace these placeholder values.
                String appName = "EmbyClientJavaFX"; // Your App Name
                String deviceName = "MyMacDesktop";     // Device Name
                String deviceId = java.util.UUID.randomUUID().toString(); // Generate a unique ID
                String appVersion = "1.0.0";             // Your App Version

                // Note the "Emby " prefix added here
                String clientAuthHeader = String.format("Emby Client=\"%s\", Device=\"%s\", DeviceId=\"%s\", Version=\"%s\"",
                        appName,
                        deviceName,
                        deviceId,
                        appVersion
                );
                // --- END MODIFIED HEADER CONSTRUCTION ---


                // Call authentication API, passing the constructed header string
                AuthenticationAuthenticationResult authResult = userService.postUsersAuthenticatebyname(authRequest, clientAuthHeader);


                // Update EmbyService with the result
                embyService.setCurrentAuthResult(authResult);

                // Update UI on JavaFX Application Thread
                Platform.runLater(() -> {
                    setStatusMessage("Login successful!");
                    loginSuccess.set(true); // Signal success
                    loginInProgress.set(false);
                });

            } catch (ApiException e) {
                // Handle API exceptions (e.g., incorrect credentials, server not found)
                System.err.println("API Exception during login: " + e.getCode());
                System.err.println("Response body: " + e.getResponseBody());
                e.printStackTrace(); // Log the full stack trace for debugging
                Platform.runLater(() -> {
                    String errorMessage = "Login failed: ";
                    if (e.getCode() == 401 || e.getCode() == 403) {
                        errorMessage += "Invalid username or password.";
                    } else if (e.getCode() == 400) {
                        // More specific message for Bad Request potentially due to header
                        errorMessage += "Bad Request. Check client identification header or parameters. (Details: " + e.getResponseBody() + ")";
                    } else if (e.getCode() == 0) {
                        errorMessage += "Could not connect to server or server address is incorrect.";
                    }
                    else {
                        errorMessage += e.getMessage() + " (Code: " + e.getCode() + ")";
                    }
                    setStatusMessage(errorMessage);
                    loginInProgress.set(false);
                });
            } catch (Exception e) {
                // Handle other potential exceptions (network issues, etc.)
                e.printStackTrace(); // Log the full stack trace for debugging
                Platform.runLater(() -> {
                    setStatusMessage("Login failed: An unexpected error occurred. " + e.getMessage());
                    loginInProgress.set(false);
                });
            }
        }).start();
    }
}

