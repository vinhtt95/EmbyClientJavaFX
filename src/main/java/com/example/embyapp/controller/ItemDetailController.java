package com.example.embyapp.controller;

import com.example.emby.modelEmby.BaseItemDto;
import com.example.embyapp.MainApp; // (*** MỚI IMPORT ***)
import com.example.embyapp.service.JsonFileHandler;
import com.example.embyapp.viewmodel.ItemDetailViewModel;
import com.example.embyapp.viewmodel.detail.TagModel; // (*** MỚI IMPORT ***)
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.ListChangeListener; // (*** MỚI IMPORT ***)
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader; // (*** MỚI IMPORT ***)
import javafx.scene.Scene; // (*** MỚI IMPORT ***)
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane; // (*** MỚI IMPORT ***)
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality; // (*** MỚI IMPORT ***)
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException; // (*** MỚI IMPORT ***)

/**
 * (CẬP NHẬT 7)
 * - Xóa TextField tagsTextField.
 * - Thêm FlowPane tagsFlowPane và Button addTagButton.
 * - Thêm logic lắng nghe ListChangeListener để cập nhật UI chip.
 * - Thêm logic mở Dialog "Thêm Tag".
 */
public class ItemDetailController {

    // --- FXML Components (TỪ FXML MỚI) ---
    @FXML private StackPane rootPane;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;
    @FXML private ScrollPane mainScrollPane;
    @FXML private VBox detailContentPane;
    @FXML private ImageView primaryImageView;

    // (MỚI) Thay đổi/Thêm
    @FXML private TextField titleTextField;
    @FXML private Button saveButton;
    @FXML private TextArea overviewTextArea;

    @FXML private Label taglineLabel;
    @FXML private Label genresLabel;
    @FXML private FlowPane imageGalleryPane;

    // FXML Fields cho Path
    @FXML private VBox pathContainer;
    @FXML private TextField pathTextField;
    @FXML private Button openButton;
    @FXML private Label actionStatusLabel;

    // (*** SỬA ĐỔI TAGS ***)
    // @FXML private TextField tagsTextField; // (ĐÃ XÓA)
    @FXML private FlowPane tagsFlowPane; // (MỚI)
    @FXML private Button addTagButton; // (MỚI)
    // (*** KẾT THÚC SỬA ĐỔI TAGS ***)

    @FXML private VBox fileOnlyContainer;
    @FXML private TextField releaseDateTextField;
    @FXML private TextField studiosTextField;
    @FXML private TextField peopleTextField;

    // (MỚI) Thêm @FXML cho các nút Import/Export
    @FXML private Button importButton;
    @FXML private Button exportButton;

    // (MỚI) Thêm @FXML cho các Container và Nút (v/x)
    @FXML private HBox reviewTitleContainer;
    @FXML private Button acceptTitleButton;
    @FXML private Button rejectTitleButton;

    @FXML private HBox reviewOverviewContainer;
    @FXML private Button acceptOverviewButton;
    @FXML private Button rejectOverviewButton;

    // (*** SỬA ĐỔI TAGS ***)
    // (FXML này không còn tồn tại trong FXML mới, nhưng để an toàn,
    // ta sẽ không binding nó nữa)
    // @FXML private HBox reviewTagsContainer;
    // @FXML private Button acceptTagsButton;
    // @FXML private Button rejectTagsButton;
    // (*** KẾT THÚC SỬA ĐỔI TAGS ***)

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
    private static final double BACKDROP_THUMBNAIL_HEIGHT = 100;

    @FXML
    public void initialize() {
        // (MỚI) Gán sự kiện onAction cho các nút (v/x)
        // Dùng lambda để gọi hàm trong ViewModel
        // Đặt ở đây vì FXML đã được inject
        acceptTitleButton.setOnAction(e -> viewModel.acceptImportField("title"));
        rejectTitleButton.setOnAction(e -> viewModel.rejectImportField("title"));

        acceptOverviewButton.setOnAction(e -> viewModel.acceptImportField("overview"));
        rejectOverviewButton.setOnAction(e -> viewModel.rejectImportField("overview"));

        // (*** SỬA ĐỔI TAGS ***)
        // (XÓA 2 DÒNG NÀY VÌ reviewTagsContainer KHÔNG CÒN Ý NGHĨA)
        // acceptTagsButton.setOnAction(e -> viewModel.acceptImportField("tags"));
        // rejectTagsButton.setOnAction(e -> viewModel.rejectImportField("tags"));
        // (*** KẾT THÚC SỬA ĐỔI TAGS ***)

        acceptReleaseDateButton.setOnAction(e -> viewModel.acceptImportField("releaseDate"));
        rejectReleaseDateButton.setOnAction(e -> viewModel.rejectImportField("releaseDate"));

        acceptStudiosButton.setOnAction(e -> viewModel.acceptImportField("studios"));
        rejectStudiosButton.setOnAction(e -> viewModel.rejectImportField("studios"));

        acceptPeopleButton.setOnAction(e -> viewModel.acceptImportField("people"));
        rejectPeopleButton.setOnAction(e -> viewModel.rejectImportField("people"));
    }

