package com.example.embyapp.controller;

// ... (Các import giữ nguyên) ...
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
 * (CẬP NHẬT 2) Thêm hành vi nhấn Enter trên keyField (khi value trống) để chuyển sang Simple Tag.
 * (CẬP NHẬT 3) Thêm quick search Simple khi gõ Key, focus SimpleField, và Tab từ Simple về JSON.
 */
public class AddTagDialogController {

    // ... (Enum, FXML fields, other fields giữ nguyên) ...
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

    private boolean isUpdatingProgrammatically = false;
    private int focusedKeyIndex = -1;
    private int focusedValueIndex = -1;
    private int focusedSimpleIndex = -1;
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

        // Radio button logic
        simpleTagRadio.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            isUpdatingProgrammatically = true;
            simpleTagPane.setVisible(isSelected);
            simpleTagPane.setManaged(isSelected);
            jsonTagPane.setVisible(!isSelected);
            jsonTagPane.setManaged(!isSelected);

            // Reset focus indices khi chuyển tab
            focusedKeyIndex = -1;
            focusedValueIndex = -1;
            focusedSimpleIndex = -1;
            clearAndSetChipFocus(suggestionKeysPane, -1);
            clearAndSetChipFocus(suggestionValuesPane, -1);
            clearAndSetChipFocus(suggestionSimplePane, -1);

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


