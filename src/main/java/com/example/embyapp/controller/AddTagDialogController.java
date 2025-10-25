package com.example.embyapp.controller;

import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.ItemRepository;
import com.example.embyapp.service.RequestEmby;
import com.example.embyapp.viewmodel.detail.SuggestionItemModel;
import com.example.embyapp.viewmodel.detail.TagModel;
import embyclient.model.UserLibraryTagItem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
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
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * (CẬP NHẬT 29) Hợp nhất logic gợi ý cho Tag, Studio, People.
 * - Cho phép Studio/People hiển thị và nhập JSON Key-Value.
 */
public class AddTagDialogController {

    public enum SuggestionContext {
        TAG, STUDIO, PEOPLE
    }

    @FXML private ToggleGroup tagTypeGroup;
    @FXML private RadioButton simpleTagRadio;
    @FXML private RadioButton jsonTagRadio;

    @FXML private GridPane simpleTagPane;
    @FXML private TextField simpleNameField;

    @FXML private GridPane jsonTagPane;
    @FXML private TextField keyField;
    @FXML private TextField valueField;

    @FXML private VBox suggestionJsonContainer;
    @FXML private FlowPane suggestionKeysPane;
    @FXML private FlowPane suggestionValuesPane;

    @FXML private VBox suggestionSimpleContainer;
    @FXML private FlowPane suggestionSimplePane;

    private final ToggleGroup keySuggestionGroup = new ToggleGroup();
    private final ToggleGroup valueSuggestionGroup = new ToggleGroup();

    private Stage dialogStage;
    private TagModel resultTag = null;

    private ItemRepository itemRepository;
    private SuggestionContext currentContext = SuggestionContext.TAG;
    // Xóa generalSuggestions, dùng rawNames/jsonGroups/simpleTags đã parse

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
        // Listener chuyển đổi pane nhập liệu (Giữ nguyên)
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

