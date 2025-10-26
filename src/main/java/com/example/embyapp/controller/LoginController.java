package com.example.embyapp.controller;

import com.example.embyapp.MainApp;
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.I18nManager; // <-- IMPORT
import com.example.embyapp.viewmodel.LoginViewModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;


public class LoginController {

    @FXML private VBox rootPane; // Added root pane reference if needed
    @FXML private Label titleLabel; // <-- ADDED
    @FXML private TextField serverUrlField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;

    private LoginViewModel viewModel;
    private MainApp mainApp;
    private EmbyService embyService; // Add EmbyService


    // Called by FXMLLoader after injecting fields
    public void initialize() {
        // Initialize EmbyService
        embyService = EmbyService.getInstance();


        // Initialize ViewModel and pass EmbyService
        viewModel = new LoginViewModel(embyService);

        // --- Setup Localization ---
        setupLocalization(); // <-- CALL NEW METHOD


        // --- Bind UI components to ViewModel properties ---
        serverUrlField.textProperty().bindBidirectional(viewModel.serverUrlProperty());
        usernameField.textProperty().bindBidirectional(viewModel.usernameProperty());
        passwordField.textProperty().bindBidirectional(viewModel.passwordProperty());
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());


        // Disable login button while login is in progress
        loginButton.disableProperty().bind(viewModel.loginInProgressProperty());

        // Change button text while logging in (optional)
        loginButton.textProperty().bind(
                Bindings.when(viewModel.loginInProgressProperty())
                        .then(I18nManager.getInstance().getString("loginView", "loginInProgress")) // <-- UPDATE
                        .otherwise(I18nManager.getInstance().getString("loginView", "loginButton")) // <-- UPDATE
        );

        // --- Listen for login success ---
        viewModel.loginSuccessProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                // Run on JavaFX Application Thread
                Platform.runLater(() -> {
                    if (mainApp != null) {
                        mainApp.showMainView(); // Switch to main view on success
                    } else {
                        System.err.println("MainApp reference is null in LoginController!");
                        // Optionally update status message here IF viewModel doesn't handle it
                        // viewModel.statusMessageProperty().set("Error: Cannot switch view."); // Example
                    }
                });
            }
        });


    }

    // <-- ADD THIS NEW METHOD -->
    private void setupLocalization() {
        I18nManager i18n = I18nManager.getInstance();
        titleLabel.setText(i18n.getString("loginView", "title"));
        serverUrlField.setPromptText(i18n.getString("loginView", "serverUrlPrompt"));
        usernameField.setPromptText(i18n.getString("loginView", "usernamePrompt"));
        passwordField.setPromptText(i18n.getString("loginView", "passwordPrompt"));
        // loginButton text is handled by binding
    }

    // Method to set the MainApp instance (called from MainApp)
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }


    // --- Event Handlers ---
    @FXML
    private void handleLoginButtonAction() {
        viewModel.login(); // Delegate login action to ViewModel
    }

    // Optional: Handle Enter key press on password field
    @FXML
    private void handlePasswordKeyPress() {
        // This assumes you added onAction="#handlePasswordKeyPress" to the PasswordField in FXML
        if (passwordField.getText() != null && !passwordField.getText().isEmpty()) {
            viewModel.login();
        }
    }
}