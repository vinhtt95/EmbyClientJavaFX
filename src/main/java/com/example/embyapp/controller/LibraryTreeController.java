package com.example.embyapp.controller;

import com.example.emby.modelEmby.BaseItemDto;
import com.example.embyapp.viewmodel.LibraryTreeViewModel;
import javafx.beans.value.ChangeListener; // Thêm import
import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

public class LibraryTreeController {

    @FXML private StackPane rootPane;
    @FXML private TreeView<BaseItemDto> treeView;

    @FXML private ProgressIndicator progressIndicator;

    private LibraryTreeViewModel viewModel;


    @FXML
    public void initialize() {
        // (Khởi tạo sau, trong setViewModel)
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

        // 1. Tùy chỉnh CellFactory (SỬA ĐỔI LỚN)
        treeView.setCellFactory(tv -> new TreeCell<>() {

            // (MỚI) Listener for expansion (arrow click)
            private final ChangeListener<Boolean> expansionListener = (obs, wasExpanded, isNowExpanded) -> {
                if (isNowExpanded && getTreeItem() != null) {
                    // Chỉ cần gọi VM.
                    // VM (LibraryTreeViewModel) sẽ tự kiểm tra xem children đã được load chưa.
                    viewModel.loadChildrenForItem(getTreeItem());
                }
            };

            // (MỚI) Keep track of the item to remove the listener on reuse
            private TreeItem<BaseItemDto> currentItem = null;

            @Override
            protected void updateItem(BaseItemDto item, boolean empty) {
                super.updateItem(item, empty);

                // --- 1. Remove old listener (QUAN TRỌNG) ---
                // Xóa listener khỏi item cũ mà cell này đã hiển thị
                if (currentItem != null) {
                    currentItem.expandedProperty().removeListener(expansionListener);
                }

                // --- 2. Clear old graphics/text ---
                setText(null);
                setGraphic(null);
                setOnMouseClicked(null); // Xóa listener double-click cũ

                // --- 3. Set new item and add listeners ---
                if (empty || item == null) {
                    currentItem = null;
                } else {
                    // Đây là item mới
                    currentItem = getTreeItem();

                    // (MỚI) Thêm listener cho "arrow click" (expansion)
                    if (currentItem != null) {
                        currentItem.expandedProperty().addListener(expansionListener);
                    }

                    // Set text
                    setText(item.getName());

                    // (GIỮ NGUYÊN) Thêm listener cho "double-click" (item click)
                    setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2 && getTreeItem() != null && getTreeItem().getValue() != null) {
                            BaseItemDto clickedItem = getTreeItem().getValue();

                            // (SỬA ĐỔI) Dùng isIsFolder() như bạn đã xác nhận
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
            // (Không thay đổi)
            viewModel.selectedTreeItemProperty().set(newItem);
        });
    }

    // Hàm này được MainController gọi
    public void loadLibraries() {
        viewModel.loadLibraries();
    }
}
