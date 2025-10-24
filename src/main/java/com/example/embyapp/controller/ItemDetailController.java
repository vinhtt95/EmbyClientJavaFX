package com.example.embyapp.controller;

import com.example.emby.modelEmby.BaseItemDto;
import com.example.embyapp.viewmodel.ItemDetailViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox; // Hoặc Pane/AnchorPane tùy FXML

/**
 * (SỬA ĐỔI) Controller cho cột phải (Item Detail).
 * Phiên bản này đã được TÁI CẤU TRÚC để sử dụng ItemDetailViewModel.
 * Controller này chỉ còn nhiệm vụ binding UI với ViewModel.
 */
public class ItemDetailController {

    @FXML private VBox detailPane; // Container chứa chi tiết
    @FXML private Label titleLabel;
    @FXML private Label yearLabel;
    @FXML private Label overviewLabel;
    @FXML private Label statusLabel; // Label "Vui lòng chọn item..."

    private ItemDetailViewModel viewModel;

    @FXML
    public void initialize() {
        // Không cần làm gì nhiều ở đây,
        // vì binding sẽ được thực hiện trong setViewModel
    }

    /**
     * Được gọi bởi MainController để inject ViewModel.
     * @param viewModel ViewModel cho view này.
     */
    public void setViewModel(ItemDetailViewModel viewModel) {
        this.viewModel = viewModel;

        // --- BINDING UI VỚI VIEWMODEL ---

        // 1. Binding các Label chi tiết
        titleLabel.textProperty().bind(viewModel.titleProperty());
        yearLabel.textProperty().bind(viewModel.yearProperty());
        overviewLabel.textProperty().bind(viewModel.overviewProperty());

        // 2. Binding Label trạng thái
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());
        statusLabel.visibleProperty().bind(viewModel.showStatusMessageProperty());

        // 3. Binding container (ẩn/hiện ngược lại với statusLabel)
        detailPane.visibleProperty().bind(viewModel.showStatusMessageProperty().not());
    }

    /**
     * Được gọi bởi MainController để ra lệnh hiển thị chi tiết.
     * Controller này chỉ cần chuyển tiếp item cho ViewModel.
     *
     * @param item Item được chọn từ Grid.
     */
    public void displayItemDetails(BaseItemDto item) {
        if (viewModel != null) {
            viewModel.setItemToDisplay(item);
        } else {
            System.err.println("ItemDetailController: ViewModel is null, cannot display data.");
        }
    }
}

