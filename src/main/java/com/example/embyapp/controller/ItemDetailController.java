package com.example.embyapp.controller;

import embyclient.model.BaseItemDto;
import embyclient.model.ImageInfo;
import com.example.embyapp.MainApp;
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.I18nManager; // <-- IMPORT
import com.example.embyapp.service.ItemRepository;
import com.example.embyapp.service.JsonFileHandler;
import com.example.embyapp.viewmodel.ItemDetailViewModel;
import com.example.embyapp.viewmodel.detail.TagModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node; // (*** THÊM IMPORT NÀY ***)
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects; // (*** THÊM IMPORT NÀY ***)
import java.util.prefs.Preferences; // (*** THÊM IMPORT MỚI ***)
import java.util.stream.Collectors;

/**
 * (CẬP NHẬT 31) Cập nhật bố cục UI Detail.
 * - Sửa FXML fields để khớp với cấu trúc mới (Path, Release Date, Fixed Footer).
 * - Cập nhật logic binding để khớp với cấu trúc mới.
 * (FIX LỖI) Khắc phục lỗi statusLabel không mất đi sau khi tải xong và xóa binding cho actionStatusLabel.
 * (CẬP NHẬT MỚI) Thêm nút review cho Tags.
 * (CẬP NHẬT MỚI) Thêm các nút chấm điểm (Rating).
 * (CẬP NHẬT MỚI) Thêm lưu vị trí AddTagDialog.
 */
public class ItemDetailController {

    // --- FXML Components ---
    @FXML private StackPane rootPane;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;
    @FXML private ScrollPane mainScrollPane;
    @FXML private VBox detailContentPane;

    // (*** ẢNH ***)
    @FXML private StackPane primaryImageContainer;
    @FXML private ImageView primaryImageView;
    @FXML private Button savePrimaryImageButton;

    // (*** TRƯỜNG TEXT ***)
    @FXML private TextField titleTextField;

    // (*** THÊM CÁC FXML FIELD CHO RATING ***)
    @FXML private FlowPane criticRatingPane;
    @FXML private HBox reviewCriticRatingContainer;
    @FXML private Button acceptCriticRatingButton;
    @FXML private Button rejectCriticRatingButton;

    @FXML private TextArea overviewTextArea;
    @FXML private Label genresLabel; // Giữ lại nhưng bị ẩn
    @FXML private Label overviewLabel; // <-- ADDED
    @FXML private Label backdropGalleryLabel; // <-- ADDED

    // (*** GALLERY ***)
    @FXML private HBox backdropHeaderHBox; // (*** KHAI BÁO MỚI ***)
    @FXML private ScrollPane imageGalleryScrollPane; // (*** KHAI BÁO MỚI ***)
    @FXML private Button addBackdropButton;
    @FXML private FlowPane imageGalleryPane;

    // (*** CÁC TRƯỜNG DỮ LIỆU MỚI ***)
    @FXML private HBox pathContainer; // Container cho path/open
    @FXML private Label pathLabel; // <-- ADDED
    @FXML private TextField pathTextField;
    @FXML private Button openButton;
    @FXML private Label actionStatusLabel; // Vẫn khai báo để tránh NPE khi FXML field bị xóa.

    @FXML private HBox releaseDateContainer; // Container cho date/tagline/runtime
    @FXML private Label releaseDateLabel; // <-- ADDED
    @FXML private TextField releaseDateTextField;

    // (*** MỚI: ORIGINAL TITLE & FETCH BUTTON ***)
    @FXML private Label originalTitleLabel;
    @FXML private TextField originalTitleTextField;
    @FXML private Button fetchReleaseDateButton;
    @FXML private HBox reviewOriginalTitleContainer;
    @FXML private Button acceptOriginalTitleButton;
    @FXML private Button rejectOriginalTitleButton;

    // (*** TAGS ***)
    @FXML private Label tagsLabel; // <-- ADDED
    @FXML private FlowPane tagsFlowPane;
    @FXML private Button addTagButton;

    // (*** STUDIOS/PEOPLE/GENRES DẠNG CHIP ***)
    @FXML private Label studiosLabel; // <-- ADDED
    @FXML private FlowPane studiosFlowPane;
    @FXML private Button addStudioButton;
    @FXML private Label peopleLabel; // <-- ADDED
    @FXML private FlowPane peopleFlowPane;
    @FXML private Button addPeopleButton;
    @FXML private Label genresLabelText; // <-- ADDED (để phân biệt với genresLabel cũ)
    @FXML private FlowPane genresFlowPane;
    @FXML private Button addGenreButton;

