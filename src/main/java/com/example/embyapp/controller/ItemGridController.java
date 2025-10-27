package com.example.embyapp.controller;

import embyclient.model.BaseItemDto;
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.I18nManager;
import com.example.embyapp.viewmodel.ItemDetailViewModel;
import com.example.embyapp.viewmodel.ItemGridViewModel;
import com.example.embyapp.viewmodel.ItemGridViewModel.ScrollAction;
import com.example.embyapp.service.ItemRepository;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.text.TextAlignment;

import java.util.Map;
import java.awt.Desktop;
import java.io.File;
import java.util.Optional;


/**
 * Controller cho ItemGridView (Cột giữa).
 * Áp dụng logic Page Replacement, và Context Menu.
 */
public class ItemGridController {

    @FXML private StackPane rootStackPane;
    @FXML private ScrollPane gridScrollPane;
    @FXML private FlowPane itemFlowPane;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;

    private ItemGridViewModel viewModel;
    private ItemDetailViewModel itemDetailViewModel;

    private boolean ignoreNextScrollEvent = false;

    private final EmbyService embyService = EmbyService.getInstance();
    private final ItemRepository itemRepository = new ItemRepository();

    private static final double CELL_HEIGHT = 320;
    private static final double CELL_WIDTH = CELL_HEIGHT * 16 / 9;
    private static final double IMAGE_HEIGHT = CELL_HEIGHT;

