package com.example.embyapp.controller;

import com.example.embyapp.viewmodel.ItemDetailViewModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings; // (MỚI)
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button; // (MỚI)
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField; // (MỚI)
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.awt.Desktop; // (MỚI)
import java.io.File; // (MỚI)

/**
 * (CẬP NHẬT LỚN) Controller cho cột phải (Item Detail).
 * Controller này đã được cập nhật để bind với FXML mới
 * và ViewModel "chủ động" mới.
 * (CẬP NHẬT 2) Thêm logic cho nút Path/Open.
 */
public class ItemDetailController {

    // --- FXML Components (TỪ FXML MỚI) ---
    @FXML private StackPane rootPane;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;
    @FXML private ScrollPane mainScrollPane;
    @FXML private VBox detailContentPane;
    @FXML private ImageView primaryImageView;
    @FXML private Label titleLabel;
    @FXML private Label taglineLabel;
    @FXML private Label yearLabel;
    @FXML private Label runtimeLabel;
    @FXML private Label genresLabel;
    @FXML private Label overviewLabel;
    @FXML private FlowPane imageGalleryPane;

    // (MỚI) FXML Fields cho Path và Nút bấm
    @FXML private VBox pathContainer;
    @FXML private TextField pathTextField;
    @FXML private Button openButton;
    @FXML private Label actionStatusLabel; // Nhãn lỗi cục bộ

    private ItemDetailViewModel viewModel;

    // Kích thước cho ảnh thumbnail trong gallery
    private static final double BACKDROP_THUMBNAIL_HEIGHT = 100;

    @FXML
    public void initialize() {
        // Không cần làm gì ở đây,
        // vì binding sẽ được thực hiện trong setViewModel
    }

    /**
     * Được gọi bởi MainController để inject ViewModel.
     * @param viewModel ViewModel cho view này.
     */
    public void setViewModel(ItemDetailViewModel viewModel) {
        this.viewModel = viewModel;

        // --- BINDING UI VỚI VIEWMODEL ---

        // 1. Binding các Label chi tiết
        titleLabel.textProperty().bind(viewModel.titleProperty());
        yearLabel.textProperty().bind(viewModel.yearProperty());
        overviewLabel.textProperty().bind(viewModel.overviewProperty());
        taglineLabel.textProperty().bind(viewModel.taglineProperty());
        genresLabel.textProperty().bind(viewModel.genresProperty());
        runtimeLabel.textProperty().bind(viewModel.runtimeProperty());
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        // 2. Binding Ảnh Primary
        primaryImageView.imageProperty().bind(viewModel.primaryImageProperty());

        // 3. Binding kiểm soát hiển thị (Loading / Status / Content)
        loadingIndicator.visibleProperty().bind(viewModel.loadingProperty());
        statusLabel.visibleProperty().bind(viewModel.showStatusMessageProperty());

        // Nội dung chính (mainScrollPane) chỉ hiển thị khi
        // KHÔNG loading VÀ KHÔNG show status
        mainScrollPane.visibleProperty().bind(
                viewModel.loadingProperty().not().and(viewModel.showStatusMessageProperty().not())
        );

        // 4. Binding Gallery (Lắng nghe danh sách URL)
        viewModel.getBackdropImageUrls().addListener((ListChangeListener<String>) c -> {
            updateImageGallery();
        });

        // Cập nhật gallery lần đầu
        updateImageGallery();

        // 5. (MỚI) Binding cho Path và Nút bấm
        if (pathContainer != null && pathTextField != null && openButton != null && actionStatusLabel != null) {
            // Bind text field
            pathTextField.textProperty().bind(viewModel.itemPathProperty());

            // Chỉ hiển thị toàn bộ VBox nếu đường dẫn hợp lệ
            pathContainer.visibleProperty().bind(
                    viewModel.itemPathProperty().isNotEmpty()
                            .and(viewModel.itemPathProperty().isNotEqualTo("Không có đường dẫn"))
            );

            // Thay đổi text của nút bấm (cho thân thiện với macOS)
            openButton.textProperty().bind(
                    Bindings.when(viewModel.isFolderProperty())
                            .then("Mở trong Finder") // Text cho Folder
                            .otherwise("Phát (Mặc định)") // Text cho File
            );

            // Hiển thị lỗi (nếu có)
            actionStatusLabel.textProperty().bind(viewModel.actionStatusMessageProperty());
        }
    }

    /**
     * (MỚI) Helper: Cập nhật FlowPane gallery
     * dựa trên danh sách URL trong ViewModel.
     */
    private void updateImageGallery() {
        Platform.runLater(() -> {
            imageGalleryPane.getChildren().clear();
            if (viewModel == null) return;

            for (String imageUrl : viewModel.getBackdropImageUrls()) {
                ImageView backdropView = new ImageView();
                try {
                    // Tải ảnh nền
                    backdropView.setImage(new Image(imageUrl, true));
                } catch (Exception e) {
                    System.err.println("Lỗi tải ảnh backdrop: " + imageUrl + " | " + e.getMessage());
                    // (Có thể set ảnh placeholder lỗi ở đây)
                }

                backdropView.setFitHeight(BACKDROP_THUMBNAIL_HEIGHT);
                backdropView.setPreserveRatio(true);
                backdropView.setSmooth(true);
                // Thêm style (nếu bạn muốn bo góc, v.v. trong CSS)
                // backdropView.getStyleClass().add("backdrop-thumbnail");

                imageGalleryPane.getChildren().add(backdropView);
            }
        });
    }

    /**
     * (MỚI) Được gọi khi nhấn nút "Mở trong Finder" / "Phát".
     * Sử dụng java.awt.Desktop để tương tác với HĐH.
     */
    @FXML
    private void handleOpenButtonAction() {
        if (viewModel == null) return;

        // Xóa thông báo lỗi cũ
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

        // Chạy trên luồng riêng để không làm treo UI
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
}