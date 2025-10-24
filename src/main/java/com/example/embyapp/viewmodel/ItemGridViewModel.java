package com.example.embyapp.viewmodel;

import com.example.emby.EmbyClient.ApiException;
import com.example.emby.modelEmby.BaseItemDto;
import com.example.emby.modelEmby.QueryResultBaseItemDto;
import com.example.embyapp.service.ItemRepository;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * (SỬA ĐỔI) ViewModel cho ItemGridView (Cột giữa).
 * Sửa lỗi: Sử dụng ReadOnly...Wrapper để expose ReadOnlyProperty.
 * (CẬP NHẬT MỚI) Thêm logic phân trang (Infinite Scrolling).
 */
public class ItemGridViewModel {

    // Hằng số cho phân trang (MỚI)
    private static final int ITEMS_PER_LOAD = 50;

    private final ItemRepository itemRepository;

    // --- Pagination State --- (MỚI)
    private String currentParentId;
    private int totalCount = 0;
    private int startIndex = 0;
    private final ReadOnlyBooleanWrapper hasMoreItems = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper isLoadingMore = new ReadOnlyBooleanWrapper(false); // Dùng để khóa load

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
     * Tải items ban đầu (chạy nền).
     * @param parentId ID của thư mục cha (hoặc null để xóa).
     */
    public void loadItemsByParentId(String parentId) {
        if (parentId == null) {
            Platform.runLater(() -> {
                items.clear();
                statusMessage.set("Vui lòng chọn một thư viện...");
                showStatusMessage.set(true);
                loading.set(false);
                selectedItem.set(null);
                // (MỚI) Reset pagination state
                currentParentId = null;
                totalCount = 0;
                startIndex = 0;
                hasMoreItems.set(false);
                isLoadingMore.set(false);
            });
            return;
        }

        if (parentId.equals(currentParentId) && !items.isEmpty() && !loading.get()) {
            return; // Đã load rồi và không cần load lại
        }

        loading.set(true);
        showStatusMessage.set(false); // Ẩn status khi bắt đầu load
        items.clear(); // Xóa items cũ
        selectedItem.set(null); // Xóa lựa chọn cũ

        // (MỚI) Khởi tạo lại trạng thái phân trang
        currentParentId = parentId;
        startIndex = 0;
        totalCount = 0;

        new Thread(() -> {
            try {
                // SỬA ĐỔI: Gọi API với pagination
                QueryResultBaseItemDto result = itemRepository.getFullByParentIdPaginated(currentParentId, startIndex, ITEMS_PER_LOAD);

                Platform.runLater(() -> {
                    // Cập nhật trạng thái
                    totalCount = result.getTotalRecordCount() != null ? result.getTotalRecordCount() : 0;
                    startIndex += result.getItems().size();
                    hasMoreItems.set(startIndex < totalCount);

                    items.setAll(result.getItems()); // Thay thế toàn bộ items

                    if (items.isEmpty()) {
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

    /**
     * (HÀM MỚI) Tải thêm items khi cuộn xuống cuối (chạy nền).
     */
    public void loadMoreItems() {
        if (isLoadingMore.get() || !hasMoreItems.get() || currentParentId == null) {
            return; // Đã load xong hoặc đang load, hoặc chưa chọn thư viện
        }

        isLoadingMore.set(true); // Khóa load

        new Thread(() -> {
            try {
                // SỬA ĐỔI: Gọi API với startIndex hiện tại và limit cố định
                QueryResultBaseItemDto result = itemRepository.getFullByParentIdPaginated(currentParentId, startIndex, ITEMS_PER_LOAD);

                Platform.runLater(() -> {
                    // Cập nhật trạng thái
                    int loadedCount = result.getItems().size();
                    startIndex += loadedCount;
                    hasMoreItems.set(startIndex < totalCount);

                    items.addAll(result.getItems()); // THÊM items vào danh sách hiện tại

                    // Cập nhật status message nhỏ
                    statusMessage.set("Đã tải " + startIndex + "/" + totalCount + " items.");

                    isLoadingMore.set(false); // Mở khóa
                });

            } catch (ApiException e) {
                System.err.println("API Error loading more grid items: " + e.getMessage());
                Platform.runLater(() -> {
                    statusMessage.set("Lỗi khi tải thêm items: " + e.getMessage());
                    isLoadingMore.set(false);
                });
            } catch (Exception e) {
                System.err.println("Generic Error loading more grid items: " + e.getMessage());
                Platform.runLater(() -> {
                    statusMessage.set("Lỗi không xác định khi tải thêm items.");
                    isLoadingMore.set(false);
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

    /** (MỚI) Có item cần load thêm không? */
    public ReadOnlyBooleanProperty hasMoreItemsProperty() {
        return hasMoreItems.getReadOnlyProperty();
    }

    /** (MỚI) Đang trong quá trình tải thêm items? */
    public ReadOnlyBooleanProperty isLoadingMoreProperty() {
        return isLoadingMore.getReadOnlyProperty();
    }
}