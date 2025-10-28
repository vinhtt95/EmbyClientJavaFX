package com.example.embyapp.controller;

import embyclient.model.BaseItemDto;
import com.example.embyapp.MainApp;
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.I18nManager;
import com.example.embyapp.service.ItemRepository;
import com.example.embyapp.service.UserRepository;
import com.example.embyapp.viewmodel.*;
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

import java.io.IOException;
import java.net.URL;
import java.util.prefs.Preferences;

/**
 * Controller Điều Phối (Coordinator) cho MainView.
 */
public class MainController {

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
     * Helper
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

        itemGridViewModel.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            itemDetailViewModel.setItemToDisplay(newVal);
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
     * Helper
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
                detailDialog.setScene(scene);

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