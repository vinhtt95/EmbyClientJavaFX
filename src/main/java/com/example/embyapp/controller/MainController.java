package com.example.embyapp.controller;

import com.example.emby.modelEmby.BaseItemDto;
import com.example.embyapp.MainApp;
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.ItemRepository;
import com.example.embyapp.service.UserRepository;
import com.example.embyapp.viewmodel.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
// SỬA LỖI 3 & 4: Thêm import
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;
import java.util.prefs.Preferences; // (CẬP NHẬT) Thêm import

/**
 * (SỬA LỖI GĐ2)
 * Controller Điều Phối (Coordinator) cho MainView.
 * (CẬP NHẬT) Thêm logic lưu/tải vị trí SplitPane.
 * (CẬP NHẬT) Sửa constructor cho ItemDetailViewModel.
 */
public class MainController {

    // --- FXML Layout ---
    @FXML private BorderPane rootPane;
    @FXML private ToolBar mainToolBar;
    @FXML private Button logoutButton;
    @FXML private HBox statusBar;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator statusProgressIndicator;

    // (CẬP NHẬT) Thêm fx:id cho SplitPane
    @FXML private SplitPane mainSplitPane;

    // FXML Container cho 3 cột
    @FXML private AnchorPane leftPaneContainer;
    @FXML private AnchorPane centerPaneContainer;
    @FXML private AnchorPane rightPaneContainer;

    // --- Services & ViewModels ---
    private MainApp mainApp;
    private EmbyService embyService;
    private MainViewModel viewModel; // ViewModel cho MainView (StatusBar, Welcome)

    // Các Repository (Services)
    private ItemRepository itemRepository;
    private UserRepository userRepository;

    // Các Controller con (để gọi hàm)
    private LibraryTreeController libraryTreeController;
    private ItemGridController itemGridController;
    private ItemDetailController itemDetailController;

    // Các ViewModel con (để inject và binding)
    private LibraryTreeViewModel libraryTreeViewModel;
    private ItemGridViewModel itemGridViewModel;
    private ItemDetailViewModel itemDetailViewModel;

    // (CẬP NHẬT) Thêm Preferences để lưu cài đặt
    private Preferences prefs;
    private static final String PREF_NODE_PATH = "/com/example/embyapp/mainview";
    private static final String KEY_DIVIDER_1 = "dividerPos1";
    private static final String KEY_DIVIDER_2 = "dividerPos2";


    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    public void initialize() {
        // 1. Khởi tạo Service và Repository
        this.embyService = EmbyService.getInstance();
        this.itemRepository = new ItemRepository();
        this.userRepository = UserRepository.getInstance();

        // 2. Khởi tạo ViewModel chính
        this.viewModel = new MainViewModel(embyService);

        // 3. Khởi tạo 3 ViewModel con (TRƯỚC KHI LOAD FXML)
        this.libraryTreeViewModel = new LibraryTreeViewModel(itemRepository);
        this.itemGridViewModel = new ItemGridViewModel(itemRepository);

        // (CẬP NHẬT) Sửa constructor
        // Dùng constructor mới (itemRepository, embyService)
        this.itemDetailViewModel = new ItemDetailViewModel(itemRepository, embyService);

        // 4. Tải FXML lồng
        try {
            // Tải Cột Trái (Tree)
            libraryTreeController = loadNestedFXML("LibraryTreeView.fxml", leftPaneContainer);
            if (libraryTreeController != null) {
                libraryTreeController.setViewModel(libraryTreeViewModel); // Inject VM
            }

            // Tải Cột Giữa (Grid)
            itemGridController = loadNestedFXML("ItemGridView.fxml", centerPaneContainer);
            if (itemGridController != null) {
                itemGridController.setViewModel(itemGridViewModel); // Inject VM
            }

            // Tải Cột Phải (Detail)
            itemDetailController = loadNestedFXML("ItemDetailView.fxml", rightPaneContainer);
            if (itemDetailController != null) {
                itemDetailController.setViewModel(itemDetailViewModel); // Inject VM
            }

        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Lỗi nghiêm trọng: Không thể tải giao diện.");
        }

        // 5. Binding UI chính (StatusBar, Welcome)
        bindMainUI();

        // 6. Thiết lập luồng dữ liệu giữa các components
        bindDataFlow();

        // 7. Tải dữ liệu ban đầu
        viewModel.loadUserData(); // Tải Welcome message
        libraryTreeViewModel.loadLibraries(); // Tải cây thư mục

        // 8. (CẬP NHẬT) Tải và Lưu vị trí SplitPane
        prefs = Preferences.userRoot().node(PREF_NODE_PATH);
        loadDividerPositions();
        // Thêm listener để lưu khi người dùng kéo
        if (mainSplitPane.getDividers().size() > 0) {
            mainSplitPane.getDividers().get(0).positionProperty().addListener((obs, oldVal, newVal) -> saveDividerPositions());
        }
        if (mainSplitPane.getDividers().size() > 1) {
            mainSplitPane.getDividers().get(1).positionProperty().addListener((obs, oldVal, newVal) -> saveDividerPositions());
        }
    }

