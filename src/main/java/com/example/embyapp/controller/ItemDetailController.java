package com.example.embyapp.controller;

import embyclient.model.BaseItemDto;
import embyclient.model.ImageInfo; // <-- THÊM IMPORT
import com.example.embyapp.MainApp;
import com.example.embyapp.service.EmbyService; // <-- THÊM IMPORT
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
import javafx.scene.input.Dragboard; // <-- THÊM IMPORT
import javafx.scene.input.TransferMode; // <-- THÊM IMPORT
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.List; // <-- THÊM IMPORT
import java.util.stream.Collectors; // <-- THÊM IMPORT

/**
 * (CẬP NHẬT 18)
 * - Thêm FXML cho các nút ảnh mới.
 * - Sửa updateImageGallery để dùng BackdropView.
 * - Thêm handler cho click/drag-drop ảnh.
 */
public class ItemDetailController {

    // --- FXML Components ---
    @FXML private StackPane rootPane;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;
    @FXML private ScrollPane mainScrollPane;
    @FXML private VBox detailContentPane;

    // (*** THÊM MỚI FXML ẢNH ***)
    @FXML private StackPane primaryImageContainer;
    @FXML private ImageView primaryImageView;
    @FXML private Button savePrimaryImageButton;
    // (*** KẾT THÚC THÊM MỚI ***)

    @FXML private TextField titleTextField;
    @FXML private Button saveButton;
    @FXML private TextArea overviewTextArea;
    @FXML private Label taglineLabel;
    @FXML private Label genresLabel;

    // (*** THÊM MỚI FXML ẢNH ***)
    @FXML private Button addBackdropButton;
    @FXML private FlowPane imageGalleryPane;
    // (*** KẾT THÚC THÊM MỚI ***)

    // ... (Các FXML cho Path, Tags, Fields, Import/Export, Review Buttons... giữ nguyên) ...
    @FXML private VBox pathContainer;
    @FXML private TextField pathTextField;
    @FXML private Button openButton;
    @FXML private Label actionStatusLabel;
    @FXML private FlowPane tagsFlowPane;
    @FXML private Button addTagButton;
    @FXML private VBox fileOnlyContainer;
    @FXML private TextField releaseDateTextField;
    @FXML private TextField studiosTextField;
    @FXML private TextField peopleTextField;
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

    private ItemDetailViewModel viewModel;

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

        // (*** THÊM MỚI: Gán sự kiện cho các nút ảnh ***)
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