        // 1. Listener cho JSON Key (Giữ nguyên)
        keySuggestionGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null) {
                valueSuggestionGroup.selectToggle(null);
            }
            populateValues(newToggle);
        });
    }

    /**
     * Thiết lập context và repository trước khi hiển thị.
     */
    public void setContext(SuggestionContext context, ItemRepository itemRepository) {
        this.currentContext = context;
        this.itemRepository = itemRepository;

        // Cập nhật UI theo context (Chỉ thay đổi tiêu đề, không ẩn JSON inputs)
        Platform.runLater(() -> {
            Stage stage = (Stage) dialogStage.getScene().getWindow();
            String title = context == SuggestionContext.STUDIO ? "Thêm Studio Mới" :
                    context == SuggestionContext.PEOPLE ? "Thêm Người Mới" :
                            "Thêm Tag Mới";
            stage.setTitle(title);

            // Đặt lại label cho Simple Tags (dù là Tag, Studio hay People)
            simpleTagRadio.setText(context == SuggestionContext.TAG ? "Tag Đơn giản" :
                    context == SuggestionContext.STUDIO ? "Tên Studio" :
                            "Tên Người");

            // Cập nhật label cho Simple Suggestions
            Label suggestionLabel = (Label) suggestionSimpleContainer.getChildren().get(0);
            suggestionLabel.setText("Gợi ý " + (context == SuggestionContext.TAG ? "Tag Đơn giản" :
                    context == SuggestionContext.STUDIO ? "Studios" :
                            "People"));

            // Luôn hiển thị cả hai radio buttons và containers
            jsonTagRadio.setVisible(true);
            jsonTagRadio.setManaged(true);
            simpleTagRadio.setVisible(true);
            simpleTagRadio.setManaged(true);

            // Tải gợi ý
            loadSuggestedTags();
        });
    }


    private void loadSuggestedTags() {
        new Thread(() -> {
            final List<String> rawNames = new ArrayList<>();

            try {
                EmbyService embyService = EmbyService.getInstance();
                if (!embyService.isLoggedIn() || embyService.getApiClient() == null) {
                    System.err.println("Chưa đăng nhập, không thể lấy gợi ý.");
                    return;
                }

                switch (currentContext) {
                    case TAG:
                        rawNames.addAll(fetchTagsFromServer());
                        break;
                    case STUDIO:
                        rawNames.addAll(itemRepository.getStudioSuggestions(embyService.getApiClient())
                                .stream().map(SuggestionItemModel::getName).collect(Collectors.toList()));
                        break;
                    case PEOPLE:
                        rawNames.addAll(itemRepository.getPeopleSuggestions(embyService.getApiClient())
                                .stream().map(SuggestionItemModel::getName).collect(Collectors.toList()));
                        break;
                }

            } catch (Exception e) {
                System.err.println("Lỗi khi lấy gợi ý từ server: " + e.getMessage());
                e.printStackTrace();
            }

            // Cập nhật UI
            Platform.runLater(() -> {
                populateSuggestedTags(rawNames);

                // Hiển thị/ẩn container JSON
                suggestionJsonContainer.setVisible(!jsonGroups.isEmpty());
                suggestionJsonContainer.setManaged(!jsonGroups.isEmpty());

                // Hiển thị/ẩn container Simple (dù là Tag, Studio hay People)
                suggestionSimpleContainer.setVisible(!suggestionSimplePane.getChildren().isEmpty());
                suggestionSimpleContainer.setManaged(!suggestionSimplePane.getChildren().isEmpty());
            });
        }).start();
    }

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
            }
        } catch (Exception e) {
            // Lỗi đã được log ở loadSuggestedTags
        }
        return Collections.emptyList();
    }

    /**
     * (HỢP NHẤT LOGIC) Phân tích danh sách tên thô thành JSON Groups và Simple Tags.
     */
    private void populateSuggestedTags(List<String> rawNames) {
        if (rawNames == null || rawNames.isEmpty()) {
            jsonGroups.clear();
            populateKeys(); // Xóa keys
            populateSimpleTags(Collections.emptyList()); // Xóa simple tags
            return;
        }

        // 1. Parse tất cả tên thô thành TagModel
        List<ParsedTag> parsedTags = rawNames.stream()
                .map(raw -> new ParsedTag(raw, TagModel.parse(raw)))
                .collect(Collectors.toList());

        // 2. Chia nhóm JSON
        jsonGroups = parsedTags.stream()
                .filter(pt -> pt.model.isJson())
                .collect(Collectors.groupingBy(pt -> pt.model.getKey()));

        // 3. Lấy danh sách Simple Tags
        List<ParsedTag> simpleTags = parsedTags.stream()
                .filter(pt -> !pt.model.isJson())
                .collect(Collectors.toList());

        // 4. Populate UI
        populateKeys(); // JSON Keys
        populateSimpleTags(simpleTags); // Simple Tags (hoặc Studio/People đơn giản)
    }

    private void populateKeys() {
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
        suggestionValuesPane.getChildren().clear();

        if (selectedKeyToggle == null) {
            suggestionValuesPane.getChildren().clear();
            return;
        }

        String selectedKey = (String) selectedKeyToggle.getUserData();
        List<ParsedTag> tagsInGroup = jsonGroups.get(selectedKey);

        if (tagsInGroup == null) return;

        // Sắp xếp theo Value
        tagsInGroup.sort((pt1, pt2) -> pt1.model.getValue().compareToIgnoreCase(pt2.model.getValue()));

        for (ParsedTag pt : tagsInGroup) {
            ToggleButton chip = new ToggleButton(pt.model.getValue());
            chip.setToggleGroup(valueSuggestionGroup);
            chip.getStyleClass().addAll("suggested-tag-button", "tag-view-json");
            chip.setUserData(pt.rawString);

            chip.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    fillFormFromSuggestion((String) chip.getUserData());
                }
            });
            suggestionValuesPane.getChildren().add(chip);
        }
    }

    /**
     * Dùng cho Tag Đơn Giản, Studio Đơn giản, People Đơn giản.
     */
    private void populateSimpleTags(List<ParsedTag> simpleTags) {
        suggestionSimplePane.getChildren().clear();

        // Sắp xếp theo DisplayName (tên)
        simpleTags.sort((pt1, pt2) -> pt1.model.getDisplayName().compareToIgnoreCase(pt2.model.getDisplayName()));

        for (ParsedTag pt : simpleTags) {
            ToggleButton chip = new ToggleButton(pt.model.getDisplayName());
            chip.setToggleGroup(valueSuggestionGroup);
            chip.getStyleClass().add("suggested-tag-button");
            chip.setUserData(pt.rawString);

            chip.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    keySuggestionGroup.selectToggle(null);
                    fillFormFromSuggestion((String) chip.getUserData());
                }
            });
            suggestionSimplePane.getChildren().add(chip);
        }
    }


    private void fillFormFromSuggestion(String selectedTagName) {
        TagModel selectedTag = TagModel.parse(selectedTagName);
        if (selectedTag.isJson()) {
            jsonTagRadio.setSelected(true);
            // Dùng TagModel getters thay vì parse lại JSON thô
            keyField.setText(selectedTag.getKey() != null ? selectedTag.getKey() : "");
            valueField.setText(selectedTag.getValue() != null ? selectedTag.getValue() : "");
            simpleNameField.clear();
        } else {
            simpleTagRadio.setSelected(true);
            simpleNameField.setText(selectedTag.getDisplayName());
            keyField.clear();
            valueField.clear();
        }
    }

    /**
     * Không cần hàm fillSimpleFormFromSuggestion riêng nữa.
     */
    // private void fillSimpleFormFromSuggestion(String name) { ... }


    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public TagModel getResultTag() {
        return resultTag;
    }

    /**
     * Logic handleOk không đổi: nó tạo TagModel.
     */
    @FXML
    private void handleOk() {
        resultTag = null;
        if (simpleTagRadio.isSelected()) {
            String simpleName = simpleNameField.getText();
            if (simpleName != null && !simpleName.trim().isEmpty()) {
                // Studio/People/Simple Tag đều được lưu dưới dạng TagModel đơn giản
                resultTag = new TagModel(simpleName.trim());
            }
        } else {
            // Luôn cho phép tạo JSON cho mọi Context
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
        dialogStage.close();
    }
}