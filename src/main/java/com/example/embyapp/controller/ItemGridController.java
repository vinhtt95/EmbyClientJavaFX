package com.example.embyapp.controller;

import com.example.emby.modelEmby.BaseItemDto;
import com.example.embyapp.viewmodel.ItemGridViewModel;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * (SỬA ĐỔI) Controller cho ItemGridView (Cột giữa).
 * Sửa lỗi:
 * 1. Gọi đúng tên hàm: viewModel.loadItemsByParentId(...)
 * 2. Gọi đúng property: viewModel.selectedItemProperty().set(...)
 */
public class ItemGridController {

    @FXML private StackPane rootStackPane;
    @FXML private ScrollPane gridScrollPane;
    @FXML private FlowPane itemFlowPane;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;

    private ItemGridViewModel viewModel;

    // Kích thước của mỗi ô
    private static final double CELL_WIDTH = 120;
    private static final double CELL_HEIGHT = 200;
    private static final double IMAGE_HEIGHT = 160;

    @FXML
    public void initialize() {
        // Cấu hình FlowPane
        itemFlowPane.setPadding(new Insets(10));
        itemFlowPane.setHgap(10);
        itemFlowPane.setVgap(10);

        // Đảm bảo ScrollPane fit chiều rộng (chỉ scroll dọc)
        gridScrollPane.setFitToWidth(true);
    }

    /**
     * Được gọi bởi MainController để inject ViewModel.
     * @param viewModel ViewModel tương ứng
     */
    public void setViewModel(ItemGridViewModel viewModel) {
        this.viewModel = viewModel;

        // --- Binding UI với ViewModel ---

        // 1. Binding trạng thái Loading
        loadingIndicator.visibleProperty().bind(viewModel.loadingProperty());
        loadingIndicator.managedProperty().bind(viewModel.loadingProperty());

        // 2. Binding trạng thái Status Message
        statusLabel.visibleProperty().bind(viewModel.showStatusMessageProperty());
        statusLabel.managedProperty().bind(viewModel.showStatusMessageProperty());
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        // 3. Ẩn/hiện ScrollPane (Grid)
        // Grid chỉ hiển thị khi KHÔNG loading VÀ KHÔNG show status
        gridScrollPane.visibleProperty().bind(
                viewModel.loadingProperty().not().and(viewModel.showStatusMessageProperty().not())
        );
        gridScrollPane.managedProperty().bind(gridScrollPane.visibleProperty());


        // 4. Lắng nghe thay đổi danh sách items trong ViewModel
        viewModel.getItems().addListener((ListChangeListener<BaseItemDto>) c -> {
            Platform.runLater(this::updateGrid);
        });

        // 5. Lắng nghe thay đổi selectedItem từ ViewModel (nếu cần)
        // (Trong trường hợp này, Controller là nơi SET selectedItem)

        // Xóa grid ban đầu
        updateGrid();
    }

    /**
     * (SỬA LỖI 1)
     * Được gọi bởi MainController để yêu cầu tải items mới.
     * Tên hàm phải khớp với tên MainController gọi.
     * @param parentId ID của thư mục cha
     */
    public void loadItemsByParentId(String parentId) {
        if (viewModel != null) {
            // Gọi đúng tên hàm trên ViewModel
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
            VBox cell = createItemCell(item);
            itemFlowPane.getChildren().add(cell);
        }
    }

    /**
     * Tạo một ô (cell) VBox cho một item.
     * @param item DTO của item
     * @return VBox đại diện cho item
     */
    private VBox createItemCell(BaseItemDto item) {
        VBox cellBox = new VBox(5);
        cellBox.setPrefSize(CELL_WIDTH, CELL_HEIGHT);
        cellBox.setMaxSize(CELL_WIDTH, CELL_HEIGHT);
        cellBox.setAlignment(Pos.TOP_CENTER);
        cellBox.getStyleClass().add("item-cell"); // Thêm CSS class nếu cần

        // 1. Hình ảnh
        ImageView imageView = new ImageView();
        imageView.setFitWidth(CELL_WIDTH);
        imageView.setFitHeight(IMAGE_HEIGHT);
        imageView.setPreserveRatio(true);

        // Logic tải ảnh (Ví dụ: dùng ảnh placeholder)
        // (Cần logic lấy URL ảnh thumbnail từ item DTO sau này)
        String imageUrl = "https://placehold.co/" + (int)CELL_WIDTH + "x" + (int)IMAGE_HEIGHT + "/333/999?text=" + (item.getType() != null ? item.getType() : "Item");
        try {
            imageView.setImage(new Image(imageUrl, true)); // true = tải nền
        } catch (Exception e) {
            System.err.println("Error loading image: " + e.getMessage());
            // Có thể set ảnh placeholder lỗi
        }

        // 2. Tiêu đề
        Label titleLabel = new Label(item.getName());
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(CELL_WIDTH - 10);
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        titleLabel.getStyleClass().add("item-title-label");

        cellBox.getChildren().addAll(imageView, titleLabel);

        // 3. Xử lý sự kiện click
        cellBox.setOnMouseClicked(event -> {
            if (viewModel != null) {
                // (SỬA LỖI 2)
                // Gọi đúng property().set(...) thay vì hàm setter
                viewModel.selectedItemProperty().set(item);
            }
        });

        return cellBox;
    }
}

