package com.example.embyapp.controller;

import com.example.embyapp.MainApp;
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.viewmodel.LoginViewModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField serverUrlField; // Make sure fx:id="serverUrlField"

    @FXML
    private TextField usernameField; // Make sure fx:id="usernameField"

    @FXML
    private PasswordField passwordField; // Make sure fx:id="passwordField"

    @FXML
    private Button loginButton; // Make sure fx:id="loginButton"

    @FXML
    private Label statusLabel; // Make sure fx:id="statusLabel"

    private LoginViewModel viewModel;
    private MainApp mainApp;
    private EmbyService embyService;

    @FXML
    public void initialize() {
        embyService = EmbyService.getInstance();
        viewModel = new LoginViewModel(embyService); // Pass the EmbyService instance

        // Bind UI fields to ViewModel properties
        serverUrlField.textProperty().bindBidirectional(viewModel.serverUrlProperty());
        usernameField.textProperty().bindBidirectional(viewModel.usernameProperty());
        passwordField.textProperty().bindBidirectional(viewModel.passwordProperty());
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());
        loginButton.disableProperty().bind(viewModel.loginInProgressProperty()); // Disable button during login

        // Listen for successful login
        viewModel.loginSuccessProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                // Login was successful, navigate to the main view
                Platform.runLater(() -> {
                    if (mainApp != null) {
                        try {
                            mainApp.showMainView(); // Navigate to main view
                        } catch (Exception e) {
                            e.printStackTrace();
                            viewModel.setStatusMessage("Error navigating to main view.");
                        }
                    }
                });
            }
        });

        // Optional: Pre-fill server URL if you want a default
        // viewModel.setServerUrl("http://localhost:8096");
    }

    @FXML
    private void handleLogin() {
        viewModel.login(); // Call the login method in the ViewModel
    }

    // Method for MainApp to set itself
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }
}

