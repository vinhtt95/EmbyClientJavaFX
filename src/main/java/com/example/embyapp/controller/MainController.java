package com.example.embyapp.controller;

import com.example.embyapp.MainApp;
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.viewmodel.MainViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Button; // Added for logout button

import java.io.IOException;

public class MainController {

    @FXML
    private Label welcomeLabel; // Ensure fx:id="welcomeLabel" exists in FXML

    @FXML // <<<=== ENSURE @FXML IS PRESENT
    private ListView<String> libraryListView; // Ensure fx:id="libraryListView" exists in FXML and matches type

    @FXML // Added for logout button
    private Button logoutButton; // Ensure fx:id="logoutButton" exists in FXML

    private MainViewModel viewModel;
    private EmbyService embyService;
    private MainApp mainApp; // To switch back to login view

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }


    @FXML
    public void initialize() {
        // Get EmbyService instance (assuming Singleton pattern)
        this.embyService = EmbyService.getInstance(); // Use Singleton instance

        // Create ViewModel, passing the service
        this.viewModel = new MainViewModel(embyService); // Pass service to ViewModel constructor

        // Bind UI elements to ViewModel properties
        if (welcomeLabel != null) {
            welcomeLabel.textProperty().bind(viewModel.welcomeMessageProperty());
        } else {
            System.err.println("WARN: welcomeLabel is null in MainController.initialize()");
        }


        // IMPORTANT: Check if libraryListView is injected before using it
        if (libraryListView != null) {
            libraryListView.setItems(viewModel.getLibraryItems()); // Bind ListView items
        } else {
            System.err.println("ERROR: libraryListView is null in MainController.initialize(). Check @FXML and fx:id.");
            // Optionally, throw an exception or handle this error appropriately
        }


        // Load initial data
        viewModel.loadUserData();
        viewModel.loadLibraryData();

        // Setup logout button action if it exists
        if (logoutButton != null) {
            logoutButton.setOnAction(event -> handleLogout());
        } else {
            System.err.println("WARN: logoutButton is null in MainController.initialize()");
        }
    }

    private void handleLogout() {
        if (embyService != null) {
            embyService.logout();
        }
        if (mainApp != null) {
            try {
                mainApp.showLoginView(); // Go back to login screen
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

