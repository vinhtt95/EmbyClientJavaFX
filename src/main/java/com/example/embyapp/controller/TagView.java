package com.example.embyapp.controller;

import com.example.embyapp.viewmodel.detail.TagModel;
import javafx.geometry.Insets;
import javafx.geometry.Orientation; // (*** MỚI IMPORT ***)
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator; // (*** MỚI IMPORT ***)
import javafx.scene.layout.HBox;
import java.util.function.Consumer;

/**
 * (MỚI) Component UI tùy chỉnh (chip) để hiển thị một TagModel.
 * Bao gồm một Label và một nút Xóa.
 *
 * (CẬP NHẬT 8):
 * - Thay đổi logic hiển thị cho tag JSON.
 * - Thay vì dùng "Key | Value" text, dùng "KeyLabel" + "Separator (UI)" + "ValueLabel".
 */
public class TagView extends HBox {

    private final TagModel tagModel;

    public TagView(TagModel tagModel, Consumer<TagModel> onDelete) {
        this.tagModel = tagModel;

        // Cấu hình HBox (cái "chip")
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        setPadding(new Insets(4, 6, 4, 10));
        getStyleClass().add("tag-view"); // CSS
        if (tagModel.isJson()) {
            getStyleClass().add("tag-view-json"); // CSS riêng cho tag JSON
        }else { // (*** THAY ĐỔI/THÊM VÀO ***)
            getStyleClass().add("tag-view-simple"); // Áp dụng style màu hồng cho tag đơn giản
        }

        // (*** LOGIC HIỂN THỊ ĐÃ SỬA ĐỔI ***)
        if (tagModel.isJson()) {
            // 1. Nếu là JSON: Key + Separator + Value
            Label keyLabel = new Label(tagModel.getDisplayName().split(" \\| ")[0]); // Lấy phần Key
            keyLabel.getStyleClass().add("tag-label-key"); // CSS riêng cho Key

            Separator separator = new Separator(Orientation.VERTICAL);
            separator.getStyleClass().add("tag-separator"); // CSS cho dấu gạch

            Label valueLabel = new Label(tagModel.getDisplayName().split(" \\| ")[1]); // Lấy phần Value
            valueLabel.getStyleClass().add("tag-label-value"); // CSS riêng cho Value

            getChildren().addAll(keyLabel, separator, valueLabel);
        } else {
            // 2. Nếu là tag thường: Chỉ là Label
            Label label = new Label(tagModel.getDisplayName());
            label.getStyleClass().add("tag-label"); // CSS
            getChildren().add(label);
        }
        // (*** KẾT THÚC SỬA ĐỔI ***)

        // Nút Xóa (Giữ nguyên)
        Button deleteButton = new Button("✕");
        deleteButton.getStyleClass().add("tag-delete-button"); // CSS
        deleteButton.setOnAction(e -> onDelete.accept(tagModel));

        getChildren().add(deleteButton); // Luôn thêm nút xóa ở cuối
    }

    public TagModel getTagModel() {
        return tagModel;
    }
}