package com.example.embyapp;

import com.example.embyapp.controller.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL; // Import URL

public class MainApp extends Application {
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Emby Client");

        showLoginView(); // Line 19
    }

    public void showLoginView() throws IOException {
        FXMLLoader loader = new FXMLLoader();

        // === Ensure this path is correct ===
        // It looks for LoginView.fxml in src/main/resources/com/example/embyapp/
        URL location = MainApp.class.getResource("LoginView.fxml"); // Line 23 (modified slightly for clarity)

        if (location == null) {
            System.err.println("Error: Cannot find LoginView.fxml in src/main/resources/com/example/embyapp/");
            // Optionally, throw an exception or show an error dialog
            throw new IOException("Cannot find LoginView.fxml resource");
        }
        loader.setLocation(location);

        Parent root = loader.load();

        LoginController controller = loader.getController();
        controller.setMainApp(this); // Pass reference to MainApp

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Login to Emby");
        primaryStage.show();
    }


    public void showMainView() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        URL location = MainApp.class.getResource("MainView.fxml"); // Assuming MainView.fxml is in the same location

        if (location == null) {
            System.err.println("Error: Cannot find MainView.fxml in src/main/resources/com/example/embyapp/");
            throw new IOException("Cannot find MainView.fxml resource");
        }
        loader.setLocation(location);
        Parent root = loader.load();

        // Optional: Get controller and pass data if needed
        // MainController controller = loader.getController();
        // controller.setSomeData(...);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Emby Browser"); // Update title for main view
        primaryStage.show();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        // Use Launcher.main(args) if you have issues with modules in some environments
        launch(args);
    }
}

