package com.example.embyapp.controller;

import com.example.emby.modelEmby.BaseItemDto;
import com.example.embyapp.service.EmbyService; // (NÂNG CẤP) Thêm import
import com.example.embyapp.viewmodel.ItemGridViewModel;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos; // (CẬP NHẬT) Thêm import
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane; // (NÂNG CẤP) Thêm import
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region; // (NÂNG CẤP) Thêm import
import javafx.scene.text.TextAlignment;

import java.util.Map; // (NÂNG CẤP) Thêm import

/**
 * (SỬA ĐỔI) Controller cho ItemGridView (Cột giữa).
 * (NÂNG CẤP 1): Tải ảnh thật thay vì placeholder.
 * (NÂNG CẤP 2): Chuyển layout cell sang StackPane với gradient overlay.
 * (SỬA LỖI): Xóa các lệnh gọi viewModel.setLoading() và setStatusMessage()
 * (SỬA LỖI 2): Sửa lỗi "effectively final" cho biến trong lambda.
 * (CẬP NHẬT): Đổi kích thước cell sang 16:9 (240x135) và căn giữa FlowPane.
 * (CẬP NHẬT 2): Cập nhật kích thước cell theo yêu cầu mới và tăng khoảng cách.
 */
public class ItemGridController {

    @FXML private StackPane rootStackPane;
    @FXML private ScrollPane gridScrollPane; // Khớp với ItemGridView.fxml
    @FXML private FlowPane itemFlowPane;
    @FXML private ProgressIndicator loadingIndicator; // Khớp với ItemGridView.fxml
    @FXML private Label statusLabel;

    private ItemGridViewModel viewModel;

    // (CẬP NHẬT 2) Kích thước cố định cho mỗi ô (cell) - Tỷ lệ 16:9
    private static final double CELL_HEIGHT = 320; // Chiều cao 9
    private static final double CELL_WIDTH = CELL_HEIGHT * 16 / 9; // Chiều rộng 16
    private static final double IMAGE_HEIGHT = CELL_HEIGHT; // Ảnh sẽ chiếm toàn bộ chiều cao cell

    @FXML
    public void initialize() {
        // Cấu hình FlowPane
        itemFlowPane.setPadding(new Insets(10));
        // (CẬP NHẬT 2) Tăng khoảng cách
        itemFlowPane.setHgap(20);
        itemFlowPane.setVgap(20);

        // (CẬP NHẬT) Căn giữa các item trong FlowPane
        itemFlowPane.setAlignment(Pos.CENTER);

        // Đảm bảo ScrollPane fit chiều rộng (chỉ scroll dọc)
        gridScrollPane.setFitToWidth(true);
    }

    /**
     * Được gọi bởi MainController để inject ViewModel.
     * @param viewModel ViewModel tương ứng
     */
    public void setViewModel(ItemGridViewModel viewModel) {
        this.viewModel = viewModel;

        // --- Binding UI với ViewModel (Giữ nguyên như file gốc) ---

        // 1. Binding trạng thái Loading
        loadingIndicator.visibleProperty().bind(viewModel.loadingProperty());
        loadingIndicator.managedProperty().bind(viewModel.loadingProperty());

        // 2. Binding trạng thái Status Message
        statusLabel.visibleProperty().bind(viewModel.showStatusMessageProperty());
        statusLabel.managedProperty().bind(viewModel.showStatusMessageProperty());
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        // 3. Ẩn/hiện ScrollPane (Grid)
        gridScrollPane.visibleProperty().bind(
                viewModel.loadingProperty().not().and(viewModel.showStatusMessageProperty().not())
        );
        gridScrollPane.managedProperty().bind(gridScrollPane.visibleProperty());


        // 4. Lắng nghe thay đổi danh sách items trong ViewModel
        viewModel.getItems().addListener((ListChangeListener<BaseItemDto>) c -> {
            Platform.runLater(this::updateGrid);
        });

        // Xóa grid ban đầu
        updateGrid();
    }

    /**
     * (SỬA LỖI)
     * Được gọi bởi MainController để yêu cầu tải items mới.
     * Tên hàm phải khớp với tên MainController gọi.
     * @param parentId ID của thư mục cha
     */
    public void loadItemsByParentId(String parentId) {
        if (viewModel != null) {
            // (SỬA LỖI) Đã xóa các dòng viewModel.setLoading() và viewModel.setStatusMessage()
            // Chỉ cần gọi hàm của ViewModel, nó sẽ tự xử lý trạng thái.
            viewModel.loadItemsByParentId(parentId);
        }
    }

    /**
     * Cập nhật FlowPane dựa trên danh sách items trong ViewModel.
     */
    private void updateGrid() {
        if (viewModel == null) return;

        itemFlowPane.getChildren().clear();
        for (BaseItemDto item : viewModel.getItems()) {
            StackPane cell = createItemCell(item); // (NÂNG CẤP 2) Thay VBox bằng StackPane
            itemFlowPane.getChildren().add(cell);
        }
    }

