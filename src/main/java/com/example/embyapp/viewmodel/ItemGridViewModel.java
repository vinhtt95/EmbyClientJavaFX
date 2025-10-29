package com.example.embyapp.viewmodel;

import embyclient.ApiException;
import embyclient.model.BaseItemDto;
import embyclient.model.QueryResultBaseItemDto;
import com.example.embyapp.service.I18nManager;
import com.example.embyapp.service.ItemRepository;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.lang.Math; // Explicit import for Math.ceil

/**
 * ViewModel cho ItemGridView (Cột giữa).
 * Chuyển sang logic PHÂN TRANG THAY THẾ (Page Replacement) khi cuộn.
 * Thêm logic cho tìm kiếm và phân trang kết quả tìm kiếm.
 * Thêm logic cho tùy chọn sắp xếp.
 * Thêm logic chọn item trước/sau.
 * Thêm logic cho hotkey hệ thống (playAfterSelect).
 * (CẬP NHẬT) Thêm sort "DateCreated" và logic "Home" (parentId = null).
 */
public class ItemGridViewModel {

    private static final int ITEMS_PER_LOAD = 50;

    public static final String SORT_BY_DATE_RELEASE = "ProductionYear,PremiereDate,SortName";
    public static final String SORT_BY_NAME = "SortName";
    public static final String SORT_BY_DATE_CREATED = "DateCreated";
    public static final String SORT_ORDER_DESCENDING = "Descending";
    public static final String SORT_ORDER_ASCENDING = "Ascending";

    private final ItemRepository itemRepository;

    private String currentParentId; // Sẽ là NULL nếu đang ở "Home"
    private int totalCount = 0;
    private int currentPageIndex = 0;
    private int totalPages = 0;

    private final ReadOnlyBooleanWrapper hasNextPage = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper hasPreviousPage = new ReadOnlyBooleanWrapper(false);

    private String currentSearchKeywords;
    private boolean isSearching = false;

    private final ReadOnlyStringWrapper currentSortBy = new ReadOnlyStringWrapper(SORT_BY_DATE_CREATED);
    private final ReadOnlyStringWrapper currentSortOrder = new ReadOnlyStringWrapper(SORT_ORDER_DESCENDING);

    private final ReadOnlyBooleanWrapper loading = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper showStatusMessage = new ReadOnlyBooleanWrapper(true);

    private final ObservableList<BaseItemDto> items = FXCollections.observableArrayList();
    private final ObjectProperty<BaseItemDto> selectedItem = new SimpleObjectProperty<>();

    private final ObjectProperty<ScrollAction> scrollAction = new SimpleObjectProperty<>(ScrollAction.NONE);

    private final BooleanProperty playAfterSelect = new SimpleBooleanProperty(false);

    public ItemGridViewModel(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
        this.statusMessage.set(I18nManager.getInstance().getString("itemGridView", "statusDefault"));
    }

    /** Enum báo hiệu hành động cuộn cần thực hiện sau khi tải trang mới. */
    public enum ScrollAction {
        NONE, SCROLL_TO_TOP, SCROLL_TO_BOTTOM
    }