    /**
     * Helper
     * Tải một FXML phụ vào một AnchorPane container.
     */
    private <T> T loadNestedFXML(String fxmlFile, AnchorPane container) throws IOException {
        // (CẬP NHẬT) Đường dẫn FXML
        URL fxmlUrl = getClass().getResource(fxmlFile);

        if (fxmlUrl == null) {
            fxmlUrl = getClass().getResource("/com/example/embyapp/" + fxmlFile);
        }

        if (fxmlUrl == null) {
            throw new IOException("Cannot find FXML file: " + fxmlFile);
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Node node = loader.load();

        // Set AnchorPane constraints để FXML con fill đầy container
        AnchorPane.setTopAnchor(node, 0.0);
        AnchorPane.setBottomAnchor(node, 0.0);
        AnchorPane.setLeftAnchor(node, 0.0);
        AnchorPane.setRightAnchor(node, 0.0);

        container.getChildren().add(node);
        return loader.getController();
    }

    /**
     * Binding UI chính (StatusBar, Welcome Label, Logout Button)
     */
    private void bindMainUI() {
        // Logout
        if (logoutButton != null) {
            logoutButton.setOnAction(event -> handleLogout());
        }

        // --- StatusBar Binding ---
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());
    }

    /**
     * Thiết lập luồng dữ liệu (Data Flow) giữa 3 cột.
     */
    private void bindDataFlow() {
        // --- Flow 1: Tree -> Grid ---
        libraryTreeViewModel.selectedTreeItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                BaseItemDto selectedDto = newVal.getValue();
                String parentId = selectedDto.getId();

                // Tải Grid
                itemGridController.loadItemsByParentId(parentId);

                // Đồng thời, xóa cột Detail (ViewModel sẽ tự gọi API khi Grid thay đổi)
                // (KHÔNG CẦN GỌI CỘT DETAIL TỪ ĐÂY NỮA)
                // itemDetailViewModel.setItemToDisplay(null); // <-- XÓA DÒNG NÀY

            } else {
                itemGridController.loadItemsByParentId(null); // Xóa Grid
                // itemDetailViewModel.setItemToDisplay(null); // <-- XÓA DÒNG NÀY
            }
        });

        // --- Flow 2: Grid -> Detail ---
        // (CẬP NHẬT) Logic này giờ là logic chính điều khiển Cột Detail
        itemGridViewModel.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            // Bất kể newVal là null hay không, cứ đẩy nó sang ItemDetailViewModel
            // ViewModel sẽ tự quyết định (hiển thị "Vui lòng chọn..." hoặc gọi API)
            itemDetailViewModel.setItemToDisplay(newVal);
        });


        // --- Flow 3: Cập nhật StatusBar (Logic phức tạp) ---
        ReadOnlyBooleanProperty treeLoading = libraryTreeViewModel.loadingProperty();
        ReadOnlyBooleanProperty gridLoading = itemGridViewModel.loadingProperty();
        // (CẬP NHẬT) Thêm trạng thái loading của Cột Detail
        ReadOnlyBooleanProperty detailLoading = itemDetailViewModel.loadingProperty();

        // 1. Bind trạng thái Loading (ProgressIndicator)
        statusProgressIndicator.visibleProperty().bind(
                Bindings.or(treeLoading, gridLoading).or(detailLoading)
        );

        // 2. Bind trạng thái Status Message
        treeLoading.addListener((obs, old, isTreeLoading) -> updateStatusMessage(isTreeLoading, gridLoading.get(), detailLoading.get()));
        gridLoading.addListener((obs, old, isGridLoading) -> updateStatusMessage(treeLoading.get(), isGridLoading, detailLoading.get()));
        detailLoading.addListener((obs, old, isDetailLoading) -> updateStatusMessage(treeLoading.get(), gridLoading.get(), isDetailLoading));

        // (CẬP NHẬT) Thêm listener cho status message của Cột Detail
        // (Nếu Detail đang hiển thị status (ví dụ: lỗi), nó sẽ ưu tiên hơn)
        itemDetailViewModel.statusMessageProperty().addListener((obs, old, newStatus) -> {
            if (itemDetailViewModel.showStatusMessageProperty().get() && !newStatus.isEmpty()) {
                // Nếu Detail VM muốn hiển thị status, hãy ưu tiên nó
                viewModel.statusMessageProperty().set(newStatus);
            }
        });
    }

    /**
     * (CẬP NHẬT) Helper
     * Cập nhật status message dựa trên trạng thái loading của 3 controller.
     */
    private void updateStatusMessage(boolean isTreeLoading, boolean isGridLoading, boolean isDetailLoading) {
        Platform.runLater(() -> {
            // Ưu tiên status message của Detail VM nếu nó đang hiển thị
            if (itemDetailViewModel.showStatusMessageProperty().get()) {
                viewModel.statusMessageProperty().set(itemDetailViewModel.statusMessageProperty().get());
                return;
            }

            // Nếu không, hiển thị status loading
            if (isTreeLoading) {
                viewModel.statusMessageProperty().set("Đang tải thư viện...");
            } else if (isGridLoading) {
                viewModel.statusMessageProperty().set("Đang tải items...");
            } else if (isDetailLoading) {
                viewModel.statusMessageProperty().set("Đang tải chi tiết..."); // (MỚI)
            } else {
                viewModel.statusMessageProperty().set("Sẵn sàng.");
            }
        });
    }

    /**
     * Xử lý Logout
     */
    private void handleLogout() {
        if (embyService != null) {
            embyService.logout();
        }
        if (mainApp != null) {
            mainApp.showLoginView(); // Quay về màn hình login
        }
    }

    // --- (CẬP NHẬT) Các hàm lưu/tải vị trí SplitPane ---

    /**
     * Lưu vị trí hiện tại của các thanh chia SplitPane.
     */
    private void saveDividerPositions() {
        if (mainSplitPane != null && prefs != null && mainSplitPane.getDividers().size() >= 2) {
            double pos1 = mainSplitPane.getDividerPositions()[0];
            double pos2 = mainSplitPane.getDividerPositions()[1];
            // System.out.println("Saving dividers: " + pos1 + ", " + pos2); // Debug
            prefs.putDouble(KEY_DIVIDER_1, pos1);
            prefs.putDouble(KEY_DIVIDER_2, pos2);
            try {
                prefs.flush();
            } catch (Exception e) {
                System.err.println("Error flushing preferences: " + e.getMessage());
            }
        }
    }

    /**
     * Tải và áp dụng vị trí đã lưu của các thanh chia SplitPane.
     */
    private void loadDividerPositions() {
        if (mainSplitPane != null && prefs != null && mainSplitPane.getDividers().size() >= 2) {
            // Lấy giá trị đã lưu, nếu không có thì dùng giá trị mặc định (ví dụ 0.25, 0.75)
            double pos1 = prefs.getDouble(KEY_DIVIDER_1, 0.25);
            double pos2 = prefs.getDouble(KEY_DIVIDER_2, 0.75);
            // System.out.println("Loading dividers: " + pos1 + ", " + pos2); // Debug

            // Phải chạy Platform.runLater() để đảm bảo SplitPane đã được render
            Platform.runLater(() -> {
                mainSplitPane.setDividerPositions(pos1, pos2);
            });
        }
    }
}

