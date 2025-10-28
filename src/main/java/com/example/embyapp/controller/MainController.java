package com.example.embyapp.controller;

import embyclient.model.BaseItemDto;
import com.example.embyapp.MainApp;
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.I18nManager;
import com.example.embyapp.service.ItemRepository;
import com.example.embyapp.service.UserRepository;
import com.example.embyapp.viewmodel.*;
// (*** THÊM IMPORT NÀY CHO DELAY ***)
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.TreeItem;
// (*** THÊM IMPORT NÀY CHO DELAY ***)
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

// Imports cho Global Hotkey
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.NativeInputEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;


/**
 * Controller Điều Phối (Coordinator) cho MainView.
 * Implement NativeKeyListener để bắt hotkey hệ thống.
 */
public class MainController implements NativeKeyListener {

    // ... (Các khai báo @FXML và biến thành viên khác giữ nguyên) ...
    @FXML private BorderPane rootPane;
    @FXML private ToolBar mainToolBar;
    @FXML private Button logoutButton;
    @FXML private HBox statusBar;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator statusProgressIndicator;
    @FXML private SplitPane mainSplitPane;
    @FXML private AnchorPane leftPaneContainer;
    @FXML private AnchorPane centerPaneContainer;
    @FXML private AnchorPane rightPaneContainer;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button sortByButton;
    @FXML private ToggleButton sortOrderButton;

    private MainApp mainApp;
    private EmbyService embyService;
    private MainViewModel viewModel;
    private ItemRepository itemRepository;
    private UserRepository userRepository;
    private LibraryTreeController libraryTreeController;
    private ItemGridController itemGridController;
    private ItemDetailController itemDetailController;
    private LibraryTreeViewModel libraryTreeViewModel;
    private ItemGridViewModel itemGridViewModel;
    private ItemDetailViewModel itemDetailViewModel;

    private Preferences prefs;
    private static final String PREF_NODE_PATH = "/com/example/embyapp/mainview";
    private static final String KEY_DIVIDER_1 = "dividerPos1";
    private static final String KEY_DIVIDER_2 = "dividerPos2";
    private static final String KEY_DIALOG_WIDTH = "dialogWidth";
    private static final String KEY_DIALOG_HEIGHT = "dialogHeight";
    private static final String KEY_DIALOG_X = "dialogX";
    private static final String KEY_DIALOG_Y = "dialogY";

    private Stage detailDialog;
    private Parent detailDialogRoot;
    private ItemDetailController detailDialogController;

    // (*** THÊM BIẾN CỜ DEBOUNCE NÀY ***)
    private volatile boolean processingHotkey = false;
    private final PauseTransition hotkeyDebounceTimer = new PauseTransition(Duration.millis(200)); // 200ms delay


    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    public void initialize() {
        setupLocalization();

        this.embyService = EmbyService.getInstance();
        this.itemRepository = new ItemRepository();
        this.userRepository = UserRepository.getInstance();
        this.viewModel = new MainViewModel(embyService);
        this.libraryTreeViewModel = new LibraryTreeViewModel(itemRepository);
        this.itemGridViewModel = new ItemGridViewModel(itemRepository);
        this.itemDetailViewModel = new ItemDetailViewModel(itemRepository, embyService);

        try {
            libraryTreeController = loadNestedFXML("LibraryTreeView.fxml", leftPaneContainer);
            if (libraryTreeController != null) {
                libraryTreeController.setViewModel(libraryTreeViewModel);
            }

            itemGridController = loadNestedFXML("ItemGridView.fxml", centerPaneContainer);
            if (itemGridController != null) {
                itemGridController.setViewModel(itemGridViewModel);
            }

            itemDetailController = loadNestedFXML("ItemDetailView.fxml", rightPaneContainer);
            if (itemDetailController != null) {
                itemDetailController.setViewModel(itemDetailViewModel);
            }

            if (itemGridController != null) {
                itemGridController.setItemDetailViewModel(itemDetailViewModel);
            }

        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText(I18nManager.getInstance().getString("mainView", "errorLoadUI"));
        }

        bindMainUI();
        bindSortingButtons();
        bindDataFlow();

        viewModel.loadUserData();
        libraryTreeViewModel.loadLibraries();

        prefs = Preferences.userRoot().node(PREF_NODE_PATH);
        loadDividerPositions();
        if (mainSplitPane.getDividers().size() > 0) {
            mainSplitPane.getDividers().get(0).positionProperty().addListener((obs, oldVal, newVal) -> saveDividerPositions());
        }
        if (mainSplitPane.getDividers().size() > 1) {
            mainSplitPane.getDividers().get(1).positionProperty().addListener((obs, oldVal, newVal) -> saveDividerPositions());
        }

        // (*** CẤU HÌNH CHO TIMER DEBOUNCE ***)
        hotkeyDebounceTimer.setOnFinished(event -> processingHotkey = false);

        // Đăng ký hotkey hệ thống
        registerSystemHotkeys();
    }

