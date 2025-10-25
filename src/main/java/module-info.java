// Trong file: src/main/java/module-info.java

module com.example.embyapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base; // Added for Properties

    requires com.google.gson;
    requires org.threeten.bp;

    requires java.prefs;
    requires java.sql;
    requires emby.sdk.java;
    requires okhttp;

    requires java.desktop;

    opens com.example.embyapp to javafx.fxml, javafx.graphics;
    opens com.example.embyapp.controller to javafx.fxml;
    opens com.example.embyapp.viewmodel to javafx.base; // Mở viewmodel chính

    // (*** THÊM DÒNG NÀY ***)
    opens com.example.embyapp.viewmodel.detail to javafx.base; // Mở viewmodel phụ

    exports com.example.embyapp;
    exports com.example.embyapp.controller;
    exports com.example.embyapp.service;
    exports com.example.embyapp.viewmodel;
}