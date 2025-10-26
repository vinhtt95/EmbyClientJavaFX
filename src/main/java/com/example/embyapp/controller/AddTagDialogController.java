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
import java.util.Set;
import java.util.stream.Collectors;


/**
 * (CẬP NHẬT 30) Thêm Genres.
 * - Cập nhật SuggestionContext và loadSuggestedTags.
 * (CẬP NHẬT 34) Thêm chức năng tìm kiếm nhanh trong các gợi ý (3 ô tìm kiếm).
 * (FIX LỖI) Sửa ClassCastException và Lỗi biên dịch rawName.
 */
public class AddTagDialogController {

    public enum SuggestionContext {
        TAG, STUDIO, PEOPLE, GENRE
    }

    @FXML private ToggleGroup tagTypeGroup;
    @FXML private RadioButton simpleTagRadio;
    @FXML private RadioButton jsonTagRadio;

    @FXML private GridPane simpleTagPane;
    @FXML private TextField simpleNameField;

    @FXML private GridPane jsonTagPane;
    @FXML private TextField keyField;
    @FXML private TextField valueField;

    // (*** THÊM MỚI: 3 ô Search ***)
    @FXML private TextField keySearchField;
    @FXML private TextField valueSearchField;
    @FXML private TextField simpleSearchField;

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

