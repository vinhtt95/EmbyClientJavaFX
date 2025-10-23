package com.example.embyapp;

/**
 * Launcher class to avoid JavaFX runtime components are missing error.
 * This is needed when creating a fat JAR.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}