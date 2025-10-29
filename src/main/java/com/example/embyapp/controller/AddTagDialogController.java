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
import javafx.scene.Node;
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
 * (CẬP NHẬT) Thêm điều hướng bằng phím mũi tên Lên/Xuống cho các suggestion chip.
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

    // Flag to prevent listener loops during programmatic changes
    private boolean isUpdatingProgrammatically = false;

    // (*** MỚI: Biến quản lý focus bằng phím mũi tên ***)
    private int focusedKeyIndex = -1;
    private int focusedValueIndex = -1;
    private int focusedSimpleIndex = -1;
    // Tên của class CSS mới
    private static final String FOCUSED_CHIP_STYLE_CLASS = "focused-chip";


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

        // Radio button logic (unchanged)
        simpleTagRadio.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            isUpdatingProgrammatically = true; // Prevent potential loops
            simpleTagPane.setVisible(isSelected);
            simpleTagPane.setManaged(isSelected);
            jsonTagPane.setVisible(!isSelected);
            jsonTagPane.setManaged(!isSelected);
            isUpdatingProgrammatically = false;

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


        // Listener for key text field changes - filters suggestions
        keyField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingProgrammatically) return;
            focusedKeyIndex = -1; // (*** MỚI: Reset focus khi gõ text ***)
            populateKeys(newVal); // Filter keys and highlight best match
            populateSimpleTags(newVal); // Filter simple tags
            updateContainerVisibility();
            // populateValues is called within populateKeys based on highlighted key
        });

        // Listener for manual key suggestion selection (user click)
        keySuggestionGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (isUpdatingProgrammatically) return; // Ignore programmatic changes

            Platform.runLater(() -> { // Schedule UI updates safely
                if (newToggle != null) {
                    isUpdatingProgrammatically = true; // Prevent text change listener loop
                    String selectedKey = (String) newToggle.getUserData();
                    keyField.setText(selectedKey); // Update text field to match selection
                    populateValues(newToggle, "");  // Show values for selected key
                    valueField.requestFocus();     // Focus value field
                    valueField.selectAll();        // Select text

                    // (*** MỚI: Cập nhật focusedKeyIndex khi click ***)
                    focusedKeyIndex = suggestionKeysPane.getChildren().indexOf(newToggle);
                    // Bỏ focus style cũ, set style mới
                    clearAndSetChipFocus(suggestionKeysPane, focusedKeyIndex);

                    isUpdatingProgrammatically = false;
                } else if (oldToggle != null) {
                    // When user deselects a key by clicking it again
                    if (oldToggle.getUserData() != null) {
                        populateValues(null, valueField.getText()); // Clear value suggestions
                    }
                }
            });
        });


        // (*** SỬA ĐỔI: Chuyển sang EventFilter để xử lý phím Tab ***)
        keyField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            handleFieldKeyPress(e, keyField, suggestionKeysPane, keySuggestionGroup);
        });

        // (*** CẬP NHẬT 6 - Bỏ qua, đã xử lý bằng handleFieldKeyPress)
        keyField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            // Check if focus was lost AND no key is currently selected by the user
            if (wasFocused && !isFocused && keySuggestionGroup.getSelectedToggle() == null) {
                String currentKeyText = keyField.getText().trim();
                if (!currentKeyText.isEmpty()) {
                    Toggle firstMatchingToggle = null;
                    String correctCaseKey = null;

                    // Find the first suggestion matching the typed text (ignore case)
                    for (Toggle toggle : keySuggestionGroup.getToggles()) {
                        if (toggle instanceof Node && toggle.getUserData() instanceof String) {
                            String toggleKey = (String) toggle.getUserData();
                            if (toggleKey.equalsIgnoreCase(currentKeyText)) {
                                firstMatchingToggle = toggle;
                                correctCaseKey = toggleKey; // Store the key with correct casing
                                break;
                            }
                        }
                    }

                    // If a matching suggestion was found
                    if (firstMatchingToggle != null && correctCaseKey != null) {
                        final Toggle finalToggle = firstMatchingToggle;
                        final String finalCorrectKey = correctCaseKey;

                        // Perform UI updates on the JavaFX Application Thread
                        Platform.runLater(() -> {
                            isUpdatingProgrammatically = true; // Prevent listeners from firing recursively

                            // 1. Auto-complete the text field with correct casing
                            keyField.setText(finalCorrectKey);

                            // 2. Update highlighting (clear others, highlight the match)
                            clearKeyHighlightAndSelection();
                            if (finalToggle instanceof Node) {
                                ((Node) finalToggle).getStyleClass().add("filtering-key");
                            }

                            // 3. Populate value suggestions based on the matched key
                            populateValues(finalToggle, "");

                            // 4. Move focus to the value field and select its content
                            // (*** SỬA ĐỔI: Không tự động chuyển focus, chỉ chuẩn bị ***)
                            // valueField.requestFocus();
                            // valueField.selectAll();
                            // (*** CẬP NHẬT: Gán focusedKeyIndex ***)
                            focusedKeyIndex = suggestionKeysPane.getChildren().indexOf(finalToggle);


                            // DO NOT call selectToggle() here to avoid focus issues
                            isUpdatingProgrammatically = false; // Allow listeners again
                        });
                    }
                    // If no match found, do nothing, keep user's text
                }
            }
        });


        // Listener for value text field changes - filters value suggestions
        valueField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingProgrammatically) return;
            focusedValueIndex = -1; // (*** MỚI: Reset focus khi gõ text ***)
            Toggle filteringKeyToggle = findFilteringKeyToggle(); // Find highlighted key first
            Toggle selectedKeyToggle = keySuggestionGroup.getSelectedToggle(); // Check if user manually selected one
            // Prioritize selected key, fallback to highlighted key for filtering values
            populateValues(selectedKeyToggle != null ? selectedKeyToggle : filteringKeyToggle, newVal);
            updateContainerVisibility();
        });

        // (*** MỚI: Thêm EventFilter cho valueField ***)
        valueField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            handleFieldKeyPress(e, valueField, suggestionValuesPane, valueSuggestionGroup);
        });


        // Listener for simple name text field changes - filters simple suggestions
        simpleNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingProgrammatically) return;
            focusedSimpleIndex = -1; // (*** MỚI: Reset focus khi gõ text ***)
            populateSimpleTags(newVal);
            updateContainerVisibility();
        });

        // (*** MỚI: Thêm EventFilter cho simpleNameField ***)
        simpleNameField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            handleFieldKeyPress(e, simpleNameField, suggestionSimplePane, valueSuggestionGroup); // Dùng chung group
        });


        // Copy button action
        copyButton.setOnAction(e -> handleCopyAction());

        // (*** XÓA: keyField.setOnKeyPressed - đã chuyển sang EventFilter ***)
    }

    // (*** HÀM MỚI: Xử lý phím cho cả 3 text field ***)
    private void handleFieldKeyPress(KeyEvent event, TextField field, FlowPane suggestionPane, ToggleGroup suggestionGroup) {
        KeyCode code = event.getCode();

        if (code == KeyCode.UP || code == KeyCode.DOWN) {
            event.consume(); // Ngăn con trỏ di chuyển trong text field

            int currentIndex = -1;
            if (field == keyField) currentIndex = focusedKeyIndex;
            else if (field == valueField) currentIndex = focusedValueIndex;
            else if (field == simpleNameField) currentIndex = focusedSimpleIndex;

            int paneSize = suggestionPane.getChildren().size();
            if (paneSize == 0) return; // Không có gì để focus

            int nextIndex = currentIndex;
            if (code == KeyCode.DOWN) {
                nextIndex++;
                if (nextIndex >= paneSize) nextIndex = 0; // Quay vòng
            } else { // KeyCode.UP
                nextIndex--;
                if (nextIndex < 0) nextIndex = paneSize - 1; // Quay vòng
            }

            // Cập nhật UI
            setChipFocus(field, suggestionPane, suggestionGroup, nextIndex);

        } else if (code == KeyCode.TAB && !event.isShiftDown()) {
            event.consume(); // Luôn ngăn Tab mặc định

            // Logic chuyển focus khi nhấn Tab
            if (field == keyField) {
                // (Optional: Nếu muốn auto-select chip đầu tiên nếu chưa chọn gì)
                // if (focusedKeyIndex == -1 && !suggestionKeysPane.getChildren().isEmpty()) {
                //     setChipFocus(keyField, suggestionKeysPane, keySuggestionGroup, 0);
                // }
                valueField.requestFocus();
            } else if (field == valueField) {
                okButton.requestFocus();
            } else if (field == simpleNameField) {
                okButton.requestFocus();
            }
        }
    }

    // (*** HÀM MỚI: Cập nhật focus của chip và text field ***)
    private void setChipFocus(TextField field, FlowPane pane, ToggleGroup group, int index) {
        if (index < 0 || index >= pane.getChildren().size()) {
            // (Optional: Xóa focus nếu index-out-of-bounds, nhưng hiện tại đang quay vòng nên không cần)
            return;
        }

        // 1. Xóa style focus cũ
        clearAndSetChipFocus(pane, index);

        // 2. Lấy chip (Toggle)
        Node chipNode = pane.getChildren().get(index);
        if (!(chipNode instanceof ToggleButton)) return;

        ToggleButton chip = (ToggleButton) chipNode;

        // 3. Cập nhật chỉ số focus
        if (field == keyField) focusedKeyIndex = index;
        else if (field == valueField) focusedValueIndex = index;
        else if (field == simpleNameField) focusedSimpleIndex = index;

        // 4. Lấy text từ chip
        String chipText = "";
        if (field == keyField) { // Key pane lưu text trong UserData
            chipText = (String) chip.getUserData();
        } else { // Value và Simple pane lưu text trong rawString (cũng trong UserData)
            TagModel model = TagModel.parse((String) chip.getUserData());
            if (field == valueField) {
                chipText = model.getValue();
            } else { // simpleNameField
                chipText = model.getDisplayName();
            }
        }

        // 5. Cập nhật TextField
        isUpdatingProgrammatically = true;
        field.setText(chipText);
        field.selectAll(); // Bôi đen
        isUpdatingProgrammatically = false;

        // 6. (Rất quan trọng) Nếu là keyField, cập nhật value pane
        if (field == keyField) {
            // Không select toggle (gây lỗi focus), chỉ truyền nó vào để populateValues
            populateValues(chip, valueField.getText());
        }
    }

    // (*** HÀM MỚI: Xóa style và set style mới ***)
    private void clearAndSetChipFocus(FlowPane pane, int indexToFocus) {
        for (int i = 0; i < pane.getChildren().size(); i++) {
            Node child = pane.getChildren().get(i);
            child.getStyleClass().remove(FOCUSED_CHIP_STYLE_CLASS);
            if (i == indexToFocus) {
                child.getStyleClass().add(FOCUSED_CHIP_STYLE_CLASS);
            }
        }
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

    /**
     * Sets the context (Tag, Studio, etc.) and loads initial suggestions.
     * @param context The type of item being added.
     * @param itemRepository Repository to fetch suggestions.
     */
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

            if (suggestionSimpleContainer != null && suggestionSimpleLabel != null) {
                suggestionSimpleLabel.setText(simpleSuggestionLabelText);
            } else if (suggestionSimpleContainer != null && !suggestionSimpleContainer.getChildren().isEmpty() && suggestionSimpleContainer.getChildren().get(0) instanceof Label) {
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
                    System.err.println("Not logged in, cannot fetch suggestions.");
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
                System.err.println("Error fetching suggestions from server: " + e.getMessage());
                e.printStackTrace();
            }

            Platform.runLater(() -> {
                this.allRawNames = rawNames;
                prepareSuggestionLists();
                String keySearch = keyField.getText();
                String simpleSearch = simpleNameField.getText();
                isUpdatingProgrammatically = true; // Prevent listener loops during initial population
                populateKeys(keySearch);
                populateSimpleTags(simpleSearch);
                isUpdatingProgrammatically = false;
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
            System.err.println("Error fetching tags: " + e.getMessage());
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
            boolean hasJsonGroups = !jsonGroups.isEmpty();
            if (suggestionJsonContainer != null) {
                suggestionJsonContainer.setVisible(hasJsonGroups);
                suggestionJsonContainer.setManaged(hasJsonGroups);
            }
            boolean hasSimpleTags = allSimpleTags != null && !allSimpleTags.isEmpty();
            if (suggestionSimpleContainer != null) {
                suggestionSimpleContainer.setVisible(hasSimpleTags);
                suggestionSimpleContainer.setManaged(hasSimpleTags);
            }
        });
    }

    /**
     * Populates the key suggestions FlowPane based on search text,
     * highlights the best match, and triggers value population for the highlighted key.
     * @param searchText Text to filter keys.
     */
    private void populateKeys(String searchText) {
        suggestionKeysPane.getChildren().clear();
        // Do not clear selection here, let user interaction or focus listener handle it
        // keySuggestionGroup.selectToggle(null);

        Set<String> allKeys = jsonGroups.keySet();
        List<String> filteredKeys;

        if (searchText == null || searchText.trim().isEmpty()) {
            filteredKeys = new ArrayList<>(allKeys);
        } else {
            String lowerCaseSearchText = searchText.trim().toLowerCase();
            filteredKeys = allKeys.stream()
                    .filter(key -> key.toLowerCase().contains(lowerCaseSearchText))
                    .collect(Collectors.toList());
        }
        Collections.sort(filteredKeys, String.CASE_INSENSITIVE_ORDER);

        ToggleButton firstButton = null;
        // Keep track of the currently selected toggle (if any) to preserve selection if possible
        Toggle previouslySelected = keySuggestionGroup.getSelectedToggle();

        // Clear existing toggles from the group *before* adding new ones
        keySuggestionGroup.getToggles().clear();

        for (String key : filteredKeys) {
            ToggleButton chip = new ToggleButton(key);
            // Re-add to the group after clearing
            chip.setToggleGroup(keySuggestionGroup);
            chip.getStyleClass().add("suggestion-key-button");
            chip.setUserData(key);
            suggestionKeysPane.getChildren().add(chip);
            if (firstButton == null) {
                firstButton = chip;
            }
            // If this key was previously selected, re-select its new instance
            if (previouslySelected != null && key.equals(previouslySelected.getUserData())) {
                chip.setSelected(true); // This might trigger the selectedToggle listener
            }
        }

        // Clear previous highlight and highlight the best match (first button)
        clearKeyHighlightOnly(); // Use a specific method to avoid deselecting
        if (firstButton != null) {
            // (*** SỬA ĐỔI: Không tự động highlight "filtering-key" nữa ***)
            // (*** Thay vào đó, nó sẽ được highlight bởi setChipFocus ***)
            // firstButton.getStyleClass().add("filtering-key");

            // Populate values based on the highlighted key only if NO key is manually selected
            if (keySuggestionGroup.getSelectedToggle() == null) {
                populateValues(firstButton, valueField.getText());
            } else {
                // If a key IS selected, populate values based on selection instead
                populateValues(keySuggestionGroup.getSelectedToggle(), valueField.getText());
            }
        } else {
            // No keys match, clear value suggestions
            populateValues(null, valueField.getText());
        }
    }

    /**
     * Finds the ToggleButton currently marked with the 'filtering-key' style.
     * @return The highlighted Toggle, or null if none.
     */
    private Toggle findFilteringKeyToggle() {
        // (*** SỬA ĐỔI: Tìm bằng "focused-chip" thay vì "filtering-key" ***)
        for (Node node : suggestionKeysPane.getChildren()) {
            if (node.getStyleClass().contains(FOCUSED_CHIP_STYLE_CLASS)) {
                if (node instanceof Toggle) {
                    return (Toggle) node;
                }
            }
        }

        // Fallback: nếu không có gì được focus, dùng key đầu tiên
        if (!suggestionKeysPane.getChildren().isEmpty()) {
            Node firstChild = suggestionKeysPane.getChildren().get(0);
            if (firstChild instanceof Toggle) {
                return (Toggle) firstChild;
            }
        }

        return null;
    }

    /**
     * Populates the value suggestions FlowPane based on the associated key and search text.
     * @param associatedKeyToggle The Toggle representing the key (selected or highlighted).
     * @param searchText Text to filter values.
     */
    private void populateValues(Toggle associatedKeyToggle, String searchText) {
        suggestionValuesPane.getChildren().clear();
        valueSuggestionGroup.getToggles().clear();

        if (associatedKeyToggle == null || associatedKeyToggle.getUserData() == null) {
            return; // No key to base values on
        }

        String associatedKey = (String) associatedKeyToggle.getUserData();
        List<ParsedTag> tagsInGroup = jsonGroups.get(associatedKey);

        if (tagsInGroup == null) return;

        final List<ParsedTag> filteredTags;
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredTags = tagsInGroup;
        } else {
            String lowerCaseSearchText = searchText.trim().toLowerCase();
            filteredTags = tagsInGroup.stream()
                    .filter(pt -> pt.model.getValue().toLowerCase().contains(lowerCaseSearchText))
                    .collect(Collectors.toList());
        }
        filteredTags.sort((pt1, pt2) -> pt1.model.getValue().compareToIgnoreCase(pt2.model.getValue()));

        for (ParsedTag pt : filteredTags) {
            ToggleButton chip = new ToggleButton(pt.model.getValue());
            chip.setToggleGroup(valueSuggestionGroup);
            chip.getStyleClass().addAll("suggested-tag-button", "tag-view-json");
            chip.setUserData(pt.rawString);
            chip.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isUpdatingProgrammatically) return;
                if (isSelected) {
                    fillFormFromSuggestion((String) chip.getUserData());

                    // (*** MỚI: Cập nhật focusedValueIndex khi click ***)
                    focusedValueIndex = suggestionValuesPane.getChildren().indexOf(chip);
                    clearAndSetChipFocus(suggestionValuesPane, focusedValueIndex);

                    Platform.runLater(() -> okButton.requestFocus());
                }
            });
            suggestionValuesPane.getChildren().add(chip);
        }
    }

    /**
     * Populates the simple tag suggestions FlowPane based on search text.
     * @param searchText Text to filter simple tags.
     */
    private void populateSimpleTags(String searchText) {
        suggestionSimplePane.getChildren().clear();
        // Assuming simple tags use the same valueSuggestionGroup for selection logic
        // valueSuggestionGroup.getToggles().clear(); // Clear might interfere if JSON values are also shown

        final List<ParsedTag> filteredTags;
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredTags = allSimpleTags;
        } else {
            String lowerCaseSearchText = searchText.trim().toLowerCase();
            filteredTags = allSimpleTags.stream()
                    .filter(pt -> pt.model.getDisplayName().toLowerCase().contains(lowerCaseSearchText))
                    .collect(Collectors.toList());
        }
        filteredTags.sort((pt1, pt2) -> pt1.model.getDisplayName().compareToIgnoreCase(pt2.model.getDisplayName()));

        for (ParsedTag pt : filteredTags) {
            ToggleButton chip = new ToggleButton(pt.model.getDisplayName());
            chip.setToggleGroup(valueSuggestionGroup); // Use the common group
            chip.getStyleClass().add("suggested-tag-button");
            if(pt.model.isJson()) chip.getStyleClass().add("tag-view-json"); // Add specific style if needed
            chip.setUserData(pt.rawString);
            chip.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isUpdatingProgrammatically) return;
                if (isSelected) {
                    clearKeyHighlightAndSelection(); // Clear key selection when simple tag is chosen
                    fillFormFromSuggestion((String) chip.getUserData());

                    // (*** MỚI: Cập nhật focusedSimpleIndex khi click ***)
                    focusedSimpleIndex = suggestionSimplePane.getChildren().indexOf(chip);
                    clearAndSetChipFocus(suggestionSimplePane, focusedSimpleIndex);

                    Platform.runLater(() -> okButton.requestFocus());
                }
            });
            suggestionSimplePane.getChildren().add(chip);
        }
    }

    /** Clears both selection and highlighting from key suggestions. */
    private void clearKeyHighlightAndSelection() {
        isUpdatingProgrammatically = true;
        if (keySuggestionGroup.getSelectedToggle() != null) {
            keySuggestionGroup.selectToggle(null);
        }
        clearKeyHighlightOnly();
        isUpdatingProgrammatically = false;
    }

    /** Clears only the highlighting ('filtering-key' style) from key suggestions. */
    private void clearKeyHighlightOnly() {
        // (*** SỬA ĐỔI: Xóa cả 2 class "filtering-key" và "focused-chip" ***)
        for (Node node : suggestionKeysPane.getChildren()) {
            node.getStyleClass().removeAll("filtering-key", FOCUSED_CHIP_STYLE_CLASS);
        }
    }


    /**
     * Fills the input fields based on a selected suggestion string.
     * @param selectedTagName The raw string representation of the selected tag.
     */
    private void fillFormFromSuggestion(String selectedTagName) {
        isUpdatingProgrammatically = true;
        // (*** MỚI: Reset tất cả các chỉ số focus ***)
        focusedKeyIndex = -1;
        focusedValueIndex = -1;
        focusedSimpleIndex = -1;

        TagModel selectedTag = TagModel.parse(selectedTagName);
        if (selectedTag.isJson()) {
            jsonTagRadio.setSelected(true); // Ensure correct radio is selected
            keyField.setText(selectedTag.getKey() != null ? selectedTag.getKey() : "");
            valueField.setText(selectedTag.getValue() != null ? selectedTag.getValue() : "");
            simpleNameField.clear();
            // Highlight the corresponding key without selecting it programmatically
            Platform.runLater(() -> highlightMatchingKey(selectedTag.getKey()));
        } else {
            simpleTagRadio.setSelected(true); // Ensure correct radio is selected
            simpleNameField.setText(selectedTag.getDisplayName());
            keyField.clear();
            valueField.clear();
            // Clear any key highlight/selection
            Platform.runLater(this::clearKeyHighlightAndSelection);
        }
        isUpdatingProgrammatically = false;
    }

    /**
     * Highlights the key suggestion button that matches the given key string.
     * @param keyToHighlight The key string to match.
     */
    private void highlightMatchingKey(String keyToHighlight) {
        clearKeyHighlightOnly(); // Clear previous highlights first
        if (keyToHighlight == null) return;
        for (Toggle toggle : keySuggestionGroup.getToggles()) {
            // Check if the toggle is a Node and its user data matches the key
            if (toggle instanceof Node && keyToHighlight.equals(toggle.getUserData())) {
                // (*** SỬA ĐỔI: Dùng "filtering-key" khi click, "focused-chip" khi dùng phím ***)
                ((Node)toggle).getStyleClass().add("filtering-key");
                // Also ensure values are populated for this newly highlighted key
                // but only if no key is currently *selected* by the user
                if(keySuggestionGroup.getSelectedToggle() == null){
                    populateValues(toggle, valueField.getText());
                }
                break; // Stop after finding the first match
            }
        }
    }

    /**
     * Sets the stage for this dialog.
     * @param dialogStage The stage.
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Gets the TagModel result created by the dialog.
     * @return The resulting TagModel, or null if cancelled or invalid.
     */
    public TagModel getResultTag() {
        return resultTag;
    }

    /**
     * Gets the property indicating if the copy action was triggered.
     * @return The StringProperty containing the source item ID if copy was triggered, otherwise null.
     */
    public StringProperty copyTriggeredIdProperty() {
        return copyTriggeredId;
    }

    /** Handles the OK button action. Validates input and closes the dialog if valid. */
    @FXML
    private void handleOk() {
        resultTag = null;
        if (simpleTagRadio.isSelected()) {
            String simpleName = simpleNameField.getText();
            if (simpleName != null && !simpleName.trim().isEmpty()) {
                resultTag = new TagModel(simpleName.trim());
            }
        } else { // JSON tag is selected
            String key = keyField.getText();
            String value = valueField.getText();
            if (key != null && !key.trim().isEmpty() && value != null && !value.trim().isEmpty()) {
                resultTag = new TagModel(key.trim(), value.trim());
            }
        }

        if (resultTag != null) {
            dialogStage.close();
        } else {
            // Input is invalid, provide feedback
            System.err.println(I18nManager.getInstance().getString("addTagDialog", "errorInvalid"));
            highlightInvalidField();
        }
    }

    /** Handles the Cancel button action. Closes the dialog without result. */
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    /** Handles the Copy button action. Sets the copy ID and closes the dialog if ID is valid. */
    @FXML
    private void handleCopyAction() {
        String id = copyIdField.getText();
        if (id != null && !id.trim().isEmpty()) {
            this.copyTriggeredId.set(id.trim());
            dialogStage.close();
        } else {
            // ID field is empty, highlight it
            copyIdField.requestFocus();
            highlightField(copyIdField);
        }
    }

    /** Handles key presses globally within the dialog (Escape and Enter). */
    @FXML
    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            handleCancel();
        } else if (event.getCode() == KeyCode.ENTER) {
            // (*** SỬA ĐỔI: Không xử lý Enter ở đây nữa, để handleFieldKeyPress xử lý ***)
            // handleEnterPressed();
        }
    }

    /** Determines action on Enter key press based on current state. */
    @FXML
    private void handleEnterPressed() {
        // (*** SỬA ĐỔI: Logic này được gọi bởi handleKeyPressed, nhưng chúng ta sẽ
        //     tích hợp nó vào handleFieldKeyPress. Tuy nhiên, giữ lại OK/Cancel
        //     cho phím Enter chung) ***

        // Kiểm tra xem focus đang ở đâu
        Node focusedNode = dialogStage.getScene().getFocusOwner();

        if (simpleTagRadio.isSelected()) {
            if (simpleNameField.getText().trim().isEmpty()) {
                // If simple field is empty, treat Enter as Cancel
                handleCancel();
            } else {
                // Otherwise, treat Enter as OK
                handleOk();
            }
        } else { // JSON mode
            String key = keyField.getText().trim();
            String value = valueField.getText().trim();

            if (key.isEmpty()) {
                // If key is empty, treat Enter as Cancel
                handleCancel();
            } else if (!value.isEmpty()) {
                // If key and value are filled, treat Enter as OK
                handleOk();
            } else { // Key is filled, but value is empty

                // Nếu đang focus ở keyField, chuyển sang valueField
                if (focusedNode == keyField) {
                    valueField.requestFocus();
                    highlightField(valueField);
                    return; // Không làm gì thêm
                }

                // Check if the entered key exists as a suggestion
                boolean keyExists = jsonGroups.keySet().stream()
                        .anyMatch(k -> k.equalsIgnoreCase(key));

                if (keyExists) {
                    // If key exists, focus value field and highlight it, waiting for input
                    valueField.requestFocus();
                    highlightField(valueField);
                } else {
                    // If key doesn't exist as a suggestion, assume user wants a simple tag
                    isUpdatingProgrammatically = true; // Prevent listener loops
                    simpleTagRadio.setSelected(true);
                    simpleNameField.setText(keyField.getText()); // Copy key text to simple field
                    isUpdatingProgrammatically = false;
                    handleOk(); // Attempt to save as simple tag
                }
            }
        }
    }

    /** Highlights the appropriate input field if validation fails during OK action. */
    private void highlightInvalidField() {
        if (simpleTagRadio.isSelected()) {
            highlightField(simpleNameField);
        } else {
            if (keyField.getText().trim().isEmpty()) {
                highlightField(keyField);
            } else { // Only remaining invalid state is empty value field
                highlightField(valueField);
            }
        }
    }

    /**
     * Temporarily highlights a text field to indicate an error or required input.
     * @param field The TextField to highlight.
     */
    private void highlightField(TextField field) {
        Platform.runLater(() -> {
            field.getStyleClass().add("validation-error");
            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> field.getStyleClass().remove("validation-error"));
            pause.play();
        });
    }
}