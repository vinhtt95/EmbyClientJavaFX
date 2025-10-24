package com.example.embyapp.controller;

import com.example.embyapp.viewmodel.ItemDetailViewModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea; // (MỚI)
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;

/**
 * (CẬP NHẬT 3)
 * Cập nhật để bind với Form UI mới (TextFields, TextArea).
 * Thêm logic ẩn/hiện fileOnlyContainer.
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

    private ItemDetailViewModel viewModel;
    private static final double BACKDROP_THUMBNAIL_HEIGHT = 100;

    @FXML
    public void initialize() {
        // (Sẽ được gọi trong setViewModel)
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

        // 2. (MỚI) Binding các trường CÓ THỂ CHỈNH SỬA (2 chiều)
        titleTextField.textProperty().bindBidirectional(viewModel.titleProperty());
        overviewTextArea.textProperty().bindBidirectional(viewModel.overviewProperty());

        // 3. (MỚI) Binding các trường CHỈ ĐỌC (1 chiều)
        tagsTextField.textProperty().bind(viewModel.tagsProperty());
        releaseDateTextField.textProperty().bind(viewModel.releaseDateProperty());
        studiosTextField.textProperty().bind(viewModel.studiosProperty());
        peopleTextField.textProperty().bind(viewModel.peopleProperty());

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
     * (MỚI) Được gọi khi nhấn nút Lưu.
     * Sẽ được triển khai ở Phase 2.
     */
    @FXML
    private void handleSaveButtonAction() {
        System.out.println("Nút Lưu đã được nhấn. Sẵn sàng để update API.");
        // TODO: Gọi một hàm trong ViewModel, ví dụ:
        // viewModel.saveChanges();
    }
}