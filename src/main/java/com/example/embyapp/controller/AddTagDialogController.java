package com.example.embyapp.controller;

import com.example.embyapp.service.EmbyService; // (*** MỚI IMPORT ***)
import com.example.embyapp.service.RequestEmby; // (*** MỚI IMPORT ***)
import com.example.embyapp.viewmodel.detail.TagModel;
import embyclient.model.UserLibraryTagItem; // (*** MỚI IMPORT ***)
import javafx.application.Platform; // (*** MỚI IMPORT ***)
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton; // (*** MỚI IMPORT ***)
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane; // (*** MỚI IMPORT ***)
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.util.ArrayList; // (*** MỚI IMPORT ***)
import java.util.Collections; // (*** MỚI IMPORT ***)
import java.util.List; // (*** MỚI IMPORT ***)
import java.util.Map; // (*** MỚI IMPORT ***)
import java.util.stream.Collectors; // (*** MỚI IMPORT ***)


/**
 * (MỚI) Controller cho cửa sổ popup AddTagDialog.
 * (CẬP NHẬT 12):
 * - Thêm logic lấy và hiển thị tag gợi ý.
 * - Mặc định chọn JSON.
 * (CẬP NHẬT 13):
 * - Sử dụng hàm API thật để lấy tag gợi ý.
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

    @FXML private FlowPane suggestionTagsPane; // (*** MỚI ***)
    private final ToggleGroup suggestionGroup = new ToggleGroup(); // (*** MỚI: Để đảm bảo chỉ chọn 1 gợi ý ***)

    private Stage dialogStage;
    private TagModel resultTag = null;

    @FXML
    public void initialize() {
        // Listener chuyển đổi pane nhập liệu
        simpleTagRadio.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            simpleTagPane.setVisible(isSelected);
            simpleTagPane.setManaged(isSelected);
            jsonTagPane.setVisible(!isSelected);
            jsonTagPane.setManaged(!isSelected);
        });

        // Kích hoạt listener lần đầu (Mặc định là JSON)
        simpleTagPane.setVisible(false);
        simpleTagPane.setManaged(false);
        jsonTagPane.setVisible(true);
        jsonTagPane.setManaged(true);
        jsonTagRadio.setSelected(true); // Đảm bảo radio JSON được chọn

        // Tải tag gợi ý (chạy nền)
        loadSuggestedTags();
    }

    // (*** MỚI: Logic tải và hiển thị gợi ý ***)
    private void loadSuggestedTags() {
        new Thread(() -> {
            // (*** GỌI HÀM API THẬT ***)
            List<String> suggestedNames = fetchTagsFromServer();
            Platform.runLater(() -> populateSuggestedTags(suggestedNames));
        }).start();
    }

    // (*** HÀM NÀY ĐÃ ĐƯỢC SỬA ĐỔI ***)
    /**
     * Lấy danh sách tên tag từ Emby Server.
     * @return List<String> chứa tên các tag, hoặc list rỗng nếu lỗi.
     */
    private List<String> fetchTagsFromServer() {
        try {
            EmbyService embyService = EmbyService.getInstance();
            if (embyService.isLoggedIn() && embyService.getApiClient() != null) {
                // Gọi hàm từ RequestEmby, truyền ApiClient vào
                List<UserLibraryTagItem> tagItems = new RequestEmby().getListTagsItem(embyService.getApiClient());

                if (tagItems != null) {
                    // Trích xuất tên từ danh sách tag items
                    return tagItems.stream()
                            .map(UserLibraryTagItem::getName) // Lấy trường Name
                            .filter(name -> name != null && !name.isEmpty()) // Lọc bỏ null hoặc rỗng
                            .distinct() // Loại bỏ trùng lặp
                            .collect(Collectors.toList());
                }
            } else {
                System.err.println("Chưa đăng nhập, không thể lấy tag gợi ý.");
            }
        } catch (Exception e) {
            // Bắt lỗi chung thay vì chỉ ApiException
            System.err.println("Lỗi khi lấy tag gợi ý từ server: " + e.getMessage());
            e.printStackTrace(); // In stack trace để debug
        }
        return Collections.emptyList(); // Trả về list rỗng nếu có lỗi hoặc không đăng nhập
    }

    // (*** HÀM NÀY GIỮ NGUYÊN TỪ LẦN TRƯỚC ***)
    private void populateSuggestedTags(List<String> tagNames) {
        suggestionTagsPane.getChildren().clear();
        suggestionGroup.getToggles().clear(); // Xóa toggle cũ khỏi group

        for (String tagName : tagNames) {
            try {
                TagModel tagModel = TagModel.parse(tagName); // Phân tích tag
                ToggleButton chip = new ToggleButton(tagModel.getDisplayName());
                chip.setToggleGroup(suggestionGroup); // Thêm vào group
                chip.getStyleClass().add("suggested-tag-button"); // CSS
                if (tagModel.isJson()) {
                    chip.getStyleClass().add("tag-view-json"); // Dùng lại style màu của chip JSON
                }

                // Lưu trữ tag gốc để dùng khi chọn
                chip.setUserData(tagName);

                // Thêm listener khi chọn/bỏ chọn
                chip.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                    if (isSelected) {
                        fillFormFromSuggestion((String) chip.getUserData());
                    }
                    // Optional: Xử lý khi bỏ chọn (ví dụ: xóa trắng form?)
                    // else { clearForm(); }
                });

                suggestionTagsPane.getChildren().add(chip);
            } catch (Exception e) {
                System.err.println("Lỗi khi tạo chip cho tag: " + tagName + " - " + e.getMessage());
            }
        }
    }

    // (*** HÀM NÀY GIỮ NGUYÊN TỪ LẦN TRƯỚC ***)
    private void fillFormFromSuggestion(String selectedTagName) {
        TagModel selectedTag = TagModel.parse(selectedTagName);
        if (selectedTag.isJson()) {
            jsonTagRadio.setSelected(true); // Chọn radio JSON
            // Cố gắng lấy key/value từ JSON gốc thay vì display name
            try {
                com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(selectedTagName).getAsJsonObject();
                Map.Entry<String, com.google.gson.JsonElement> firstEntry = jsonObject.entrySet().stream().findFirst().orElse(null);
                if(firstEntry != null) {
                    keyField.setText(firstEntry.getKey());
                    valueField.setText(firstEntry.getValue().getAsString());
                } else { // Fallback nếu parse lỗi
                    keyField.setText("Lỗi Key");
                    valueField.setText("Lỗi Value");
                }
            } catch (Exception e) { // Fallback nếu parse lỗi
                System.err.println("Lỗi parse JSON khi điền form gợi ý: " + selectedTagName + " - " + e.getMessage());
                keyField.setText("Lỗi Key");
                valueField.setText("Lỗi Value");
            }
            simpleNameField.clear();
        } else {
            simpleTagRadio.setSelected(true); // Chọn radio Simple
            simpleNameField.setText(selectedTag.getDisplayName()); // Lấy tên đơn giản
            keyField.clear();
            valueField.clear();
        }
    }


    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public TagModel getResultTag() {
        return resultTag;
    }

    @FXML
    private void handleOk() {
        resultTag = null; // Reset trước khi tạo mới
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