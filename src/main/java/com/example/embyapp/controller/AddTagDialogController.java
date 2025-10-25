package com.example.embyapp.controller;

import com.example.embyapp.viewmodel.detail.TagModel;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

/**
 * (MỚI) Controller cho cửa sổ popup AddTagDialog.
 */
public class AddTagDialogController {

    @FXML private ToggleGroup tagTypeGroup;
    @FXML private RadioButton simpleTagRadio;
    @FXML private RadioButton jsonTagRadio;

    @FXML private GridPane simpleTagPane;
    @FXML private TextField simpleNameField;

    @FXML private GridPane jsonTagPane;
    @FXML private TextField keyField;
    @FXML private TextField valueField;

    private Stage dialogStage;
    private TagModel resultTag = null;

    @FXML
    public void initialize() {
        // Thêm listener để chuyển đổi hiển thị
        simpleTagRadio.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            simpleTagPane.setVisible(isSelected);
            simpleTagPane.setManaged(isSelected);
            jsonTagPane.setVisible(!isSelected);
            jsonTagPane.setManaged(!isSelected);
        });

        // Kích hoạt listener lần đầu
        simpleTagPane.setVisible(true);
        simpleTagPane.setManaged(true);
        jsonTagPane.setVisible(false);
        jsonTagPane.setManaged(false);
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public TagModel getResultTag() {
        return resultTag;
    }

    @FXML
    private void handleOk() {
        if (simpleTagRadio.isSelected()) {
            String simpleName = simpleNameField.getText();
            if (simpleName != null && !simpleName.trim().isEmpty()) {
                resultTag = new TagModel(simpleName.trim());
            }
        } else {
            String key = keyField.getText();
            String value = valueField.getText();
            if (key != null && !key.trim().isEmpty() && value != null && !value.trim().isEmpty()) {
                resultTag = new TagModel(key.trim(), value.trim());
            }
        }

        if (resultTag != null) {
            dialogStage.close();
        } else {
            // (Có thể thêm Label lỗi)
            System.err.println("Dữ liệu tag không hợp lệ.");
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
}