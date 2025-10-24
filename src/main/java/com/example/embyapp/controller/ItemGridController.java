package com.example.embyapp.controller;

import com.example.emby.modelEmby.BaseItemDto;
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.viewmodel.ItemDetailViewModel;
import com.example.embyapp.viewmodel.ItemGridViewModel;
import com.example.embyapp.service.ItemRepository;
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
import javafx.scene.layout.Region;
import javafx.scene.text.TextAlignment;

import java.util.Map;
import java.awt.Desktop;
import java.io.File;


/**
 * (SỬA ĐỔI) Controller cho ItemGridView (Cột giữa).
 * (CẬP NHẬT 5) FIX LỖI: Double-Click chỉ Phát file, không Mở Finder cho Folder.
 * (CẬP NHẬT MỚI) Thêm chức năng Infinite Scrolling.
 */
public class ItemGridController {

    @FXML private StackPane rootStackPane;
    @FXML private ScrollPane gridScrollPane; // Khớp với ItemGridView.fxml
    @FXML private FlowPane itemFlowPane;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;

    private ItemGridViewModel viewModel;
    private ItemDetailViewModel itemDetailViewModel;

    // Thêm Service và Repository để thực hiện API call ngay tại đây
    private final EmbyService embyService = EmbyService.getInstance();
    private final ItemRepository itemRepository = new ItemRepository();

    // Kích thước cố định cho mỗi ô (cell) - Tỷ lệ 16:9
    private static final double CELL_HEIGHT = 320;
    private static final double CELL_WIDTH = CELL_HEIGHT * 16 / 9;
    private static final double IMAGE_HEIGHT = CELL_HEIGHT;