    // (*** FIXED BOTTOM BAR ***)
    @FXML private HBox bottomButtonBar;
    @FXML private Button saveButton;
    @FXML private Button importButton;
    @FXML private Button exportButton;

    // (*** REVIEW BUTTONS ***)
    @FXML private HBox reviewTitleContainer;
    @FXML private Button acceptTitleButton;
    @FXML private Button rejectTitleButton;
    @FXML private HBox reviewOverviewContainer;
    @FXML private Button acceptOverviewButton;
    @FXML private Button rejectOverviewButton;
    @FXML private HBox reviewReleaseDateContainer;
    @FXML private Button acceptReleaseDateButton;
    @FXML private Button rejectReleaseDateButton;
    @FXML private HBox reviewStudiosContainer;
    @FXML private Button acceptStudiosButton;
    @FXML private Button rejectStudiosButton;
    @FXML private HBox reviewPeopleContainer;
    @FXML private Button acceptPeopleButton;
    @FXML private Button rejectPeopleButton;
    @FXML private HBox reviewGenresContainer;
    @FXML private Button acceptGenresButton;
    @FXML private Button rejectGenresButton;

    // (*** THÊM 3 DÒNG NÀY CHO TAGS ***)
    @FXML private HBox reviewTagsContainer;
    @FXML private Button acceptTagsButton;
    @FXML private Button rejectTagsButton;


    private ItemDetailViewModel viewModel;
    private final ItemRepository itemRepository = new ItemRepository();

    // (*** THÊM MỚI: Preferences ***)
    private Preferences prefs;
    private static final String PREF_NODE_PATH = "/com/example/embyapp/mainview"; // Dùng chung node với MainController
    private static final String KEY_ADD_TAG_DIALOG_X = "addTagDialogX";
    private static final String KEY_ADD_TAG_DIALOG_Y = "addTagDialogY";
    // (*** KẾT THÚC THÊM MỚI ***)

    @FXML
    public void initialize() {
        // --- Setup Localization ---
        setupLocalization(); // <-- CALL NEW METHOD

        // (*** THÊM MỚI: Khởi tạo Preferences ***)
        prefs = Preferences.userRoot().node(PREF_NODE_PATH);
        // (*** KẾT THÚC THÊM MỚI ***)

        // (Gán sự kiện onAction cho các nút (v/x) giữ nguyên)
        acceptTitleButton.setOnAction(e -> viewModel.acceptImportField("title"));
        rejectTitleButton.setOnAction(e -> viewModel.rejectImportField("title"));

        // (*** THÊM SỰ KIỆN CHO NÚT REVIEW RATING ***)
        acceptCriticRatingButton.setOnAction(e -> viewModel.acceptImportField("criticRating"));
        rejectCriticRatingButton.setOnAction(e -> viewModel.rejectImportField("criticRating"));

        acceptOverviewButton.setOnAction(e -> viewModel.acceptImportField("overview"));
        rejectOverviewButton.setOnAction(e -> viewModel.rejectImportField("overview"));
        acceptReleaseDateButton.setOnAction(e -> viewModel.acceptImportField("releaseDate"));
        rejectReleaseDateButton.setOnAction(e -> viewModel.rejectImportField("releaseDate"));

        acceptOriginalTitleButton.setOnAction(e -> viewModel.acceptImportField("originalTitle"));
        rejectOriginalTitleButton.setOnAction(e -> viewModel.rejectImportField("originalTitle"));

        acceptStudiosButton.setOnAction(e -> viewModel.acceptImportField("studios"));
        rejectStudiosButton.setOnAction(e -> viewModel.rejectImportField("studios"));
        acceptPeopleButton.setOnAction(e -> viewModel.acceptImportField("people"));
        rejectPeopleButton.setOnAction(e -> viewModel.rejectImportField("people"));
        acceptGenresButton.setOnAction(e -> viewModel.acceptImportField("genres"));
        rejectGenresButton.setOnAction(e -> viewModel.rejectImportField("genres"));

        // (*** THÊM 2 DÒNG NÀY CHO TAGS ***)
        acceptTagsButton.setOnAction(e -> viewModel.acceptImportField("tags"));
        rejectTagsButton.setOnAction(e -> viewModel.rejectImportField("tags"));


        // (*** NÚT ẢNH ***)
        primaryImageContainer.setOnMouseClicked(e -> {
            if (viewModel != null) {
                viewModel.selectNewPrimaryImage((Stage) rootPane.getScene().getWindow());
            }
        });
        savePrimaryImageButton.setOnAction(e -> {
            if (viewModel != null) viewModel.saveNewPrimaryImage();
        });
        addBackdropButton.setOnAction(e -> {
            if (viewModel != null) {
                viewModel.selectNewBackdrops((Stage) rootPane.getScene().getWindow());
            }
        });

        // (*** DRAG-DROP ***)
        setupBackdropDragAndDrop();

        // (*** GÁN LẠI SỰ KIỆN CHO NÚT DƯỚI ĐÁY ***)
        saveButton.setOnAction(e -> handleSaveButtonAction());
        importButton.setOnAction(e -> handleImportButtonAction());
        exportButton.setOnAction(e -> handleExportButtonAction());
    }

