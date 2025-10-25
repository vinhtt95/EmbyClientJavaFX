package com.example.embyapp.controller;

import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.RequestEmby;
import com.example.embyapp.viewmodel.detail.TagModel;
import embyclient.model.UserLibraryTagItem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label; // (*** MỚI IMPORT ***)
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator; // (*** MỚI IMPORT ***)
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox; // (*** MỚI IMPORT ***)
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * (MỚI) Controller cho cửa sổ popup AddTagDialog.
 * (CẬP NHẬT 12):
 * - Thêm logic lấy và hiển thị tag gợi ý.
 * - Mặc định chọn JSON.
 * (CẬP NHẬT 13):
 * - Sử dụng hàm API thật để lấy tag gợi ý.
 * (CẬP NHẬT 14):
 * - Thay đổi @FXML suggestionTagsPane từ FlowPane sang VBox.
 * - Viết lại populateSuggestedTags để nhóm tag JSON theo Key.
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

    // (*** THAY ĐỔI: Giờ đây là VBox, không phải FlowPane ***)
    @FXML private VBox suggestionTagsPane;
    private final ToggleGroup suggestionGroup = new ToggleGroup();

    private Stage dialogStage;
    private TagModel resultTag = null;

    // (*** Lớp helper nội bộ để giữ cả chuỗi gốc và model đã parse ***)
    private static class ParsedTag {
        final String rawString;
        final TagModel model;

        ParsedTag(String rawString, TagModel model) {
            this.rawString = rawString;
            this.model = model;
        }
    }


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

    private void loadSuggestedTags() {
        new Thread(() -> {
            List<String> suggestedNames = fetchTagsFromServer();
            Platform.runLater(() -> populateSuggestedTags(suggestedNames));
        }).start();
    }

    /**
     * Lấy danh sách tên tag từ Emby Server.
     */
    private List<String> fetchTagsFromServer() {
        try {
            EmbyService embyService = EmbyService.getInstance();
            if (embyService.isLoggedIn() && embyService.getApiClient() != null) {
                List<UserLibraryTagItem> tagItems = new RequestEmby().getListTagsItem(embyService.getApiClient());

                if (tagItems != null) {
                    return tagItems.stream()
                            .map(UserLibraryTagItem::getName)
                            .filter(name -> name != null && !name.isEmpty())
                            .distinct()
                            .collect(Collectors.toList());
                }
            } else {
                System.err.println("Chưa đăng nhập, không thể lấy tag gợi ý.");
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy tag gợi ý từ server: " + e.getMessage());
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    /**
     * (*** HÀM NÀY ĐÃ VIẾT LẠI HOÀN TOÀN ***)
     * Phân tích, nhóm và hiển thị các tag gợi ý.
     */
    private void populateSuggestedTags(List<String> tagNames) {
        suggestionTagsPane.getChildren().clear();
        suggestionGroup.getToggles().clear();

        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }

        // 1. Phân tích tất cả tag, giữ lại chuỗi gốc
        List<ParsedTag> parsedTags = tagNames.stream()
                .map(raw -> new ParsedTag(raw, TagModel.parse(raw)))
                .collect(Collectors.toList());

        // 2. Tách tag JSON và tag Đơn giản
        Map<String, List<ParsedTag>> jsonGroups = parsedTags.stream()
                .filter(pt -> pt.model.isJson())
                .collect(Collectors.groupingBy(pt -> pt.model.getKey())); // Nhóm theo Key

        List<ParsedTag> simpleTags = parsedTags.stream()
                .filter(pt -> !pt.model.isJson())
                .collect(Collectors.toList());

        // 3. Hiển thị các nhóm JSON
        // Sắp xếp các nhóm theo Key (A-Z)
        List<Map.Entry<String, List<ParsedTag>>> sortedJsonGroups = new ArrayList<>(jsonGroups.entrySet());
        sortedJsonGroups.sort(Map.Entry.comparingByKey());

        for (Map.Entry<String, List<ParsedTag>> entry : sortedJsonGroups) {
            String key = entry.getKey();
            List<ParsedTag> tagsInGroup = entry.getValue();

            // Tạo Tiêu đề (Key)
            Label groupTitle = new Label(key);
            groupTitle.getStyleClass().add("suggestion-group-title");
            suggestionTagsPane.getChildren().add(groupTitle);

            // Tạo FlowPane cho các Value
            FlowPane valuesPane = new FlowPane();
            valuesPane.getStyleClass().add("suggestion-value-pane");
            valuesPane.setHgap(8.0);
            valuesPane.setVgap(8.0);

            // Sắp xếp các value trong nhóm (A-Z)
            tagsInGroup.sort((pt1, pt2) -> pt1.model.getValue().compareToIgnoreCase(pt2.model.getValue()));

            for (ParsedTag pt : tagsInGroup) {
                // Tạo chip CHỈ HIỂN THỊ VALUE
                ToggleButton chip = new ToggleButton(pt.model.getValue());
                chip.setToggleGroup(suggestionGroup);
                chip.getStyleClass().addAll("suggested-tag-button", "tag-view-json");

                // (QUAN TRỌNG) UserData vẫn lưu CHUỖI GỐC
                chip.setUserData(pt.rawString);

                // Listener (giống như cũ)
                chip.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                    if (isSelected) {
                        fillFormFromSuggestion((String) chip.getUserData());
                    }
                });
                valuesPane.getChildren().add(chip);
            }
            suggestionTagsPane.getChildren().add(valuesPane); // Thêm FlowPane vào VBox
        }

        // 4. Hiển thị các tag Đơn giản (nếu có)
        if (!simpleTags.isEmpty()) {
            // Thêm dấu ngăn cách
            if (!jsonGroups.isEmpty()) {
                suggestionTagsPane.getChildren().add(new Separator());
            }

            Label simpleTitle = new Label("Tags Đơn Giản");
            simpleTitle.getStyleClass().add("suggestion-group-title");
            suggestionTagsPane.getChildren().add(simpleTitle);

            FlowPane simplePane = new FlowPane();
            simplePane.getStyleClass().add("suggestion-value-pane");
            simplePane.setHgap(8.0);
            simplePane.setVgap(8.0);

            // Sắp xếp tag đơn giản (A-Z)
            simpleTags.sort((pt1, pt2) -> pt1.model.getDisplayName().compareToIgnoreCase(pt2.model.getDisplayName()));

            for (ParsedTag pt : simpleTags) {
                ToggleButton chip = new ToggleButton(pt.model.getDisplayName());
                chip.setToggleGroup(suggestionGroup);
                chip.getStyleClass().add("suggested-tag-button");

                // UserData lưu CHUỖI GỐC
                chip.setUserData(pt.rawString);

                chip.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                    if (isSelected) {
                        fillFormFromSuggestion((String) chip.getUserData());
                    }
                });
                simplePane.getChildren().add(chip);
            }
            suggestionTagsPane.getChildren().add(simplePane);
        }
    }


    /**
     * (*** HÀM NÀY GIỮ NGUYÊN - KHÔNG THAY ĐỔI ***)
     * Vì UserData vẫn được set là chuỗi JSON/string gốc, logic này hoạt động hoàn hảo.
     */
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