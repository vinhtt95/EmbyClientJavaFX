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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;

/**
 * (SỬA LỖI GĐ2)
 * Controller Điều Phối (Coordinator) cho MainView.
 *
 * Sửa lỗi:
 * - (Lỗi 1) Gọi new UserRepository(); (constructor rỗng)
 * - (Lỗi 2) Gọi new MainViewModel(embyService); (constructor 1 tham số)
 * - (Lỗi 3,4) Thêm import ReadOnlyBooleanProperty
 * - (Lỗi 5) Sửa lỗi 'private access' cho UserRepository (dùng .getInstance()).
 * - (Lỗi 6) Sửa lỗi binding cho ProgressIndicator (lỗi incompatible types).
 * - (Lỗi 7) Sửa lỗi kiểu dữ liệu cho 'treeLoading' (dòng 224)
 * - (Lỗi 8) Sửa lỗi đường dẫn FXML (thêm "../")
 */
public class MainController {

    // --- FXML Layout ---
    @FXML private BorderPane rootPane;
    @FXML private ToolBar mainToolBar;
    @FXML private Button logoutButton;
    @FXML private HBox statusBar;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator statusProgressIndicator;

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


    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    public void initialize() {
        // 1. Khởi tạo Service và Repository
        this.embyService = EmbyService.getInstance();

        // SỬA LỖI 1: Gọi constructor rỗng (nó sẽ tự .getInstance() bên trong)
        this.itemRepository = new ItemRepository();
        // SỬA LỖI 1 & 5: Gọi getInstance() vì constructor là private (Singleton)
        this.userRepository = UserRepository.getInstance();

        // 2. Khởi tạo ViewModel chính
        // SỬA LỖI 2: Gọi constructor 1 tham số
        this.viewModel = new MainViewModel(embyService);

        // 3. Khởi tạo 3 ViewModel con (TRƯỚC KHI LOAD FXML)
        this.libraryTreeViewModel = new LibraryTreeViewModel(itemRepository);
        this.itemGridViewModel = new ItemGridViewModel(itemRepository);
        this.itemDetailViewModel = new ItemDetailViewModel();

        // 4. Tải FXML lồng
        try {
            // SỬA LỖI 8: Thêm "../" để tìm FXML ở thư mục cha
            // (vì MainController ở package controller, còn FXML ở package embyapp)

            // Tải Cột Trái (Tree)
            libraryTreeController = loadNestedFXML("../LibraryTreeView.fxml", leftPaneContainer);
            if (libraryTreeController != null) {
                libraryTreeController.setViewModel(libraryTreeViewModel); // Inject VM
            }

            // Tải Cột Giữa (Grid)
            itemGridController = loadNestedFXML("../ItemGridView.fxml", centerPaneContainer);
            if (itemGridController != null) {
                itemGridController.setViewModel(itemGridViewModel); // Inject VM
            }

            // Tải Cột Phải (Detail)
            itemDetailController = loadNestedFXML("../ItemDetailView.fxml", rightPaneContainer);
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
    }

    /**
     * Helper
     * Tải một FXML phụ vào một AnchorPane container.
     */
    private <T> T loadNestedFXML(String fxmlFile, AnchorPane container) throws IOException {
        // SỬA LỖI 8: Dòng này đã được sửa ở hàm initialize()
        // (Hoặc sửa ở đây cũng được)
        // URL fxmlUrl = getClass().getResource(fxmlFile); // LỖI

        // Sửa ở đây cho chắc chắn:
        URL fxmlUrl = getClass().getResource(fxmlFile);

        if (fxmlUrl == null) {
            // Thử tìm ở thư mục gốc nếu không thấy
            // (Nhưng logic ở initialize() đã thêm ../ rồi)
            fxmlUrl = getClass().getResource("/com/example/embyapp/" + fxmlFile.replace("../", ""));
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
        // (Welcome Label đã bị xóa khỏi MainView.fxml,
        // nếu cần, sếp thêm Label vào ToolBar và bind ở đây)
        // welcomeLabel.textProperty().bind(viewModel.welcomeMessageProperty());

        // Logout
        if (logoutButton != null) {
            logoutButton.setOnAction(event -> handleLogout());
        }

        // --- StatusBar Binding ---
        // Chỉ bind statusLabel.
        // ProgressIndicator sẽ được bind trong bindDataFlow()
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());
        // SỬA LỖI 6: Xóa dòng bind ProgressIndicator ở đây (sẽ bind trong dataflow)
        // statusProgressIndicator.visibleProperty().bind(viewModel.loadingProperty());
    }

    /**
     * Thiết lập luồng dữ liệu (Data Flow) giữa 3 cột.
     * Đây là logic "Điều Phối" (Coordination) chính.
     */
    private void bindDataFlow() {
        // --- Flow 1: Tree -> Grid ---
        // Khi TreeView (trong LibraryTreeViewModel) thay đổi lựa chọn...
        libraryTreeViewModel.selectedTreeItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                // Sửa lỗi: newVal chính là TreeItem,
                // chúng ta cần .getValue() để lấy BaseItemDto
                BaseItemDto selectedDto = newVal.getValue();
                String parentId = selectedDto.getId();

                // ...thì gọi Grid (ItemGridController) tải items mới
                // Sửa lỗi: Gọi đúng tên hàm
                itemGridController.loadItemsByParentId(parentId);

                // Đồng thời, xóa cột Detail
                // Sửa lỗi: Gọi đúng hàm trên VM
                itemDetailViewModel.setItemToDisplay(null);

            } else {
                // Nếu không chọn gì (ví dụ: click vào khoảng trống)
                // Sửa lỗi: Gọi đúng tên hàm
                itemGridController.loadItemsByParentId(null); // Xóa Grid
                itemDetailViewModel.setItemToDisplay(null); // Xóa Detail
            }
        });

        // --- Flow 2: Grid -> Detail ---
        // Khi Grid (trong ItemGridViewModel) thay đổi lựa chọn...
        // SỬA LỖI: Lắng nghe 'itemGridViewModel', không phải 'itemGridController'
        itemGridViewModel.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // ...thì gọi Detail (ItemDetailController) hiển thị chi tiết
                // Sửa lỗi: Gọi đúng hàm trên VM
                itemDetailViewModel.setItemToDisplay(newVal);
            } else {
                // Sửa lỗi: Gọi đúng hàm trên VM
                itemDetailViewModel.setItemToDisplay(null);
            }
        });


        // --- Flow 3: Cập nhật StatusBar (Logic phức tạp) ---

        // Lấy property (Lưu ý: treeLoading là BooleanProperty (RW),
        // gridLoading là ReadOnlyBooleanProperty (RO))

        // SỬA LỖI 7: Kiểu dữ liệu phải là ReadOnlyBooleanProperty
        ReadOnlyBooleanProperty treeLoading = libraryTreeViewModel.loadingProperty();
        ReadOnlyBooleanProperty gridLoading = itemGridViewModel.loadingProperty();

        // 1. Bind trạng thái Loading (ProgressIndicator)
        // SỬA LỖI 6: Bind trực tiếp Indicator, thay vì bind VM
        // (Đây là nguyên nhân gây lỗi 219)
        statusProgressIndicator.visibleProperty().bind(
                Bindings.or(treeLoading, gridLoading)
        );

        // Xóa dòng bind viewModel, vì nó là ReadOnly
        // viewModel.loadingProperty().bind( Bindings.or(treeLoading, gridLoading) ); // <- LỖI 219

        // 2. Bind trạng thái Status Message
        // SỬA LỖI 6: Thêm listener kép để cập nhật Status Message
        treeLoading.addListener((obs, old, isTreeLoading) -> updateStatusMessage(isTreeLoading, gridLoading.get()));
        gridLoading.addListener((obs, old, isGridLoading) -> updateStatusMessage(treeLoading.get(), isGridLoading));

        // Xóa listener cũ (nó bind vào viewModel.loadingProperty() đã bị xóa)
        /*
        viewModel.loadingProperty().addListener((obs, oldLoading, newLoading) -> {
            Platform.runLater(() -> {
                if (newLoading) {
                    if (treeLoading.get()) {
                        viewModel.statusMessageProperty().set("Đang tải thư viện...");
                    } else if (gridLoading.get()) {
                        viewModel.statusMessageProperty().set("Đang tải items...");
                    }
                } else {
                    viewModel.statusMessageProperty().set("Sẵn sàng.");
                }
            });
        });
        */
    }

    /**
     * SỬA LỖI 6: Helper
     * Cập nhật status message dựa trên trạng thái loading của 2 controller.
     */
    private void updateStatusMessage(boolean isTreeLoading, boolean isGridLoading) {
        Platform.runLater(() -> {
            if (isTreeLoading) {
                viewModel.statusMessageProperty().set("Đang tải thư viện...");
            } else if (isGridLoading) {
                viewModel.statusMessageProperty().set("Đang tải items...");
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
}

