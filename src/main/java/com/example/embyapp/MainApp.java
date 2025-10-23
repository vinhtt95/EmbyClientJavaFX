package com.example.embyapp;

import com.example.embyapp.controller.LoginController;
import com.example.embyapp.controller.MainController;
import com.example.embyapp.service.EmbyService; // Import EmbyService
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL; // Import URL

public class MainApp extends Application {

    private Stage primaryStage;
    private EmbyService embyService; // Hold EmbyService instance


    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.embyService = EmbyService.getInstance(); // Get EmbyService instance


        // Try to restore session
        if (embyService.tryRestoreSession()) {
            System.out.println("Session restored, showing main view.");
            showMainView(); // Show main view if session restored
        } else {
            System.out.println("No valid session found, showing login view.");
            showLoginView(); // Show login view otherwise
        }
    }


    public void showLoginView() {
        try {
            // Use getResource() which is more reliable with modules
            URL fxmlUrl = getClass().getResource("LoginView.fxml");
            if (fxmlUrl == null) {
                System.err.println("Cannot find LoginView.fxml!");
                // Handle error appropriately, e.g., show an alert or exit
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();


            LoginController controller = loader.getController();
            controller.setMainApp(this); // Pass MainApp instance


            Scene scene = new Scene(root);
            primaryStage.setTitle("Emby Login");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Error loading LoginView.fxml:");
            e.printStackTrace();
            // Handle error (e.g., show error dialog)
        } catch (IllegalStateException e) {
            System.err.println("Error during FXML loading (check controller/FXML):");
            e.printStackTrace();
        }
    }


    public void showMainView() {
        try {
            URL fxmlUrl = getClass().getResource("MainView.fxml");
            if (fxmlUrl == null) {
                System.err.println("Cannot find MainView.fxml!");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();


            MainController controller = loader.getController();
            controller.setMainApp(this); // Pass MainApp instance for logout


            Scene scene = new Scene(root);
            primaryStage.setTitle("Emby Client");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Error loading MainView.fxml:");
            e.printStackTrace();
            // Handle error
        } catch (IllegalStateException e) {
            System.err.println("Error during FXML loading (check controller/FXML):");
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}

