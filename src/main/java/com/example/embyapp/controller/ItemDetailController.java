package com.example.embyapp.controller;

import embyclient.model.BaseItemDto;
import embyclient.model.ImageInfo;
import com.example.embyapp.MainApp;
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.I18nManager;
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
import javafx.scene.Node;
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
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
/**
 * Controller for the Item Detail view (right pane).
 * Handles user interaction and binds UI elements to the ItemDetailViewModel.
 * (CẬP NHẬT) Thêm hàm xử lý click cho chip.
 * (CẬP NHẬT) Thay đổi click ảnh primary để mở thư mục screenshot.
 */
public class ItemDetailController {

    @FXML private StackPane rootPane;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;
    @FXML private ScrollPane mainScrollPane;
    @FXML private VBox detailContentPane;
    @FXML private StackPane primaryImageContainer;
    @FXML private ImageView primaryImageView;
    @FXML private Button savePrimaryImageButton;
    @FXML private TextField titleTextField;
    @FXML private FlowPane criticRatingPane;
    @FXML private HBox reviewCriticRatingContainer;
    @FXML private Button acceptCriticRatingButton;
    @FXML private Button rejectCriticRatingButton;
    @FXML private TextArea overviewTextArea;
    @FXML private Label genresLabel; // Kept but hidden
    @FXML private Label overviewLabel;
    @FXML private Label backdropGalleryLabel;
    @FXML private HBox backdropHeaderHBox;
    @FXML private ScrollPane imageGalleryScrollPane;
    @FXML private Button addBackdropButton;
    @FXML private FlowPane imageGalleryPane;
    @FXML private HBox pathContainer;
    @FXML private Label pathLabel;
    @FXML private TextField pathTextField;
    @FXML private Button openButton;
    @FXML private Button openSubtitleButton; // <-- THÊM FIELD NÀY
    @FXML private Label actionStatusLabel; // Declared but not visible
    @FXML private HBox releaseDateContainer;
    @FXML private Label releaseDateLabel;
    @FXML private TextField releaseDateTextField;
    @FXML private Label originalTitleLabel;
    @FXML private TextField originalTitleTextField;
    @FXML private Button fetchReleaseDateButton;
    @FXML private HBox reviewOriginalTitleContainer;
    @FXML private Button acceptOriginalTitleButton;
    @FXML private Button rejectOriginalTitleButton;
    @FXML private Label tagsLabel;
    @FXML private FlowPane tagsFlowPane;
    @FXML private Button addTagButton;
    @FXML private Button cloneTagButton; // New clone button
    @FXML private Label studiosLabel;
    @FXML private FlowPane studiosFlowPane;
    @FXML private Button addStudioButton;
    @FXML private Button cloneStudioButton; // New clone button
    @FXML private Label peopleLabel;
    @FXML private FlowPane peopleFlowPane;
    @FXML private Button addPeopleButton;
    @FXML private Button clonePeopleButton; // New clone button
    @FXML private Label genresLabelText;
    @FXML private FlowPane genresFlowPane;
    @FXML private Button addGenreButton;
    @FXML private Button cloneGenreButton; // New clone button
    @FXML private HBox bottomButtonBar;
    @FXML private Button saveButton;
    @FXML private Button importButton;
    @FXML private Button exportButton;
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
    @FXML private HBox reviewTagsContainer;
    @FXML private Button acceptTagsButton;
    @FXML private Button rejectTagsButton;

    private ItemDetailViewModel viewModel;
    private final ItemRepository itemRepository = new ItemRepository();
    private Preferences prefs;
    private static final String PREF_NODE_PATH = "/com/example/embyapp/mainview";
    private static final String KEY_ADD_TAG_DIALOG_X = "addTagDialogX";
    private static final String KEY_ADD_TAG_DIALOG_Y = "addTagDialogY";
    private static final String KEY_ADD_TAG_DIALOG_WIDTH = "addTagDialogWidth";
    private static final String KEY_ADD_TAG_DIALOG_HEIGHT = "addTagDialogHeight";

    // (*** MỚI - LƯU CONTEXT VỪA CHỌN CHO HOTKEY ENTER ***)
    private AddTagDialogController.SuggestionContext lastAddContext = null;


