package com.example.embyapp.viewmodel;

import embyclient.model.AuthenticationAuthenticationResult;
import embyclient.model.UserDto;
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.I18nManager; // <-- IMPORT
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * (SỬA ĐỔI) ViewModel cho MainView.
 * File này đã được rút gọn. Logic tải thư viện (ListView cũ) đã chuyển sang LibraryTreeController.
 * ViewModel này giờ đây chỉ quản lý trạng thái chung của MainView
 * (như Welcome Label và Status Bar).
 */
public class MainViewModel {

    // Property cho WelcomeLabel (giữ lại)
    private final StringProperty welcomeMessage; // <-- MODIFIED

    // Property MỚI cho StatusBar
    private final StringProperty statusMessage; // <-- MODIFIED
    private final BooleanProperty loading = new SimpleBooleanProperty(false); // MỚI

    private final EmbyService embyService;
    private final I18nManager i18n; // <-- ADDED

    public MainViewModel(EmbyService embyService) {
        this.embyService = embyService;
        this.i18n = I18nManager.getInstance(); // <-- ADDED

        // <-- MODIFIED: Load text from I18nManager -->
        this.welcomeMessage = new SimpleStringProperty(i18n.getString("mainViewModel", "loadingUser"));
        this.statusMessage = new SimpleStringProperty(i18n.getString("mainViewModel", "ready"));
    }

    // --- Getters cho Properties ---

    public StringProperty welcomeMessageProperty() {
        return welcomeMessage;
    }

    /**
     * (MỚI) Property cho nội dung của StatusBar.
     * MainController sẽ set giá trị này.
     */
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    /**
     * (MỚI) Property cho trạng thái loading của StatusBar.
     * MainController sẽ set giá trị này.
     */
    public BooleanProperty loadingProperty() {
        return loading;
    }

    // --- Logic (chỉ giữ lại loadUserData) ---

    /**
     * Tải thông tin người dùng (để cập nhật welcome message).
     * Hàm này được gọi bởi MainController khi khởi tạo.
     */
    public void loadUserData() {
        AuthenticationAuthenticationResult authResult = embyService.getCurrentAuthResult();
        if (authResult != null && authResult.getUser() != null) {
            UserDto user = authResult.getUser();
            Platform.runLater(() -> welcomeMessage.set(i18n.getString("mainViewModel", "welcomeUser", user.getName()))); // <-- MODIFIED
        } else {
            Platform.runLater(() -> welcomeMessage.set(i18n.getString("mainViewModel", "welcomeGuest"))); // <-- MODIFIED
        }
    }

    // (Logic loadLibraryData() cũ đã được XÓA và chuyển sang LibraryTreeController)
}