    private void setupLocalization() {
        I18nManager i18n = I18nManager.getInstance();
        logoutButton.setText(i18n.getString("mainView", "logoutButton"));
        searchField.setPromptText(i18n.getString("mainView", "searchPrompt"));
        searchButton.setText(i18n.getString("mainView", "searchButton"));
        statusLabel.setText(i18n.getString("mainView", "statusInitializing"));
    }

    private void bindSortingButtons() {
        if (itemGridViewModel == null || sortByButton == null || sortOrderButton == null) return;

        sortByButton.textProperty().bind(
                Bindings.createStringBinding(() -> {
                    I18nManager i18n = I18nManager.getInstance();
                    String currentSortBy = itemGridViewModel.currentSortByProperty().get();
                    if (currentSortBy.equals(ItemGridViewModel.SORT_BY_NAME)) {
                        return i18n.getString("mainView", "sortByName");
                    } else if (currentSortBy.equals(ItemGridViewModel.SORT_BY_DATE_RELEASE)) {
                        return i18n.getString("mainView", "sortByDateRelease");
                    }
                    return i18n.getString("mainView", "sortByDefault");
                }, itemGridViewModel.currentSortByProperty())
        );
        sortByButton.disableProperty().bind(itemGridViewModel.loadingProperty().or(Bindings.createBooleanBinding(itemGridViewModel::isSearching, itemGridViewModel.statusMessageProperty())));

        sortOrderButton.textProperty().bind(
                Bindings.createStringBinding(() -> {
                    I18nManager i18n = I18nManager.getInstance();
                    String currentOrder = itemGridViewModel.currentSortOrderProperty().get();
                    if (currentOrder.equals(ItemGridViewModel.SORT_ORDER_ASCENDING)) {
                        return i18n.getString("mainView", "orderAsc");
                    } else {
                        return i18n.getString("mainView", "orderDesc");
                    }
                }, itemGridViewModel.currentSortOrderProperty())
        );
        sortOrderButton.disableProperty().bind(itemGridViewModel.loadingProperty().or(Bindings.createBooleanBinding(itemGridViewModel::isSearching, itemGridViewModel.statusMessageProperty())));
        sortOrderButton.setSelected(itemGridViewModel.currentSortOrderProperty().get().equals(ItemGridViewModel.SORT_ORDER_ASCENDING));
    }


    /**
     * Tải một FXML phụ vào một AnchorPane container.
     */
    private <T> T loadNestedFXML(String fxmlFile, AnchorPane container) throws IOException {
        URL fxmlUrl = getClass().getResource(fxmlFile);
        if (fxmlUrl == null) {
            fxmlUrl = getClass().getResource("/com/example/embyapp/" + fxmlFile);
        }
        if (fxmlUrl == null) {
            throw new IOException("Cannot find FXML file: " + fxmlFile);
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Node node = loader.load();

        AnchorPane.setTopAnchor(node, 0.0);
        AnchorPane.setBottomAnchor(node, 0.0);
        AnchorPane.setLeftAnchor(node, 0.0);
        AnchorPane.setRightAnchor(node, 0.0);

        container.getChildren().add(node);
        return loader.getController();
    }

    private void bindMainUI() {
        if (logoutButton != null) {
            logoutButton.setOnAction(event -> handleLogout());
        }
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());
    }

