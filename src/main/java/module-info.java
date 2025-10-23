module com.example.embyapp.embyjavafxclient {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.embyapp.embyjavafxclient to javafx.fxml;
    exports com.example.embyapp.embyjavafxclient;
}