    private void loadPage(int pageIndex, String parentId) {
        if (loading.get() || pageIndex < 0 || (totalPages > 0 && pageIndex >= totalPages)) {
            return;
        }

        loading.set(true);
        showStatusMessage.set(false);
        scrollAction.set(ScrollAction.NONE);

        if (!isSearching) {
            currentSearchKeywords = null;
        }

        int startIndex = pageIndex * ITEMS_PER_LOAD;
        I18nManager i18n = I18nManager.getInstance();

        final String sortBy = currentSortBy.get();
        final String sortOrder = currentSortOrder.get();

        Platform.runLater(() -> {
            statusMessage.set(i18n.getString("itemGridView", "statusPageLoading", (pageIndex + 1), (totalPages > 0 ? totalPages : "...")));
        });

        new Thread(() -> {
            try {
                QueryResultBaseItemDto result = itemRepository.getFullByParentIdPaginated(
                        parentId, startIndex, ITEMS_PER_LOAD, sortOrder, sortBy
                );

                int calculatedTotalCount = result.getTotalRecordCount() != null ? result.getTotalRecordCount() : 0;
                int calculatedTotalPages = (int) Math.ceil((double) calculatedTotalCount / ITEMS_PER_LOAD);
                List<BaseItemDto> pageItems = result.getItems();

                BaseItemDto itemToSelect = null;
                if (!pageItems.isEmpty()) {
                    if (pageIndex == 0) {
                        itemToSelect = pageItems.get(0);
                    } else if (pageIndex < currentPageIndex) {
                        itemToSelect = pageItems.get(pageItems.size() - 1);
                    } else if (pageIndex > currentPageIndex) {
                        itemToSelect = pageItems.get(0);
                    }
                }
                final BaseItemDto finalItemToSelect = itemToSelect;

                Platform.runLater(() -> {
                    currentParentId = parentId;
                    totalCount = calculatedTotalCount;
                    totalPages = calculatedTotalPages;
                    currentPageIndex = pageIndex;
                    isSearching = false;

                    hasNextPage.set(currentPageIndex < totalPages - 1);
                    hasPreviousPage.set(currentPageIndex > 0);

                    items.setAll(pageItems);

                    I18nManager i18n_later = I18nManager.getInstance();
                    if (items.isEmpty() && totalCount > 0) {
                        statusMessage.set(i18n_later.getString("itemGridView", "statusPageEmpty"));
                        showStatusMessage.set(true);
                    } else if (items.isEmpty() && parentId == null) {
                        statusMessage.set(i18n_later.getString("itemGridView", "statusAllItemsEmpty"));
                        showStatusMessage.set(true);
                    } else if (items.isEmpty()) {
                        statusMessage.set(i18n_later.getString("itemGridView", "statusLibraryEmpty"));
                        showStatusMessage.set(true);
                    } else {
                        if (parentId == null) {
                            statusMessage.set(i18n_later.getString("itemGridView", "statusDisplayingAll", (pageIndex + 1), totalPages, totalCount));
                        } else {
                            statusMessage.set(i18n_later.getString("itemGridView", "statusDisplaying", (pageIndex + 1), totalPages, totalCount));
                        }

                        if (finalItemToSelect != null) {
                            selectedItem.set(finalItemToSelect);
                        }
                    }

                    loading.set(false);
                });

            } catch (ApiException e) {
                System.err.println("API Error loading page: " + e.getMessage());
                Platform.runLater(() -> {
                    statusMessage.set(I18nManager.getInstance().getString("itemGridView", "errorLoadingPage", e.getMessage()));
                    showStatusMessage.set(true);
                    loading.set(false);
                });
            } catch (Exception e) {
                System.err.println("Generic Error loading page: " + e.getMessage());
                Platform.runLater(() -> {
                    statusMessage.set(I18nManager.getInstance().getString("itemGridView", "errorLoadingPageGeneric"));
                    showStatusMessage.set(true);
                    loading.set(false);
                });
            }
        }).start();
    }