    // <-- ADD THIS NEW METHOD -->
    private void setupLocalization() {
        I18nManager i18n = I18nManager.getInstance();

        statusLabel.setText(i18n.getString("itemDetailView", "statusDefault"));
        savePrimaryImageButton.setText(i18n.getString("itemDetailView", "saveImageButton"));
        titleTextField.setPromptText(i18n.getString("itemDetailView", "titlePrompt"));

        acceptTitleButton.setText(i18n.getString("itemDetailView", "acceptButton"));
        rejectTitleButton.setText(i18n.getString("itemDetailView", "rejectButton"));

        // (*** THÊM LOCALIZATION CHO NÚT REVIEW RATING ***)
        acceptCriticRatingButton.setText(i18n.getString("itemDetailView", "acceptButton"));
        rejectCriticRatingButton.setText(i18n.getString("itemDetailView", "rejectButton"));

        releaseDateLabel.setText(i18n.getString("itemDetailView", "releaseDateLabel"));
        releaseDateTextField.setPromptText(i18n.getString("itemDetailView", "releaseDatePrompt"));
        acceptReleaseDateButton.setText(i18n.getString("itemDetailView", "acceptButton"));
        rejectReleaseDateButton.setText(i18n.getString("itemDetailView", "rejectButton"));

        originalTitleLabel.setText(i18n.getString("itemDetailView", "originalTitleLabel"));
        originalTitleTextField.setPromptText(i18n.getString("itemDetailView", "originalTitlePrompt"));
        fetchReleaseDateButton.setText(i18n.getString("itemDetailView", "fetchReleaseDateButton"));
        acceptOriginalTitleButton.setText(i18n.getString("itemDetailView", "acceptButton"));
        rejectOriginalTitleButton.setText(i18n.getString("itemDetailView", "rejectButton"));

        pathLabel.setText(i18n.getString("itemDetailView", "pathLabel"));
        // openButton text is handled by binding

        tagsLabel.setText(i18n.getString("itemDetailView", "tagsLabel"));
        acceptTagsButton.setText(i18n.getString("itemDetailView", "acceptButton"));
        rejectTagsButton.setText(i18n.getString("itemDetailView", "rejectButton"));
        addTagButton.setText(i18n.getString("itemDetailView", "addButton"));

        genresLabelText.setText(i18n.getString("itemDetailView", "genresLabel"));
        acceptGenresButton.setText(i18n.getString("itemDetailView", "acceptButton"));
        rejectGenresButton.setText(i18n.getString("itemDetailView", "rejectButton"));
        addGenreButton.setText(i18n.getString("itemDetailView", "addButton"));

        studiosLabel.setText(i18n.getString("itemDetailView", "studiosLabel"));
        acceptStudiosButton.setText(i18n.getString("itemDetailView", "acceptButton"));
        rejectStudiosButton.setText(i18n.getString("itemDetailView", "rejectButton"));
        addStudioButton.setText(i18n.getString("itemDetailView", "addButton"));

        peopleLabel.setText(i18n.getString("itemDetailView", "peopleLabel"));
        acceptPeopleButton.setText(i18n.getString("itemDetailView", "acceptButton"));
        rejectPeopleButton.setText(i18n.getString("itemDetailView", "rejectButton"));
        addPeopleButton.setText(i18n.getString("itemDetailView", "addButton"));

        overviewLabel.setText(i18n.getString("itemDetailView", "overviewLabel"));
        acceptOverviewButton.setText(i18n.getString("itemDetailView", "acceptButton"));
        rejectOverviewButton.setText(i18n.getString("itemDetailView", "rejectButton"));
        overviewTextArea.setPromptText(i18n.getString("itemDetailView", "overviewPrompt"));

        backdropGalleryLabel.setText(i18n.getString("itemDetailView", "backdropGalleryLabel"));
        addBackdropButton.setText(i18n.getString("itemDetailView", "addButton"));

        saveButton.setText(i18n.getString("itemDetailView", "saveButton"));
        importButton.setText(i18n.getString("itemDetailView", "importButton"));
        exportButton.setText(i18n.getString("itemDetailView", "exportButton"));
    }

