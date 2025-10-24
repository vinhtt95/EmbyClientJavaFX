package com.example.embyapp.viewmodel;

import com.example.emby.modelEmby.BaseItemDto;
import javafx.application.Platform;
import javafx.beans.property.*;

/**
 * (SỬA ĐỔI) ViewModel cho ItemDetailView (Cột phải).
 * Sửa lỗi: Sử dụng ReadOnly...Wrapper để expose ReadOnlyProperty.
 */
public class ItemDetailViewModel {

    // --- Properties (SỬA LỖI: Dùng Wrappers) ---
    private final ReadOnlyStringWrapper title = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper year = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper overview = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper("Vui lòng chọn một item từ danh sách...");
    private final ReadOnlyBooleanWrapper showStatusMessage = new ReadOnlyBooleanWrapper(true);


    public ItemDetailViewModel() {
        // Constructor rỗng
    }

    /**
     * Cập nhật ViewModel với item được chọn.
     * @param item Item được chọn (hoặc null để xóa).
     */
    public void setItemToDisplay(BaseItemDto item) {
        // Chạy trên JavaFX thread
        Platform.runLater(() -> {
            if (item == null) {
                // Xóa chi tiết, hiển thị status
                title.set("");
                year.set("");
                overview.set("");
                statusMessage.set("Vui lòng chọn một item từ danh sách...");
                showStatusMessage.set(true);
            } else {
                // Cập nhật chi tiết, ẩn status
                title.set(item.getName() != null ? item.getName() : "Không có tiêu đề");
                year.set(item.getProductionYear() != null ? String.valueOf(item.getProductionYear()) : "");
                overview.set(item.getOverview() != null ? item.getOverview() : "Không có mô tả.");
                showStatusMessage.set(false);
            }
        });
    }


    // --- Getters cho Properties (Dùng bởi Controller) ---

    // SỬA LỖI: Giờ hàm này đã hợp lệ
    public ReadOnlyStringProperty titleProperty() {
        return title.getReadOnlyProperty();
    }

    // SỬA LỖI: Giờ hàm này đã hợp lệ
    public ReadOnlyStringProperty yearProperty() {
        return year.getReadOnlyProperty();
    }

    // SỬA LỖI: Giờ hàm này đã hợp lệ
    public ReadOnlyStringProperty overviewProperty() {
        return overview.getReadOnlyProperty();
    }

    // SỬA LỖI: Giờ hàm này đã hợp lệ
    public ReadOnlyStringProperty statusMessageProperty() {
        return statusMessage.getReadOnlyProperty();
    }

    // SỬA LỖI: Giờ hàm này đã hợp lệ
    public ReadOnlyBooleanProperty showStatusMessageProperty() {
        return showStatusMessage.getReadOnlyProperty();
    }
}

