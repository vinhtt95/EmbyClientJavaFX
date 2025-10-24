package com.example.embyapp.controller;

import com.example.embyapp.viewmodel.ItemDetailViewModel;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * (CẬP NHẬT LỚN) Controller cho cột phải (Item Detail).
 * Controller này đã được cập nhật để bind với FXML mới
 * và ViewModel "chủ động" mới.
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
     * (HÀM CŨ - BỊ XÓA)
     * Logic này đã được chuyển sang MainController -> ViewModel
     */
    // public void displayItemDetails(BaseItemDto item) { ... }
}
