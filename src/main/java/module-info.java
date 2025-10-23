module com.example.embyapp {
    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics; // Added for completeness, often needed indirectly
    requires javafx.base;     // Added because you open viewmodel to it

    // Core Java modules needed by dependencies or SDK
    requires java.sql; // <<<=== ADDED THIS LINE

    // Dependencies explicitly required
    requires okhttp3;
    requires okhttp3.logging; // If using logging interceptor
    requires com.google.gson;
    requires org.threeten.bp;
    requires emby.sdk.java; // Assuming this is correct now

    // NOTE: Requires for SDK and gsonfire removed to avoid "module not found" build errors,
    // relying on them being on the module path.

    // Open packages for JavaFX FXML and reflection (e.g., by Gson)
    opens com.example.embyapp to javafx.fxml;
    opens com.example.embyapp.controller to javafx.fxml;
    opens com.example.embyapp.viewmodel to javafx.base; // Open to javafx.base for property binding

    // Export the main package if needed (optional, depends on how you run)
    exports com.example.embyapp;
}

