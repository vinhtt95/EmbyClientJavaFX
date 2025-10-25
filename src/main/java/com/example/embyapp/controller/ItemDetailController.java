package com.example.embyapp.controller;

import com.example.emby.modelEmby.BaseItemDto; // (MỚI)
import com.example.embyapp.service.JsonFileHandler; // (MỚI)
import com.example.embyapp.viewmodel.ItemDetailViewModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty; // (MỚI)
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox; // (MỚI)
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage; // (MỚI)

import java.awt.Desktop;
import java.io.File;

/**
 * (CẬP NHẬT 3)
 * Cập nhật để bind với Form UI mới (TextFields, TextArea).
 * Thêm logic ẩn/hiện fileOnlyContainer.
 * (CẬP NHẬT 4)
 * Thêm logic Import/Export/Review.
 * (CẬP NHẬT 5)
 * Thay đổi binding sang 2-way và bind nút Save.
 * (CẬP NHẬT 6)
 * Sửa logic export filename để dùng original title.
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

    // (MỚI) Fields cho UI linh hoạt
    @FXML private TextField tagsTextField;
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

    @FXML private HBox reviewTagsContainer;
    @FXML private Button acceptTagsButton;
    @FXML private Button rejectTagsButton;

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

        acceptTagsButton.setOnAction(e -> viewModel.acceptImportField("tags"));
        rejectTagsButton.setOnAction(e -> viewModel.rejectImportField("tags"));

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
        tagsTextField.textProperty().bindBidirectional(viewModel.tagsProperty());
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
        bindReviewContainer(reviewTagsContainer, viewModel.showTagsReviewProperty());
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
            System.err.println("Lỗi binding: container hoặc property bị null.");
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