    @FXML
    public void initialize() {
        setupLocalization();
        prefs = Preferences.userRoot().node(PREF_NODE_PATH);

        acceptTitleButton.setOnAction(e -> viewModel.acceptImportField("title"));
        rejectTitleButton.setOnAction(e -> viewModel.rejectImportField("title"));
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
        acceptTagsButton.setOnAction(e -> viewModel.acceptImportField("tags"));
        rejectTagsButton.setOnAction(e -> viewModel.rejectImportField("tags"));

        // Setup Drag-and-Drop
        setupBackdropDragAndDrop();
        setupPrimaryImageDragAndDrop();

        // --- (*** BẮT ĐẦU SỬA ĐỔI ***) ---
        // Setup click actions
        primaryImageContainer.setOnMouseClicked(e -> {
            // Gỡ bỏ logic cũ:
            // if (viewModel != null) {
            //     viewModel.selectNewPrimaryImage((Stage) rootPane.getScene().getWindow());
            // }

            // Logic mới: Mở thư mục screenshot
            handleOpenScreenshotFolder();
        });
        // --- (*** KẾT THÚC SỬA ĐỔI ***) ---

        savePrimaryImageButton.setOnAction(e -> {
            if (viewModel != null) viewModel.saveNewPrimaryImage();
        });
        addBackdropButton.setOnAction(e -> {
            if (viewModel != null) {
                viewModel.selectNewBackdrops((Stage) rootPane.getScene().getWindow());
            }
        });

        saveButton.setOnAction(e -> handleSaveButtonAction());
        importButton.setOnAction(e -> handleImportButtonAction());
        exportButton.setOnAction(e -> handleExportButtonAction());

        // Assign actions to new clone buttons
        cloneTagButton.setOnAction(e -> {
            final ItemDetailViewModel.CloneType type = ItemDetailViewModel.CloneType.TAGS;
            String typeName = getCloneTypeName(type);
            String title = I18nManager.getInstance().getString("itemDetailView", "cloneConfirmTitle");
            String content = I18nManager.getInstance().getString("itemDetailView", "cloneConfirmMessage", typeName);

            if (showConfirmationDialog(title, content)) {
                if (viewModel != null) viewModel.clonePropertiesToTreeChildren(type);
            }
        });
        cloneGenreButton.setOnAction(e -> {
            final ItemDetailViewModel.CloneType type = ItemDetailViewModel.CloneType.GENRES;
            String typeName = getCloneTypeName(type);
            String title = I18nManager.getInstance().getString("itemDetailView", "cloneConfirmTitle");
            String content = I18nManager.getInstance().getString("itemDetailView", "cloneConfirmMessage", typeName);

            if (showConfirmationDialog(title, content)) {
                if (viewModel != null) viewModel.clonePropertiesToTreeChildren(type);
            }
        });
        cloneStudioButton.setOnAction(e -> {
            final ItemDetailViewModel.CloneType type = ItemDetailViewModel.CloneType.STUDIOS;
            String typeName = getCloneTypeName(type);
            String title = I18nManager.getInstance().getString("itemDetailView", "cloneConfirmTitle");
            String content = I18nManager.getInstance().getString("itemDetailView", "cloneConfirmMessage", typeName);

            if (showConfirmationDialog(title, content)) {
                if (viewModel != null) viewModel.clonePropertiesToTreeChildren(type);
            }
        });
        clonePeopleButton.setOnAction(e -> {
            final ItemDetailViewModel.CloneType type = ItemDetailViewModel.CloneType.PEOPLE;
            String typeName = getCloneTypeName(type);
            String title = I18nManager.getInstance().getString("itemDetailView", "cloneConfirmTitle");
            String content = I18nManager.getInstance().getString("itemDetailView", "cloneConfirmMessage", typeName);

            if (showConfirmationDialog(title, content)) {
                if (viewModel != null) viewModel.clonePropertiesToTreeChildren(type);
            }
        });
    }

    // Helper method to get localized name (needed for clone confirmation message)
    private String getCloneTypeName(ItemDetailViewModel.CloneType type) {
        I18nManager i18n = I18nManager.getInstance();
        switch (type) {
            case TAGS: return i18n.getString("itemDetailView", "tagsLabel");
            case STUDIOS: return i18n.getString("itemDetailView", "studiosLabel");
            case PEOPLE: return i18n.getString("itemDetailView", "peopleLabel");
            case GENRES: return i18n.getString("itemDetailView", "genresLabel");
            default: return "";
        }
    }