    /**
     * Được gọi bởi MainController để inject ViewModel.
     */
    public void setViewModel(ItemDetailViewModel viewModel) {
        this.viewModel = viewModel;

        // --- BINDING UI VỚI VIEWMODEL ---

        if (genresLabel != null) {
            genresLabel.visibleProperty().set(false); // Hide Genres Label (legacy)
            genresLabel.managedProperty().set(false); // Hide Genres Label (legacy)
        }

        // BINDING CHUNG
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());
        titleTextField.textProperty().bindBidirectional(viewModel.titleProperty());
        originalTitleTextField.textProperty().bindBidirectional(viewModel.originalTitleProperty());

        // (*** THÊM LOGIC SETUP VÀ BINDING CHO RATING ***)
        setupCriticRatingButtons(); // Gọi hàm helper để tạo 10 nút
        // Thêm listener: Khi property trong VM thay đổi, cập nhật UI
        viewModel.criticRatingProperty().addListener((obs, oldVal, newVal) -> updateRatingButtonSelection());

        overviewTextArea.textProperty().bindBidirectional(viewModel.overviewProperty());
        releaseDateTextField.textProperty().bindBidirectional(viewModel.releaseDateProperty());

        // 2. Binding cho Tags
        viewModel.getTagItems().addListener((ListChangeListener<TagModel>) c -> {
            Platform.runLater(this::updateTagsFlowPane);
        });
        updateTagsFlowPane();

        // 3. Binding cho Studios
        viewModel.getStudioItems().addListener((ListChangeListener<TagModel>) c -> {
            Platform.runLater(this::updateStudiosFlowPane);
        });
        updateStudiosFlowPane();

        // 4. Binding cho People
        viewModel.getPeopleItems().addListener((ListChangeListener<TagModel>) c -> {
            Platform.runLater(this::updatePeopleFlowPane);
        });
        updatePeopleFlowPane();

        // 5. Binding cho Genres
        viewModel.getGenreItems().addListener((ListChangeListener<TagModel>) c -> {
            Platform.runLater(this::updateGenresFlowPane);
        });
        updateGenresFlowPane();

        // (*** PHỤC HỒI: 6. Binding Gallery (Lắng nghe danh sách ImageInfo) ***)
        viewModel.getBackdropImages().addListener((ListChangeListener<ImageInfo>) c -> {
            Platform.runLater(this::updateImageGallery);
        });
        updateImageGallery(); // Cập nhật lần đầu

        // 7. Binding Ảnh Primary
        primaryImageView.imageProperty().bind(viewModel.primaryImageProperty());

        // 8. Binding kiểm soát hiển thị (Loading / Status / Content)
        loadingIndicator.visibleProperty().bind(viewModel.loadingProperty());

        // *** FIX LỖI: Thêm lại binding cho statusLabel.visibleProperty (Ưu tiên hiển thị status nếu loading HOẶC có message) ***
        statusLabel.visibleProperty().bind(viewModel.loadingProperty().or(viewModel.showStatusMessageProperty()));

        mainScrollPane.visibleProperty().bind(
                viewModel.loadingProperty().not().and(viewModel.showStatusMessageProperty().not())
        );
        mainScrollPane.managedProperty().bind(mainScrollPane.visibleProperty());

        bottomButtonBar.visibleProperty().bind(mainScrollPane.visibleProperty());
        bottomButtonBar.managedProperty().bind(mainScrollPane.visibleProperty());

        // (*** THÊM BINDING MỚI CHO GALLERY SCROLLPANE VÀ HEADER ***)
        imageGalleryScrollPane.visibleProperty().bind(mainScrollPane.visibleProperty());
        imageGalleryScrollPane.managedProperty().bind(mainScrollPane.visibleProperty());

        backdropHeaderHBox.visibleProperty().bind(mainScrollPane.visibleProperty());
        backdropHeaderHBox.managedProperty().bind(mainScrollPane.visibleProperty());
        // (*** KẾT THÚC THÊM BINDING MỚI ***)