        // Listener for key text field changes
        keyField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingProgrammatically) return;
            focusedKeyIndex = -1; // Reset focus
            populateKeys(newVal); // Filter keys
            // (*** CẬP NHẬT 3: Quick search simple tags ***)
            populateSimpleTags(newVal); // Filter simple tags (dù đang ẩn)
            updateContainerVisibility();
            // populateValues called within populateKeys
        });

        // Listener for manual key suggestion selection (user click)
        keySuggestionGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (isUpdatingProgrammatically) return;

            Platform.runLater(() -> {
                if (newToggle != null) {
                    isUpdatingProgrammatically = true;
                    String selectedKey = (String) newToggle.getUserData();
                    keyField.setText(selectedKey);
                    populateValues(newToggle, "");

                    focusedKeyIndex = suggestionKeysPane.getChildren().indexOf(newToggle);
                    clearAndSetChipFocus(suggestionKeysPane, focusedKeyIndex);

                    isUpdatingProgrammatically = false;
                    // Không chuyển focus value tự động
                } else if (oldToggle != null) {
                    // Khi user deselect key bằng cách click lại
                    if (oldToggle.getUserData() != null) {
                        populateValues(null, valueField.getText()); // Clear value suggestions
                        // (*** MỚI: Reset cả focusedKeyIndex khi deselect ***)
                        focusedKeyIndex = -1;
                        clearAndSetChipFocus(suggestionKeysPane, -1);
                    }
                }
            });
        });


        // EventFilter cho keyField (Xử lý Up/Down/Tab/Enter)
        keyField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            handleFieldKeyPress(e, keyField, suggestionKeysPane, keySuggestionGroup);
        });

        // Focus listener for auto-complete (giữ nguyên logic cũ nhưng không tự chuyển focus value)
        keyField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (wasFocused && !isFocused && keySuggestionGroup.getSelectedToggle() == null) {
                String currentKeyText = keyField.getText().trim();
                if (!currentKeyText.isEmpty()) {
                    Toggle firstMatchingToggle = null;
                    String correctCaseKey = null;

                    for (Toggle toggle : keySuggestionGroup.getToggles()) {
                        if (toggle instanceof Node && toggle.getUserData() instanceof String) {
                            String toggleKey = (String) toggle.getUserData();
                            if (toggleKey.equalsIgnoreCase(currentKeyText)) {
                                firstMatchingToggle = toggle;
                                correctCaseKey = toggleKey;
                                break;
                            }
                        }
                    }

                    if (firstMatchingToggle != null && correctCaseKey != null) {
                        final Toggle finalToggle = firstMatchingToggle;
                        final String finalCorrectKey = correctCaseKey;

                        Platform.runLater(() -> {
                            isUpdatingProgrammatically = true;
                            keyField.setText(finalCorrectKey);
                            clearKeyHighlightAndSelection();

                            populateValues(finalToggle, "");

                            focusedKeyIndex = suggestionKeysPane.getChildren().indexOf(finalToggle);
                            clearAndSetChipFocus(suggestionKeysPane, focusedKeyIndex);


                            isUpdatingProgrammatically = false;
                        });
                    }
                }
            }
        });


        // Listener for value text field changes
        valueField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingProgrammatically) return;
            focusedValueIndex = -1; // Reset focus
            Toggle effectiveKeyToggle = keySuggestionGroup.getSelectedToggle() != null
                    ? keySuggestionGroup.getSelectedToggle()
                    : findFocusedKeyToggle();
            populateValues(effectiveKeyToggle, newVal);
            updateContainerVisibility();
        });

        // EventFilter cho valueField (Xử lý Up/Down/Tab/Enter)
        valueField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            handleFieldKeyPress(e, valueField, suggestionValuesPane, valueSuggestionGroup);
        });


        // Listener for simple name text field changes
        simpleNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingProgrammatically) return;
            focusedSimpleIndex = -1; // Reset focus
            populateSimpleTags(newVal);
            updateContainerVisibility();
        });

        // EventFilter cho simpleNameField (Xử lý Up/Down/Tab/Enter)
        simpleNameField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            handleFieldKeyPress(e, simpleNameField, suggestionSimplePane, valueSuggestionGroup); // Dùng chung group
        });


        // Copy button action
        copyButton.setOnAction(e -> handleCopyAction());

    }

    // (*** HÀM MỚI: Xử lý phím cho cả 3 text field ***)
    private void handleFieldKeyPress(KeyEvent event, TextField field, FlowPane suggestionPane, ToggleGroup suggestionGroup) {
        KeyCode code = event.getCode();
        int currentIndex = -1;
        int paneSize = suggestionPane.getChildren().size();

        if (field == keyField) currentIndex = focusedKeyIndex;
        else if (field == valueField) currentIndex = focusedValueIndex;
        else if (field == simpleNameField) currentIndex = focusedSimpleIndex;

        if (code == KeyCode.UP || code == KeyCode.DOWN) {
            event.consume(); // Ngăn con trỏ di chuyển trong text field
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
                if (focusedKeyIndex != -1 && focusedKeyIndex < suggestionKeysPane.getChildren().size()) {
                    Node focusedNode = suggestionKeysPane.getChildren().get(focusedKeyIndex);
                    if (focusedNode instanceof ToggleButton) {
                        isUpdatingProgrammatically = true;
                        keySuggestionGroup.selectToggle((ToggleButton)focusedNode);
                        isUpdatingProgrammatically = false;
                        populateValues((ToggleButton)focusedNode, "");
                    }
                }
                valueField.requestFocus();
            } else if (field == valueField) {
                if (focusedValueIndex != -1 && focusedValueIndex < suggestionValuesPane.getChildren().size()) {
                    Node focusedNode = suggestionValuesPane.getChildren().get(focusedValueIndex);
                    if (focusedNode instanceof ToggleButton) {
                        isUpdatingProgrammatically = true;
                        valueSuggestionGroup.selectToggle((ToggleButton)focusedNode);
                        isUpdatingProgrammatically = false;
                    }
                }
                okButton.requestFocus();
            } else if (field == simpleNameField) {
                // (*** CẬP NHẬT 3: Tab từ Simple về JSON ***)
                String currentSimpleText = simpleNameField.getText();
                isUpdatingProgrammatically = true;
                jsonTagRadio.setSelected(true); // Chuyển radio
                keyField.setText(currentSimpleText); // Copy text qua key
                simpleNameField.clear(); // Xóa simple field
                valueField.clear(); // Xóa value field
                isUpdatingProgrammatically = false;

                // Populate lại suggestions và focus key field
                final String textToSearch = currentSimpleText;
                Platform.runLater(() -> {
                    populateKeys(textToSearch);
                    populateSimpleTags(textToSearch); // Vẫn populate simple (dù ẩn)
                    keyField.requestFocus();
                    keyField.positionCaret(keyField.getText().length());
                });
            }
        } else if (code == KeyCode.ENTER) {
            // Enter trên keyField + value trống
            if (field == keyField && valueField.getText().trim().isEmpty()) {
                event.consume();
                String keyText = keyField.getText().trim();
                if (!keyText.isEmpty()) {
                    if (focusedKeyIndex != -1 && focusedKeyIndex < suggestionKeysPane.getChildren().size()) {
                        Node focusedNode = suggestionKeysPane.getChildren().get(focusedKeyIndex);
                        if (focusedNode instanceof ToggleButton && focusedNode.getUserData() instanceof String) {
                            keyText = (String) focusedNode.getUserData();
                        }
                    }

                    // Chuyển sang Simple Mode
                    isUpdatingProgrammatically = true;
                    simpleTagRadio.setSelected(true);
                    simpleNameField.setText(keyText);
                    keyField.clear();
                    valueField.clear();
                    isUpdatingProgrammatically = false;

                    final String textToMatch = keyText;
                    Platform.runLater(() -> {
                        populateSimpleTags(textToMatch); // Populate lại list simple
                        int matchIndex = findSimpleChipIndex(textToMatch);

                        // (*** CẬP NHẬT 3: Focus vào simpleNameField ***)
                        simpleNameField.requestFocus(); // Focus field trước
                        if (matchIndex != -1) {
                            // Focus chip tìm thấy (style và cập nhật field)
                            setChipFocus(simpleNameField, suggestionSimplePane, valueSuggestionGroup, matchIndex);
                        } else {
                            // Nếu không có chip khớp, chỉ cần select all text
                            simpleNameField.selectAll();
                        }
                        // Không chuyển focus okButton nữa
                        // okButton.requestFocus();
                    });
                } else {
                    handleCancel();
                }

            }
            // Enter trên valueField (hoặc chip value được focus) -> Chọn chip (nếu focus) rồi OK
            else if (field == valueField) {
                if (focusedValueIndex != -1 && focusedValueIndex < suggestionValuesPane.getChildren().size()) {
                    Node focusedNode = suggestionValuesPane.getChildren().get(focusedValueIndex);
                    if (focusedNode instanceof ToggleButton) {
                        event.consume();
                        isUpdatingProgrammatically = true;
                        valueSuggestionGroup.selectToggle((ToggleButton)focusedNode); // Chọn chip
                        // Listener sẽ cập nhật field
                        isUpdatingProgrammatically = false;
                        handleOk(); // Gọi OK
                    }
                } else if (!valueField.getText().trim().isEmpty()) {
                    event.consume();
                    handleOk(); // Có text -> OK
                }
                // Nếu value trống, để handleEnterPressed() xử lý (cancel)
            }
            // Enter trên simpleNameField (hoặc chip simple được focus) -> Chọn chip (nếu focus) rồi OK
            else if (field == simpleNameField) {
                if (focusedSimpleIndex != -1 && focusedSimpleIndex < suggestionSimplePane.getChildren().size()) {
                    Node focusedNode = suggestionSimplePane.getChildren().get(focusedSimpleIndex);
                    if (focusedNode instanceof ToggleButton) {
                        event.consume();
                        isUpdatingProgrammatically = true;
                        valueSuggestionGroup.selectToggle((ToggleButton)focusedNode); // Chọn chip
                        isUpdatingProgrammatically = false;
                        handleOk(); // Gọi OK
                    }
                } else if (!simpleNameField.getText().trim().isEmpty()) {
                    event.consume();
                    handleOk(); // Có text -> OK
                }
                // Nếu simple trống, để handleEnterPressed() xử lý (cancel)
            }
            // Nếu Enter không bị consume, handleEnterPressed (gắn với toàn dialog) sẽ chạy
        }
    }

    // ... (findSimpleChipIndex, setChipFocus, clearAndSetChipFocus giữ nguyên) ...
    private int findSimpleChipIndex(String text) {
        if (text == null || text.isEmpty()) return -1;
        String lowerText = text.toLowerCase();
        for (int i = 0; i < suggestionSimplePane.getChildren().size(); i++) {
            Node node = suggestionSimplePane.getChildren().get(i);
            if (node instanceof ToggleButton && node.getUserData() instanceof String) {
                TagModel model = TagModel.parse((String)node.getUserData());
                if (model.getDisplayName().equalsIgnoreCase(lowerText)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void setChipFocus(TextField field, FlowPane pane, ToggleGroup group, int index) {
        if (index < 0 || index >= pane.getChildren().size()) {
            clearAndSetChipFocus(pane, -1);
            if (field == keyField) focusedKeyIndex = -1;
            else if (field == valueField) focusedValueIndex = -1;
            else if (field == simpleNameField) focusedSimpleIndex = -1;
            return;
        }

        clearAndSetChipFocus(pane, index);

        Node chipNode = pane.getChildren().get(index);
        if (!(chipNode instanceof ToggleButton)) return;

        ToggleButton chip = (ToggleButton) chipNode;

        if (field == keyField) focusedKeyIndex = index;
        else if (field == valueField) focusedValueIndex = index;
        else if (field == simpleNameField) focusedSimpleIndex = index;

        String chipText = "";
        String rawChipData = (String) chip.getUserData();

        if (field == keyField) {
            chipText = rawChipData;
        } else {
            TagModel model = TagModel.parse(rawChipData);
            if (field == valueField) {
                chipText = model.getValue();
            } else {
                chipText = model.getDisplayName();
            }
        }

        isUpdatingProgrammatically = true;
        field.setText(chipText);
        field.positionCaret(chipText.length());
        isUpdatingProgrammatically = false;

        if (field == keyField) {
            populateValues(chip, "");
            focusedValueIndex = -1;
            clearAndSetChipFocus(suggestionValuesPane, -1);
        }
    }

    private void clearAndSetChipFocus(FlowPane pane, int indexToFocus) {
        Platform.runLater(()-> {
            for (int i = 0; i < pane.getChildren().size(); i++) {
                Node child = pane.getChildren().get(i);
                child.getStyleClass().remove(FOCUSED_CHIP_STYLE_CLASS);
                if (i == indexToFocus) {
                    child.getStyleClass().add(FOCUSED_CHIP_STYLE_CLASS);
                }
            }
        });
    }

    // ... (setupLocalization, setContext, loadSuggestedTags, fetchTagsFromServer, prepareSuggestionLists, updateContainerVisibility giữ nguyên) ...
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
            boolean showJsonSuggestions = jsonTagRadio.isSelected() && hasJsonGroups;
            if (suggestionJsonContainer != null) {
                suggestionJsonContainer.setVisible(showJsonSuggestions);
                suggestionJsonContainer.setManaged(showJsonSuggestions);
            }
            boolean hasSimpleTags = allSimpleTags != null && !allSimpleTags.isEmpty();
            boolean showSimpleSuggestions = simpleTagRadio.isSelected() && hasSimpleTags;
            if (suggestionSimpleContainer != null) {
                // (*** CẬP NHẬT 3: Hiển thị simple suggestions cả khi đang ở JSON mode ***)
                suggestionSimpleContainer.setVisible(hasSimpleTags); // Luôn hiển thị nếu có tag
                suggestionSimpleContainer.setManaged(hasSimpleTags);
                // Làm mờ đi nếu đang ở JSON mode (tùy chọn)
                suggestionSimpleContainer.setOpacity(jsonTagRadio.isSelected() ? 0.6 : 1.0);

                // suggestionSimpleContainer.setVisible(showSimpleSuggestions);
                // suggestionSimpleContainer.setManaged(showSimpleSuggestions);
            }
        });
    }

    // ... (populateKeys giữ nguyên) ...
    private void populateKeys(String searchText) {
        suggestionKeysPane.getChildren().clear();
        keySuggestionGroup.getToggles().clear(); // Luôn clear group trước khi thêm

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

        for (String key : filteredKeys) {
            ToggleButton chip = new ToggleButton(key);
            chip.setToggleGroup(keySuggestionGroup); // Thêm vào group
            chip.getStyleClass().add("suggestion-key-button");
            chip.setUserData(key);
            suggestionKeysPane.getChildren().add(chip);
            if (firstButton == null) {
                firstButton = chip;
            }
        }

        clearKeyHighlightOnly();
        if (keySuggestionGroup.getSelectedToggle() == null) {
            // Populate value dựa trên key được focus (nếu có) hoặc không có gì
            populateValues(findFocusedKeyToggle(), valueField.getText());
        } else {
            populateValues(keySuggestionGroup.getSelectedToggle(), valueField.getText());
        }
    }

    // ... (findFocusedKeyToggle giữ nguyên) ...
    private Toggle findFocusedKeyToggle() {
        for (Node node : suggestionKeysPane.getChildren()) {
            if (node.getStyleClass().contains(FOCUSED_CHIP_STYLE_CLASS)) {
                if (node instanceof Toggle) {
                    return (Toggle) node;
                }
            }
        }
        return null;
    }


    // ... (populateValues giữ nguyên) ...
    private void populateValues(Toggle associatedKeyToggle, String searchText) {
        suggestionValuesPane.getChildren().clear();
        valueSuggestionGroup.getToggles().clear(); // Luôn clear group

        if (associatedKeyToggle == null || associatedKeyToggle.getUserData() == null) {
            return;
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
            chip.setToggleGroup(valueSuggestionGroup); // Thêm vào group
            chip.getStyleClass().addAll("suggested-tag-button", "tag-view-json");
            chip.setUserData(pt.rawString); // Lưu raw string
            chip.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isUpdatingProgrammatically) return;
                if (isSelected) {
                    fillFormFromSuggestion((String) chip.getUserData());

                    focusedValueIndex = suggestionValuesPane.getChildren().indexOf(chip);
                    clearAndSetChipFocus(suggestionValuesPane, focusedValueIndex);

                    Platform.runLater(() -> okButton.requestFocus());
                }
            });
            suggestionValuesPane.getChildren().add(chip);
        }
    }

    // ... (populateSimpleTags giữ nguyên) ...
    private void populateSimpleTags(String searchText) {
        suggestionSimplePane.getChildren().clear();
        // Không clear valueSuggestionGroup ở đây nếu muốn giữ selection giữa JSON và Simple

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
            chip.setToggleGroup(valueSuggestionGroup); // Dùng chung group
            chip.getStyleClass().add("suggested-tag-button");
            // Thêm style simple
            chip.getStyleClass().add("tag-view-simple");
            chip.setUserData(pt.rawString); // Lưu raw string
            chip.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isUpdatingProgrammatically) return;
                if (isSelected) {
                    clearKeyHighlightAndSelection(); // Clear key selection when simple tag is chosen
                    fillFormFromSuggestion((String) chip.getUserData());

                    focusedSimpleIndex = suggestionSimplePane.getChildren().indexOf(chip);
                    clearAndSetChipFocus(suggestionSimplePane, focusedSimpleIndex);

                    Platform.runLater(() -> okButton.requestFocus());
                }
            });
            suggestionSimplePane.getChildren().add(chip);
        }
    }

    // ... (clearKeyHighlightAndSelection, clearKeyHighlightOnly giữ nguyên) ...
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
        // Xóa cả 2 class "filtering-key" và "focused-chip"
        for (Node node : suggestionKeysPane.getChildren()) {
            node.getStyleClass().removeAll("filtering-key", FOCUSED_CHIP_STYLE_CLASS);
        }
    }

    // ... (fillFormFromSuggestion giữ nguyên) ...
    /**
     * Fills the input fields based on a selected suggestion string.
     * @param selectedTagName The raw string representation of the selected tag.
     */
    private void fillFormFromSuggestion(String selectedTagName) {
        isUpdatingProgrammatically = true;
        // Reset tất cả các chỉ số focus
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

    // ... (highlightMatchingKey giữ nguyên) ...
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
                // Dùng "filtering-key" khi click, "focused-chip" khi dùng phím
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

    // ... (setDialogStage, getResultTag, copyTriggeredIdProperty giữ nguyên) ...
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public TagModel getResultTag() {
        return resultTag;
    }

    public StringProperty copyTriggeredIdProperty() {
        return copyTriggeredId;
    }

    // ... (handleOk, handleCancel, handleCopyAction giữ nguyên) ...
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
            // ID field is empty, highlight it
            copyIdField.requestFocus();
            highlightField(copyIdField);
        }
    }

    // ... (handleKeyPressed - chỉ còn xử lý ESCAPE) ...
    /** Handles key presses globally within the dialog (Escape). */
    @FXML
    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            handleCancel();
        }
        // Enter được xử lý trong handleFieldKeyPress
    }


    // ... (handleEnterPressed giữ nguyên) ...
    /** Determines action on Enter key press based on current state (trừ trường hợp đặc biệt đã xử lý). */
    @FXML
    private void handleEnterPressed() {
        // Trường hợp Enter trên keyField + value trống đã được handleFieldKeyPress xử lý và consume
        Node focusedNode = dialogStage.getScene().getFocusOwner();

        if (simpleTagRadio.isSelected()) {
            if (simpleNameField.getText().trim().isEmpty() && focusedNode != okButton && focusedNode != cancelButton) {
                handleCancel();
            } else {
                handleOk();
            }
        } else { // JSON mode
            String key = keyField.getText().trim();
            String value = valueField.getText().trim();

            if (key.isEmpty() && focusedNode != okButton && focusedNode != cancelButton) {
                handleCancel();
            }
            else if (!key.isEmpty() && !value.isEmpty()) {
                handleOk();
            }
            else if (!key.isEmpty() && value.isEmpty() && focusedNode == valueField) {
                // Đang ở value field trống, nhấn Enter -> Cancel
                handleCancel();
            }
            else if (!key.isEmpty() && value.isEmpty() && focusedNode == keyField) {
                // Trường hợp này không nên xảy ra vì đã bị consume, nhưng để phòng hờ
                valueField.requestFocus();
                highlightField(valueField);
            } else if (focusedNode == okButton || focusedNode == cancelButton) {
                // Nếu focus ở nút thì cứ để nút thực hiện action
            }
            else {
                // Các trường hợp khác (ví dụ focus vào copy field) -> Cancel
                // Hoặc có thể là OK nếu key hợp lệ? Tạm thời giữ Cancel cho an toàn
                handleCancel();
            }
        }
    }

    // ... (highlightInvalidField, highlightField giữ nguyên) ...
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