    @FXML
    public void initialize() {
        // Cấu hình FlowPane
        itemFlowPane.setPadding(new Insets(20));
        itemFlowPane.setHgap(20);
        itemFlowPane.setVgap(20);

        itemFlowPane.setAlignment(Pos.CENTER);

        // Đảm bảo ScrollPane fit chiều rộng (chỉ scroll dọc)
        gridScrollPane.setFitToWidth(true);

        // (MỚI) Thêm listener cho ScrollPane để thực hiện Lazy Loading
        gridScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (viewModel == null) return;
            // Nếu cuộn gần tới cuối (ví dụ: > 95% cuộn)
            // và có item cần load, và không đang load
            if (newVal.doubleValue() > 0.95 && viewModel.hasMoreItemsProperty().get() && !viewModel.isLoadingMoreProperty().get()) {
                // Gọi VM để tải thêm
                viewModel.loadMoreItems();
            }
        });
    }

    public void setViewModel(ItemGridViewModel viewModel) {
        this.viewModel = viewModel;

        // --- Binding UI với ViewModel ---

        // 1. Binding trạng thái Loading
        // SỬA ĐỔI: Binding loadingIndicator với loading CHUNG và loading MORE
        loadingIndicator.visibleProperty().bind(viewModel.loadingProperty().or(viewModel.isLoadingMoreProperty()));
        loadingIndicator.managedProperty().bind(viewModel.loadingProperty().or(viewModel.isLoadingMoreProperty()));

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
            Platform.runLater(() -> {
                while (c.next()) {
                    if (c.wasPermutated() || c.wasReplaced() || c.wasAdded() && c.getFrom() == 0) {
                        // Trường hợp setAll/load mới hoàn toàn (loadItemsByParentId)
                        itemFlowPane.getChildren().clear();
                        for (BaseItemDto item : viewModel.getItems()) {
                            itemFlowPane.getChildren().add(createItemCell(item));
                        }
                    } else if (c.wasAdded()) {
                        // Trường hợp addAll/loadMoreItems (tải thêm)
                        // Chỉ thêm các item mới vào cuối FlowPane
                        for (BaseItemDto item : c.getAddedSubList()) {
                            itemFlowPane.getChildren().add(createItemCell(item));
                        }
                    }
                    // Bỏ qua trường hợp xóa (c.wasRemoved())
                }
            });
        });
    }

    /**
     * Setter để nhận ItemDetailViewModel từ MainController.
     */
    public void setItemDetailViewModel(ItemDetailViewModel itemDetailViewModel) {
        this.itemDetailViewModel = itemDetailViewModel;
    }

    public void loadItemsByParentId(String parentId) {
        if (viewModel != null) {
            viewModel.loadItemsByParentId(parentId);
        }
    }

    // XÓA HÀM updateGrid() CŨ
    // private void updateGrid() {
    //     if (viewModel == null) return;
    //
    //     itemFlowPane.getChildren().clear();
    //     for (BaseItemDto item : viewModel.getItems()) {
    //         StackPane cell = createItemCell(item);
    //         itemFlowPane.getChildren().add(cell);
    //     }
    // }

    private StackPane createItemCell(BaseItemDto item) {
        StackPane cellContainer = new StackPane();
        cellContainer.setPrefSize(CELL_WIDTH, CELL_HEIGHT);
        cellContainer.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        cellContainer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        cellContainer.getStyleClass().add("item-cell");

        // --- (Logic tạo ImageView) ---
        ImageView imageView = new ImageView();
        imageView.setFitWidth(CELL_WIDTH);
        imageView.setFitHeight(IMAGE_HEIGHT);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("item-image");

        String imageUrl = null;
        String placeholderUrl;

        try {
            String serverUrl = embyService.getApiClient().getBasePath();
            String itemId = item.getId();

            Map<String, String> imageTags = item.getImageTags();
            if (itemId != null && imageTags != null && imageTags.containsKey("Primary")) {
                String imageTag = imageTags.get("Primary");
                imageUrl = String.format("%s/Items/%s/Images/Primary?tag=%s&maxWidth=%d&quality=80",
                        serverUrl, itemId, imageTag, (int)CELL_WIDTH * 2);
            }
        } catch (Exception e) {
            // Lỗi này không nghiêm trọng
        }

        placeholderUrl = "https://placehold.co/" + (int)CELL_WIDTH + "x" + (int)IMAGE_HEIGHT + "/333/999?text=" + (item.getType() != null ? item.getType() : "Item");

        final String urlToLoad = (imageUrl != null) ? imageUrl : placeholderUrl;
        final String finalPlaceholderUrl = placeholderUrl;

        try {
            Image image = new Image(urlToLoad, true);
            imageView.setImage(image);

            image.errorProperty().addListener((obs, wasError, isError) -> {
                if (isError) {
                    imageView.setImage(new Image(finalPlaceholderUrl, true));
                }
            });

        } catch (Exception e) {
            imageView.setImage(new Image(finalPlaceholderUrl, true));
        }

        VBox overlay = new VBox();
        overlay.setAlignment(Pos.BOTTOM_LEFT);
        overlay.setPadding(new Insets(8));
        overlay.getStyleClass().add("item-title-overlay");

        Label titleLabel = new Label(item.getName());
        titleLabel.setWrapText(true);
        titleLabel.setTextAlignment(TextAlignment.LEFT);
        titleLabel.getStyleClass().add("item-title-label");

        overlay.getChildren().add(titleLabel);
        cellContainer.getChildren().addAll(imageView, overlay);
        // --- (End Logic tạo ImageView) ---


        // 4. Xử lý sự kiện click (SINGLE-CLICK và DOUBLE-CLICK)
        cellContainer.setOnMouseClicked(event -> {
            // Logic Single-Click (Duy trì chọn item cho cột Detail)
            if (viewModel != null) {
                viewModel.selectedItemProperty().set(item);
            }

            // Logic Double-Click (Mở/Phát File)
            if (event.getClickCount() == 2) {
                handleDoubleClick(item);
            }
        });

        return cellContainer;
    }

    /**
     * Hàm xử lý logic Mở/Phát khi Double-Click.
     * FIX: Phải gọi API chi tiết để có được Path VÀ chỉ Phát nếu KHÔNG phải là folder.
     */
    private void handleDoubleClick(BaseItemDto item) {
        // 1. Báo trạng thái (Clear lỗi cũ)
        if (itemDetailViewModel != null) {
            itemDetailViewModel.clearActionError();
            itemDetailViewModel.reportActionError("Đang lấy đường dẫn chi tiết...");
        } else {
            System.err.println("ItemDetailViewModel is null, cannot report status.");
        }

        // --- Bắt đầu chạy luồng nền để gọi API ---
        new Thread(() -> {
            try {
                String userId = embyService.getCurrentUserId();
                if (userId == null) {
                    throw new IllegalStateException("Chưa đăng nhập. Không thể lấy đường dẫn.");
                }

                // 1. Gọi API để lấy DTO chi tiết (có Path)
                BaseItemDto fullDetails = itemRepository.getFullItemDetails(userId, item.getId());

                // 2. KIỂM TRA FOLDER (FIX LỖI)
                if (fullDetails.isIsFolder() != null && fullDetails.isIsFolder()) {
                    if (itemDetailViewModel != null) {
                        itemDetailViewModel.reportActionError("Double-click chỉ dùng để Phát file. Vui lòng dùng nút ở cột chi tiết nếu bạn muốn Mở Finder.");
                    }
                    return; // Dừng lại nếu là Folder
                }

                // 3. Xử lý Path (Chỉ cho file)
                String path = fullDetails.getPath();
                if (path == null || path.isEmpty()) {
                    throw new IllegalStateException("Không tìm thấy đường dẫn file media này.");
                }

                // 4. Thực hiện hành động mở file
                if (!Desktop.isDesktopSupported()) {
                    throw new UnsupportedOperationException("Hệ điều hành không hỗ trợ Desktop API (Mở/Phát).");
                }

                File fileOrDir = new File(path);
                if (!fileOrDir.exists()) {
                    throw new java.io.FileNotFoundException("Đường dẫn không tồn tại trên hệ thống: " + path);
                }

                Desktop.getDesktop().open(fileOrDir);

                // 5. Báo cáo thành công
                if (itemDetailViewModel != null) {
                    itemDetailViewModel.reportActionError("Phát file: " + item.getName());
                }

            } catch (Exception e) {
                System.err.println("Lỗi khi Phát từ Grid: " + e.getMessage());
                if (itemDetailViewModel != null) {
                    itemDetailViewModel.reportActionError("Lỗi Phát file: " + e.getMessage());
                }
            }
        }).start();
    }
}