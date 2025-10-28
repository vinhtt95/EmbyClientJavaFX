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
import javafx.scene.Node; // <-- THÊM IMPORT NÀY
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
 * (CẬP NHẬT 44) Sửa lỗi NullPointerException và hành vi tự động chọn/nhảy focus.
 * (CẬP NHẬT 45) Bỏ auto-select, chỉ highlight key đang lọc value.
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
    private final ToggleGroup valueSuggestionGroup = new ToggleGroup(); // Giữ lại cho Value và Simple

    private Stage dialogStage;
    private TagModel resultTag = null;
    private StringProperty copyTriggeredId = new SimpleStringProperty(null);

    private ItemRepository itemRepository;
    private SuggestionContext currentContext = SuggestionContext.TAG;

    private List<String> allRawNames = Collections.emptyList();
    private Map<String, List<ParsedTag>> jsonGroups = new HashMap<>();
    private List<ParsedTag> allSimpleTags = Collections.emptyList();

    // Cờ kiểm soát (có thể không cần nữa, nhưng giữ lại phòng trường hợp khác)
    private boolean isFilteringSuggestions = false;


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

        // --- (SỬA ĐỔI LISTENER KEY SUGGESTION - Chỉ phản ứng khi click) ---
        // Trong hàm initialize() của AddTagDialogController.java

        keySuggestionGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            // Chỉ xử lý khi người dùng click
            if (!isFilteringSuggestions) {
                // --- BỌC TOÀN BỘ LOGIC CLICK TRONG Platform.runLater ---
                Platform.runLater(() -> {
                    if (newToggle != null) {
                        // Logic khi chọn một key mới
                        valueSuggestionGroup.selectToggle(null); // Bỏ chọn value cũ
                        String selectedKey = (String) newToggle.getUserData();
                        keyField.setText(selectedKey); // Điền key đã chọn
                        populateValues(newToggle, ""); // Populate value dựa trên key mới
                        valueField.requestFocus(); // Chuyển focus xuống valueField
                    } else if (oldToggle != null) {
                        // Logic khi bỏ chọn key (click lại key đang chọn)
                        // Kiểm tra xem oldToggle có còn là nút hợp lệ không trước khi dùng
                        if (oldToggle.getUserData() != null) {
                            populateValues(null, valueField.getText()); // Clear value suggestions
                        }
                    }
                });
                // --- KẾT THÚC BỌC ---
            }
            // Không cần else vì khi isFilteringSuggestions = true, không cần làm gì ở đây nữa
        });

        // --- (SỬA ĐỔI LISTENER KEY FIELD - Chỉ lọc, không chọn) ---
        keyField.textProperty().addListener((obs, oldVal, newVal) -> {
            isFilteringSuggestions = true; // Bật cờ trước khi lọc
            populateKeys(newVal); // Lọc Keys và highlight key phù hợp nhất để lọc Value
            populateSimpleTags(newVal); // Cũng lọc luôn danh sách tag đơn giản
            isFilteringSuggestions = false; // Tắt cờ sau khi xong
            updateContainerVisibility();
            // Việc gọi populateValues đã được chuyển vào trong populateKeys
        });
        // --- (KẾT THÚC SỬA ĐỔI) ---


        valueField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Chỉ populate value dựa trên key đang được *highlight* (không nhất thiết phải selected) hoặc key đang được *selected*
            Toggle filteringKeyToggle = findFilteringKeyToggle(); // Tìm key đang được highlight
            Toggle selectedKeyToggle = keySuggestionGroup.getSelectedToggle(); // Lấy key đang được user click chọn
            // Ưu tiên key user chọn, nếu không có thì dùng key highlight
            populateValues(selectedKeyToggle != null ? selectedKeyToggle : filteringKeyToggle, newVal);
            updateContainerVisibility();
        });

        simpleNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            populateSimpleTags(newVal);
            updateContainerVisibility();
        });

        copyButton.setOnAction(e -> handleCopyAction());

        // Xử lý Tab từ Key Field (Giữ nguyên)
        keyField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.TAB && !event.isShiftDown()) {
                valueField.requestFocus();
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

            // Sửa lỗi NullPointerException khi suggestionSimpleContainer chưa được khởi tạo
            if (suggestionSimpleContainer != null && suggestionSimpleLabel != null) {
                suggestionSimpleLabel.setText(simpleSuggestionLabelText);
            } else if (suggestionSimpleContainer != null && !suggestionSimpleContainer.getChildren().isEmpty() && suggestionSimpleContainer.getChildren().get(0) instanceof Label) {
                // Fallback nếu FXML cấu trúc khác
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

                // Chỉ populate khi dialog đã sẵn sàng
                String keySearch = keyField.getText();
                String valueSearch = valueField.getText();
                String simpleSearch = simpleNameField.getText();

                isFilteringSuggestions = true; // Tạm thời bật cờ
                populateKeys(keySearch); // Lọc và highlight key phù hợp
                populateSimpleTags(simpleSearch);
                // populateValues sẽ được gọi bên trong populateKeys
                isFilteringSuggestions = false; // Tắt cờ

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
            // Kiểm tra null trước khi truy cập
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

    // --- (SỬA ĐỔI HÀM POPULATEKEYS - Chỉ highlight, không select) ---
    private void populateKeys(String searchText) {
        suggestionKeysPane.getChildren().clear();
        // Không cần giữ selected key cũ vì chúng ta không select nữa
        // Toggle currentSelectedToggle = keySuggestionGroup.getSelectedToggle();
        // String currentSelectedKey = currentSelectedToggle != null ? (String) currentSelectedToggle.getUserData() : null;
        keySuggestionGroup.getToggles().clear(); // Vẫn xóa các toggle cũ khỏi group

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

        ToggleButton firstButton = null; // Key phù hợp nhất để lọc value
        for (String key : filteredKeys) {
            ToggleButton chip = new ToggleButton(key);
            chip.setToggleGroup(keySuggestionGroup); // Thêm vào group
            chip.getStyleClass().add("suggestion-key-button");
            chip.setUserData(key);
            suggestionKeysPane.getChildren().add(chip); // Thêm vào UI

            // Xác định key đầu tiên (phù hợp nhất)
            if (firstButton == null) {
                firstButton = chip;
            }
        }

        // Xóa highlight cũ và highlight key phù hợp nhất (nếu có)
        for(Node node : suggestionKeysPane.getChildren()) {
            node.getStyleClass().remove("filtering-key");
        }
        if (firstButton != null) {
            firstButton.getStyleClass().add("filtering-key");
            // Gọi populateValues dựa trên key được highlight này
            populateValues(firstButton, valueField.getText());
        } else {
            // Nếu không có key nào phù hợp, clear value suggestions
            populateValues(null, valueField.getText());
        }

        // BỎ HOÀN TOÀN VIỆC GỌI selectToggle bằng code
    }
    // --- (KẾT THÚC SỬA ĐỔI) ---

    // --- (HÀM HELPER MỚI - Tìm key đang highlight) ---
    private Toggle findFilteringKeyToggle() {
        for (Toggle toggle : keySuggestionGroup.getToggles()) {
            if (toggle instanceof Node && ((Node)toggle).getStyleClass().contains("filtering-key")) {
                return toggle;
            }
        }
        return null; // Không tìm thấy
    }
    // --- (KẾT THÚC HÀM HELPER) ---


    private void populateValues(Toggle associatedKeyToggle, String searchText) { // Đổi tên tham số cho rõ ràng
        suggestionValuesPane.getChildren().clear();
        valueSuggestionGroup.getToggles().clear();

        // Nếu không có key nào liên quan (cả highlight lẫn selected), thì không hiển thị value
        if (associatedKeyToggle == null) {
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
            chip.setToggleGroup(valueSuggestionGroup);
            chip.getStyleClass().addAll("suggested-tag-button", "tag-view-json");
            chip.setUserData(pt.rawString);

            chip.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    fillFormFromSuggestion((String) chip.getUserData());
                    Platform.runLater(() -> okButton.requestFocus());
                }
            });
            suggestionValuesPane.getChildren().add(chip);
        }
    }

    private void populateSimpleTags(String searchText) {
        suggestionSimplePane.getChildren().clear();
        // Tạm thời vẫn dùng chung valueSuggestionGroup, cần xem xét lại nếu gây lỗi
        // valueSuggestionGroup.getToggles().clear(); // Nếu dùng group riêng thì phải clear ở đây

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
            chip.setUserData(pt.rawString);

            chip.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    // Khi chọn simple tag, đảm bảo không có key nào đang được *selected* (click)
                    // và xóa highlight key
                    clearKeyHighlightAndSelection();
                    fillFormFromSuggestion((String) chip.getUserData());
                    Platform.runLater(() -> okButton.requestFocus());
                }
            });
            suggestionSimplePane.getChildren().add(chip);
        }
    }

    // --- (HÀM HELPER MỚI - Xóa highlight và selection của key) ---
    private void clearKeyHighlightAndSelection() {
        // Xóa selection trước
        if (keySuggestionGroup.getSelectedToggle() != null) {
            keySuggestionGroup.selectToggle(null);
        }
        // Xóa highlight
        for (Node node : suggestionKeysPane.getChildren()) {
            node.getStyleClass().remove("filtering-key");
        }
    }
    // --- (KẾT THÚC HÀM HELPER) ---


    private void fillFormFromSuggestion(String selectedTagName) {
        TagModel selectedTag = TagModel.parse(selectedTagName);
        if (selectedTag.isJson()) {
            jsonTagRadio.setSelected(true);
            keyField.setText(selectedTag.getKey() != null ? selectedTag.getKey() : "");
            valueField.setText(selectedTag.getValue() != null ? selectedTag.getValue() : "");
            simpleNameField.clear();
            // Khi điền form từ suggestion JSON, đảm bảo key tương ứng được highlight (nếu chưa selected)
            Platform.runLater(() -> highlightMatchingKey(selectedTag.getKey()));
        } else {
            simpleTagRadio.setSelected(true);
            simpleNameField.setText(selectedTag.getDisplayName());
            keyField.clear();
            valueField.clear();
            // Khi điền form từ suggestion Simple, xóa highlight key
            Platform.runLater(this::clearKeyHighlightAndSelection);
        }
    }

    // --- (HÀM HELPER MỚI - Highlight key khớp) ---
    private void highlightMatchingKey(String keyToHighlight) {
        clearKeyHighlightAndSelection(); // Xóa highlight và selection cũ
        if (keyToHighlight == null) return;

        for (Toggle toggle : keySuggestionGroup.getToggles()) {
            if (toggle instanceof Node && keyToHighlight.equals(toggle.getUserData())) {
                ((Node)toggle).getStyleClass().add("filtering-key");
                // Gọi populateValues để đảm bảo value list khớp với key vừa highlight
                populateValues(toggle, valueField.getText());
                break; // Chỉ highlight một key
            }
        }
    }
    // --- (KẾT THÚC HÀM HELPER) ---

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
                } else if (valueField.getText().trim().isEmpty()) { // <-- Lỗi xảy ra ở đây
                    highlightField(valueField); // <--- Dòng này vẫn đúng
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
            } else if (!key.isEmpty() && value.isEmpty()) { // <-- Trường hợp value rỗng
                boolean keyExists = jsonGroups.keySet().stream()
                        .anyMatch(k -> k.equalsIgnoreCase(key));

                if (keyExists) {
                    valueField.requestFocus();
                    highlightField(valueField); // Highlight ô value yêu cầu nhập
                } else {
                    simpleTagRadio.setSelected(true);
                    simpleNameField.setText(keyField.getText());
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