        // 9. Binding UI linh hoạt, Path...
        pathTextField.textProperty().bind(viewModel.itemPathProperty());
        pathContainer.visibleProperty().bind(
                viewModel.itemPathProperty().isNotEmpty()
                        .and(viewModel.itemPathProperty().isNotEqualTo("Không có đường dẫn"))
        );
        pathContainer.managedProperty().bind(pathContainer.visibleProperty());

        // actionStatusLabel KHÔNG CÒN HIỂN THỊ NỮA. (Chỉ hiển thị lỗi Mở/Phát)
        // actionStatusLabel là lỗi xảy ra trong luồng Mở/Phát.
        // Ta sẽ giữ lại logic quản lý lỗi, nhưng không hiển thị label này nữa.
        if (actionStatusLabel != null) {
            actionStatusLabel.managedProperty().set(false);
            actionStatusLabel.visibleProperty().set(false);
            // Giữ lại binding text để logic cũ vẫn hoạt động:
            // actionStatusLabel.textProperty().bind(viewModel.actionStatusMessageProperty());
        }

        openButton.textProperty().bind(
                Bindings.when(viewModel.isFolderProperty())
                        .then(I18nManager.getInstance().getString("itemDetailView", "openButtonFolder")) // <-- UPDATE
                        .otherwise(I18nManager.getInstance().getString("itemDetailView", "openButtonFile")) // <-- UPDATE
        );

        releaseDateContainer.visibleProperty().bind(viewModel.isFolderProperty().not());
        releaseDateContainer.managedProperty().bind(viewModel.isFolderProperty().not());

        // 10. Binding Review Containers
        bindReviewContainer(reviewTitleContainer, viewModel.showTitleReviewProperty());
        // (*** THÊM BINDING CHO NÚT REVIEW RATING ***)
        bindReviewContainer(reviewCriticRatingContainer, viewModel.showCriticRatingReviewProperty());
        bindReviewContainer(reviewOverviewContainer, viewModel.showOverviewReviewProperty());
        bindReviewContainer(reviewReleaseDateContainer, viewModel.showReleaseDateReviewProperty());
        bindReviewContainer(reviewOriginalTitleContainer, viewModel.showOriginalTitleReviewProperty());
        bindReviewContainer(reviewStudiosContainer, viewModel.showStudiosReviewProperty());
        bindReviewContainer(reviewPeopleContainer, viewModel.showPeopleReviewProperty());
        bindReviewContainer(reviewGenresContainer, viewModel.showGenresReviewProperty());

        // (*** THÊM DÒNG NÀY CHO TAGS ***)
        bindReviewContainer(reviewTagsContainer, viewModel.showTagsReviewProperty());

        // 11. Binding nút Save
        saveButton.disableProperty().bind(viewModel.isDirtyProperty().not());

