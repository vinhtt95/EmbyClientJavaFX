package com.example.embyapp;

import com.example.embyapp.controller.LoginController;
import com.example.embyapp.controller.MainController;
import com.example.embyapp.service.EmbyService; // Import EmbyService
import com.example.embyapp.service.I18nManager; // <-- IMPORT
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL; // Import URL
import java.util.prefs.Preferences; // (CẬP NHẬT) Thêm import

public class MainApp extends Application {

    private Stage primaryStage;
    private EmbyService embyService; // Hold EmbyService instance
    private I18nManager i18n; // <-- ADDED

    // (CẬP NHẬT) Dùng chung một đường dẫn Preferences
    private static final String PREFS_NODE_PATH = "/com/example/embyapp";


    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.embyService = EmbyService.getInstance(); // Get EmbyService instance
        this.i18n = I18nManager.getInstance(); // <-- ADDED: Initialize I18nManager

        // (CẬP NHẬT) Tải và áp dụng kích thước/vị trí cửa sổ đã lưu
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE_PATH);
        primaryStage.setX(prefs.getDouble("windowX", 100)); // Default 100
        primaryStage.setY(prefs.getDouble("windowY", 100)); // Default 100
        // (Kích thước sẽ được set trong showMainView)

        // (CẬP NHẬT) Thêm listener để lưu kích thước/vị trí cửa sổ khi đóng
        primaryStage.setOnCloseRequest(e -> {
            prefs.putDouble("windowX", primaryStage.getX());
            prefs.putDouble("windowY", primaryStage.getY());
            prefs.putDouble("windowWidth", primaryStage.getWidth());
            prefs.putDouble("windowHeight", primaryStage.getHeight());
            System.out.println("Đã lưu vị trí và kích thước cửa sổ.");
        });

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


            Scene scene = new Scene(root); // Để kích thước login view tự động

            // (CẬP NHẬT) Thêm CSS cho LoginView
            try {
                URL cssUrl = getClass().getResource("styles.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    System.err.println("Không tìm thấy styles.css cho LoginView.");
                }
            } catch (NullPointerException e) {
                System.err.println("Lỗi khi tải styles.css: " + e.getMessage());
            }

            primaryStage.setTitle(i18n.getString("mainApp", "loginTitle")); // <-- MODIFIED
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) { // Bắt Exception chung
            System.err.println("Error loading LoginView.fxml:");
            e.printStackTrace();
            // Handle error (e.g., show error dialog)
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

            // (CẬP NHẬT) Lấy kích thước đã lưu, với default mới là 2000x1400
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE_PATH);
            double width = prefs.getDouble("windowWidth", 2000);
            double height = prefs.getDouble("windowHeight", 1400);

            // (CẬP NHẬT) Sửa Scene constructor để set kích thước đã lưu (hoặc default)
            Scene scene = new Scene(root, width, height);

            // (CẬP NHẬT) Thêm CSS cho MainView
            try {
                URL cssUrl = getClass().getResource("styles.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    System.err.println("Không tìm thấy styles.css cho MainView.");
                }
            } catch (NullPointerException e) {
                System.err.println("Lỗi khi tải styles.css: " + e.getMessage());
            }

            primaryStage.setTitle(i18n.getString("mainApp", "mainTitle")); // <-- MODIFIED
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) { // Bắt Exception chung
            System.err.println("Error loading MainView.fxml:");
            e.printStackTrace();
            // Handle error
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}