package com.example.embyapp.viewmodel;

import com.example.emby.EmbyClient.ApiException;
import com.example.emby.modelEmby.BaseItemDto;
import com.example.embyapp.service.ItemRepository;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * (SỬA ĐỔI) ViewModel cho ItemGridView (Cột giữa).
 * Sửa lỗi: Sử dụng ReadOnly...Wrapper để expose ReadOnlyProperty.
 */
public class ItemGridViewModel {

    private final ItemRepository itemRepository;

    // --- Properties (SỬA LỖI: Dùng Wrappers) ---
    private final ReadOnlyBooleanWrapper loading = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper("Vui lòng chọn một thư viện...");
    private final ReadOnlyBooleanWrapper showStatusMessage = new ReadOnlyBooleanWrapper(true);

    // --- Properties (Không đổi) ---
    private final ObservableList<BaseItemDto> items = FXCollections.observableArrayList();
    private final ObjectProperty<BaseItemDto> selectedItem = new SimpleObjectProperty<>();

    public ItemGridViewModel(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    /**
     * Tải items dựa trên parentId (chạy nền).
     * @param parentId ID của thư mục cha (hoặc null để xóa).
     */
    public void loadItemsByParentId(String parentId) {
        if (parentId == null) {
            Platform.runLater(() -> {
                items.clear();
                statusMessage.set("Vui lòng chọn một thư viện...");
                showStatusMessage.set(true);
                loading.set(false);
                selectedItem.set(null); // Xóa lựa chọn cũ
            });
            return;
        }

        loading.set(true);
        showStatusMessage.set(false); // Ẩn status khi bắt đầu load
        items.clear(); // Xóa items cũ
        selectedItem.set(null); // Xóa lựa chọn cũ

        new Thread(() -> {
            try {
                List<BaseItemDto> resultItems = itemRepository.getItemsByParentId(parentId);

                Platform.runLater(() -> {
                    items.setAll(resultItems);
                    if (resultItems.isEmpty()) {
                        statusMessage.set("Thư viện này không có items.");
                        showStatusMessage.set(true);
                    }
                    // else: showStatusMessage vẫn là false, Grid tự hiển thị
                    loading.set(false);
                });

            } catch (ApiException e) {
                System.err.println("API Error loading grid items: " + e.getMessage());
                Platform.runLater(() -> {
                    statusMessage.set("Lỗi khi tải items: " + e.getMessage());
                    showStatusMessage.set(true);
                    loading.set(false);
                });
            } catch (Exception e) {
                System.err.println("Generic Error loading grid items: " + e.getMessage());
                Platform.runLater(() -> {
                    statusMessage.set("Lỗi không xác định khi tải items.");
                    showStatusMessage.set(true);
                    loading.set(false);
                });
            }
        }).start();
    }


    // --- Getters cho Properties (Dùng bởi Controller) ---

    // SỬA LỖI: Giờ hàm này đã hợp lệ
    public ReadOnlyBooleanProperty loadingProperty() {
        return loading.getReadOnlyProperty();
    }

    // SỬA LỖI: Giờ hàm này đã hợp lệ
    public ReadOnlyStringProperty statusMessageProperty() {
        return statusMessage.getReadOnlyProperty();
    }

    // SỬA LỖI: Giờ hàm này đã hợp lệ
    public ReadOnlyBooleanProperty showStatusMessageProperty() {
        return showStatusMessage.getReadOnlyProperty();
    }

    public ObservableList<BaseItemDto> getItems() {
        return items;
    }

    public ObjectProperty<BaseItemDto> selectedItemProperty() {
        // Expose ra ngoài dạng ObjectProperty (cho phép Controller set giá trị)
        // Hoặc có thể dùng ReadOnlyObjectWrapper nếu chỉ muốn VM này set
        return selectedItem;
    }
}

