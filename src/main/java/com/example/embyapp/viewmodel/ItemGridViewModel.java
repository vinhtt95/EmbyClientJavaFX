package com.example.embyapp.viewmodel;

import embyclient.ApiException;
import embyclient.model.BaseItemDto;
import embyclient.model.QueryResultBaseItemDto;
import com.example.embyapp.service.ItemRepository;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * (SỬA ĐỔI) ViewModel cho ItemGridView (Cột giữa).
 * (CẬP NHẬT MỚI) Chuyển sang logic PHÂN TRANG THAY THẾ (Page Replacement) khi cuộn.
 */
public class ItemGridViewModel {

    // Hằng số cho phân trang
    private static final int ITEMS_PER_LOAD = 50;

    private final ItemRepository itemRepository;

    // --- Pagination State --- (SỬA ĐỔI)
    private String currentParentId;
    private int totalCount = 0;
    private int currentPageIndex = 0; // Trang hiện tại (bắt đầu từ 0)
    private int totalPages = 0; // Tổng số trang

    private final ReadOnlyBooleanWrapper hasNextPage = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper hasPreviousPage = new ReadOnlyBooleanWrapper(false);

    // Dùng để khóa load và báo hiệu trạng thái
    private final ReadOnlyBooleanWrapper loading = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper("Vui lòng chọn một thư viện...");
    private final ReadOnlyBooleanWrapper showStatusMessage = new ReadOnlyBooleanWrapper(true);

    // --- Properties (Không đổi) ---
    private final ObservableList<BaseItemDto> items = FXCollections.observableArrayList();
    private final ObjectProperty<BaseItemDto> selectedItem = new SimpleObjectProperty<>();

    // (MỚI) Dùng để báo hiệu cho Controller biết cần phải cuộn
    private final ObjectProperty<ScrollAction> scrollAction = new SimpleObjectProperty<>(ScrollAction.NONE);

    public ItemGridViewModel(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    /** Enum báo hiệu hành động cuộn cần thực hiện sau khi tải trang mới. */
    public enum ScrollAction {
        NONE, SCROLL_TO_TOP, SCROLL_TO_BOTTOM
    }

    // --- Core Loading Logic ---

    private void loadPage(int pageIndex, String parentId) {
        if (loading.get() || pageIndex < 0 || (totalPages > 0 && pageIndex >= totalPages)) {
            return;
        }

        loading.set(true);
        showStatusMessage.set(false);
        scrollAction.set(ScrollAction.NONE); // Reset scroll action

        int startIndex = pageIndex * ITEMS_PER_LOAD;

        // Cập nhật trạng thái tải
        Platform.runLater(() -> {
            statusMessage.set("Đang tải trang " + (pageIndex + 1) + "/" + (totalPages > 0 ? totalPages : "...") + "...");
        });


        new Thread(() -> {
            try {
                QueryResultBaseItemDto result = itemRepository.getFullByParentIdPaginated(parentId, startIndex, ITEMS_PER_LOAD);

                // Tính toán tổng số trang/item
                int calculatedTotalCount = result.getTotalRecordCount() != null ? result.getTotalRecordCount() : 0;
                // Tính toán tổng số trang
                int calculatedTotalPages = (int) Math.ceil((double) calculatedTotalCount / ITEMS_PER_LOAD);

                // Ghi nhận item hiện tại (vì đây là thay thế trang)
                List<BaseItemDto> pageItems = result.getItems();

                Platform.runLater(() -> {
                    // 1. Cập nhật Pagination State
                    currentParentId = parentId;
                    totalCount = calculatedTotalCount;
                    totalPages = calculatedTotalPages;
                    currentPageIndex = pageIndex;

                    hasNextPage.set(currentPageIndex < totalPages - 1);
                    hasPreviousPage.set(currentPageIndex > 0);

                    // 2. Cập nhật Items (Thay thế nội dung)
                    items.setAll(pageItems);

                    if (items.isEmpty() && totalCount > 0) {
                        // Trường hợp hiếm: API trả về trang rỗng giữa chừng, nên giữ trạng thái loading
                        statusMessage.set("Trang này rỗng.");
                        showStatusMessage.set(true);
                    } else if (items.isEmpty()) {
                        statusMessage.set("Thư viện này không có items.");
                        showStatusMessage.set(true);
                    } else {
                        // Cập nhật status
                        statusMessage.set("Đang hiển thị: " + (pageIndex + 1) + "/" + totalPages + " (" + totalCount + " items)");
                    }

                    // 3. Hoàn thành loading
                    loading.set(false);
                });

            } catch (ApiException e) {
                System.err.println("API Error loading page: " + e.getMessage());
                Platform.runLater(() -> {
                    statusMessage.set("Lỗi khi tải trang: " + e.getMessage());
                    showStatusMessage.set(true);
                    loading.set(false);
                });
            } catch (Exception e) {
                System.err.println("Generic Error loading page: " + e.getMessage());
                Platform.runLater(() -> {
                    statusMessage.set("Lỗi không xác định khi tải trang.");
                    showStatusMessage.set(true);
                    loading.set(false);
                });
            }
        }).start();
    }

    // --- Public Page Control ---

    /**
     * Tải items ban đầu (trang 1).
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

                // Reset pagination state
                currentParentId = null;
                totalCount = 0;
                currentPageIndex = 0;
                totalPages = 0;
                hasNextPage.set(false);
                hasPreviousPage.set(false);
            });
            return;
        }

        if (parentId.equals(currentParentId) && !items.isEmpty() && currentPageIndex == 0 && !loading.get()) {
            return; // Đã load trang đầu tiên
        }

        // Tải trang 0
        loadPage(0, parentId);
    }

    /**
     * Tải trang tiếp theo và yêu cầu cuộn lên đầu.
     */
    public void loadNextPage() {
        if (!hasNextPage.get() || loading.get()) return;

        int nextPage = currentPageIndex + 1;

        // SỬA LỖI: Khai báo listener là biến cục bộ để có thể remove
        ChangeListener<Boolean> cleanupListener = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> obs, Boolean oldVal, Boolean newVal) {
                if (!newVal) { // Nếu load xong
                    Platform.runLater(() -> scrollAction.set(ScrollAction.SCROLL_TO_TOP));
                    loading.removeListener(this); // Cleanup
                }
            }
        };

