package com.example.embyapp;

import com.example.embyapp.controller.LoginController;
import com.example.embyapp.controller.MainController;
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.I18nManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.prefs.Preferences;

public class MainApp extends Application {

    private Stage primaryStage;
    private EmbyService embyService;
    private I18nManager i18n;

    private static final String PREFS_NODE_PATH = "/com/example/embyapp";

    // Giữ tham chiếu đến MainController để gọi shutdown
    private MainController mainController;


    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.embyService = EmbyService.getInstance();
        this.i18n = I18nManager.getInstance();

        Preferences prefs = Preferences.userRoot().node(PREFS_NODE_PATH);
        primaryStage.setX(prefs.getDouble("windowX", 100));
        primaryStage.setY(prefs.getDouble("windowY", 100));

        // Thêm listener để lưu vị trí/kích thước VÀ gọi shutdown hook
        primaryStage.setOnCloseRequest(e -> {
            prefs.putDouble("windowX", primaryStage.getX());
            prefs.putDouble("windowY", primaryStage.getY());
            prefs.putDouble("windowWidth", primaryStage.getWidth());
            prefs.putDouble("windowHeight", primaryStage.getHeight());
            System.out.println("Đã lưu vị trí và kích thước cửa sổ.");

            // Gọi shutdown để hủy đăng ký global hotkey hook
            if (mainController != null) {
                mainController.shutdown();
            }
        });

        if (embyService.tryRestoreSession()) {
            System.out.println("Session restored, showing main view.");
            showMainView();
        } else {
            System.out.println("No valid session found, showing login view.");
            showLoginView();
        }
    }


    public void showLoginView() {
        try {
            URL fxmlUrl = getClass().getResource("LoginView.fxml");
            if (fxmlUrl == null) {
                System.err.println("Cannot find LoginView.fxml!");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            LoginController controller = loader.getController();
            controller.setMainApp(this);

            Scene scene = new Scene(root);

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

            primaryStage.setTitle(i18n.getString("mainApp", "loginTitle"));
            primaryStage.setScene(scene);
            primaryStage.show();

            // Đảm bảo controller là null khi ở màn hình login
            this.mainController = null;

        } catch (Exception e) {
            System.err.println("Error loading LoginView.fxml:");
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

            // Lưu tham chiếu đến controller
            this.mainController = loader.getController();
            this.mainController.setMainApp(this);

            Preferences prefs = Preferences.userRoot().node(PREFS_NODE_PATH);
            double width = prefs.getDouble("windowWidth", 2000);
            double height = prefs.getDouble("windowHeight", 1400);

            Scene scene = new Scene(root, width, height);

            // Gọi đăng ký hotkey (cho scene)
            this.mainController.registerGlobalHotkeys(scene);

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

            primaryStage.setTitle(i18n.getString("mainApp", "mainTitle"));
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Error loading MainView.fxml:");
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}