    /**
     * Phương thức tải trang cho kết quả tìm kiếm.
     */
    private void loadSearchPage(int pageIndex, String keywords) {
        if (loading.get() || pageIndex < 0 || (totalPages > 0 && pageIndex >= totalPages)) {
            return;
        }

        loading.set(true);
        showStatusMessage.set(false);
        scrollAction.set(ScrollAction.NONE);

        int startIndex = pageIndex * ITEMS_PER_LOAD;
        I18nManager i18n = I18nManager.getInstance();

        Platform.runLater(() -> {
            statusMessage.set(i18n.getString("itemGridView", "statusSearchPageLoading", (pageIndex + 1), (totalPages > 0 ? totalPages : "...")));
        });

        new Thread(() -> {
            try {
                QueryResultBaseItemDto result = itemRepository.searchItemsPaginated(
                        keywords, startIndex, ITEMS_PER_LOAD, SORT_ORDER_ASCENDING, SORT_BY_NAME
                );

                int calculatedTotalCount = result.getTotalRecordCount() != null ? result.getTotalRecordCount() : 0;
                int calculatedTotalPages = (int) Math.ceil((double) calculatedTotalCount / ITEMS_PER_LOAD);
                List<BaseItemDto> pageItems = result.getItems();

                BaseItemDto itemToSelect = null;
                if (!pageItems.isEmpty()) {
                    if (pageIndex == 0) {
                        itemToSelect = pageItems.get(0);
                    } else if (pageIndex < currentPageIndex) {
                        itemToSelect = pageItems.get(pageItems.size() - 1);
                    } else if (pageIndex > currentPageIndex) {
                        itemToSelect = pageItems.get(0);
                    }
                }
                final BaseItemDto finalItemToSelect = itemToSelect;

                Platform.runLater(() -> {
                    currentSearchKeywords = keywords;
                    currentParentId = null;
                    totalCount = calculatedTotalCount;
                    totalPages = calculatedTotalPages;
                    currentPageIndex = pageIndex;
                    isSearching = true;

                    hasNextPage.set(currentPageIndex < totalPages - 1);
                    hasPreviousPage.set(currentPageIndex > 0);

                    items.setAll(pageItems);

                    if (items.isEmpty() && totalCount > 0) {
                        statusMessage.set(i18n.getString("itemGridView", "statusPageEmpty"));
                        showStatusMessage.set(true);
                    } else if (items.isEmpty()) {
                        statusMessage.set(i18n.getString("itemGridView", "statusSearchEmpty", keywords));
                        showStatusMessage.set(true);
                    } else {
                        statusMessage.set(i18n.getString("itemGridView", "statusSearchResult", (pageIndex + 1), totalPages, totalCount));
                        if (finalItemToSelect != null) {
                            selectedItem.set(finalItemToSelect);
                        } else if (!pageItems.isEmpty()) {
                            selectedItem.set(pageItems.get(0));
                        }
                    }

                    loading.set(false);
                });

            } catch (ApiException e) {
                System.err.println("API Error loading search page: " + e.getMessage());
                Platform.runLater(() -> {
                    statusMessage.set(i18n.getString("itemGridView", "errorLoadingSearch", e.getMessage()));
                    showStatusMessage.set(true);
                    loading.set(false);
                });
            } catch (Exception e) {
                System.err.println("Generic Error loading search page: " + e.getMessage());
                Platform.runLater(() -> {
                    statusMessage.set(i18n.getString("itemGridView", "errorLoadingSearchGeneric"));
                    showStatusMessage.set(true);
                    loading.set(false);
                });
            }
        }).start();
    }

    /**
     * Tải items ban đầu (trang 1).
     * @param parentId ID của thư mục cha (hoặc null để tải "Home").
     */
    public void loadItemsByParentId(String parentId) {
        if (parentId == null && currentParentId == null && !items.isEmpty() && !isSearching) {
            // Đang ở "Home" và nhấn "Home" lần nữa -> không làm gì
        }

        if (isSearching) {
            isSearching = false;
            currentSearchKeywords = null;
        }

        loadPage(0, parentId);
    }

    /**
     * Bắt đầu tìm kiếm items theo từ khóa.
     */
    public void searchItemsByKeywords(String keywords) {
        if (keywords == null || keywords.trim().isEmpty()) {
            loadItemsByParentId(null);
            return;
        }
        loadSearchPage(0, keywords);
    }