        // (*** THÊM MỚI: Xử lý Drag-Drop cho Backdrop ***)
        setupBackdropDragAndDrop();
    }

    /**
     * Được gọi bởi MainController để inject ViewModel.
     */
    public void setViewModel(ItemDetailViewModel viewModel) {
        this.viewModel = viewModel;

        // --- BINDING UI VỚI VIEWMODEL ---
        // (1, 2, 3 - Binding Labels, TextFields, Tags... giữ nguyên)
        taglineLabel.textProperty().bind(viewModel.taglineProperty());
        genresLabel.textProperty().bind(viewModel.genresProperty());
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());
        titleTextField.textProperty().bindBidirectional(viewModel.titleProperty());
        overviewTextArea.textProperty().bindBidirectional(viewModel.overviewProperty());
        viewModel.getTagItems().addListener((ListChangeListener<TagModel>) c -> {
            Platform.runLater(this::updateTagsFlowPane);
        });
        updateTagsFlowPane();
        releaseDateTextField.textProperty().bindBidirectional(viewModel.releaseDateProperty());
        studiosTextField.textProperty().bindBidirectional(viewModel.studiosProperty());
        peopleTextField.textProperty().bindBidirectional(viewModel.peopleProperty());

        // 4. Binding Ảnh Primary
        primaryImageView.imageProperty().bind(viewModel.primaryImageProperty());

        // 5. Binding kiểm soát hiển thị (Loading / Status / Content)
        loadingIndicator.visibleProperty().bind(viewModel.loadingProperty());
        statusLabel.visibleProperty().bind(viewModel.showStatusMessageProperty());
        mainScrollPane.visibleProperty().bind(
                viewModel.loadingProperty().not().and(viewModel.showStatusMessageProperty().not())
        );

        // 6. (*** SỬA ĐỔI: Binding Gallery (Lắng nghe danh sách ImageInfo) ***)
        viewModel.getBackdropImages().addListener((ListChangeListener<ImageInfo>) c -> {
            updateImageGallery();
        });
        updateImageGallery(); // Cập nhật lần đầu
        // (*** KẾT THÚC SỬA ĐỔI ***)

        // (7, 8 - Binding UI linh hoạt, Path... giữ nguyên)
        fileOnlyContainer.visibleProperty().bind(viewModel.isFolderProperty().not());
        fileOnlyContainer.managedProperty().bind(viewModel.isFolderProperty().not());
        if (pathContainer != null) {
            pathTextField.textProperty().bind(viewModel.itemPathProperty());
            pathContainer.visibleProperty().bind(
                    viewModel.itemPathProperty().isNotEmpty()
                            .and(viewModel.itemPathProperty().isNotEqualTo("Không có đường dẫn"))
            );
            openButton.textProperty().bind(
                    Bindings.when(viewModel.isFolderProperty())
                            .then("Mở trong Finder")
                            .otherwise("Phát (Mặc định)")
            );
            actionStatusLabel.textProperty().bind(viewModel.actionStatusMessageProperty());
        }

        // 9. (Binding (v/x) giữ nguyên)
        bindReviewContainer(reviewTitleContainer, viewModel.showTitleReviewProperty());
        bindReviewContainer(reviewOverviewContainer, viewModel.showOverviewReviewProperty());
        bindReviewContainer(reviewReleaseDateContainer, viewModel.showReleaseDateReviewProperty());
        bindReviewContainer(reviewStudiosContainer, viewModel.showStudiosReviewProperty());
        bindReviewContainer(reviewPeopleContainer, viewModel.showPeopleReviewProperty());

        // 10. (Binding nút Save giữ nguyên)
        saveButton.disableProperty().bind(viewModel.isDirtyProperty().not());

        // 11. (*** THÊM MỚI: Binding nút Lưu ảnh Primary ***)
        savePrimaryImageButton.visibleProperty().bind(viewModel.primaryImageDirtyProperty());
        savePrimaryImageButton.managedProperty().bind(viewModel.primaryImageDirtyProperty());
    }

    // (Hàm helper bindReviewContainer giữ nguyên)
    private void bindReviewContainer(HBox container, ReadOnlyBooleanProperty visibilityProperty) {
        if (container != null && visibilityProperty != null) {
            container.visibleProperty().bind(visibilityProperty);
            container.managedProperty().bind(visibilityProperty);
        }
    }

    /**
     * (*** SỬA ĐỔI: Helper: Cập nhật FlowPane gallery ***)
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
                        viewModel::deleteBackdrop // Truyền hàm delete
                );
                imageGalleryPane.getChildren().add(backdropView);
            }
        });
    }

    // (Hàm updateTagsFlowPane giữ nguyên)
    private void updateTagsFlowPane() {
        if (viewModel == null || tagsFlowPane == null) return;
        tagsFlowPane.getChildren().clear();
        for (TagModel tag : viewModel.getTagItems()) {
            TagView tagChip = new TagView(tag, viewModel::removeTag);
            tagsFlowPane.getChildren().add(tagChip);
        }
    }

    // (Hàm handleOpenButtonAction giữ nguyên)
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
                Desktop.getDesktop().open(fileOrDir);
            } catch (Exception e) {
                System.err.println("Lỗi khi mở đường dẫn: " + path + " | " + e.getMessage());
                viewModel.reportActionError("Lỗi: " + e.getMessage());
            }
        }).start();
    }

    // (Hàm handleAddTagButtonAction giữ nguyên)
    @FXML
    private void handleAddTagButtonAction() {
        if (viewModel == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("AddTagDialog.fxml"));
            VBox page = loader.load();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Thêm Tag Mới");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner((Stage) rootPane.getScene().getWindow());
            Scene scene = new Scene(page);
            scene.getStylesheets().addAll(rootPane.getScene().getStylesheets());
            dialogStage.setScene(scene);
            AddTagDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();
            TagModel newTag = controller.getResultTag();
            if (newTag != null) {
                viewModel.addTag(newTag);
            }
        } catch (IOException e) {
            e.printStackTrace();
            viewModel.reportActionError("Lỗi: Không thể mở dialog thêm tag.");
        } catch (Exception e) {
            e.printStackTrace();
            viewModel.reportActionError("Lỗi không xác định: " + e.getMessage());
        }
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
                                viewModel.reportActionError("Đã tải " + selectedFile.getName() + ". Vui lòng duyệt thay đổi.");
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


    // (*** HÀM MỚI: Cài đặt Drag-Drop ***)
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