    /**
     * Được gọi bởi MainController để inject ViewModel.
     * @param viewModel ViewModel cho view này.
     */
    public void setViewModel(ItemDetailViewModel viewModel) {
        this.viewModel = viewModel;

        // --- BINDING UI VỚI VIEWMODEL ---

        // 1. Binding các Label chi tiết (CHỈ CÁC LABEL CÒN LẠI)
        taglineLabel.textProperty().bind(viewModel.taglineProperty());
        genresLabel.textProperty().bind(viewModel.genresProperty());
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        // 2. (SỬA ĐỔI) Binding 2 CHIỀU cho TẤT CẢ các trường edit
        titleTextField.textProperty().bindBidirectional(viewModel.titleProperty());
        overviewTextArea.textProperty().bindBidirectional(viewModel.overviewProperty());

        // (*** SỬA ĐỔI TAGS ***)
        // (XÓA DÒNG NÀY)
        // tagsTextField.textProperty().bindBidirectional(viewModel.tagsProperty());

        // (MỚI) Lắng nghe danh sách TagModel
        viewModel.getTagItems().addListener((ListChangeListener<TagModel>) c -> {
            Platform.runLater(this::updateTagsFlowPane);
        });
        updateTagsFlowPane(); // Cập nhật lần đầu
        // (*** KẾT THÚC SỬA ĐỔI TAGS ***)

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

        // 6. Binding Gallery (Lắng nghe danh sách URL)
        viewModel.getBackdropImageUrls().addListener((ListChangeListener<String>) c -> {
            updateImageGallery();
        });
        updateImageGallery(); // Cập nhật lần đầu

        // 7. (MỚI) Binding UI linh hoạt
        // Ẩn/hiện toàn bộ VBox "chỉ file" dựa trên property isFolder
        fileOnlyContainer.visibleProperty().bind(viewModel.isFolderProperty().not());
        fileOnlyContainer.managedProperty().bind(viewModel.isFolderProperty().not());

        // 8. Binding Path (từ bước trước)
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

        // 9. (MỚI) Binding hiển thị cho các nút (v/x)
        bindReviewContainer(reviewTitleContainer, viewModel.showTitleReviewProperty());
        bindReviewContainer(reviewOverviewContainer, viewModel.showOverviewReviewProperty());

        // (*** SỬA ĐỔI TAGS ***)
        // (XÓA DÒNG NÀY)
        // bindReviewContainer(reviewTagsContainer, viewModel.showTagsReviewProperty());
        // (*** KẾT THÚC SỬA ĐỔI TAGS ***)

        bindReviewContainer(reviewReleaseDateContainer, viewModel.showReleaseDateReviewProperty());
        bindReviewContainer(reviewStudiosContainer, viewModel.showStudiosReviewProperty());
        bindReviewContainer(reviewPeopleContainer, viewModel.showPeopleReviewProperty());

        // 10. (MỚI) Binding cho nút Save
        saveButton.disableProperty().bind(viewModel.isDirtyProperty().not());
    }

    /**
     * (MỚI) Hàm helper để bind container (v/x)
     */
    private void bindReviewContainer(HBox container, ReadOnlyBooleanProperty visibilityProperty) {
        if (container != null && visibilityProperty != null) {
            container.visibleProperty().bind(visibilityProperty);
            container.managedProperty().bind(visibilityProperty);
        } else {
            // System.err.println("Lỗi binding: container hoặc property bị null.");
        }
    }

    /**
     * Helper: Cập nhật FlowPane gallery
     */
    private void updateImageGallery() {
        Platform.runLater(() -> {
            imageGalleryPane.getChildren().clear();
            if (viewModel == null) return;

            for (String imageUrl : viewModel.getBackdropImageUrls()) {
                ImageView backdropView = new ImageView();
                try {
                    backdropView.setImage(new Image(imageUrl, true));
                } catch (Exception e) {
                    System.err.println("Lỗi tải ảnh backdrop: " + imageUrl + " | " + e.getMessage());
                }
                backdropView.setFitHeight(BACKDROP_THUMBNAIL_HEIGHT);
                backdropView.setPreserveRatio(true);
                backdropView.setSmooth(true);
                imageGalleryPane.getChildren().add(backdropView);
            }
        });
    }