    // (*** Danh sách gốc và các nhóm đã phân tách ***)
    private List<String> allRawNames = Collections.emptyList();
    private Map<String, List<ParsedTag>> jsonGroups = new HashMap<>(); // Key -> List<ParsedTag>
    private List<ParsedTag> allSimpleTags = Collections.emptyList(); // List<ParsedTag> cho Simple


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
            // Gọi hàm populateValues để lọc và hiển thị
            if (valueSearchField != null) {
                populateValues(newToggle, valueSearchField.getText());
            } else {
                populateValues(newToggle, "");
            }
        });

        // (*** THÊM MỚI: 3 Listener cho 3 ô Search ***)
        if (keySearchField != null) {
            keySearchField.textProperty().addListener((obs, oldVal, newVal) -> {
                applyFiltersAndPopulate(true, false, false); // Chỉ lọc Keys
            });
        }
        if (valueSearchField != null) {
            valueSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
                // Chỉ lọc Values (chỉ cần gọi populateValues lại)
                populateValues(keySuggestionGroup.getSelectedToggle(), newVal);
            });
        }
        if (simpleSearchField != null) {
            simpleSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
                applyFiltersAndPopulate(false, false, true); // Chỉ lọc Simple
            });
        }
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
                            context == SuggestionContext.GENRE ? "Thêm Thể Loại Mới" :
                                    "Thêm Tag Mới";
            stage.setTitle(title);

            // Đặt lại label cho Simple Tags (dù là Tag, Studio, People hay Genre)
            simpleTagRadio.setText(context == SuggestionContext.TAG ? "Tag Đơn giản" :
                    context == SuggestionContext.STUDIO ? "Tên Studio" :
                            context == SuggestionContext.PEOPLE ? "Tên Người" :
                                    "Tên Thể Loại");

            // Cập nhật label cho Simple Suggestions
            // (*** ĐÃ SỬA LỖI CAST TẠI ĐÂY ***)
            // Lấy HBox chứa Label và TextField (index 0 của suggestionSimpleContainer)
            HBox headerHBox = (HBox) suggestionSimpleContainer.getChildren().get(0);
            // Lấy Label (index 0 của headerHBox)
            Label suggestionLabel = (Label) headerHBox.getChildren().get(0);

            suggestionLabel.setText("Gợi ý " + (context == SuggestionContext.TAG ? "Tag Đơn giản" :
                    context == SuggestionContext.STUDIO ? "Studios" :
                            context == SuggestionContext.PEOPLE ? "People" :
                                    "Genres"));

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
                    case GENRE:
                        rawNames.addAll(itemRepository.getGenreSuggestions(embyService.getApiClient())
                                .stream().map(SuggestionItemModel::getName).collect(Collectors.toList()));
                        break;
                }

            } catch (Exception e) {
                System.err.println("Lỗi khi lấy gợi ý từ server: " + e.getMessage());
                e.printStackTrace();
            }

            // Cập nhật UI
            Platform.runLater(() -> {
                this.allRawNames = rawNames;
                prepareSuggestionLists(); // Chuẩn bị danh sách gốc

                // Áp dụng filter ban đầu (đọc từ text fields, nếu có)
                String keySearch = keySearchField != null ? keySearchField.getText() : "";
                String valueSearch = valueSearchField != null ? valueSearchField.getText() : "";
                String simpleSearch = simpleSearchField != null ? simpleSearchField.getText() : "";

                // Gọi applyFiltersAndPopulate sau khi đã có dữ liệu gốc
                populateKeys(keySearch);
                populateSimpleTags(simpleSearch);
                populateValues(keySuggestionGroup.getSelectedToggle(), valueSearch);

                // Cập nhật hiển thị container (làm thủ công vì không gọi applyFiltersAndPopulate)
                suggestionJsonContainer.setVisible(!jsonGroups.isEmpty());
                suggestionJsonContainer.setManaged(!jsonGroups.isEmpty());
                suggestionSimpleContainer.setVisible(!allSimpleTags.isEmpty());
                suggestionSimpleContainer.setManaged(!allSimpleTags.isEmpty());
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
     * (*** TÁCH LOGIC: Chuẩn bị danh sách gốc (Không lọc) ***)
     */
    private void prepareSuggestionLists() {
        if (allRawNames == null || allRawNames.isEmpty()) {
            jsonGroups.clear();
            allSimpleTags = Collections.emptyList();
            return;
        }

        // 1. Parse tất cả tên thô thành TagModel
        List<ParsedTag> parsedTags = allRawNames.stream()
                .map(raw -> new ParsedTag(raw, TagModel.parse(raw)))
                .collect(Collectors.toList());

        // 2. Chia nhóm JSON
        jsonGroups = parsedTags.stream()
                .filter(pt -> pt.model.isJson())
                .collect(Collectors.groupingBy(pt -> pt.model.getKey()));

        // 3. Lấy danh sách Simple Tags
        allSimpleTags = parsedTags.stream()
                .filter(pt -> !pt.model.isJson())
                .collect(Collectors.toList());
    }


    /**
     * (*** HÀM TỔNG HỢP: Gọi khi nhấn Enter/thay đổi Text (cho Keys/Simple) ***)
     */
    private void applyFiltersAndPopulate(boolean filterKeys, boolean filterValues, boolean filterSimple) {
        if (filterKeys) {
            populateKeys(keySearchField.getText());
        }

        if (filterValues) {
            // Gọi populateValues cho Key đang được chọn
            populateValues(keySuggestionGroup.getSelectedToggle(), valueSearchField.getText());
        }

        if (filterSimple) {
            populateSimpleTags(simpleSearchField.getText());
        }

        // Cập nhật hiển thị container
        Platform.runLater(() -> {
            // Hiển thị/ẩn container JSON
            suggestionJsonContainer.setVisible(!jsonGroups.isEmpty());
            suggestionJsonContainer.setManaged(!jsonGroups.isEmpty());

            // Hiển thị/ẩn container Simple
            boolean hasSimpleSuggestions = suggestionSimplePane.getChildren().size() > 0;
            suggestionSimpleContainer.setVisible(hasSimpleSuggestions);
            suggestionSimpleContainer.setManaged(hasSimpleSuggestions);
        });
    }

    /**
     * Lọc và populate Keys dựa trên searchText.
     * @param searchText Text từ keySearchField.
     */
    private void populateKeys(String searchText) {
        suggestionKeysPane.getChildren().clear();
        keySuggestionGroup.getToggles().clear();

        Set<String> allKeys = jsonGroups.keySet();

        // 1. Lọc Keys
        List<String> filteredKeys;
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredKeys = new ArrayList<>(allKeys);
        } else {
            String lowerCaseSearchText = searchText.trim().toLowerCase();
            filteredKeys = allKeys.stream()
                    .filter(key -> key.toLowerCase().contains(lowerCaseSearchText))
                    .collect(Collectors.toList());
        }

        // 2. Sắp xếp và Populate
        Collections.sort(filteredKeys, String.CASE_INSENSITIVE_ORDER);

        for (String key : filteredKeys) {
            ToggleButton chip = new ToggleButton(key);
            chip.setToggleGroup(keySuggestionGroup);
            chip.getStyleClass().add("suggestion-key-button");
            chip.setUserData(key);
            suggestionKeysPane.getChildren().add(chip);
        }

        // Giữ lại key đã chọn nếu nó vẫn còn trong danh sách đã lọc
        if (keySuggestionGroup.getSelectedToggle() == null && !filteredKeys.isEmpty()) {
            keySuggestionGroup.selectToggle(keySuggestionGroup.getToggles().get(0));
        } else if (keySuggestionGroup.getSelectedToggle() != null) {
            // Kiểm tra xem toggle hiện tại có bị lọc mất không
            String selectedKey = (String) keySuggestionGroup.getSelectedToggle().getUserData();
            if (!filteredKeys.contains(selectedKey) && !filteredKeys.isEmpty()) {
                // Chọn lại cái đầu tiên nếu cái cũ bị lọc mất
                keySuggestionGroup.selectToggle(keySuggestionGroup.getToggles().get(0));
            } else if (!filteredKeys.contains(selectedKey) && filteredKeys.isEmpty()) {
                // Nếu không có key nào, clear values
                populateValues(null, valueSearchField.getText());
            }
        }
    }

    /**
     * Lọc và populate Values của Key đang được chọn.
     * @param selectedKeyToggle Toggle của Key đang được chọn.
     * @param searchText Text từ valueSearchField.
     */
    private void populateValues(Toggle selectedKeyToggle, String searchText) {
        suggestionValuesPane.getChildren().clear();
        valueSuggestionGroup.getToggles().clear(); // Reset ToggleGroup cho Values

        if (selectedKeyToggle == null) {
            return;
        }

        String selectedKey = (String) selectedKeyToggle.getUserData();
        List<ParsedTag> tagsInGroup = jsonGroups.get(selectedKey);

        if (tagsInGroup == null) return;

        // 1. Lọc Values
        final List<ParsedTag> filteredTags;
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredTags = tagsInGroup;
        } else {
            String lowerCaseSearchText = searchText.trim().toLowerCase();
            filteredTags = tagsInGroup.stream()
                    .filter(pt -> pt.model.getValue().toLowerCase().contains(lowerCaseSearchText))
                    .collect(Collectors.toList());
        }

        // 2. Sắp xếp và Populate
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

    /**
     * Lọc và Populate Simple Tags.
     * @param searchText Text từ simpleSearchField.
     */
    private void populateSimpleTags(String searchText) {
        suggestionSimplePane.getChildren().clear();
        // Không reset valueSuggestionGroup ở đây, vì nó dùng chung với value/key

        final List<ParsedTag> filteredTags;
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredTags = allSimpleTags;
        } else {
            String lowerCaseSearchText = searchText.trim().toLowerCase();
            filteredTags = allSimpleTags.stream()
                    .filter(pt -> pt.model.getDisplayName().toLowerCase().contains(lowerCaseSearchText))
                    .collect(Collectors.toList());
        }

        // Sắp xếp theo DisplayName (tên)
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