        // 12. Binding nút Lưu ảnh Primary
        savePrimaryImageButton.visibleProperty().bind(viewModel.primaryImageDirtyProperty());
        savePrimaryImageButton.managedProperty().bind(viewModel.primaryImageDirtyProperty());
    }

    /**
     * Hàm helper bindReviewContainer giữ nguyên.
     */
    private void bindReviewContainer(HBox container, ReadOnlyBooleanProperty visibilityProperty) {
        if (container != null && visibilityProperty != null) {
            container.visibleProperty().bind(visibilityProperty);
            container.managedProperty().bind(visibilityProperty);
        }
    }

    /**
     * Helper: Cập nhật FlowPane gallery.
     */
    private void updateImageGallery() {
        Platform.runLater(() -> {
            imageGalleryPane.getChildren().clear();
            if (viewModel == null || viewModel.getEmbyService() == null || viewModel.getCurrentItemId() == null) return;

            // Lấy các thông tin cần thiết từ ViewModel
            String serverUrl = viewModel.getEmbyService().getApiClient().getBasePath();
            String itemId = viewModel.getCurrentItemId();

            for (ImageInfo imageInfo : viewModel.getBackdropImages()) {
                // Tạo BackdropView (component tùy chỉnh)
                BackdropView backdropView = new BackdropView(
                        imageInfo,
                        serverUrl,
                        itemId,
                        viewModel::deleteBackdrop
                );
                imageGalleryPane.getChildren().add(backdropView);
            }
        });
    }

    /**
     * Helper: Cập nhật FlowPane cho Tags.
     */
    private void updateTagsFlowPane() {
        if (viewModel == null || tagsFlowPane == null) return;
        tagsFlowPane.getChildren().clear();
        for (TagModel tag : viewModel.getTagItems()) {
            TagView tagChip = new TagView(tag, viewModel::removeTag);
            tagsFlowPane.getChildren().add(tagChip);
        }
    }

    /**
     * Helper: Cập nhật FlowPane cho Studios (MỚI).
     */
    private void updateStudiosFlowPane() {
        if (viewModel == null || studiosFlowPane == null) return;
        studiosFlowPane.getChildren().clear();
        for (TagModel studio : viewModel.getStudioItems()) {
            TagView studioChip = new TagView(studio, viewModel::removeStudio);
            studiosFlowPane.getChildren().add(studioChip);
        }
    }

    /**
     * Helper: Cập nhật FlowPane cho People (MỚI).
     */
    private void updatePeopleFlowPane() {
        if (viewModel == null || peopleFlowPane == null) return;
        peopleFlowPane.getChildren().clear();
        for (TagModel person : viewModel.getPeopleItems()) {
            TagView personChip = new TagView(person, viewModel::removePerson);
            peopleFlowPane.getChildren().add(personChip);
        }
    }

    /**
     * Helper: Cập nhật FlowPane cho Genres (MỚI).
     */
    private void updateGenresFlowPane() {
        if (viewModel == null || genresFlowPane == null) return;
        genresFlowPane.getChildren().clear();
        for (TagModel genre : viewModel.getGenreItems()) {
            TagView genreChip = new TagView(genre, viewModel::removeGenre);
            genresFlowPane.getChildren().add(genreChip);
        }
        if (genresLabel != null) {
            genresLabel.visibleProperty().set(false);
            genresLabel.managedProperty().set(false);
        }
    }


    /**
     * (*** HÀM MỚI ***)
     * Tạo 10 nút rating và thêm vào FlowPane.
     */
    private void setupCriticRatingButtons() {
        if (criticRatingPane == null || viewModel == null) return;
        criticRatingPane.getChildren().clear();

        for (int i = 1; i <= 10; i++) {
            final int ratingValue = i;
            Button ratingButton = new Button(String.valueOf(ratingValue));
            ratingButton.getStyleClass().add("rating-button");
            ratingButton.setUserData(ratingValue); // Lưu giá trị (int) 1-10

            // Xử lý click để CẬP NHẬT rating trong ViewModel
            ratingButton.setOnAction(e -> {
                // Chuyển đổi int (1-10) sang Float (1.0 - 10.0)
                Float newRating = (float) ratingValue;

                // Nếu click vào nút đang được chọn, set rating về null (bỏ chọn)
                if (Objects.equals(viewModel.criticRatingProperty().get(), newRating)) {
                    viewModel.criticRatingProperty().set(null);
                } else {
                    // Ngược lại, set rating mới
                    viewModel.criticRatingProperty().set(newRating);
                }
            });

            criticRatingPane.getChildren().add(ratingButton);
        }
        // Cập nhật selection lần đầu
        updateRatingButtonSelection();
    }

    /**
     * (*** HÀM MỚI ***)
     * Cập nhật trạng thái selected của các nút rating dựa trên giá trị của ViewModel.
     */
    private void updateRatingButtonSelection() {
        if (criticRatingPane == null || viewModel == null) return;

        Float currentRating = viewModel.criticRatingProperty().get();

        // Làm tròn rating về số nguyên gần nhất để so sánh
        Integer selectedValue = null;
        if (currentRating != null) {
            // Emby lưu rating 0-10, ta dùng 1-10
            selectedValue = Math.round(currentRating);
        }

        for (Node node : criticRatingPane.getChildren()) {
            if (node instanceof Button && node.getUserData() instanceof Integer) {
                Button button = (Button) node;
                int buttonValue = (Integer) button.getUserData();

                // Nếu giá trị của nút khớp với rating đã chọn (đã làm tròn)
                if (selectedValue != null && buttonValue == selectedValue) {
                    // Thêm class 'selected' (màu hồng) nếu chưa có
                    if (!button.getStyleClass().contains("selected")) {
                        button.getStyleClass().add("selected");
                    }
                } else {
                    // Ngược lại, xóa class 'selected' (trở về màu xám)
                    button.getStyleClass().remove("selected");
                }
            }
        }
    }

    /**
     * Helper chung để mở dialog thêm Studio/People/Tag/Genre.
     * (*** ĐÃ CẬP NHẬT ĐỂ LƯU VỊ TRÍ ***)
     */
    private void showAddTagDialog(AddTagDialogController.SuggestionContext context) {
        if (viewModel == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("AddTagDialog.fxml"));
            VBox page = loader.load();
            Stage dialogStage = new Stage();

            // Lấy controller và thiết lập context (thao tác quan trọng)
            AddTagDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setContext(context, itemRepository);

            // Cấu hình Stage
            String title; // <-- Logic is now in the dialog controller, but we set title here
            I18nManager i18n = I18nManager.getInstance();
            switch (context) {
                case STUDIO: title = i18n.getString("addTagDialog", "addStudioTitle"); break;
                case PEOPLE: title = i18n.getString("addTagDialog", "addPeopleTitle"); break;
                case GENRE:  title = i18n.getString("addTagDialog", "addGenreTitle"); break;
                case TAG:
                default:
                    title = i18n.getString("addTagDialog", "addTagTitle"); break;
            }

            dialogStage.setTitle(title); // <-- UPDATE
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner((Stage) rootPane.getScene().getWindow());
            Scene scene = new Scene(page);
            scene.getStylesheets().addAll(rootPane.getScene().getStylesheets());
            dialogStage.setScene(scene);

            // (*** THÊM MỚI: Tải và Lưu vị trí Dialog ***)
            // 1. Tải vị trí đã lưu
            // Dùng -1 làm giá trị mặc định (không thể có) để biết đây là lần đầu
            double savedX = prefs.getDouble(KEY_ADD_TAG_DIALOG_X, -1);
            double savedY = prefs.getDouble(KEY_ADD_TAG_DIALOG_Y, -1);

            // 2. Áp dụng vị trí nếu nó hợp lệ (không phải lần đầu)
            if (savedX != -1 && savedY != -1) {
                dialogStage.setX(savedX);
                dialogStage.setY(savedY);
            }
            // Nếu không (lần đầu), nó sẽ tự động căn giữa do initOwner()

            // 3. Thêm listener để LƯU vị trí khi đóng
            dialogStage.setOnCloseRequest(e -> {
                try {
                    prefs.putDouble(KEY_ADD_TAG_DIALOG_X, dialogStage.getX());
                    prefs.putDouble(KEY_ADD_TAG_DIALOG_Y, dialogStage.getY());
                    prefs.flush(); // Lưu ngay lập tức
                } catch (Exception ex) {
                    System.err.println("Lỗi khi lưu vị trí AddTagDialog: " + ex.getMessage());
                }
            });
            // (*** KẾT THÚC THÊM MỚI ***)

            dialogStage.showAndWait();

            TagModel newModel = controller.getResultTag();
            if (newModel != null) {
                switch (context) {
                    case STUDIO:
                        viewModel.addStudio(newModel);
                        break;
                    case PEOPLE:
                        viewModel.addPerson(newModel);
                        break;
                    case GENRE:
                        viewModel.addGenre(newModel);
                        break;
                    case TAG:
                        viewModel.addTag(newModel);
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorOpenDialog", context.name().toLowerCase())); // <-- UPDATE
        } catch (Exception e) {
            e.printStackTrace();
            viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorOpenDialogGeneric", e.getMessage())); // <-- UPDATE
        }
    }


    /**
     * Xử lý nút Mở/Phát.
     */
    @FXML
    private void handleOpenButtonAction() {
        if (viewModel == null) return;
        viewModel.clearActionError();
        String path = pathTextField.getText();
        if (path == null || path.isEmpty() || path.equals("Không có đường dẫn")) {
            viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorInvalidPath")); // <-- UPDATE
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorDesktopAPINotSupported")); // <-- UPDATE
            return;
        }
        new Thread(() -> {
            try {
                File fileOrDir = new File(path);
                if (!fileOrDir.exists()) {
                    viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorPathNotExist")); // <-- UPDATE
                    return;
                }

                // 1. Mở file/folder (như cũ)
                Desktop.getDesktop().open(fileOrDir);

                // (*** Yêu cầu Pop-out NẾU LÀ FILE ***)
                if (viewModel != null && !viewModel.isFolderProperty().get()) {
                    // Chạy trên luồng JavaFX
                    Platform.runLater(() -> viewModel.requestPopOut());
                }

            } catch (Exception e) {
                System.err.println("Lỗi khi mở đường dẫn: " + path + " | " + e.getMessage());
                viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorOpenPath", e.getMessage())); // <-- UPDATE
            }
        }).start();
    }

    @FXML
    private void handleFetchReleaseDateAction() {
        if (viewModel != null) {
            viewModel.fetchReleaseDate();
        }
    }

    /**
     * Mở dialog thêm Tag.
     */
    @FXML
    private void handleAddTagButtonAction() {
        showAddTagDialog(AddTagDialogController.SuggestionContext.TAG);
    }

    /**
     * Mở dialog thêm Studio.
     */
    @FXML
    private void handleAddStudioButtonAction() {
        showAddTagDialog(AddTagDialogController.SuggestionContext.STUDIO);
    }

    /**
     * Mở dialog thêm People.
     */
    @FXML
    private void handleAddPeopleButtonAction() {
        showAddTagDialog(AddTagDialogController.SuggestionContext.PEOPLE);
    }

    /**
     * Mở dialog thêm Genre (MỚI).
     */
    @FXML
    private void handleAddGenreButtonAction() {
        showAddTagDialog(AddTagDialogController.SuggestionContext.GENRE);
    }


    // (Hàm handleSaveButtonAction, handleImportButtonAction, handleExportButtonAction giữ nguyên)
    @FXML
    private void handleSaveButtonAction() {
        System.out.println("Nút Lưu đã được nhấn. Gọi ViewModel.saveChanges().");
        if (viewModel != null) {
            viewModel.saveChanges();
        }
    }
    @FXML
    private void handleImportButtonAction() {
        if (viewModel == null) return;
        viewModel.clearActionError();
        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            File selectedFile = JsonFileHandler.showOpenJsonDialog(stage);
            if (selectedFile != null) {
                viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorImportRead")); // <-- UPDATE
                new Thread(() -> {
                    try {
                        BaseItemDto importedDto = JsonFileHandler.readJsonFileToObject(selectedFile);
                        if (importedDto != null) {
                            Platform.runLater(() -> {
                                viewModel.importAndPreview(importedDto);
                            });
                        } else {
                            throw new Exception(I18nManager.getInstance().getString("itemDetailView", "errorImportInvalid")); // <-- UPDATE
                        }
                    } catch (Exception ex) {
                        System.err.println("Lỗi khi Import (luồng nền): " + ex.getMessage());
                        Platform.runLater(() -> viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorImportReadThread", ex.getMessage()))); // <-- UPDATE
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi Import (hiển thị dialog): " + e.getMessage());
            viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorImportDialog", e.getMessage())); // <-- UPDATE
        }
    }
    @FXML
    private void handleExportButtonAction() {
        if (viewModel == null) return;
        viewModel.clearActionError();
        try {
            BaseItemDto dtoToExport = viewModel.getItemForExport();
            if (dtoToExport == null) {
                viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorExportNoData")); // <-- UPDATE
                return;
            }
            Stage stage = (Stage) rootPane.getScene().getWindow();
            String originalTitle = viewModel.getOriginalTitleForExport();
            System.out.println(originalTitle);
            String initialFileName = (originalTitle != null ? originalTitle.replaceAll("[^a-zA-Z0-9.-]", "_") : "item") + ".json";
            System.out.println(initialFileName);
            File targetFile = JsonFileHandler.showSaveJsonDialog(stage, initialFileName);
            if (targetFile != null) {
                new Thread(() -> {
                    try {
                        JsonFileHandler.writeObjectToJsonFile(dtoToExport, targetFile);
                        Platform.runLater(() -> viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "exportSuccess", targetFile.getName()))); // <-- UPDATE
                    } catch (Exception ex) {
                        System.err.println("Lỗi khi Export (luồng nền): " + ex.getMessage());
                        Platform.runLater(() -> viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorExportWriteThread", ex.getMessage()))); // <-- UPDATE
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi Export (hiển thị dialog): " + e.getMessage());
            viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorExportDialog", e.getMessage())); // <-- UPDATE
        }
    }


    // (*** HÀM Cài đặt Drag-Drop giữ nguyên ***)
    private void setupBackdropDragAndDrop() {
        if (imageGalleryPane == null) return;

        imageGalleryPane.setOnDragOver(event -> {
            if (event.getGestureSource() != imageGalleryPane && event.getDragboard().hasFiles()) {
                // Chỉ chấp nhận file ảnh
                List<File> files = event.getDragboard().getFiles();
                boolean hasImage = files.stream().anyMatch(f -> {
                    String name = f.getName().toLowerCase();
                    return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".webp");
                });
                if (hasImage) {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
            }
            event.consume();
        });

        imageGalleryPane.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && viewModel != null) {
                List<File> imageFiles = db.getFiles().stream()
                        .filter(f -> {
                            String name = f.getName().toLowerCase();
                            return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".webp");
                        })
                        .collect(Collectors.toList());

                if (!imageFiles.isEmpty()) {
                    viewModel.uploadDroppedBackdropFiles(imageFiles); // <-- Gọi hàm VM
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }
}