    /**
     * (SỬA ĐỔI) Tạo một ô (cell) StackPane cho một item, thay vì VBox.
     * (NÂNG CẤP 2) Sử dụng StackPane, ImageView nền, lớp phủ gradient và tiêu đề.
     * @param item DTO của item
     * @return StackPane đại diện cho item
     */
    private StackPane createItemCell(BaseItemDto item) {
        StackPane cellContainer = new StackPane();
        cellContainer.setPrefSize(CELL_WIDTH, CELL_HEIGHT);
        cellContainer.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        cellContainer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        cellContainer.getStyleClass().add("item-cell"); // Thêm CSS class để style

        // 1. Hình ảnh (Nền)
        ImageView imageView = new ImageView();
        imageView.setFitWidth(CELL_WIDTH);
        imageView.setFitHeight(IMAGE_HEIGHT);
        imageView.setPreserveRatio(false); // (NÂNG CẤP 2) Không giữ tỷ lệ để ảnh fill hoàn toàn StackPane
        imageView.setSmooth(true); // Để ảnh trông mượt mà hơn khi co giãn
        imageView.getStyleClass().add("item-image"); // Thêm CSS class cho ImageView

        String imageUrl = null;
        String placeholderUrl; // Dùng cho cả trường hợp lỗi và không có ảnh

        try {
            EmbyService embyService = EmbyService.getInstance();
            String serverUrl = embyService.getApiClient().getBasePath();
            String itemId = item.getId();

            Map<String, String> imageTags = item.getImageTags();
            if (itemId != null && imageTags != null && imageTags.containsKey("Primary")) {
                String imageTag = imageTags.get("Primary");
                // (NÂNG CẤP 1) Tạo URL ảnh thật
                // (CẬP NHẬT) Sửa (int)CELL_WIDTH*2 để dùng kích thước mới
                imageUrl = String.format("%s/Items/%s/Images/Primary?tag=%s&maxWidth=%d&quality=80",
                        serverUrl, itemId, imageTag, (int)CELL_WIDTH * 2); // *2 để ảnh nét hơn trên màn hình HiDPI
            }
        } catch (Exception e) {
            System.err.println("Error getting image URL components for item " + item.getName() + ": " + e.getMessage());
        }

        // (CẬP NHẬT) Sửa kích thước placeholder
        placeholderUrl = "https://placehold.co/" + (int)CELL_WIDTH + "x" + (int)IMAGE_HEIGHT + "/333/999?text=" + (item.getType() != null ? item.getType() : "Item");

        // (SỬA LỖI 2) Tạo biến final để dùng trong lambda
        final String urlToLoad = (imageUrl != null) ? imageUrl : placeholderUrl;
        final String finalPlaceholderUrl = placeholderUrl;

        try {
            Image image = new Image(urlToLoad, true); // true = tải nền
            imageView.setImage(image);

            // (NÂNG CẤP 1) Fallback nếu tải ảnh thật lỗi
            image.errorProperty().addListener((obs, wasError, isError) -> {
                if (isError) {
                    // (SỬA LỖI 2) Dùng biến final urlToLoad
//                    System.err.println("Failed to load image: " + urlToLoad + ". Falling back to placeholder.");
                    // (SỬA LỖI 2) Dùng biến final finalPlaceholderUrl
                    imageView.setImage(new Image(finalPlaceholderUrl, true));
                }
            });

        } catch (Exception e) {
            System.err.println("Error initiating image load: " + e.getMessage());
            // (SỬA LỖI 2) Dùng biến final
            imageView.setImage(new Image(finalPlaceholderUrl, true));
        }

        // 2. Lớp phủ gradient (đặt dưới tiêu đề)
        VBox overlay = new VBox();
        overlay.setAlignment(Pos.BOTTOM_LEFT); // Căn chỉnh nội dung ở dưới cùng bên trái
        overlay.setPadding(new Insets(8)); // Khoảng cách đệm
        overlay.getStyleClass().add("item-title-overlay"); // Thêm CSS class

        // 3. Tiêu đề
        Label titleLabel = new Label(item.getName());
        titleLabel.setWrapText(true);
        titleLabel.setTextAlignment(TextAlignment.LEFT); // (NÂNG CẤP 2) Căn trái
        titleLabel.getStyleClass().add("item-title-label"); // Thêm CSS class

        // Thêm tiêu đề vào lớp phủ
        overlay.getChildren().add(titleLabel);

        // Thêm các thành phần vào StackPane
        cellContainer.getChildren().addAll(imageView, overlay);

        // 4. Xử lý sự kiện click
        cellContainer.setOnMouseClicked(event -> {
            if (viewModel != null) {
                viewModel.selectedItemProperty().set(item);
            }
        });

        return cellContainer;
    }
}

