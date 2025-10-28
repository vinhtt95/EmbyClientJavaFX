package com.example.embyapp.controller;

import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.I18nManager;
import com.example.embyapp.service.ItemRepository;
import com.example.embyapp.service.RequestEmby;
import com.example.embyapp.viewmodel.detail.SuggestionItemModel;
import com.example.embyapp.viewmodel.detail.TagModel;
import embyclient.model.UserLibraryTagItem;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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

import javafx.animation.PauseTransition;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Controller for the Add Tag Dialog.
 * Includes advanced input handling features.
 * (CẬP NHẬT 39) Thêm logic tự động chọn key gợi ý, bôi đen value khi Tab, bỏ qua value khi nhập key.
 */
public class AddTagDialogController {

    public enum SuggestionContext {
        TAG, STUDIO, PEOPLE, GENRE
    }

    @FXML private Label titleLabel;
    @FXML private ToggleGroup tagTypeGroup;
    @FXML private RadioButton simpleTagRadio;
    @FXML private RadioButton jsonTagRadio;

    @FXML private GridPane simpleTagPane;
    @FXML private Label contentLabel;
    @FXML private TextField simpleNameField;

    @FXML private GridPane jsonTagPane;
    @FXML private Label keyLabel;
    @FXML private TextField keyField;
    @FXML private Label valueLabel;
    @FXML private TextField valueField;

    @FXML private VBox suggestionJsonContainer;
    @FXML private Label suggestionKeyLabel;
    @FXML private FlowPane suggestionKeysPane;
    @FXML private Label suggestionValueLabel;
    @FXML private FlowPane suggestionValuesPane;

    @FXML private VBox suggestionSimpleContainer;
    @FXML private Label suggestionSimpleLabel;
    @FXML private FlowPane suggestionSimplePane;

    @FXML private TextField copyIdField;
    @FXML private Button copyButton;

    @FXML private Button cancelButton;
    @FXML private Button okButton;

    private final ToggleGroup keySuggestionGroup = new ToggleGroup();
    private final ToggleGroup valueSuggestionGroup = new ToggleGroup();

    private Stage dialogStage;
    private TagModel resultTag = null;
    private StringProperty copyTriggeredId = new SimpleStringProperty(null);

    private ItemRepository itemRepository;
    private SuggestionContext currentContext = SuggestionContext.TAG;

    private List<String> allRawNames = Collections.emptyList();
    private Map<String, List<ParsedTag>> jsonGroups = new HashMap<>();
    private List<ParsedTag> allSimpleTags = Collections.emptyList();

    // Cờ để kiểm soát việc populate value khi key thay đổi tự động
    private boolean isAutoSelectingKey = false;


    private static class ParsedTag {
        final String rawString;
        final TagModel model;

        public ParsedTag(String rawString, TagModel model) {
            this.rawString = rawString;
            this.model = model;
        }
    }