    private void bindDataFlow() {
        libraryTreeViewModel.selectedTreeItemProperty().addListener((obs, oldVal, newVal) -> {
            if (itemDetailViewModel != null) {
                itemDetailViewModel.selectedTreeItemProperty().set(newVal);
            }
            if (newVal != null && newVal.getValue() != null) {
                BaseItemDto selectedDto = newVal.getValue();
                String parentId = selectedDto.getId();
                if (searchField != null) {
                    searchField.setText("");
                }
                itemGridController.loadItemsByParentId(parentId);
            } else {
                itemGridController.loadItemsByParentId(null);
            }
        });

        // Sửa listener này để xử lý `playAfterSelect`
        itemGridViewModel.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            itemDetailViewModel.setItemToDisplay(newVal);

            // Logic mới để "phát" tự động khi hotkey hệ thống được nhấn
            if (newVal != null && itemGridViewModel.isPlayAfterSelect()) {
                System.out.println("Hotkey: Phát tự động item: " + newVal.getName());
                itemGridController.playItem(newVal); // Gọi hàm public của ItemGridController
                itemGridViewModel.clearPlayAfterSelect(); // Xóa cờ
            }
        });

        ReadOnlyBooleanProperty treeLoading = libraryTreeViewModel.loadingProperty();
        ReadOnlyBooleanProperty gridLoading = itemGridViewModel.loadingProperty();
        ReadOnlyBooleanProperty detailLoading = itemDetailViewModel.loadingProperty();
        ReadOnlyStringProperty actionStatus = itemDetailViewModel.actionStatusMessageProperty();

        statusProgressIndicator.visibleProperty().bind(
                Bindings.or(treeLoading, gridLoading).or(detailLoading)
        );

        treeLoading.addListener((obs, old, isTreeLoading) -> updateStatusMessage(isTreeLoading, gridLoading.get(), detailLoading.get(), actionStatus.get()));
        gridLoading.addListener((obs, old, isGridLoading) -> updateStatusMessage(treeLoading.get(), isGridLoading, detailLoading.get(), actionStatus.get()));
        detailLoading.addListener((obs, old, isDetailLoading) -> updateStatusMessage(treeLoading.get(), gridLoading.get(), isDetailLoading, actionStatus.get()));
        actionStatus.addListener((obs, old, newActionStatus) -> updateStatusMessage(treeLoading.get(), gridLoading.get(), detailLoading.get(), newActionStatus));

        itemDetailViewModel.statusMessageProperty().addListener((obs, old, newStatus) -> {
            if (itemDetailViewModel.showStatusMessageProperty().get() && !newStatus.isEmpty()) {
                viewModel.statusMessageProperty().set(newStatus);
            }
        });

        itemDetailViewModel.popOutRequestProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && newV) {
                showDetailDialog();
                Platform.runLater(() -> itemDetailViewModel.popOutRequestProperty().set(null));
            }
        });
    }

    /**
     * Cập nhật status message dựa trên trạng thái loading và lỗi hành động.
     */
    private void updateStatusMessage(boolean isTreeLoading, boolean isGridLoading, boolean isDetailLoading, String actionStatus) {
        Platform.runLater(() -> {
            I18nManager i18n = I18nManager.getInstance();
            if (actionStatus != null && !actionStatus.isEmpty()) {
                viewModel.statusMessageProperty().set(actionStatus);
                return;
            }
            if (itemDetailViewModel.showStatusMessageProperty().get()) {
                viewModel.statusMessageProperty().set(itemDetailViewModel.statusMessageProperty().get());
                return;
            }
            if (isTreeLoading) {
                viewModel.statusMessageProperty().set(i18n.getString("mainView", "statusLoadingLibrary"));
            } else if (isGridLoading) {
                viewModel.statusMessageProperty().set(i18n.getString("mainView", "statusLoadingItems"));
            } else if (isDetailLoading) {
                viewModel.statusMessageProperty().set(i18n.getString("mainView", "statusLoadingDetail"));
            } else {
                viewModel.statusMessageProperty().set(i18n.getString("mainView", "statusReady"));
            }
        });
    }

    private void handleLogout() {
        if (embyService != null) {
            embyService.logout();
        }
        if (mainApp != null) {
            mainApp.showLoginView();
        }
    }

    private void saveDividerPositions() {
        if (mainSplitPane != null && prefs != null && mainSplitPane.getDividers().size() >= 2) {
            double[] positions = mainSplitPane.getDividerPositions();
            double pos1 = positions[0];
            double pos2 = positions[1];
            prefs.putDouble(KEY_DIVIDER_1, pos1);
            prefs.putDouble(KEY_DIVIDER_2, pos2);
            try {
                prefs.flush();
            } catch (Exception e) {
                System.err.println("Error flushing preferences: " + e.getMessage());
            }
        }
    }

    private void loadDividerPositions() {
        if (mainSplitPane != null && prefs != null && mainSplitPane.getDividers().size() >= 2) {
            double pos1 = prefs.getDouble(KEY_DIVIDER_1, 0.25);
            double pos2 = prefs.getDouble(KEY_DIVIDER_2, 0.75);
            Platform.runLater(() -> {
                mainSplitPane.setDividerPositions(pos1, pos2);
            });
        }
    }

    private void showDetailDialog() {
        try {
            if (detailDialog == null) {
                System.out.println("Đang tạo Pop-out Detail Dialog lần đầu...");

                URL fxmlUrl = getClass().getResource("/com/example/embyapp/ItemDetailView.fxml");
                if (fxmlUrl == null) {
                    throw new IOException("Không thể tìm thấy /com/example/embyapp/ItemDetailView.fxml");
                }

                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                detailDialogRoot = loader.load();
                detailDialogController = loader.getController();
                detailDialogController.setViewModel(this.itemDetailViewModel);

                Scene scene = new Scene(detailDialogRoot);
                if (rootPane.getScene() != null && rootPane.getScene().getStylesheets() != null) {
                    scene.getStylesheets().addAll(rootPane.getScene().getStylesheets());
                }

                // Đăng ký TẤT CẢ hotkey cho scene của dialog
                registerHotkeysForScene(scene);


                double defaultWidth = 1000;
                double defaultHeight = 800;
                if (rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
                    defaultWidth = rootPane.getScene().getWindow().getWidth() * 0.8;
                    defaultHeight = rootPane.getScene().getWindow().getHeight() * 0.8;
                }

                double savedWidth = prefs.getDouble(KEY_DIALOG_WIDTH, defaultWidth);
                double savedHeight = prefs.getDouble(KEY_DIALOG_HEIGHT, defaultHeight);
                double savedX = prefs.getDouble(KEY_DIALOG_X, -1);
                double savedY = prefs.getDouble(KEY_DIALOG_Y, -1);

                detailDialog = new Stage();
                detailDialog.setTitle(I18nManager.getInstance().getString("itemDetailView", "popOutTitle"));
                detailDialog.setWidth(savedWidth);
                detailDialog.setHeight(savedHeight);
                if (savedX != -1 && savedY != -1) {
                    detailDialog.setX(savedX);
                    detailDialog.setY(savedY);
                }
                detailDialog.initModality(Modality.NONE);
                detailDialog.setScene(scene); // Gán scene đã được đăng ký hotkey

                detailDialog.setOnCloseRequest(e -> {
                    prefs.putDouble(KEY_DIALOG_WIDTH, detailDialog.getWidth());
                    prefs.putDouble(KEY_DIALOG_HEIGHT, detailDialog.getHeight());
                    prefs.putDouble(KEY_DIALOG_X, detailDialog.getX());
                    prefs.putDouble(KEY_DIALOG_Y, detailDialog.getY());
                    try {
                        prefs.flush();
                        System.out.println("Đã lưu kích thước Dialog: " + detailDialog.getWidth() + "x" + detailDialog.getHeight());
                    } catch (Exception ex) {
                        System.err.println("Error flushing dialog size preferences: " + ex.getMessage());
                    }
                    if (detailDialog != null) {
                        detailDialog.hide();
                    }
                    e.consume();
                });
            }

            double savedX = prefs.getDouble(KEY_DIALOG_X, -1);
            double savedY = prefs.getDouble(KEY_DIALOG_Y, -1);
            if (savedX != -1 && savedY != -1) {
                detailDialog.setX(savedX);
                detailDialog.setY(savedY);
            }

            if (!detailDialog.isShowing()) {
                detailDialog.show();
            }
            detailDialog.toFront();

        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText(I18nManager.getInstance().getString("mainView", "errorDialog") + e.getMessage());
        }
    }

    /**
     * Đăng ký Hotkeys (khi app được focus) trên Scene sau khi nó đã được load.
     * @param scene Scene của MainView.
     */
    public void registerGlobalHotkeys(Scene scene) {
        if (scene == null) return;
        // Chỉ cần gọi hàm helper
        registerHotkeysForScene(scene);
    }

    /**
     * Hàm helper đăng ký tất cả các phím tắt cho một Scene cụ thể.
     * @param scene Scene (của cửa sổ chính HOẶC cửa sổ pop-out)
     */
    private void registerHotkeysForScene(Scene scene) {
        if (scene == null) return;

        // --- Lặp lại dialog Add Tag (Phím ENTER) ---
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown() && !event.isControlDown() && !event.isAltDown() && !event.isMetaDown()) {

                Node focusedNode = scene.getFocusOwner();

                boolean isBlockingControl = focusedNode instanceof TextInputControl
                        || focusedNode instanceof Button
                        || focusedNode instanceof ToggleButton;

                if (focusedNode == null || !isBlockingControl) {
                    // Dùng controller của cửa sổ chính hay của dialog?
                    // Nếu detailDialog đang hiển thị VÀ scene này là scene của dialog
                    if (detailDialog != null && detailDialog.isShowing() && scene == detailDialog.getScene()) {
                        if (detailDialogController != null) {
                            detailDialogController.handleRepeatAddTagDialog();
                            event.consume();
                        }
                    }
                    // Ngược lại, dùng controller của cửa sổ chính
                    else if (itemDetailController != null) {
                        itemDetailController.handleRepeatAddTagDialog();
                        event.consume();
                    }
                }
            }
        });


        // --- Cmd + S (Save) ---
        final KeyCombination saveShortcut = new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN);
        scene.getAccelerators().put(saveShortcut, () -> {
            // Tương tự, kiểm tra xem controller nào đang active
            if (detailDialog != null && detailDialog.isShowing() && scene == detailDialog.getScene()) {
                if (detailDialogController != null) {
                    detailDialogController.handleSaveHotkey();
                }
            } else if (itemDetailController != null) {
                itemDetailController.handleSaveHotkey();
            }
        });

        // --- Cmd + N (Next Item - CHỈ CHỌN) ---
        final KeyCombination nextShortcut = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);
        scene.getAccelerators().put(nextShortcut, () -> {
            if (itemGridViewModel != null) {
                itemGridViewModel.selectNextItem();
            }
        });

        // --- Cmd + P (Previous Item - CHỈ CHỌN) ---
        final KeyCombination prevShortcut = new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN);
        scene.getAccelerators().put(prevShortcut, () -> {
            if (itemGridViewModel != null) {
                itemGridViewModel.selectPreviousItem();
            }
        });

        // --- Cmd + ENTER (Play Item) ---
        final KeyCombination playShortcut = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN);
        scene.getAccelerators().put(playShortcut, () -> {
            if (itemGridViewModel != null && itemGridController != null) {
                BaseItemDto selectedItem = itemGridViewModel.selectedItemProperty().get();
                if (selectedItem != null) {
                    System.out.println("Hotkey Cmd+ENTER: Playing item: " + selectedItem.getName());
                    itemGridController.playItem(selectedItem);
                } else {
                    System.out.println("Hotkey Cmd+ENTER: No item selected to play.");
                }
            }
        });

        System.out.println("Hotkeys registered for scene: " + scene.hashCode());
    }


    // --- Các hàm cho Hotkey Hệ Thống (JNativeHook) ---

    /**
     * Đăng ký trình lắng nghe hotkey toàn hệ thống.
     */
    private void registerSystemHotkeys() {
        // Tắt log của JNativeHook
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);

        try {
            GlobalScreen.registerNativeHook();
            System.out.println("Global hotkey hook đã đăng ký.");
        } catch (NativeHookException ex) {
            System.err.println("Lỗi nghiêm trọng khi đăng ký global hotkey hook.");
            System.err.println(ex.getMessage());
            return;
        }

        GlobalScreen.addNativeKeyListener(this);
    }

    /**
     * Hủy đăng ký trình lắng nghe hotkey (gọi khi thoát ứng dụng).
     */
    public void shutdown() {
        try {
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.unregisterNativeHook();
            System.out.println("Global hotkey hook đã được hủy đăng ký.");
        } catch (NativeHookException ex) {
            System.err.println("Lỗi khi hủy đăng ký global hotkey hook.");
            System.err.println(ex.getMessage());
        }
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        // Không sử dụng
    }

    /**
     * (*** SỬA LỖI: LOGIC MỚI VỚI DEBOUNCE ***)
     * Xử lý sự kiện nhấn phím toàn hệ thống.
     */
    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        // (*** THÊM KIỂM TRA CỜ DEBOUNCE ***)
        if (processingHotkey) {
            // System.out.println("Debounce: Ignored key press while processing hotkey");
            return; // Bỏ qua nếu đang trong thời gian chờ debounce
        }

        // Lấy các modifier đang được nhấn TẠI THỜI ĐIỂM SỰ KIỆN NÀY
        int modifiers = e.getModifiers();

        // Kiểm tra xem cả Meta (Cmd/Win) và Shift có đang được nhấn không
        boolean metaPressed = (modifiers & NativeInputEvent.META_MASK) != 0;
        boolean shiftPressed = (modifiers & NativeInputEvent.SHIFT_MASK) != 0;

        // Kiểm tra tổ hợp phím
        if (metaPressed && shiftPressed) {
            Runnable action = null; // Hành động cần thực thi trên luồng JavaFX

            if (e.getKeyCode() == NativeKeyEvent.VC_N) {
                // Hotkey: Cmd + Shift + N
                System.out.println("Global Hotkey: Cmd+Shift+N (Next & Play) detected!");
                action = () -> {
                    if (itemGridViewModel != null) {
                        itemGridViewModel.selectAndPlayNextItem();
                    }
                };

            } else if (e.getKeyCode() == NativeKeyEvent.VC_P) {
                // Hotkey: Cmd + Shift + P
                System.out.println("Global Hotkey: Cmd+Shift+P (Prev & Play) detected!");
                action = () -> {
                    if (itemGridViewModel != null) {
                        itemGridViewModel.selectAndPlayPreviousItem();
                    }
                };
            }

            // Nếu có hành động cần thực thi
            if (action != null) {
                // (*** ĐẶT CỜ VÀ CHẠY TIMER DEBOUNCE ***)
                processingHotkey = true; // Đánh dấu bắt đầu xử lý
                hotkeyDebounceTimer.stop(); // Dừng timer cũ (nếu có)
                hotkeyDebounceTimer.playFromStart(); // Bắt đầu timer mới

                // Thực thi hành động trên luồng JavaFX
                Platform.runLater(action);
            }
        }
    }


    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        // Không cần làm gì ở đây nữa
    }
    // --- Kết thúc các hàm Hotkey Hệ Thống ---

    @FXML
    private void handleSearchAction() {
        String keywords = searchField.getText();
        if (keywords != null && !keywords.trim().isEmpty()) {
            itemGridViewModel.searchItemsByKeywords(keywords.trim());
        } else {
            viewModel.statusMessageProperty().set(I18nManager.getInstance().getString("mainView", "errorSearchKeywords"));
            itemGridViewModel.loadItemsByParentId(null);
        }
    }

    @FXML
    private void handleSortByToggle() {
        if (itemGridViewModel != null) {
            itemGridViewModel.toggleSortBy();
        }
    }

    @FXML
    private void handleSortOrderToggle() {
        if (itemGridViewModel != null) {
            itemGridViewModel.toggleSortOrder();
        }
    }
}