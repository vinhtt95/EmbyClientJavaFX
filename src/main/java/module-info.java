module com.example.embyapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base; // Added for Properties

    //requires emby.sdk.java; // Keep this commented out for now, let Maven handle module path
    // OkHttp 2.x is likely not a module, so remove requires for okhttp3

    requires com.google.gson;
    requires org.threeten.bp; // Keep requires for threetenbp if needed by SDK
    // requires io.gsonfire; // Keep removed unless proven necessary

    requires java.prefs; // Added for Preferences API
    requires java.sql;
    requires emby.sdk.java;
    requires okhttp; // Added for java.sql.Date possibly used by SDK/Gson

    requires java.desktop; // (MỚI) Thêm module này để dùng java.awt.Desktop

    opens com.example.embyapp to javafx.fxml, javafx.graphics;
    opens com.example.embyapp.controller to javafx.fxml;
    opens com.example.embyapp.viewmodel to javafx.base; // Open viewmodel to javafx.base for properties

    // If using Gson to reflectively access SDK models, open them
    // opens com.example.emby.EmbyClient to com.google.gson;
    exports com.example.embyapp;
    exports com.example.embyapp.controller;
    exports com.example.embyapp.service;
    exports com.example.embyapp.viewmodel;

}