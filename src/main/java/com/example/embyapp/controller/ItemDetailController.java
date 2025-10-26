package com.example.embyapp.controller;

import embyclient.model.BaseItemDto;
import embyclient.model.ImageInfo;
import com.example.embyapp.MainApp;
import com.example.embyapp.service.EmbyService;
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
import java.util.stream.Collectors;

/**
 * (CẬP NHẬT 31) Cập nhật bố cục UI Detail.
 * - Sửa FXML fields để khớp với cấu trúc mới (Path, Release Date, Fixed Footer).
 * - Cập nhật logic binding để khớp với cấu trúc mới.
 * (FIX LỖI) Khắc phục lỗi statusLabel không mất đi sau khi tải xong và xóa binding cho actionStatusLabel.
 * (CẬP NHẬT MỚI) Thêm nút review cho Tags.
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
    @FXML private TextArea overviewTextArea;
    @FXML private Label genresLabel; // Giữ lại nhưng bị ẩn

    // (*** GALLERY ***)
    @FXML private HBox backdropHeaderHBox; // (*** KHAI BÁO MỚI ***)
    @FXML private ScrollPane imageGalleryScrollPane; // (*** KHAI BÁO MỚI ***)
    @FXML private Button addBackdropButton;
    @FXML private FlowPane imageGalleryPane;

    // (*** CÁC TRƯỜNG DỮ LIỆU MỚI ***)
    @FXML private HBox pathContainer; // Container cho path/open
    @FXML private TextField pathTextField;
    @FXML private Button openButton;
    @FXML private Label actionStatusLabel; // Vẫn khai báo để tránh NPE khi FXML field bị xóa.

    @FXML private HBox releaseDateContainer; // Container cho date/tagline/runtime
    @FXML private TextField releaseDateTextField;

    // (*** TAGS ***)
    @FXML private FlowPane tagsFlowPane;
    @FXML private Button addTagButton;

    // (*** STUDIOS/PEOPLE/GENRES DẠNG CHIP ***)
    @FXML private FlowPane studiosFlowPane;
    @FXML private Button addStudioButton;
    @FXML private FlowPane peopleFlowPane;
    @FXML private Button addPeopleButton;
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

    @FXML
    public void initialize() {
        // (Gán sự kiện onAction cho các nút (v/x) giữ nguyên)
        acceptTitleButton.setOnAction(e -> viewModel.acceptImportField("title"));
        rejectTitleButton.setOnAction(e -> viewModel.rejectImportField("title"));
        acceptOverviewButton.setOnAction(e -> viewModel.acceptImportField("overview"));
        rejectOverviewButton.setOnAction(e -> viewModel.rejectImportField("overview"));
        acceptReleaseDateButton.setOnAction(e -> viewModel.acceptImportField("releaseDate"));
        rejectReleaseDateButton.setOnAction(e -> viewModel.rejectImportField("releaseDate"));
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
                        .then("Mở trong Finder")
                        .otherwise("Phát (Mặc định)")
        );

        releaseDateContainer.visibleProperty().bind(viewModel.isFolderProperty().not());
        releaseDateContainer.managedProperty().bind(viewModel.isFolderProperty().not());

        // 10. Binding Review Containers
        bindReviewContainer(reviewTitleContainer, viewModel.showTitleReviewProperty());
        bindReviewContainer(reviewOverviewContainer, viewModel.showOverviewReviewProperty());
        bindReviewContainer(reviewReleaseDateContainer, viewModel.showReleaseDateReviewProperty());
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
     * Helper chung để mở dialog thêm Studio/People/Tag/Genre.
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
            String title = context == AddTagDialogController.SuggestionContext.STUDIO ? "Thêm Studio Mới" :
                    context == AddTagDialogController.SuggestionContext.PEOPLE ? "Thêm Người Mới" :
                            context == AddTagDialogController.SuggestionContext.GENRE ? "Thêm Thể Loại Mới" :
                                    "Thêm Tag Mới";
            dialogStage.setTitle(title);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner((Stage) rootPane.getScene().getWindow());
            Scene scene = new Scene(page);
            scene.getStylesheets().addAll(rootPane.getScene().getStylesheets());
            dialogStage.setScene(scene);

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
            viewModel.reportActionError("Lỗi: Không thể mở dialog thêm " + context.name().toLowerCase() + ".");
        } catch (Exception e) {
            e.printStackTrace();
            viewModel.reportActionError("Lỗi không xác định: " + e.getMessage());
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
            viewModel.reportActionError("Lỗi: Đường dẫn không hợp lệ.");
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            viewModel.reportActionError("Lỗi: Desktop API không được hỗ trợ.");
            return;
        }
        new Thread(() -> {
            try {
                File fileOrDir = new File(path);
                if (!fileOrDir.exists()) {
                    viewModel.reportActionError("Lỗi: Đường dẫn không tồn tại.");
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
                viewModel.reportActionError("Lỗi: " + e.getMessage());
            }
        }).start();
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
                viewModel.reportActionError("Đang đọc file JSON...");
                new Thread(() -> {
                    try {
                        BaseItemDto importedDto = JsonFileHandler.readJsonFileToObject(selectedFile);
                        if (importedDto != null) {
                            Platform.runLater(() -> {
                                viewModel.importAndPreview(importedDto);
                            });
                        } else {
                            throw new Exception("File JSON không hợp lệ hoặc rỗng.");
                        }
                    } catch (Exception ex) {
                        System.err.println("Lỗi khi Import (luồng nền): " + ex.getMessage());
                        Platform.runLater(() -> viewModel.reportActionError("Lỗi Import: " + ex.getMessage()));
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi Import (hiển thị dialog): " + e.getMessage());
            viewModel.reportActionError("Lỗi Import: " + e.getMessage());
        }
    }
    @FXML
    private void handleExportButtonAction() {
        if (viewModel == null) return;
        viewModel.clearActionError();
        try {
            BaseItemDto dtoToExport = viewModel.getItemForExport();
            if (dtoToExport == null) {
                viewModel.reportActionError("Lỗi: Không có dữ liệu item để export.");
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
                        Platform.runLater(() -> viewModel.reportActionError("Đã export thành công ra " + targetFile.getName()));
                    } catch (Exception ex) {
                        System.err.println("Lỗi khi Export (luồng nền): " + ex.getMessage());
                        Platform.runLater(() -> viewModel.reportActionError("Lỗi Export: " + ex.getMessage()));
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi Export (hiển thị dialog): " + e.getMessage());
            viewModel.reportActionError("Lỗi Export: " + e.getMessage());
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