    private void setupLocalization() {
        I18nManager i18n = I18nManager.getInstance();
        String cloneText = i18n.getString("itemDetailView", "cloneButton");

        statusLabel.setText(i18n.getString("itemDetailView", "statusDefault"));
        savePrimaryImageButton.setText(i18n.getString("itemDetailView", "saveImageButton"));
        titleTextField.setPromptText(i18n.getString("itemDetailView", "titlePrompt"));
        acceptTitleButton.setText(i18n.getString("itemDetailView", "acceptButton"));
        rejectTitleButton.setText(i18n.getString("itemDetailView", "rejectButton"));
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
        openSubtitleButton.setText(i18n.getString("itemDetailView", "openButtonSubtitle")); // <-- THÊM DÒNG NÀY
        tagsLabel.setText(i18n.getString("itemDetailView", "tagsLabel"));
        acceptTagsButton.setText(i18n.getString("itemDetailView", "acceptButton"));
        rejectTagsButton.setText(i18n.getString("itemDetailView", "rejectButton"));
        addTagButton.setText(i18n.getString("itemDetailView", "addButton"));
        cloneTagButton.setText(cloneText);
        genresLabelText.setText(i18n.getString("itemDetailView", "genresLabel"));
        acceptGenresButton.setText(i18n.getString("itemDetailView", "acceptButton"));
        rejectGenresButton.setText(i18n.getString("itemDetailView", "rejectButton"));
        addGenreButton.setText(i18n.getString("itemDetailView", "addButton"));
        cloneGenreButton.setText(cloneText);
        studiosLabel.setText(i18n.getString("itemDetailView", "studiosLabel"));
        acceptStudiosButton.setText(i18n.getString("itemDetailView", "acceptButton"));
        rejectStudiosButton.setText(i18n.getString("itemDetailView", "rejectButton"));
        addStudioButton.setText(i18n.getString("itemDetailView", "addButton"));
        cloneStudioButton.setText(cloneText);
        peopleLabel.setText(i18n.getString("itemDetailView", "peopleLabel"));
        acceptPeopleButton.setText(i18n.getString("itemDetailView", "acceptButton"));
        rejectPeopleButton.setText(i18n.getString("itemDetailView", "rejectButton"));
        addPeopleButton.setText(i18n.getString("itemDetailView", "addButton"));
        clonePeopleButton.setText(cloneText);
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
     * Called by MainController to inject the ViewModel.
     * @param viewModel the shared ItemDetailViewModel instance
     */
    public void setViewModel(ItemDetailViewModel viewModel) {
        this.viewModel = viewModel;

        if (genresLabel != null) {
            genresLabel.visibleProperty().set(false);
            genresLabel.managedProperty().set(false);
        }

        statusLabel.textProperty().bind(viewModel.statusMessageProperty());
        titleTextField.textProperty().bindBidirectional(viewModel.titleProperty());
        originalTitleTextField.textProperty().bindBidirectional(viewModel.originalTitleProperty());

        setupCriticRatingButtons();
        viewModel.criticRatingProperty().addListener((obs, oldVal, newVal) -> updateRatingButtonSelection());

        overviewTextArea.textProperty().bindBidirectional(viewModel.overviewProperty());
        releaseDateTextField.textProperty().bindBidirectional(viewModel.releaseDateProperty());

        viewModel.getTagItems().addListener((ListChangeListener<TagModel>) c -> Platform.runLater(this::updateTagsFlowPane));
        updateTagsFlowPane();
        viewModel.getStudioItems().addListener((ListChangeListener<TagModel>) c -> Platform.runLater(this::updateStudiosFlowPane));
        updateStudiosFlowPane();
        viewModel.getPeopleItems().addListener((ListChangeListener<TagModel>) c -> Platform.runLater(this::updatePeopleFlowPane));
        updatePeopleFlowPane();
        viewModel.getGenreItems().addListener((ListChangeListener<TagModel>) c -> Platform.runLater(this::updateGenresFlowPane));
        updateGenresFlowPane();
        viewModel.getBackdropImages().addListener((ListChangeListener<ImageInfo>) c -> Platform.runLater(this::updateImageGallery));
        updateImageGallery();

        primaryImageView.imageProperty().bind(viewModel.primaryImageProperty());
        loadingIndicator.visibleProperty().bind(viewModel.loadingProperty());
        statusLabel.visibleProperty().bind(viewModel.loadingProperty().or(viewModel.showStatusMessageProperty()));
        mainScrollPane.visibleProperty().bind(viewModel.loadingProperty().not().and(viewModel.showStatusMessageProperty().not()));
        mainScrollPane.managedProperty().bind(mainScrollPane.visibleProperty());
        bottomButtonBar.visibleProperty().bind(mainScrollPane.visibleProperty());
        bottomButtonBar.managedProperty().bind(mainScrollPane.visibleProperty());
        imageGalleryScrollPane.visibleProperty().bind(mainScrollPane.visibleProperty());
        imageGalleryScrollPane.managedProperty().bind(mainScrollPane.visibleProperty());
        backdropHeaderHBox.visibleProperty().bind(mainScrollPane.visibleProperty());
        backdropHeaderHBox.managedProperty().bind(mainScrollPane.visibleProperty());

        pathTextField.textProperty().bind(viewModel.itemPathProperty());
        pathContainer.visibleProperty().bind(
                viewModel.itemPathProperty().isNotEmpty()
                        .and(viewModel.itemPathProperty().isNotEqualTo(I18nManager.getInstance().getString("itemDetailLoader", "noPath"))) // <-- Sửa lỗi hardcode
        );
        pathContainer.managedProperty().bind(pathContainer.visibleProperty());

        if (actionStatusLabel != null) {
            actionStatusLabel.managedProperty().set(false);
            actionStatusLabel.visibleProperty().set(false);
        }

        openButton.textProperty().bind(
                Bindings.when(viewModel.isFolderProperty())
                        .then(I18nManager.getInstance().getString("itemDetailView", "openButtonFolder"))
                        .otherwise(I18nManager.getInstance().getString("itemDetailView", "openButtonFile"))
        );

        // --- THÊM BINDING CHO NÚT SUBTITLE ---
        openSubtitleButton.visibleProperty().bind(
                pathContainer.visibleProperty().and(viewModel.isFolderProperty().not())
        );
        openSubtitleButton.managedProperty().bind(openSubtitleButton.visibleProperty());
        // --- KẾT THÚC THÊM BINDING ---

        releaseDateContainer.visibleProperty().bind(viewModel.isFolderProperty().not());
        releaseDateContainer.managedProperty().bind(viewModel.isFolderProperty().not());

        bindReviewContainer(reviewTitleContainer, viewModel.showTitleReviewProperty());
        bindReviewContainer(reviewCriticRatingContainer, viewModel.showCriticRatingReviewProperty());
        bindReviewContainer(reviewOverviewContainer, viewModel.showOverviewReviewProperty());
        bindReviewContainer(reviewReleaseDateContainer, viewModel.showReleaseDateReviewProperty());
        bindReviewContainer(reviewOriginalTitleContainer, viewModel.showOriginalTitleReviewProperty());
        bindReviewContainer(reviewStudiosContainer, viewModel.showStudiosReviewProperty());
        bindReviewContainer(reviewPeopleContainer, viewModel.showPeopleReviewProperty());
        bindReviewContainer(reviewGenresContainer, viewModel.showGenresReviewProperty());
        bindReviewContainer(reviewTagsContainer, viewModel.showTagsReviewProperty());

        saveButton.disableProperty().bind(viewModel.isDirtyProperty().not());
        savePrimaryImageButton.visibleProperty().bind(viewModel.primaryImageDirtyProperty());
        savePrimaryImageButton.managedProperty().bind(viewModel.primaryImageDirtyProperty());
    }

    private void bindReviewContainer(HBox container, ReadOnlyBooleanProperty visibilityProperty) {
        if (container != null && visibilityProperty != null) {
            container.visibleProperty().bind(visibilityProperty);
            container.managedProperty().bind(visibilityProperty);
        }
    }

    /** Helper: Updates the image gallery FlowPane. */
    private void updateImageGallery() {
        Platform.runLater(() -> {
            imageGalleryPane.getChildren().clear();
            if (viewModel == null || viewModel.getEmbyService() == null || viewModel.getCurrentItemId() == null) return;
            String serverUrl = viewModel.getEmbyService().getApiClient().getBasePath();
            String itemId = viewModel.getCurrentItemId();
            for (ImageInfo imageInfo : viewModel.getBackdropImages()) {
                BackdropView backdropView = new BackdropView(imageInfo, serverUrl, itemId, viewModel::deleteBackdrop);
                imageGalleryPane.getChildren().add(backdropView);
            }
        });
    }

    /** Helper: Updates the tags FlowPane. */
    private void updateTagsFlowPane() {
        if (viewModel == null || tagsFlowPane == null) return;
        tagsFlowPane.getChildren().clear();
        for (TagModel tag : viewModel.getTagItems()) {
            // <-- SỬA ĐỔI DÒNG NÀY: Thêm 'this::handleTagChipClicked' -->
            TagView tagChip = new TagView(tag, viewModel::removeTag, this::handleTagChipClicked);
            tagsFlowPane.getChildren().add(tagChip);
        }
    }

    /** Helper: Updates the studios FlowPane. */
    private void updateStudiosFlowPane() {
        if (viewModel == null || studiosFlowPane == null) return;
        studiosFlowPane.getChildren().clear();
        for (TagModel studio : viewModel.getStudioItems()) {
            // <-- SỬA ĐỔI DÒNG NÀY: Thêm 'this::handleStudioChipClicked' -->
            TagView studioChip = new TagView(studio, viewModel::removeStudio, this::handleStudioChipClicked);
            studiosFlowPane.getChildren().add(studioChip);
        }
    }

    /** Helper: Updates the people FlowPane. */
    private void updatePeopleFlowPane() {
        if (viewModel == null || peopleFlowPane == null) return;
        peopleFlowPane.getChildren().clear();
        for (TagModel person : viewModel.getPeopleItems()) {
            // <-- SỬA ĐỔI DÒNG NÀY: Thêm 'this::handlePeopleChipClicked' -->
            TagView personChip = new TagView(person, viewModel::removePerson, this::handlePeopleChipClicked);
            peopleFlowPane.getChildren().add(personChip);
        }
    }

    /** Helper: Updates the genres FlowPane. */
    private void updateGenresFlowPane() {
        if (viewModel == null || genresFlowPane == null) return;
        genresFlowPane.getChildren().clear();
        for (TagModel genre : viewModel.getGenreItems()) {
            // <-- SỬA ĐỔI DÒNG NÀY: Thêm 'this::handleGenreChipClicked' -->
            TagView genreChip = new TagView(genre, viewModel::removeGenre, this::handleGenreChipClicked);
            genresFlowPane.getChildren().add(genreChip);
        }
        if (genresLabel != null) {
            genresLabel.visibleProperty().set(false);
            genresLabel.managedProperty().set(false);
        }
    }

    // <-- THÊM 4 HÀM MỚI DƯỚI ĐÂY -->

    /** (MỚI) Xử lý khi click vào chip Tag. */
    private void handleTagChipClicked(TagModel model) {
        if (viewModel != null) {
            viewModel.loadItemsByTagChip(model, "TAG");
        }
    }

    /** (MỚI) Xử lý khi click vào chip Studio. */
    private void handleStudioChipClicked(TagModel model) {
        if (viewModel != null) {
            viewModel.loadItemsByTagChip(model, "STUDIO");
        }
    }

    /** (MỚI) Xử lý khi click vào chip People. */
    private void handlePeopleChipClicked(TagModel model) {
        if (viewModel != null) {
            viewModel.loadItemsByTagChip(model, "PEOPLE");
        }
    }

    /** (MỚI) Xử lý khi click vào chip Genre. */
    private void handleGenreChipClicked(TagModel model) {
        if (viewModel != null) {
            viewModel.loadItemsByTagChip(model, "GENRE");
        }
    }

    /** Sets up the 10 rating buttons. */
    private void setupCriticRatingButtons() {
        if (criticRatingPane == null || viewModel == null) return;
        criticRatingPane.getChildren().clear();
        for (int i = 1; i <= 10; i++) {
            final int ratingValue = i;
            Button ratingButton = new Button(String.valueOf(ratingValue));
            ratingButton.getStyleClass().add("rating-button");
            ratingButton.setUserData(ratingValue);

            // (*** SỬA ĐỔI PHẦN NÀY ***)
            ratingButton.setOnAction(e -> {
                Float newRating;
                float buttonRating = (float) ratingValue;

                // Xác định giá trị mới (toggle on/off)
                if (Objects.equals(viewModel.criticRatingProperty().get(), buttonRating)) {
                    newRating = null; // Tắt rating
                } else {
                    newRating = buttonRating; // Bật rating
                }

                // 1. Cập nhật UI ngay lập tức
                viewModel.criticRatingProperty().set(newRating);

                // 2. Gọi hàm lưu độc lập của ViewModel
                viewModel.saveCriticRatingImmediately(newRating);
            });
            // (*** KẾT THÚC SỬA ĐỔI ***)

            criticRatingPane.getChildren().add(ratingButton);
        }
        updateRatingButtonSelection();
    }

    /** Updates the visual selection state of the rating buttons. */
    private void updateRatingButtonSelection() {
        if (criticRatingPane == null || viewModel == null) return;
        Float currentRating = viewModel.criticRatingProperty().get();
        Integer selectedValue = null;
        if (currentRating != null) {
            selectedValue = Math.round(currentRating);
        }
        for (Node node : criticRatingPane.getChildren()) {
            if (node instanceof Button && node.getUserData() instanceof Integer) {
                Button button = (Button) node;
                int buttonValue = (Integer) button.getUserData();
                if (selectedValue != null && buttonValue == selectedValue) {
                    if (!button.getStyleClass().contains("selected")) {
                        button.getStyleClass().add("selected");
                    }
                } else {
                    button.getStyleClass().remove("selected");
                }
            }
        }
    }

    /** Shows the dialog for adding a new Tag, Studio, Person, or Genre. */
    private void showAddTagDialog(AddTagDialogController.SuggestionContext context) {
        if (viewModel == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("AddTagDialog.fxml"));
            VBox page = loader.load();
            Stage dialogStage = new Stage();
            AddTagDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setContext(context, itemRepository);

            String title;
            I18nManager i18n = I18nManager.getInstance();
            switch (context) {
                case STUDIO: title = i18n.getString("addTagDialog", "addStudioTitle"); break;
                case PEOPLE: title = i18n.getString("addTagDialog", "addPeopleTitle"); break;
                case GENRE:  title = i18n.getString("addTagDialog", "addGenreTitle"); break;
                case TAG: default: title = i18n.getString("addTagDialog", "addTagTitle"); break;
            }

            dialogStage.setTitle(title);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner((Stage) rootPane.getScene().getWindow());
            Scene scene = new Scene(page);
            scene.getStylesheets().addAll(rootPane.getScene().getStylesheets());
            dialogStage.setScene(scene);

            double savedX = prefs.getDouble(KEY_ADD_TAG_DIALOG_X, -1);
            double savedY = prefs.getDouble(KEY_ADD_TAG_DIALOG_Y, -1);
            double savedWidth = prefs.getDouble(KEY_ADD_TAG_DIALOG_WIDTH, -1);
            double savedHeight = prefs.getDouble(KEY_ADD_TAG_DIALOG_HEIGHT, -1);

            if (savedX != -1 && savedY != -1) {
                dialogStage.setX(savedX);
                dialogStage.setY(savedY);
            }
            if (savedWidth > 0) {
                dialogStage.setWidth(savedWidth);
            }
            if (savedHeight > 0) {
                dialogStage.setHeight(savedHeight);
            }

            dialogStage.setOnCloseRequest(e -> {
                try {
                    prefs.putDouble(KEY_ADD_TAG_DIALOG_X, dialogStage.getX());
                    prefs.putDouble(KEY_ADD_TAG_DIALOG_Y, dialogStage.getY());
                    prefs.putDouble(KEY_ADD_TAG_DIALOG_WIDTH, dialogStage.getWidth());
                    prefs.putDouble(KEY_ADD_TAG_DIALOG_HEIGHT, dialogStage.getHeight());
                    prefs.flush();
                } catch (Exception ex) {
                    System.err.println("Lỗi khi lưu vị trí/kích thước AddTagDialog: " + ex.getMessage());
                }
            });

            dialogStage.showAndWait();
            Platform.runLater(() -> {
                // Đặt focus về rootPane của ItemDetailController để đảm bảo không có input field nào đang focus,
                // cho phép global hotkey ENTER được kích hoạt.
                if (rootPane != null) {
                    rootPane.requestFocus();
                    // Log này có thể giúp bạn debug nếu cần
                    // System.out.println("Focus set to rootPane after dialog close.");
                }
            });

            // (*** LƯU CONTEXT VỪA CHỌN SAU KHI DIALOG ĐÓNG ***)
            this.lastAddContext = context;

            TagModel newModel = controller.getResultTag();
            String copyId = controller.copyTriggeredIdProperty().get();

            if (newModel != null) {
                switch (context) {
                    case STUDIO: viewModel.addStudio(newModel); break;
                    case PEOPLE: viewModel.addPerson(newModel); break;
                    case GENRE: viewModel.addGenre(newModel); break;
                    case TAG: viewModel.addTag(newModel); break;
                }
            } else if (copyId != null) {
                viewModel.copyPropertiesFromItem(copyId, context);
            }

        } catch (IOException e) {
            e.printStackTrace();
            viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorOpenDialog", context.name().toLowerCase()));
        } catch (Exception e) {
            e.printStackTrace();
            viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorOpenDialogGeneric", e.getMessage()));
        }
    }