    /**
     * Tải trang tiếp theo và yêu cầu cuộn lên đầu.
     */
    public void loadNextPage() {
        if (!hasNextPage.get() || loading.get()) return;

        int nextPage = currentPageIndex + 1;

        // *** SỬA LỖI BIÊN DỊCH: Định nghĩa đầy đủ lambda ***
        ChangeListener<Boolean> cleanupListener = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> obs, Boolean oldVal, Boolean newVal) {
                if (!newVal) { // Nếu load xong
                    Platform.runLater(() -> scrollAction.set(ScrollAction.SCROLL_TO_TOP));
                    loading.removeListener(this); // Cleanup
                }
            }
        };

        if (isSearching) {
            loadSearchPage(nextPage, currentSearchKeywords);
        } else {
            loadPage(nextPage, currentParentId);
        }

        loading.addListener(cleanupListener);
    }

    /**
     * Tải trang trước đó và yêu cầu cuộn xuống cuối.
     */
    public void loadPreviousPage() {
        if (!hasPreviousPage.get() || loading.get()) return;

        int previousPage = currentPageIndex - 1;

        // *** SỬA LỖI BIÊN DỊCH: Định nghĩa đầy đủ lambda ***
        ChangeListener<Boolean> cleanupListener = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> obs, Boolean oldVal, Boolean newVal) {
                if (!newVal) { // Nếu load xong
                    Platform.runLater(() -> scrollAction.set(ScrollAction.SCROLL_TO_BOTTOM));
                    loading.removeListener(this); // Cleanup
                }
            }
        };

        if (isSearching) {
            loadSearchPage(previousPage, currentSearchKeywords);
        } else {
            loadPage(previousPage, currentParentId);
        }

        loading.addListener(cleanupListener);
    }


    /**
     * Chuyển đổi tiêu chí sắp xếp và tải lại trang 0.
     */
    public void toggleSortBy() {
        if (isSearching || loading.get()) return;

        String current = currentSortBy.get();
        if (current.equals(SORT_BY_DATE_RELEASE)) {
            currentSortBy.set(SORT_BY_NAME);
        } else if (current.equals(SORT_BY_NAME)) {
            currentSortBy.set(SORT_BY_DATE_CREATED);
        } else {
            currentSortBy.set(SORT_BY_DATE_RELEASE);
        }

        loadPage(0, currentParentId);
    }

    /**
     * Chuyển đổi thứ tự sắp xếp (Tăng dần <-> Giảm dần) và tải lại trang 0.
     */
    public void toggleSortOrder() {
        if (isSearching || loading.get()) return;

        if (currentSortOrder.get().equals(SORT_ORDER_DESCENDING)) {
            currentSortOrder.set(SORT_ORDER_ASCENDING);
        } else {
            currentSortOrder.set(SORT_ORDER_DESCENDING);
        }

        loadPage(0, currentParentId);
    }

    /**
     * Chọn item tiếp theo trong danh sách hiện tại.
     * Nếu là item cuối cùng của trang, tự động chuyển trang tiếp theo.
     */
    public void selectNextItem() {
        if (items.isEmpty() || loading.get()) return;
        BaseItemDto current = selectedItem.get();
        int currentIndex = items.indexOf(current);

        if (currentIndex != -1 && currentIndex < items.size() - 1) {
            selectedItem.set(items.get(currentIndex + 1));
        } else if (currentIndex == items.size() - 1 && hasNextPage.get()) {
            loadNextPage();
        }
    }

    /**
     * Chọn item liền trước trong danh sách hiện tại.
     * Nếu là item đầu tiên của trang, tự động chuyển về trang trước đó.
     */
    public void selectPreviousItem() {
        if (items.isEmpty() || loading.get()) return;
        BaseItemDto current = selectedItem.get();
        int currentIndex = items.indexOf(current);

        if (currentIndex > 0) {
            selectedItem.set(items.get(currentIndex - 1));
        } else if (currentIndex == 0 && hasPreviousPage.get()) {
            loadPreviousPage();
        }
    }

    /**
     * Chọn item tiếp theo và đặt cờ để phát tự động.
     */
    public void selectAndPlayNextItem() {
        playAfterSelect.set(true);
        selectNextItem();
    }

    /**
     * Chọn item trước đó và đặt cờ để phát tự động.
     */
    public void selectAndPlayPreviousItem() {
        playAfterSelect.set(true);
        selectPreviousItem();
    }

    /**
     * Kiểm tra cờ phát tự động.
     */
    public boolean isPlayAfterSelect() {
        return playAfterSelect.get();
    }

    /**
     * Xóa cờ phát tự động.
     */
    public void clearPlayAfterSelect() {
        playAfterSelect.set(false);
    }

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

    public ReadOnlyBooleanProperty hasNextPageProperty() {
        return hasNextPage.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty hasPreviousPageProperty() {
        return hasPreviousPage.getReadOnlyProperty();
    }

    public ObjectProperty<ScrollAction> scrollActionProperty() {
        return scrollAction;
    }

    /** Lấy chỉ số trang hiện tại (0-based) */
    public int getCurrentPageIndex() {
        return currentPageIndex;
    }

    /** Lấy tổng số trang */
    public int getTotalPages() {
        return totalPages;
    }

    public ReadOnlyStringProperty currentSortByProperty() {
        return currentSortBy.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty currentSortOrderProperty() {
        return currentSortOrder.getReadOnlyProperty();
    }

    public boolean isSearching() {
        return isSearching;
    }
}