        // Tải trang
        loadPage(nextPage, currentParentId);

        // Yêu cầu Controller cuộn lên đầu sau khi tải
        loading.addListener(cleanupListener);
    }

    /**
     * Tải trang trước đó và yêu cầu cuộn xuống cuối.
     */
    public void loadPreviousPage() {
        if (!hasPreviousPage.get() || loading.get()) return;

        int previousPage = currentPageIndex - 1;

        // SỬA LỖI: Khai báo listener là biến cục bộ để có thể remove
        ChangeListener<Boolean> cleanupListener = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> obs, Boolean oldVal, Boolean newVal) {
                if (!newVal) { // Nếu load xong
                    Platform.runLater(() -> scrollAction.set(ScrollAction.SCROLL_TO_BOTTOM));
                    loading.removeListener(this); // Cleanup
                }
            }
        };

        // Tải trang
        loadPage(previousPage, currentParentId);

        // Yêu cầu Controller cuộn xuống cuối sau khi tải
        loading.addListener(cleanupListener);
    }

    // --- Getters cho Properties ---

    public ReadOnlyBooleanProperty loadingProperty() {
        return loading.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty statusMessageProperty() {
        return statusMessage.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty showStatusMessageProperty() {
        return showStatusMessage.getReadOnlyProperty();
    }

    public ObservableList<BaseItemDto> getItems() {
        return items;
    }

    public ObjectProperty<BaseItemDto> selectedItemProperty() {
        return selectedItem;
    }

    // (MỚI) Getters cho trạng thái trang
    public ReadOnlyBooleanProperty hasNextPageProperty() {
        return hasNextPage.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty hasPreviousPageProperty() {
        return hasPreviousPage.getReadOnlyProperty();
    }

    // (MỚI) Getter cho hành động cuộn
    public ObjectProperty<ScrollAction> scrollActionProperty() {
        return scrollAction;
    }

    /** (MỚI) Lấy chỉ số trang hiện tại (0-based) */
    public int getCurrentPageIndex() {
        return currentPageIndex;
    }

    /** (MỚI) Lấy tổng số trang */
    public int getTotalPages() {
        return totalPages;
    }
}