package com.example.embyapp.controller;

import embyclient.model.BaseItemDto;
import com.example.embyapp.service.I18nManager;
import com.example.embyapp.viewmodel.LibraryTreeViewModel;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

public class LibraryTreeController {

    @FXML private StackPane rootPane;
    @FXML private TreeView<BaseItemDto> treeView;

    @FXML private ProgressIndicator progressIndicator;

    private LibraryTreeViewModel viewModel;
    private I18nManager i18n;


    @FXML
    public void initialize() {
        this.i18n = I18nManager.getInstance();
    }

    public void setViewModel(LibraryTreeViewModel viewModel) {
        this.viewModel = viewModel;

        // BINDINGS
        if (progressIndicator != null) {
            progressIndicator.visibleProperty().bind(viewModel.loadingProperty());
        } else {
            System.err.println("LibraryTreeController: progressIndicator is null! Check FXML fx:id.");
        }

        treeView.rootProperty().bind(viewModel.rootItemProperty());
        treeView.setShowRoot(false); // Ẩn node gốc "Root"

        // 1. Tùy chỉnh CellFactory
        treeView.setCellFactory(tv -> new TreeCell<>() {

            private final ChangeListener<Boolean> expansionListener = (obs, wasExpanded, isNowExpanded) -> {
                if (isNowExpanded && getTreeItem() != null) {
                    viewModel.loadChildrenForItem(getTreeItem());
                }
            };

            private TreeItem<BaseItemDto> currentItem = null;

            private final ContextMenu contextMenu = new ContextMenu();
            private final MenuItem copyIdItem = new MenuItem();

            {
                copyIdItem.setText(i18n.getString("contextMenu", "copyId"));
                copyIdItem.setOnAction(e -> {
                    if (getItem() != null && getItem().getId() != null) {
                        final Clipboard clipboard = Clipboard.getSystemClipboard();
                        final ClipboardContent content = new ClipboardContent();
                        content.putString(getItem().getId());
                        clipboard.setContent(content);
                    }
                });
                contextMenu.getItems().add(copyIdItem);
            }


            @Override
            protected void updateItem(BaseItemDto item, boolean empty) {
                super.updateItem(item, empty);

                if (currentItem != null) {
                    currentItem.expandedProperty().removeListener(expansionListener);
                }

                setText(null);
                setGraphic(null);
                setOnMouseClicked(null);
                setContextMenu(null);

                if (empty || item == null) {
                    currentItem = null;
                } else {
                    currentItem = getTreeItem();

                    if (currentItem != null) {
                        currentItem.expandedProperty().addListener(expansionListener);
                    }

                    setText(item.getName());

                    setContextMenu(contextMenu);

                    setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2 && getTreeItem() != null && getTreeItem().getValue() != null) {
                            BaseItemDto clickedItem = getTreeItem().getValue();

                            if (clickedItem.isIsFolder() != null && clickedItem.isIsFolder()) {
                                viewModel.loadChildrenForItem(getTreeItem());
                            }
                        }
                    });
                }
            }
        });

        // 2. Lắng nghe sự kiện click (single-click)
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newItem) -> {
            viewModel.selectedTreeItemProperty().set(newItem);
        });
    }

    /**
     * Hàm này được MainController gọi
     */
    public void loadLibraries() {
        viewModel.loadLibraries();
    }
}