    /** Handles the Open/Play button action. */
    @FXML
    private void handleOpenButtonAction() {
        if (viewModel == null) return;
        viewModel.clearActionError();
        String path = pathTextField.getText();
        if (path == null || path.isEmpty() || path.equals(I18nManager.getInstance().getString("itemDetailLoader", "noPath"))) { // <-- Sửa lỗi hardcode
            viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorInvalidPath"));
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorDesktopAPINotSupported"));
            return;
        }
        new Thread(() -> {
            try {
                File fileOrDir = new File(path);
                if (!fileOrDir.exists()) {
                    viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorPathNotExist"));
                    return;
                }
                Desktop.getDesktop().open(fileOrDir);
                if (viewModel != null && !viewModel.isFolderProperty().get()) {
                    Platform.runLater(() -> viewModel.requestPopOut());
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi mở đường dẫn: " + path + " | " + e.getMessage());
                viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorOpenPath", e.getMessage()));
            }
        }).start();
    }

    // <-- HÀM MỚI (BAO GỒM LOGIC MỚI CỦA BẠN) -->
    /**
     * (HÀM MỚI) Xử lý nút Mở Subtitle.
     * Tạo file .srt nếu chưa tồn tại, sau đó mở file .srt VÀ thư mục chứa nó.
     */
    @FXML
    private void handleOpenSubtitleAction() {
        if (viewModel == null) return;
        viewModel.clearActionError();
        I18nManager i18n = I18nManager.getInstance();

        // 1. Lấy đường dẫn file media
        String mediaPath = pathTextField.getText();

        // (*** BẮT ĐẦU THAY ĐỔI: Lấy tiêu đề item ***)
        final String itemTitle = titleTextField.getText(); // Lấy tiêu đề từ text field
        // (*** KẾT THÚC THAY ĐỔI ***)

        if (mediaPath == null || mediaPath.isEmpty() || mediaPath.equals(i18n.getString("itemDetailLoader", "noPath"))) {
            viewModel.reportActionError(i18n.getString("itemDetailView", "errorInvalidPath"));
            return;
        }

        // 2. Kiểm tra Desktop API
        if (!Desktop.isDesktopSupported()) {
            viewModel.reportActionError(i18n.getString("itemDetailView", "errorDesktopAPINotSupported"));
            return;
        }

        // 3. (Double check) Không chạy nếu là thư mục
        if (viewModel.isFolderProperty().get()) {
            return;
        }

        // 4. Tạo đường dẫn file .srt
        String srtPath;
        int dotIndex = mediaPath.lastIndexOf('.');
        if (dotIndex == -1) {
            // Không có phần mở rộng file, không thể tạo đường dẫn .srt
            viewModel.reportActionError(i18n.getString("itemDetailView", "errorInvalidPath"));
            return;
        }
        String pathWithoutExt = mediaPath.substring(0, dotIndex);
        srtPath = pathWithoutExt + ".srt";

        // 5. Chạy logic trong luồng nền
        new Thread(() -> { // Biến itemTitle (final) sẽ được lambda capture
            try {
                File mediaFile = new File(mediaPath);
                File srtFile = new File(srtPath);

                // Lấy thư mục cha
                File parentFolder = mediaFile.getParentFile();

                // 6. Kiểm tra file media gốc và thư mục cha
                if (!mediaFile.exists()) {
                    Platform.runLater(() -> viewModel.reportActionError(i18n.getString("itemDetailView", "errorPathNotExist")));
                    return;
                }
                if (parentFolder == null || !parentFolder.exists()) {
                    throw new IOException("Không thể tìm thấy thư mục cha.");
                }

                // 7. (Logic 1 - SỬA ĐỔI) Kiểm tra và TẠO file .srt UTF-8 BOM nếu chưa có
                if (!srtFile.exists()) {
                    try {
                        // Sử dụng try-with-resources để tự đóng writer
                        try (FileOutputStream fos = new FileOutputStream(srtFile);
                             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                             BufferedWriter writer = new BufferedWriter(osw)) {

                            // Ghi BOM (Byte Order Mark) cho UTF-8
                            writer.write('\uFEFF');

                            // Ghi nội dung SRT cơ bản
                            writer.write("1");
                            writer.newLine();
                            writer.write("00:00:03,000 --> 00:00:10,000");
                            writer.newLine();

                            // (*** BẮT ĐẦU THAY ĐỔI: Ghi tiêu đề làm sub đầu tiên ***)
                            if (itemTitle != null && !itemTitle.isEmpty()) {
                                writer.write(itemTitle);
                            } else {
                                writer.write(""); // Ghi rỗng nếu title rỗng
                            }
                            // (*** KẾT THÚC THAY ĐỔI ***)
                            writer.newLine();
                            writer.newLine();
                            writer.write("2");
                            writer.newLine();
                            writer.write("00:30:00,000 --> 00:30:10,000");
                            writer.newLine();
                            writer.write("");
                            writer.newLine();
                            writer.write("3");
                            writer.newLine();
                            writer.write("01:00:00,000 --> 01:00:10,000");
                            writer.newLine();
                            writer.write("");
                            writer.newLine();
                            writer.write("4");
                            writer.newLine();
                            writer.write("01:30:00,000 --> 01:30:10,000");
                            writer.newLine();
                            writer.write("");
                            writer.newLine();
                            writer.write("5");
                            writer.newLine();
                            writer.write("01:50:00,000 --> 01:50:10,000");
                            writer.newLine();
                            writer.write("");
                            writer.newLine();
                            writer.newLine();

                            writer.flush(); // Đảm bảo dữ liệu được ghi
                        }
                        // Thông báo tạo file thành công
                        Platform.runLater(() -> viewModel.reportActionError(i18n.getString("itemDetailView", "statusSubtitleCreated", srtFile.getName())));
                    } catch (IOException createEx) {
                        System.err.println("Lỗi khi tạo file subtitle: " + createEx.getMessage());
                        Platform.runLater(() -> viewModel.reportActionError(i18n.getString("itemDetailView", "errorSubtitleCreate", createEx.getMessage())));
                        return; // Dừng lại nếu không tạo được file
                    }
                }

                // 8. (Logic 2) Mở file .srt
                try {
                    Desktop.getDesktop().open(srtFile);
                } catch (Exception openSubEx) {
                    // Báo lỗi nhưng vẫn tiếp tục để thử mở thư mục
                    Platform.runLater(() -> viewModel.reportActionError(i18n.getString("itemDetailView", "errorSubtitleOpen", openSubEx.getMessage())));
                }

                // 9. (Logic 3 - MỚI) Mở thư mục cha
                try {
                    Desktop.getDesktop().open(parentFolder);
                } catch (Exception openFolderEx) {
                    // Báo lỗi mở thư mục
                    Platform.runLater(() -> viewModel.reportActionError(i18n.getString("itemDetailView", "errorSubtitleOpenFolder", openFolderEx.getMessage())));
                }

            } catch (Exception e) {
                // Bắt các lỗi chung khác (ví dụ: lỗi quyền khi mở file, lỗi tìm thư mục cha)
                System.err.println("Lỗi khi mở/tạo subtitle hoặc thư mục: " + srtPath + " | " + e.getMessage());
                Platform.runLater(() -> viewModel.reportActionError(i18n.getString("itemDetailView", "errorSubtitleOpen", e.getMessage())));
            }
        }).start();
    }
    // <-- KẾT THÚC HÀM MỚI -->

    // --- (*** BẮT ĐẦU HÀM MỚI ***) ---
    /**
     * (HÀM MỚI) Xử lý click vào ảnh primary để mở thư mục screenshot.
     */
    private void handleOpenScreenshotFolder() {
        if (viewModel == null) return;
        viewModel.clearActionError();
        I18nManager i18n = I18nManager.getInstance();

        // 1. Lấy đường dẫn file media
        String mediaPath = pathTextField.getText();
        if (mediaPath == null || mediaPath.isEmpty() || mediaPath.equals(i18n.getString("itemDetailLoader", "noPath"))) {
            viewModel.reportActionError(i18n.getString("itemDetailView", "errorInvalidPath"));
            return;
        }

        // 2. Lấy đường dẫn base từ config
        String basePath = i18n.getString("appSettings", "screenshotBasePath");
        if (basePath == null || basePath.isEmpty() || basePath.equals("appSettings.screenshotBasePath")) {
            viewModel.reportActionError(i18n.getString("itemDetailView", "errorScreenshotPathBase"));
            return;
        }

        // 3. Kiểm tra Desktop API
        if (!Desktop.isDesktopSupported()) {
            viewModel.reportActionError(i18n.getString("itemDetailView", "errorDesktopAPINotSupported"));
            return;
        }

        new Thread(() -> {
            try {
                // 4. Lấy tên file (bao gồm đuôi)
                File mediaFile = new File(mediaPath);
                String mediaFileName = mediaFile.getName();
                if (mediaFileName.isEmpty()) {
                    throw new IOException("Không thể lấy tên file từ đường dẫn: " + mediaPath);
                }

                // 5. Xây dựng đường dẫn thư mục screenshot
                File screenshotFolder = new File(basePath, mediaFileName);

                // 6. Kiểm tra thư mục
                if (!screenshotFolder.exists()) {
                    // Tùy chọn: Tự động tạo thư mục nếu chưa có
                    // if (!screenshotFolder.mkdirs()) {
                    //     throw new IOException("Không thể tạo thư mục: " + screenshotFolder.getAbsolutePath());
                    // }
                    // Hoặc: Báo lỗi nếu chưa tồn tại
                    throw new FileNotFoundException("Thư mục screenshot không tồn tại: " + screenshotFolder.getAbsolutePath());
                }

                if (!screenshotFolder.isDirectory()) {
                    throw new IOException("Đường dẫn screenshot không phải là thư mục: " + screenshotFolder.getAbsolutePath());
                }

                // 7. Mở thư mục
                Desktop.getDesktop().open(screenshotFolder);

            } catch (Exception e) {
                System.err.println("Lỗi khi mở thư mục screenshot: " + e.getMessage());
                viewModel.reportActionError(i18n.getString("itemDetailView", "errorScreenshotPathOpen", e.getMessage()));
            }
        }).start();
    }
    // --- (*** KẾT THÚC HÀM MỚI ***) ---


    /** Handles the Fetch Release Date button action. */
    @FXML
    private void handleFetchReleaseDateAction() {
        if (viewModel != null) {
            viewModel.fetchReleaseDate();
        }
    }

    /** Handles the Add Tag button action. */
    @FXML
    private void handleAddTagButtonAction() {
        showAddTagDialog(AddTagDialogController.SuggestionContext.TAG);
    }

    /** Handles the Add Studio button action. */
    @FXML
    private void handleAddStudioButtonAction() {
        showAddTagDialog(AddTagDialogController.SuggestionContext.STUDIO);
    }

    /** Handles the Add People button action. */
    @FXML
    private void handleAddPeopleButtonAction() {
        showAddTagDialog(AddTagDialogController.SuggestionContext.PEOPLE);
    }

    /** Handles the Add Genre button action. */
    @FXML
    private void handleAddGenreButtonAction() {
        showAddTagDialog(AddTagDialogController.SuggestionContext.GENRE);
    }

    /** Handles the Save button action. */
    @FXML
    private void handleSaveButtonAction() {
        // System.out.println("Nút Lưu đã được nhấn. Gọi ViewModel.saveChanges().");
        if (viewModel != null) {
            viewModel.saveChanges();
        }
    }

    /**
     * (*** MỚI - HOTKEY LOGIC - Request 1 ***)
     * Mở lại dialog "Add Tag" gần nhất (dùng cho phím ENTER).
     */
    public void handleRepeatAddTagDialog() {
        // Kiểm tra xem có item nào đang được hiển thị không
        if (viewModel == null || viewModel.getCurrentItemId() == null) {
            // Chỉ báo lỗi nếu có viewModel (không phải dialog pop-out)
            if (rootPane.getScene() != null && rootPane.getScene().getWindow() instanceof Stage && ((Stage)rootPane.getScene().getWindow()).getModality() != Modality.NONE) {
                // Nếu là dialog pop-out thì bỏ qua
            } else {
                viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailViewModel", "errorSave")); // Dùng lại thông báo lỗi không có item
            }
            return;
        }
        // Kiểm tra xem đã mở dialog nào trước đó chưa
        if (lastAddContext != null) {
            showAddTagDialog(lastAddContext);
        } else {
            // Nếu chưa mở lần nào, mặc định mở dialog Add Tag
            showAddTagDialog(AddTagDialogController.SuggestionContext.TAG);
        }
    }

    /**
     * (*** MỚI - HOTKEY LOGIC - Request 2 ***)
     * Xử lý hotkey Cmd+S (Save) từ MainController.
     */
    public void handleSaveHotkey() {
        if (saveButton.disableProperty().get() == false) {
            handleSaveButtonAction();
        } else {
            // Có thể báo lỗi nhẹ nếu cần, nhưng thường chỉ cần bỏ qua
            // System.out.println("Hotkey Save bị chặn: Không có thay đổi.");
        }
    }


    /** Handles the Import JSON button action. */
    @FXML
    private void handleImportButtonAction() {
        if (viewModel == null) return;
        viewModel.clearActionError();
        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            File selectedFile = JsonFileHandler.showOpenJsonDialog(stage);
            if (selectedFile != null) {
                viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorImportRead"));
                new Thread(() -> {
                    try {
                        BaseItemDto importedDto = JsonFileHandler.readJsonFileToObject(selectedFile);
                        if (importedDto != null) {
                            Platform.runLater(() -> {
                                viewModel.importAndPreview(importedDto);
                            });
                        } else {
                            throw new Exception(I18nManager.getInstance().getString("itemDetailView", "errorImportInvalid"));
                        }
                    } catch (Exception ex) {
                        System.err.println("Lỗi khi Import (luồng nền): " + ex.getMessage());
                        Platform.runLater(() -> viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorImportReadThread", ex.getMessage())));
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi Import (hiển thị dialog): " + e.getMessage());
            viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorImportDialog", e.getMessage()));
        }
    }

    /** Handles the Export JSON button action. */
    @FXML
    private void handleExportButtonAction() {
        if (viewModel == null) return;
        viewModel.clearActionError();
        try {
            BaseItemDto dtoToExport = viewModel.getItemForExport();
            if (dtoToExport == null) {
                viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorExportNoData"));
                return;
            }
            Stage stage = (Stage) rootPane.getScene().getWindow();
            String originalTitle = viewModel.getOriginalTitleForExport();
            // System.out.println(originalTitle);
            String initialFileName = (originalTitle != null ? originalTitle.replaceAll("[^a-zA-Z0-9.-]", "_") : "item") + ".json";
            // System.out.println(initialFileName);
            File targetFile = JsonFileHandler.showSaveJsonDialog(stage, initialFileName);
            if (targetFile != null) {
                new Thread(() -> {
                    try {
                        JsonFileHandler.writeObjectToJsonFile(dtoToExport, targetFile);
                        Platform.runLater(() -> viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "exportSuccess", targetFile.getName())));
                    } catch (Exception ex) {
                        System.err.println("Lỗi khi Export (luồng nền): " + ex.getMessage());
                        Platform.runLater(() -> viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorExportWriteThread", ex.getMessage())));
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi Export (hiển thị dialog): " + e.getMessage());
            viewModel.reportActionError(I18nManager.getInstance().getString("itemDetailView", "errorExportDialog", e.getMessage()));
        }
    }

    /**
     * (HELPER) Kiểm tra xem file có phải là ảnh hợp lệ không.
     */
    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".webp");
    }

    /**
     * (HELPER) Lấy danh sách file ảnh từ Dragboard.
     */
    private List<File> getImageFilesFromDragboard(Dragboard db) {
        if (db.hasFiles()) {
            return db.getFiles().stream()
                    .filter(this::isImageFile)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * (HELPER) Lấy file ảnh ĐẦU TIÊN từ Dragboard.
     */
    private Optional<File> getFirstImageFileFromDragboard(Dragboard db) {
        if (db.hasFiles()) {
            return db.getFiles().stream()
                    .filter(this::isImageFile)
                    .findFirst();
        }
        return Optional.empty();
    }

    /**
     * (HÀM MỚI) Sets up drag and drop functionality for the PRIMARY image.
     */
    private void setupPrimaryImageDragAndDrop() {
        if (primaryImageContainer == null) return;

        primaryImageContainer.setOnDragOver(event -> {
            if (event.getGestureSource() != primaryImageContainer && event.getDragboard().hasFiles()) {
                if (getFirstImageFileFromDragboard(event.getDragboard()).isPresent()) {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
            }
            event.consume();
        });

        primaryImageContainer.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && viewModel != null) {
                Optional<File> imageFile = getFirstImageFileFromDragboard(db);

                if (imageFile.isPresent()) {
                    viewModel.setDroppedPrimaryImage(imageFile.get());
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }


    /**
     * (SỬA ĐỔI) Sets up drag and drop functionality for the backdrop gallery.
     */
    private void setupBackdropDragAndDrop() {
        if (imageGalleryPane == null) return;

        Node[] dropTargets = {imageGalleryPane, imageGalleryScrollPane};

        for (Node target : dropTargets) {
            target.setOnDragOver(event -> {
                if (event.getGestureSource() != target && event.getDragboard().hasFiles()) {
                    List<File> files = getImageFilesFromDragboard(event.getDragboard());
                    if (!files.isEmpty()) {
                        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    }
                }
                event.consume();
            });

            target.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles() && viewModel != null) {
                    List<File> imageFiles = getImageFilesFromDragboard(db);

                    if (!imageFiles.isEmpty()) {
                        viewModel.uploadDroppedBackdropFiles(imageFiles);
                        success = true;
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            });
        }
    }

    /**
     * (HÀM MỚI) Hiển thị hộp thoại xác nhận.
     * @param title Tiêu đề hộp thoại.
     * @param content Nội dung thông báo.
     * @return true nếu người dùng chọn Yes, false nếu chọn No/Cancel.
     */
    private boolean showConfirmationDialog(String title, String content) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        if (rootPane.getScene() != null && rootPane.getScene().getStylesheets() != null) {
            alert.getDialogPane().getStylesheets().addAll(rootPane.getScene().getStylesheets());
        }
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }
}