    @FXML
    public void initialize() {
        itemFlowPane.setPadding(new Insets(20));
        itemFlowPane.setHgap(20);
        itemFlowPane.setVgap(20);
        itemFlowPane.setAlignment(Pos.CENTER);

        gridScrollPane.setFitToWidth(true);

        gridScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (viewModel == null || viewModel.loadingProperty().get()) return;

            if (ignoreNextScrollEvent) {
                return;
            }

            // 1. SCROLL TỚI CUỐI (Load Trang Kế Tiếp)
            if (newVal.doubleValue() > 0.95 && oldVal.doubleValue() < 0.95) {
                if (viewModel.hasNextPageProperty().get()) {
                    int nextPageDisplay = viewModel.getCurrentPageIndex() + 2;
                    int totalPages = viewModel.getTotalPages();

                    if (showConfirmationDialog(
                            I18nManager.getInstance().getString("itemGridView", "confirmDialogTitle"),
                            I18nManager.getInstance().getString("itemGridView", "confirmNextPage", nextPageDisplay, totalPages)
                    )) {
                        viewModel.loadNextPage();
                    }
                }
            }

            // 2. SCROLL LÊN ĐẦU (Load Trang Trước Đó)
            else if (newVal.doubleValue() < 0.05 && oldVal.doubleValue() > 0.05) {
                if (viewModel.hasPreviousPageProperty().get()) {
                    int prevPageDisplay = viewModel.getCurrentPageIndex();
                    int totalPages = viewModel.getTotalPages();

                    if (showConfirmationDialog(
                            I18nManager.getInstance().getString("itemGridView", "confirmDialogTitle"),
                            I18nManager.getInstance().getString("itemGridView", "confirmPrevPage", prevPageDisplay, totalPages)
                    )) {
                        viewModel.loadPreviousPage();
                    }
                }
            }
        });

        statusLabel.setText(I18nManager.getInstance().getString("itemGridView", "statusDefault"));
    }

    public void setViewModel(ItemGridViewModel viewModel) {
        this.viewModel = viewModel;

        loadingIndicator.visibleProperty().bind(viewModel.loadingProperty());
        loadingIndicator.managedProperty().bind(viewModel.loadingProperty());

        statusLabel.visibleProperty().bind(viewModel.showStatusMessageProperty());
        statusLabel.managedProperty().bind(viewModel.showStatusMessageProperty());
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        gridScrollPane.visibleProperty().bind(
                viewModel.loadingProperty().not().and(viewModel.showStatusMessageProperty().not())
        );
        gridScrollPane.managedProperty().bind(gridScrollPane.visibleProperty());


        viewModel.getItems().addListener((ListChangeListener<BaseItemDto>) c -> {
            Platform.runLater(() -> {
                itemFlowPane.getChildren().clear();
                for (BaseItemDto item : viewModel.getItems()) {
                    itemFlowPane.getChildren().add(createItemCell(item));
                }
            });
        });

        viewModel.scrollActionProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == ScrollAction.SCROLL_TO_TOP) {
                ignoreNextScrollEvent = true;
                Platform.runLater(() -> {
                    gridScrollPane.setVvalue(0.0);
                    Platform.runLater(() -> ignoreNextScrollEvent = false);
                });
                viewModel.scrollActionProperty().set(ScrollAction.NONE);
            } else if (newVal == ScrollAction.SCROLL_TO_BOTTOM) {
                ignoreNextScrollEvent = true;
                Platform.runLater(() -> {
                    gridScrollPane.setVvalue(1.0);
                    Platform.runLater(() -> ignoreNextScrollEvent = false);
                });
                viewModel.scrollActionProperty().set(ScrollAction.NONE);
            }
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

    private StackPane createItemCell(BaseItemDto item) {
        StackPane cellContainer = new StackPane();
        cellContainer.setPrefSize(CELL_WIDTH, CELL_HEIGHT);
        cellContainer.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        cellContainer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        cellContainer.getStyleClass().add("item-cell");

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


        cellContainer.setOnMouseClicked(event -> {
            if (viewModel != null) {
                viewModel.selectedItemProperty().set(item);
            }

            if (event.getClickCount() == 2) {
                handleDoubleClick(item);
            }
        });

        final ContextMenu contextMenu = new ContextMenu();
        final MenuItem copyIdItem = new MenuItem(I18nManager.getInstance().getString("contextMenu", "copyId"));
        copyIdItem.setOnAction(e -> {
            if (item != null && item.getId() != null) {
                final Clipboard clipboard = Clipboard.getSystemClipboard();
                final ClipboardContent content = new ClipboardContent();
                content.putString(item.getId());
                clipboard.setContent(content);
            }
        });
        contextMenu.getItems().add(copyIdItem);

        cellContainer.setOnContextMenuRequested(event -> {
            contextMenu.show(cellContainer, event.getScreenX(), event.getScreenY());
            event.consume();
        });

        return cellContainer;
    }

    /**
     * Hàm xử lý logic Mở/Phát khi Double-Click.
     */
    private void handleDoubleClick(BaseItemDto item) {
        I18nManager i18n = I18nManager.getInstance();
        if (itemDetailViewModel != null) {
            itemDetailViewModel.clearActionError();
            itemDetailViewModel.reportActionError(i18n.getString("itemGridController", "statusGetPath"));
        } else {
            System.err.println(i18n.getString("itemGridController", "errorDetailVMNull"));
        }

        new Thread(() -> {
            try {
                String userId = embyService.getCurrentUserId();
                if (userId == null) {
                    throw new IllegalStateException(i18n.getString("itemGridController", "errorNoLogin"));
                }

                BaseItemDto fullDetails = itemRepository.getFullItemDetails(userId, item.getId());

                if (fullDetails.isIsFolder() != null && fullDetails.isIsFolder()) {
                    if (itemDetailViewModel != null) {
                        itemDetailViewModel.reportActionError(i18n.getString("itemGridController", "errorFolderDoubleClick"));
                    }
                    return;
                }

                String path = fullDetails.getPath();
                if (path == null || path.isEmpty()) {
                    throw new IllegalStateException(i18n.getString("itemGridController", "errorNoPath"));
                }

                if (!Desktop.isDesktopSupported()) {
                    throw new UnsupportedOperationException(i18n.getString("itemGridController", "errorNoDesktopSupport"));
                }

                File fileOrDir = new File(path);
                if (!fileOrDir.exists()) {
                    throw new java.io.FileNotFoundException(i18n.getString("itemGridController", "errorPathNotExist", path));
                }

                Desktop.getDesktop().open(fileOrDir);

                if (itemDetailViewModel != null) {
                    itemDetailViewModel.reportActionError(i18n.getString("itemGridController", "statusPlayFile", item.getName()));
                }

            } catch (Exception e) {
                System.err.println("Lỗi khi Phát từ Grid: " + e.getMessage());
                if (itemDetailViewModel != null) {
                    itemDetailViewModel.reportActionError(i18n.getString("itemGridController", "errorPlayFile", e.getMessage()));
                }
            }
        }).start();
    }

    /**
     * Hiển thị hộp thoại xác nhận.
     * @return true nếu người dùng chọn Yes, false nếu chọn No/Cancel.
     */
    private boolean showConfirmationDialog(String title, String content) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }
}