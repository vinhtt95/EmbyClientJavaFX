package com.example.embyapp.controller;

import com.example.emby.modelEmby.BaseItemDto;
import com.example.embyapp.viewmodel.LibraryTreeViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;

public class LibraryTreeController {

    @FXML private StackPane rootPane;
    @FXML private TreeView<BaseItemDto> treeView;

    // SỬA LỖI: Đổi tên biến "loadingIndicator" thành "progressIndicator"
    // để khớp với fx:id="progressIndicator" trong LibraryTreeView.fxml
    @FXML private ProgressIndicator progressIndicator;

    private LibraryTreeViewModel viewModel;


    @FXML
    public void initialize() {
        // (Khởi tạo sau, trong setViewModel)
    }

    public void setViewModel(LibraryTreeViewModel viewModel) {
        this.viewModel = viewModel;

        // BINDINGS
        // SỬA LỖI: Dùng biến đã sửa tên
        if (progressIndicator != null) {
            progressIndicator.visibleProperty().bind(viewModel.loadingProperty());
        } else {
            System.err.println("LibraryTreeController: progressIndicator is null! Check FXML fx:id.");
        }

        treeView.rootProperty().bind(viewModel.rootItemProperty());
        treeView.setShowRoot(false); // Ẩn node gốc "Root"

        // 1. Tùy chỉnh cách hiển thị Tên
        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(BaseItemDto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName()); // Hiển thị tên
                }
            }
        });

        // 2. Lắng nghe sự kiện click
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newItem) -> {
            // SỬA LỖI: Gọi .set() trên property,
            // thay vì gọi hàm setter không tồn tại
            viewModel.selectedTreeItemProperty().set(newItem);
        });
    }

    // Hàm này được MainController gọi
    public void loadLibraries() {
        viewModel.loadLibraries();
    }
}

