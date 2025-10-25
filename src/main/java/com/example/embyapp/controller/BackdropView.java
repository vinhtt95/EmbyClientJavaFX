package com.example.embyapp.controller;

import com.example.emby.modelEmby.ImageInfo;
import com.example.embyapp.viewmodel.detail.ImageUrlHelper;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.util.function.Consumer;

/**
 * Component UI tùy chỉnh hiển thị 1 ảnh Backdrop và nút Xóa.
 */
public class BackdropView extends StackPane {

    private static final double THUMBNAIL_HEIGHT = 100;

    public BackdropView(ImageInfo imageInfo, String serverUrl, String itemId, Consumer<ImageInfo> onDelete) {
        setPrefHeight(THUMBNAIL_HEIGHT);
        getStyleClass().add("backdrop-view");

        // 1. Ảnh
        ImageView imageView = new ImageView();
        imageView.setFitHeight(THUMBNAIL_HEIGHT);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // 2. Loading
        ProgressIndicator loading = new ProgressIndicator();
        loading.setMaxSize(40, 40);

        // 3. Nút Xóa
        Button deleteButton = new Button("✕");
        deleteButton.getStyleClass().add("backdrop-delete-button");
        deleteButton.setOnAction(e -> onDelete.accept(imageInfo));
        StackPane.setAlignment(deleteButton, Pos.TOP_RIGHT);

        getChildren().addAll(loading, imageView, deleteButton);

        // Tải ảnh
        String imageUrl = ImageUrlHelper.getImageUrl(serverUrl, itemId, imageInfo, 400);
        if (imageUrl != null) {
            Image image = new Image(imageUrl, true); // true = tải nền
            imageView.setImage(image);

            image.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() < 1.0) {
                    loading.setVisible(true);
                } else {
                    loading.setVisible(false);
                }
            });
            image.errorProperty().addListener((obs, wasError, isError) -> {
                if(isError) {
                    // (Có thể set ảnh placeholder)
                    System.err.println("Lỗi tải backdrop: " + imageUrl);
                    loading.setVisible(false);
                }
            });
        } else {
            loading.setVisible(false);
            // (Set ảnh placeholder ở đây)
        }
    }
}