    @FXML
    public void initialize() {
        setupLocalization();

        simpleTagRadio.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            simpleTagPane.setVisible(isSelected);
            simpleTagPane.setManaged(isSelected);
            jsonTagPane.setVisible(!isSelected);
            jsonTagPane.setManaged(!isSelected);

            Platform.runLater(() -> {
                if (isSelected) {
                    simpleNameField.requestFocus();
                } else {
                    keyField.requestFocus();
                }
            });
        });

        simpleTagPane.setVisible(false);
        simpleTagPane.setManaged(false);
        jsonTagPane.setVisible(true);
        jsonTagPane.setManaged(true);
        jsonTagRadio.setSelected(true);

        // Listener khi người dùng *CLICK* chọn Key suggestion
        keySuggestionGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (isAutoSelectingKey) {
                // Nếu là do tự động chọn, chỉ populate value dựa trên key mới
                populateValues(newToggle, ""); // Bỏ qua value hiện tại
            } else if (newToggle != null) {
                // Nếu là do user click
                valueSuggestionGroup.selectToggle(null); // Bỏ chọn value cũ
                String selectedKey = (String) newToggle.getUserData();
                keyField.setText(selectedKey); // Điền key đã chọn vào keyField
                Platform.runLater(() -> valueField.requestFocus()); // Chuyển focus xuống valueField
                populateValues(newToggle, valueField.getText()); // Populate value dựa trên key và value hiện tại
            } else {
                // Nếu không có key nào được chọn (ví dụ: key bị lọc hết)
                populateValues(null, valueField.getText()); // Clear value suggestions
            }
        });


        keyField.textProperty().addListener((obs, oldVal, newVal) -> {
            isAutoSelectingKey = true; // Bật cờ trước khi populate keys
            populateKeys(newVal); // Lọc Keys (và có thể tự động chọn key đầu tiên)
            isAutoSelectingKey = false; // Tắt cờ sau khi xong
            // populateValues sẽ được gọi bởi listener của keySuggestionGroup nếu có key được auto-select
            if (keySuggestionGroup.getSelectedToggle() == null) {
                // Nếu không có key nào được auto-select (vì không có gợi ý), clear value suggestions
                populateValues(null, "");
            }
            updateContainerVisibility();
        });


        valueField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Chỉ populate value dựa trên key đang được chọn và text value mới
            populateValues(keySuggestionGroup.getSelectedToggle(), newVal);
            updateContainerVisibility();
        });

        simpleNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            populateSimpleTags(newVal);
            updateContainerVisibility();
        });

        copyButton.setOnAction(e -> handleCopyAction());

        // Xử lý Tab từ Key Field
        keyField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.TAB && !event.isShiftDown()) {
                valueField.requestFocus();
                // Bôi đen toàn bộ text trong value field
                Platform.runLater(() -> valueField.selectAll());
                event.consume();
            }
        });
    }

    private void setupLocalization() {
        I18nManager i18n = I18nManager.getInstance();
        jsonTagRadio.setText(i18n.getString("addTagDialog", "labelJson"));
        contentLabel.setText(i18n.getString("addTagDialog", "contentLabel"));
        simpleNameField.setPromptText(i18n.getString("addTagDialog", "suggestionSimplePrompt"));
        keyLabel.setText(i18n.getString("addTagDialog", "keyLabel"));
        keyField.setPromptText(i18n.getString("addTagDialog", "suggestionKeyPrompt"));
        valueLabel.setText(i18n.getString("addTagDialog", "valueLabel"));
        valueField.setPromptText(i18n.getString("addTagDialog", "suggestionValuePrompt"));
        suggestionKeyLabel.setText(i18n.getString("addTagDialog", "suggestionKeyLabel"));
        suggestionValueLabel.setText(i18n.getString("addTagDialog", "suggestionValueLabel"));
        copyIdField.setPromptText(i18n.getString("addTagDialog", "copyIdPrompt"));
        copyButton.setText(i18n.getString("addTagDialog", "copyButton"));
        cancelButton.setText(i18n.getString("addTagDialog", "cancelButton"));
        okButton.setText(i18n.getString("addTagDialog", "okButton"));
    }

    public void setContext(SuggestionContext context, ItemRepository itemRepository) {
        this.currentContext = context;
        this.itemRepository = itemRepository;

        Platform.runLater(() -> {
            I18nManager i18n = I18nManager.getInstance();
            Stage stage = (Stage) dialogStage.getScene().getWindow();
            String title;
            String simpleRadioText;
            String simpleSuggestionLabelText;

            switch (context) {
                case STUDIO:
                    title = i18n.getString("addTagDialog", "addStudioTitle");
                    simpleRadioText = i18n.getString("addTagDialog", "labelSimpleStudio");
                    simpleSuggestionLabelText = i18n.getString("addTagDialog", "suggestionSimpleStudioLabel");
                    break;
                case PEOPLE:
                    title = i18n.getString("addTagDialog", "addPeopleTitle");
                    simpleRadioText = i18n.getString("addTagDialog", "labelSimplePeople");
                    simpleSuggestionLabelText = i18n.getString("addTagDialog", "suggestionSimplePeopleLabel");
                    break;
                case GENRE:
                    title = i18n.getString("addTagDialog", "addGenreTitle");
                    simpleRadioText = i18n.getString("addTagDialog", "labelSimpleGenre");
                    simpleSuggestionLabelText = i18n.getString("addTagDialog", "suggestionSimpleGenreLabel");
                    break;
                case TAG:
                default:
                    title = i18n.getString("addTagDialog", "addTagTitle");
                    simpleRadioText = i18n.getString("addTagDialog", "labelSimple");
                    simpleSuggestionLabelText = i18n.getString("addTagDialog", "suggestionSimpleLabel");
                    break;
            }

            stage.setTitle(title);
            titleLabel.setText(title);
            simpleTagRadio.setText(simpleRadioText);

            if (suggestionSimpleContainer != null && !suggestionSimpleContainer.getChildren().isEmpty() && suggestionSimpleContainer.getChildren().get(0) instanceof Label) {
                Label suggestionLabel = (Label) suggestionSimpleContainer.getChildren().get(0);
                suggestionLabel.setText(simpleSuggestionLabelText);
            }


            jsonTagRadio.setVisible(true);
            jsonTagRadio.setManaged(true);
            simpleTagRadio.setVisible(true);
            simpleTagRadio.setManaged(true);

            loadSuggestedTags();

            if (jsonTagRadio.isSelected()) {
                Platform.runLater(() -> keyField.requestFocus());
            } else {
                Platform.runLater(() -> simpleNameField.requestFocus());
            }
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
                    case GENRE:
                        rawNames.addAll(itemRepository.getGenreSuggestions(embyService.getApiClient())
                                .stream().map(SuggestionItemModel::getName).collect(Collectors.toList()));
                        break;
                }

            } catch (Exception e) {
                System.err.println("Lỗi khi lấy gợi ý từ server: " + e.getMessage());
                e.printStackTrace();
            }

            Platform.runLater(() -> {
                this.allRawNames = rawNames;
                prepareSuggestionLists();

                String keySearch = keyField.getText();
                String valueSearch = valueField.getText(); // Giữ lại value search ban đầu nếu có
                String simpleSearch = simpleNameField.getText();

                isAutoSelectingKey = true; // Bật cờ trước khi populate keys ban đầu
                populateKeys(keySearch);
                isAutoSelectingKey = false; // Tắt cờ
                populateSimpleTags(simpleSearch);
                // Populate values dựa trên key đã auto-select (nếu có) và value search ban đầu
                populateValues(keySuggestionGroup.getSelectedToggle(), valueSearch);


                updateContainerVisibility();
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
            // Lỗi đã được log
        }
        return Collections.emptyList();
    }

    private void prepareSuggestionLists() {
        if (allRawNames == null || allRawNames.isEmpty()) {
            jsonGroups.clear();
            allSimpleTags = Collections.emptyList();
            return;
        }

        List<ParsedTag> parsedTags = allRawNames.stream()
                .map(raw -> new ParsedTag(raw, TagModel.parse(raw)))
                .collect(Collectors.toList());

        jsonGroups = parsedTags.stream()
                .filter(pt -> pt.model.isJson())
                .collect(Collectors.groupingBy(pt -> pt.model.getKey()));

        allSimpleTags = parsedTags.stream()
                .filter(pt -> !pt.model.isJson())
                .collect(Collectors.toList());
    }


    private void updateContainerVisibility() {
        Platform.runLater(() -> {
            suggestionJsonContainer.setVisible(!jsonGroups.isEmpty());
            suggestionJsonContainer.setManaged(!jsonGroups.isEmpty());

            boolean hasOriginalSimpleSuggestions = allSimpleTags != null && !allSimpleTags.isEmpty();
            suggestionSimpleContainer.setVisible(hasOriginalSimpleSuggestions);
            suggestionSimpleContainer.setManaged(hasOriginalSimpleSuggestions);
        });
    }

    private void populateKeys(String searchText) {
        suggestionKeysPane.getChildren().clear();
        Toggle oldSelectedToggle = keySuggestionGroup.getSelectedToggle(); // Lưu lại lựa chọn cũ
        keySuggestionGroup.getToggles().clear();

        Set<String> allKeys = jsonGroups.keySet();

        List<String> filteredKeys;
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredKeys = new ArrayList<>(allKeys);
        } else {
            String lowerCaseSearchText = searchText.trim().toLowerCase(); // Đảm bảo case-insensitive
            filteredKeys = allKeys.stream()
                    .filter(key -> key.toLowerCase().contains(lowerCaseSearchText)) // Đảm bảo case-insensitive
                    .collect(Collectors.toList());
        }

        Collections.sort(filteredKeys, String.CASE_INSENSITIVE_ORDER);

        ToggleButton firstButton = null;
        for (String key : filteredKeys) {
            ToggleButton chip = new ToggleButton(key);
            chip.setToggleGroup(keySuggestionGroup);
            chip.getStyleClass().add("suggestion-key-button");
            chip.setUserData(key);
            suggestionKeysPane.getChildren().add(chip);
            if (firstButton == null) {
                firstButton = chip;
            }
        }

        // Tự động chọn key đầu tiên nếu danh sách lọc không rỗng
        if (firstButton != null) {
            // Chỉ gọi selectToggle nếu lựa chọn hiện tại khác null VÀ khác với cái đầu tiên
            // HOẶC nếu lựa chọn hiện tại là null
            Toggle currentSelection = keySuggestionGroup.getSelectedToggle();
            if (currentSelection == null || !currentSelection.getUserData().equals(firstButton.getUserData())) {
                keySuggestionGroup.selectToggle(firstButton); // -> Sẽ trigger listener của keySuggestionGroup
            }
        } else {
            // Nếu không có key nào khớp -> bỏ chọn key hiện tại (nếu có)
            keySuggestionGroup.selectToggle(null); // -> Sẽ trigger listener của keySuggestionGroup
        }
    }


    private void populateValues(Toggle selectedKeyToggle, String searchText) {
        suggestionValuesPane.getChildren().clear();
        valueSuggestionGroup.getToggles().clear();

        if (selectedKeyToggle == null) {
            return;
        }

        String selectedKey = (String) selectedKeyToggle.getUserData();
        List<ParsedTag> tagsInGroup = jsonGroups.get(selectedKey);

        if (tagsInGroup == null) return;

        final List<ParsedTag> filteredTags;
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredTags = tagsInGroup;
        } else {
            String lowerCaseSearchText = searchText.trim().toLowerCase(); // Đảm bảo case-insensitive
            filteredTags = tagsInGroup.stream()
                    .filter(pt -> pt.model.getValue().toLowerCase().contains(lowerCaseSearchText)) // Đảm bảo case-insensitive
                    .collect(Collectors.toList());
        }

        filteredTags.sort((pt1, pt2) -> pt1.model.getValue().compareToIgnoreCase(pt2.model.getValue()));


        for (ParsedTag pt : filteredTags) {
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

    private void populateSimpleTags(String searchText) {
        suggestionSimplePane.getChildren().clear();

        final List<ParsedTag> filteredTags;
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredTags = allSimpleTags;
        } else {
            String lowerCaseSearchText = searchText.trim().toLowerCase(); // Đảm bảo case-insensitive
            filteredTags = allSimpleTags.stream()
                    .filter(pt -> pt.model.getDisplayName().toLowerCase().contains(lowerCaseSearchText)) // Đảm bảo case-insensitive
                    .collect(Collectors.toList());
        }

        filteredTags.sort((pt1, pt2) -> pt1.model.getDisplayName().compareToIgnoreCase(pt2.model.getDisplayName()));


        for (ParsedTag pt : filteredTags) {
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

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public TagModel getResultTag() {
        return resultTag;
    }

    public StringProperty copyTriggeredIdProperty() {
        return copyTriggeredId;
    }

    @FXML
    private void handleOk() {
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
            System.err.println(I18nManager.getInstance().getString("addTagDialog", "errorInvalid"));
            if (simpleTagRadio.isSelected()) {
                highlightField(simpleNameField);
            } else {
                if (keyField.getText().trim().isEmpty()) {
                    highlightField(keyField);
                } else if (valueField.getText().trim().isEmpty()) {
                    highlightField(valueField);
                }
            }
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    @FXML
    private void handleCopyAction() {
        String id = copyIdField.getText();
        if (id != null && !id.trim().isEmpty()) {
            this.copyTriggeredId.set(id.trim());
            dialogStage.close();
        } else {
            copyIdField.requestFocus();
            highlightField(copyIdField);
        }
    }

    @FXML
    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            handleCancel();
        } else if (event.getCode() == KeyCode.ENTER) {
            handleEnterPressed();
        }
    }

    private void handleEnterPressed() {
        if (simpleTagRadio.isSelected()) {
            if (simpleNameField.getText().trim().isEmpty()) {
                handleCancel();
            } else {
                handleOk();
            }
        } else {
            String key = keyField.getText().trim();
            String value = valueField.getText().trim();

            if (key.isEmpty()) {
                handleCancel();
            } else if (!key.isEmpty() && !value.isEmpty()) {
                handleOk();
            } else if (!key.isEmpty() && value.isEmpty()) {
                boolean keyExists = jsonGroups.keySet().stream()
                        .anyMatch(k -> k.equalsIgnoreCase(key)); // Đảm bảo case-insensitive

                if (keyExists) {
                    valueField.requestFocus();
                    highlightField(valueField);
                } else {
                    simpleTagRadio.setSelected(true);
                    simpleNameField.setText(keyField.getText()); // Dùng text gốc, không trim()
                    handleOk();
                }
            }
        }
    }

    private void highlightField(TextField field) {
        Platform.runLater(() -> {
            field.getStyleClass().add("validation-error");
            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> field.getStyleClass().remove("validation-error"));
            pause.play();
        });
    }
}