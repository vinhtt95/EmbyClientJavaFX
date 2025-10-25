module com.example.embyapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    requires com.google.gson;

    requires java.prefs;
    requires java.sql;
    requires okhttp;

    requires java.desktop;
    requires eemby.sdk.java;

    // XÓA DÒNG "opens java.time to com.google.gson;" KHỎI ĐÂY

    opens com.example.embyapp to javafx.fxml, javafx.graphics;
    opens com.example.embyapp.controller to javafx.fxml;
    opens com.example.embyapp.viewmodel to javafx.base;
    opens com.example.embyapp.viewmodel.detail to javafx.base;

    exports com.example.embyapp;
    exports com.example.embyapp.controller;
    exports com.example.embyapp.service;
    exports com.example.embyapp.viewmodel;
}