    // (*** MỚI: HÀM CẬP NHẬT UI CHO TAGS ***)
    /**
     * Đồng bộ hóa FlowPane (View) với danh sách TagModel (ViewModel).
     */
    private void updateTagsFlowPane() {
        if (viewModel == null || tagsFlowPane == null) return;

        tagsFlowPane.getChildren().clear(); // Xóa tất cả chip cũ
        for (TagModel tag : viewModel.getTagItems()) {
            // Tạo một TagView (chip) mới
            TagView tagChip = new TagView(tag, (tagToDelete) -> {
                // Định nghĩa hành động cho nút Xóa
                viewModel.removeTag(tagToDelete);
            });
            tagsFlowPane.getChildren().add(tagChip);
        }
    }

    /**
     * Xử lý sự kiện khi nhấn nút "Mở trong Finder" / "Phát".
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
                Desktop.getDesktop().open(fileOrDir);
            } catch (Exception e) {
                System.err.println("Lỗi khi mở đường dẫn: " + path + " | " + e.getMessage());
                viewModel.reportActionError("Lỗi: " + e.getMessage());
            }
        }).start();
    }

    // (*** MỚI: HÀM XỬ LÝ NÚT "THÊM TAG" ***)
    @FXML
    private void handleAddTagButtonAction() {
        if (viewModel == null) return;

        try {
            // 1. Tải FXML của Dialog
            // Dùng getResource() từ class MainApp (vì nó ở root)
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("AddTagDialog.fxml"));
            VBox page = loader.load();

            // 2. Tạo Stage (cửa sổ) cho Dialog
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Thêm Tag Mới");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner((Stage) rootPane.getScene().getWindow());
            Scene scene = new Scene(page);

            // 3. Lấy CSS từ cửa sổ chính
            scene.getStylesheets().addAll(rootPane.getScene().getStylesheets());
            dialogStage.setScene(scene);

            // 4. Inject Stage vào Controller của Dialog
            AddTagDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);

            // 5. Hiển thị Dialog và chờ
            dialogStage.showAndWait();

            // 6. Lấy kết quả
            TagModel newTag = controller.getResultTag();
            if (newTag != null) {
                // Thêm tag mới vào ViewModel
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

    /**
     * (SỬA ĐỔI) Được gọi khi nhấn nút Lưu.
     * Giờ đây nó gọi hàm saveChanges() trong ViewModel.
     */
    @FXML
    private void handleSaveButtonAction() {
        System.out.println("Nút Lưu đã được nhấn. Gọi ViewModel.saveChanges().");
        if (viewModel != null) {
            viewModel.saveChanges();
        }
    }

    /**
     * (MỚI) Xử lý sự kiện nhấn nút Import.
     */
    @FXML
    private void handleImportButtonAction() {
        if (viewModel == null) return;
        viewModel.clearActionError(); // Xóa lỗi cũ

        try {
            // Lấy Stage hiện tại
            Stage stage = (Stage) rootPane.getScene().getWindow();
            File selectedFile = JsonFileHandler.showOpenJsonDialog(stage);

            if (selectedFile != null) {
                viewModel.reportActionError("Đang đọc file JSON...");
                // Chạy đọc file trong luồng nền
                new Thread(() -> {
                    try {
                        BaseItemDto importedDto = JsonFileHandler.readJsonFileToObject(selectedFile);
                        if (importedDto != null) {
                            // Cập nhật UI trên JavaFX Thread
                            Platform.runLater(() -> {
                                viewModel.importAndPreview(importedDto); // Gửi DTO sang ViewModel
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
            // Lỗi này thường là lỗi khi hiển thị dialog (hiếm)
            System.err.println("Lỗi khi Import (hiển thị dialog): " + e.getMessage());
            viewModel.reportActionError("Lỗi Import: " + e.getMessage());
        }
    }

    /**
     * (MỚI) Xử lý sự kiện nhấn nút Export.
     */
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

            // (*** SỬA ĐỔI ***)
            // Lấy tên file gợi ý từ Tiêu đề GỐC (originalTitle), không phải từ DTO
            String originalTitle = viewModel.getOriginalTitleForExport();
            System.out.println(originalTitle);
            String initialFileName = (originalTitle != null ? originalTitle.replaceAll("[^a-zA-Z0-9.-]", "_") : "item") + ".json";
            System.out.println(initialFileName);
            File targetFile = JsonFileHandler.showSaveJsonDialog(stage, initialFileName);

            if (targetFile != null) {
                // Chạy ghi file trong luồng nền
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
}