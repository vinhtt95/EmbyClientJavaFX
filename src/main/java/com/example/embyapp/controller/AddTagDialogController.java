package com.example.embyapp.controller;

import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.RequestEmby;
import com.example.embyapp.viewmodel.detail.TagModel;
import embyclient.model.UserLibraryTagItem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
// (*** XÓA IMPORT Separator ***)
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * (CẬP NHẬT 19)
 * - Trả lại layout Tag Đơn Giản về 1 dòng.
 * - Xóa simpleKeySuggestionGroup.
 * - Cập nhật logic listener cho 2 group (JSONKey và Value/Simple).
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

    // (*** THAY ĐỔI FXML ***)
    @FXML private VBox suggestionJsonContainer;
    @FXML private FlowPane suggestionKeysPane;
    @FXML private FlowPane suggestionValuesPane;

    @FXML private VBox suggestionSimpleContainer;
    // (*** SỬA: Quay về FXML gốc cho Simple ***)
    @FXML private FlowPane suggestionSimplePane;
    // (*** KẾT THÚC THAY ĐỔI FXML ***)

    // (*** SỬA: Chỉ 2 ToggleGroup ***)
    private final ToggleGroup keySuggestionGroup = new ToggleGroup();
    private final ToggleGroup valueSuggestionGroup = new ToggleGroup(); // Dùng chung cho JSON Value và Simple

    private Stage dialogStage;
    private TagModel resultTag = null;

    private static class ParsedTag {
        final String rawString;
        final TagModel model;

        ParsedTag(String rawString, TagModel model) {
            this.rawString = rawString;
            this.model = model;
        }
    }

    private Map<String, List<ParsedTag>> jsonGroups = new HashMap<>();


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
        jsonTagRadio.setSelected(true);

        // (*** SỬA: Cập nhật Listeners cho 2 Group ***)

        // 1. Listener cho JSON Key
        keySuggestionGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null) {
                // Hủy chọn các tag trong nhóm Value/Simple
                valueSuggestionGroup.selectToggle(null);
            }
            // Điền value cho JSON
            populateValues(newToggle);
        });

        // 2. Listener cho Value/Simple (Không cần, vì đã gán trên từng chip)

        // Tải tag gợi ý (chạy nền)
        loadSuggestedTags();
    }

    private void loadSuggestedTags() {
        // (Logic hàm này giữ nguyên)
        new Thread(() -> {
            List<String> suggestedNames = fetchTagsFromServer();
            Platform.runLater(() -> populateSuggestedTags(suggestedNames));
        }).start();
    }

    private List<String> fetchTagsFromServer() {
        // (Logic hàm này giữ nguyên)
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

    private void populateSuggestedTags(List<String> tagNames) {
        // (Logic hàm này giữ nguyên)
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }

        List<ParsedTag> parsedTags = tagNames.stream()
                .map(raw -> new ParsedTag(raw, TagModel.parse(raw)))
                .collect(Collectors.toList());

        jsonGroups = parsedTags.stream()
                .filter(pt -> pt.model.isJson())
                .collect(Collectors.groupingBy(pt -> pt.model.getKey()));

        List<ParsedTag> simpleTags = parsedTags.stream()
                .filter(pt -> !pt.model.isJson())
                .collect(Collectors.toList());

        populateKeys(); // JSON Keys
        populateSimpleTags(simpleTags); // Simple Tags

        suggestionJsonContainer.setVisible(!jsonGroups.isEmpty());
        suggestionJsonContainer.setManaged(!jsonGroups.isEmpty());
        suggestionSimpleContainer.setVisible(!simpleTags.isEmpty());
        suggestionSimpleContainer.setManaged(!simpleTags.isEmpty());
    }

    private void populateKeys() {
        // (Logic hàm này giữ nguyên - có auto-select)
        suggestionKeysPane.getChildren().clear();
        keySuggestionGroup.getToggles().clear();

        List<String> sortedKeys = new ArrayList<>(jsonGroups.keySet());
        Collections.sort(sortedKeys, String.CASE_INSENSITIVE_ORDER);

        for (String key : sortedKeys) {
            ToggleButton chip = new ToggleButton(key);
            chip.setToggleGroup(keySuggestionGroup);
            chip.getStyleClass().add("suggestion-key-button");
            chip.setUserData(key);
            suggestionKeysPane.getChildren().add(chip);
        }

        if (!keySuggestionGroup.getToggles().isEmpty()) {
            keySuggestionGroup.selectToggle(keySuggestionGroup.getToggles().get(0));
        }
    }

    private void populateValues(Toggle selectedKeyToggle) {
        // (Logic hàm này giữ nguyên)
        suggestionValuesPane.getChildren().clear();
        // (*** KHÔNG XÓA Toggles khỏi valueSuggestionGroup ở đây ***)
        // valueSuggestionGroup.getToggles().clear(); // <--- XÓA DÒNG NÀY

        if (selectedKeyToggle == null) {
            // Nếu không chọn Key, thì xóa Value
            suggestionValuesPane.getChildren().clear();
            return;
        }

        String selectedKey = (String) selectedKeyToggle.getUserData();
        List<ParsedTag> tagsInGroup = jsonGroups.get(selectedKey);

        if (tagsInGroup == null) return;

        tagsInGroup.sort((pt1, pt2) -> pt1.model.getValue().compareToIgnoreCase(pt2.model.getValue()));

        for (ParsedTag pt : tagsInGroup) {
            ToggleButton chip = new ToggleButton(pt.model.getValue());
            chip.setToggleGroup(valueSuggestionGroup); // Thêm vào group chung
            chip.getStyleClass().addAll("suggested-tag-button", "tag-view-json");
            chip.setUserData(pt.rawString);

            // Listener này chỉ fill, không cần hủy chọn
            chip.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    fillFormFromSuggestion((String) chip.getUserData());
                }
            });
            suggestionValuesPane.getChildren().add(chip);
        }
    }

    /**
     * (*** SỬA: Trả lại logic 1 dòng cho Tag Đơn Giản ***)
     */
    private void populateSimpleTags(List<ParsedTag> simpleTags) {
        suggestionSimplePane.getChildren().clear();
        // (*** KHÔNG XÓA Toggles khỏi valueSuggestionGroup ở đây ***)
        // valueSuggestionGroup.getToggles().clear(); // <--- XÓA DÒNG NÀY

        simpleTags.sort((pt1, pt2) -> pt1.model.getDisplayName().compareToIgnoreCase(pt2.model.getDisplayName()));

        for (ParsedTag pt : simpleTags) {
            ToggleButton chip = new ToggleButton(pt.model.getDisplayName());
            chip.setToggleGroup(valueSuggestionGroup); // Dùng chung group Value
            chip.getStyleClass().add("suggested-tag-button"); // Style Xám
            chip.setUserData(pt.rawString);

            // (*** SỬA: Listener hủy chọn JSON Key ***)
            chip.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    keySuggestionGroup.selectToggle(null); // Hủy chọn JSON Key
                    fillFormFromSuggestion((String) chip.getUserData());
                }
            });
            suggestionSimplePane.getChildren().add(chip);
        }
    }


    private void fillFormFromSuggestion(String selectedTagName) {
        // (Logic hàm này giữ nguyên)
        TagModel selectedTag = TagModel.parse(selectedTagName);
        if (selectedTag.isJson()) {
            jsonTagRadio.setSelected(true);
            try {
                com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(selectedTagName).getAsJsonObject();
                Map.Entry<String, com.google.gson.JsonElement> firstEntry = jsonObject.entrySet().stream().findFirst().orElse(null);
                if(firstEntry != null) {
                    keyField.setText(firstEntry.getKey());
                    valueField.setText(firstEntry.getValue().getAsString());
                } else {
                    keyField.setText("Lỗi Key");
                    valueField.setText("Lỗi Value");
                }
            } catch (Exception e) {
                System.err.println("Lỗi parse JSON khi điền form gợi ý: " + selectedTagName + " - " + e.getMessage());
                keyField.setText("Lỗi Key");
                valueField.setText("Lỗi Value");
            }
            simpleNameField.clear();
        } else {
            simpleTagRadio.setSelected(true);
            simpleNameField.setText(selectedTag.getDisplayName());
            keyField.clear();
            valueField.clear();
        }
    }

    public void setDialogStage(Stage dialogStage) {
        // (Logic hàm này giữ nguyên)
        this.dialogStage = dialogStage;
    }

    public TagModel getResultTag() {
        // (Logic hàm này giữ nguyên)
        return resultTag;
    }

    @FXML
    private void handleOk() {
        // (Logic hàm này giữ nguyên)
        resultTag = null;
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
            System.err.println("Dữ liệu tag không hợp lệ.");
        }
    }

    @FXML
    private void handleCancel() {
        // (Logic hàm này giữ nguyên)
        dialogStage.close();
    }
}