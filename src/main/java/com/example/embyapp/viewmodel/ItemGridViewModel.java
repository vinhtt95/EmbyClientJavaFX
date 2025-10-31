package com.example.embyapp.viewmodel;

import embyclient.ApiException;
import embyclient.model.BaseItemDto;
import embyclient.model.QueryResultBaseItemDto;
import com.example.embyapp.service.I18nManager;
import com.example.embyapp.service.ItemRepository;
// (*** THÊM CÁC IMPORT MỚI ***)
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.viewmodel.detail.TagModel;
import java.util.Collections;
import java.util.Optional;
import java.util.Stack;
// (*** KẾT THÚC THÊM IMPORT ***)
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
 * ...
 * (CẬP NHẬT) Thêm logic điều hướng "Back" (backward) và "Forward".
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

    // (*** THÊM CÁC TRƯỜNG MỚI CHO NAVIGATION ***)
    private final Stack<GridNavigationState> navigationHistory = new Stack<>();
    private final Stack<GridNavigationState> forwardHistory = new Stack<>(); // <-- MỚI
    private final ReadOnlyBooleanWrapper canGoBack = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper canGoForward = new ReadOnlyBooleanWrapper(false); // <-- MỚI
    private boolean isRestoringState = false; // Cờ để ngăn push lịch sử khi đang back/forward

    // (*** THÊM CÁC TRƯỜNG LƯU TRẠNG THÁI HIỆN TẠI ***)
    private GridNavigationState.StateType currentStateType = GridNavigationState.StateType.FOLDER;
    private TagModel currentChipModel = null;
    private String currentChipType = null;
    // (*** KẾT THÚC THÊM MỚI ***)


    public ItemGridViewModel(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
        this.statusMessage.set(I18nManager.getInstance().getString("itemGridView", "statusDefault"));
    }

    /** Enum báo hiệu hành động cuộn cần thực hiện sau khi tải trang mới. */
    public enum ScrollAction {
        NONE, SCROLL_TO_TOP, SCROLL_TO_BOTTOM
    }

    private void loadPageInternal(int pageIndex, String parentId, String itemIdToSelect) {
        if (loading.get() || pageIndex < 0 || (totalPages > 0 && pageIndex >= totalPages)) {
            // Nếu đang restore mà trang không hợp lệ, thử về trang 0
            if (isRestoringState && pageIndex != 0) {
                loadPageInternal(0, parentId, itemIdToSelect);
            }
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
                if (itemIdToSelect != null) {
                    // 1. Thử tìm item từ state
                    Optional<BaseItemDto> itemToReselect = pageItems.stream()
                            .filter(i -> i.getId() != null && i.getId().equals(itemIdToSelect))
                            .findFirst();
                    if (itemToReselect.isPresent()) {
                        itemToSelect = itemToReselect.get();
                    }
                }

                if (itemToSelect == null && !pageItems.isEmpty()) {
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

                    // (*** SỬA ĐỔI: Cập nhật trạng thái ***)
                    if (!isRestoringState) {
                        currentStateType = GridNavigationState.StateType.FOLDER;
                        currentChipModel = null;
                        currentChipType = null;
                    }


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

    private void loadPage(int pageIndex, String parentId) {
        pushCurrentStateToHistory();
        loadPageInternal(pageIndex, parentId, null);
    }


    private void loadSearchPageInternal(int pageIndex, String keywords, String itemIdToSelect) {
        if (loading.get() || pageIndex < 0 || (totalPages > 0 && pageIndex >= totalPages)) {
            if (isRestoringState && pageIndex != 0) {
                loadSearchPageInternal(0, keywords, itemIdToSelect);
            }
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
                if (itemIdToSelect != null) {
                    Optional<BaseItemDto> itemToReselect = pageItems.stream()
                            .filter(i -> i.getId() != null && i.getId().equals(itemIdToSelect))
                            .findFirst();
                    if (itemToReselect.isPresent()) {
                        itemToSelect = itemToReselect.get();
                    }
                }

                if (itemToSelect == null && !pageItems.isEmpty()) {
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

                    // (*** SỬA ĐỔI: Cập nhật trạng thái ***)
                    if (!isRestoringState) {
                        currentStateType = GridNavigationState.StateType.SEARCH;
                        currentChipModel = null;
                        currentChipType = null;
                    }

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
                    statusMessage.set(I18nManager.getInstance().getString("itemGridView", "errorLoadingSearch", e.getMessage()));
                    showStatusMessage.set(true);
                    loading.set(false);
                });
            } catch (Exception e) {
                System.err.println("Generic Error loading search page: " + e.getMessage());
                Platform.runLater(() -> {
                    statusMessage.set(I18nManager.getInstance().getString("itemGridView", "errorLoadingSearchGeneric"));
                    showStatusMessage.set(true);
                    loading.set(false);
                });
            }
        }).start();
    }

    private void loadSearchPage(int pageIndex, String keywords) {
        pushCurrentStateToHistory();
        loadSearchPageInternal(pageIndex, keywords, null);
    }

    private void loadItemsByTagChipInternal(TagModel model, String type, EmbyService embyService, String itemIdToSelect) {
        if (loading.get()) return;

        loading.set(true);
        isSearching = true; // Coi như đây là một dạng "search"
        currentParentId = null; // Xóa parent ID
        currentSearchKeywords = model.getDisplayName(); // Dùng tên hiển thị cho status
        I18nManager i18n = I18nManager.getInstance();

        Platform.runLater(() -> {
            statusMessage.set(i18n.getString("itemGridView", "statusSearchPageLoading", 1, "..."));
            showStatusMessage.set(false); // Ẩn status, hiện loading
            scrollAction.set(ScrollAction.SCROLL_TO_TOP); // Cuộn lên đầu
        });

        new Thread(() -> {
            List<BaseItemDto> resultItems = null;
            try {
                String apiParam;
                switch (type) {
                    case "TAG":
                        apiParam = model.serialize(); // Tag dùng tên đã serialize
                        resultItems = embyService.getListItemByTagId(apiParam, 0, 500, true);
                        break;
                    case "STUDIO":
                        apiParam = model.getId(); // Studio dùng ID
                        if (apiParam == null) throw new ApiException("Studio ID is null for: " + model.getDisplayName());
                        resultItems = embyService.getListItemByStudioId(apiParam, 0, 500, true);
                        break;
                    case "PEOPLE":
                        apiParam = model.getId(); // People dùng ID
                        if (apiParam == null) throw new ApiException("People ID is null for: " + model.getDisplayName());
                        resultItems = embyService.getListPeopleByID(apiParam, 0, 500, true);
                        break;
                    case "GENRE":
                        apiParam = model.getDisplayName(); // Genre dùng tên hiển thị
                        resultItems = embyService.getListItemByGenreId(apiParam, 0, 500, true);
                        break;
                    default:
                        resultItems = Collections.emptyList();
                }
            } catch (Exception e) {
                System.err.println("API Error loading by tag chip (" + type + "): " + e.getMessage());
                final String errorMsg = e.getMessage();
                Platform.runLater(() -> {
                    statusMessage.set(I18nManager.getInstance().getString("itemGridView", "errorLoadingSearch", errorMsg));
                    showStatusMessage.set(true);
                    loading.set(false);
                });
                resultItems = Collections.emptyList();
            }

            final List<BaseItemDto> finalItems = resultItems != null ? resultItems : Collections.emptyList();

            Platform.runLater(() -> {
                items.setAll(finalItems); // Cập nhật danh sách
                totalCount = finalItems.size();
                totalPages = totalCount > 0 ? 1 : 0; // Chỉ 1 trang
                currentPageIndex = 0;
                hasNextPage.set(false);
                hasPreviousPage.set(false);

                // (*** SỬA ĐỔI: Cập nhật trạng thái ***)
                if (!isRestoringState) {
                    currentStateType = GridNavigationState.StateType.CHIP;
                    currentChipModel = model;
                    currentChipType = type;
                }

                if (finalItems.isEmpty()) {
                    statusMessage.set(i18n.getString("itemGridView", "statusSearchEmpty", currentSearchKeywords));
                    showStatusMessage.set(true);
                } else {
                    statusMessage.set(i18n.getString("itemGridView", "statusSearchResult", 1, 1, totalCount));
                    showStatusMessage.set(false); // Ẩn status, hiện grid
                }

                if (itemIdToSelect != null) {
                    Optional<BaseItemDto> itemToReselect = finalItems.stream()
                            .filter(i -> i.getId() != null && i.getId().equals(itemIdToSelect))
                            .findFirst();
                    // Vẫn giữ logic KHÔNG auto-select khi restore state chip
                }

                loading.set(false);
            });
        }).start();
    }

    public void loadItemsByTagChip(TagModel model, String type, EmbyService embyService) {
        pushCurrentStateToHistory();
        loadItemsByTagChipInternal(model, type, embyService, null);
    }


    public void loadItemsByParentId(String parentId) {
        if (parentId == null && currentParentId == null && !items.isEmpty() && !isSearching) {
            return;
        }

        if (isSearching) {
            isSearching = false;
            currentSearchKeywords = null;
        }

        loadPage(0, parentId);
    }

    public void searchItemsByKeywords(String keywords) {
        if (keywords == null || keywords.trim().isEmpty()) {
            loadItemsByParentId(null);
            return;
        }
        loadSearchPage(0, keywords);
    }

    public void loadNextPage() {
        if (!hasNextPage.get() || loading.get()) return;

        int nextPage = currentPageIndex + 1;

        ChangeListener<Boolean> cleanupListener = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> obs, Boolean oldVal, Boolean newVal) {
                if (!newVal) { // Nếu load xong
                    Platform.runLater(() -> scrollAction.set(ScrollAction.SCROLL_TO_TOP));
                    loading.removeListener(this); // Sửa lỗi biên dịch
                }
            }
        };

        if (isSearching) {
            loadSearchPageInternal(nextPage, currentSearchKeywords, null);
        } else {
            loadPageInternal(nextPage, currentParentId, null);
        }

        loading.addListener(cleanupListener);
    }

    public void loadPreviousPage() {
        if (!hasPreviousPage.get() || loading.get()) return;

        int previousPage = currentPageIndex - 1;

        ChangeListener<Boolean> cleanupListener = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> obs, Boolean oldVal, Boolean newVal) {
                if (!newVal) { // Nếu load xong
                    Platform.runLater(() -> scrollAction.set(ScrollAction.SCROLL_TO_BOTTOM));
                    loading.removeListener(this); // Sửa lỗi biên dịch
                }
            }
        };

        if (isSearching) {
            loadSearchPageInternal(previousPage, currentSearchKeywords, null);
        } else {
            loadPageInternal(previousPage, currentParentId, null);
        }

        loading.addListener(cleanupListener);
    }


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

    public void toggleSortOrder() {
        if (isSearching || loading.get()) return;

        if (currentSortOrder.get().equals(SORT_ORDER_DESCENDING)) {
            currentSortOrder.set(SORT_ORDER_ASCENDING);
        } else {
            currentSortOrder.set(SORT_ORDER_DESCENDING);
        }

        loadPage(0, currentParentId);
    }

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

    public void selectAndPlayNextItem() {
        playAfterSelect.set(true);
        selectNextItem();
    }

    public void selectAndPlayPreviousItem() {
        playAfterSelect.set(true);
        selectPreviousItem();
    }

    public boolean isPlayAfterSelect() {
        return playAfterSelect.get();
    }

    public void clearPlayAfterSelect() {
        playAfterSelect.set(false);
    }

    // (*** THÊM VÀ SỬA ĐỔI CÁC HÀM NAVIGATION DƯỚI ĐÂY ***)

    /**
     * (MỚI) Helper để chụp lại trạng thái hiện tại.
     */
    private GridNavigationState createCurrentState() {
        String selectedId = getSelectedItemId();
        switch (currentStateType) {
            case SEARCH:
                return new GridNavigationState(
                        GridNavigationState.StateType.SEARCH,
                        currentSearchKeywords,
                        currentSortBy.get(),
                        currentSortOrder.get(),
                        currentPageIndex,
                        selectedId
                );
            case CHIP:
                return new GridNavigationState(
                        GridNavigationState.StateType.CHIP,
                        currentChipModel,
                        currentChipType,
                        currentSortBy.get(),
                        currentSortOrder.get(),
                        currentPageIndex,
                        selectedId
                );
            case FOLDER:
            default:
                return new GridNavigationState(
                        GridNavigationState.StateType.FOLDER,
                        currentParentId,
                        currentSortBy.get(),
                        currentSortOrder.get(),
                        currentPageIndex,
                        selectedId
                );
        }
    }

    private String getSelectedItemId() {
        return (selectedItem.get() != null) ? selectedItem.get().getId() : null;
    }

    /**
     * (SỬA ĐỔI) Đẩy trạng thái hiện tại vào stack, và XÓA stack forward.
     */
    private void pushCurrentStateToHistory() {
        if (isRestoringState) return; // Không push nếu đang khôi phục

        GridNavigationState currentState = createCurrentState();

        // Chỉ push nếu đây là lần đầu hoặc state mới khác state trên đỉnh
        if (navigationHistory.isEmpty() || !navigationHistory.peek().equals(currentState)) {
            navigationHistory.push(currentState);
            canGoBack.set(true);

            // Một hành động mới sẽ xóa lịch sử "forward"
            forwardHistory.clear();
            canGoForward.set(false);
        }
    }

    /**
     * (MỚI) Listener dùng chung để tắt cờ isRestoringState khi load xong.
     */
    private ChangeListener<Boolean> createRestoreStateListener() {
        return new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> obs, Boolean oldV, Boolean newV) {
                if (!newV) { // Khi loading chuyển từ true -> false
                    isRestoringState = false;
                    loading.removeListener(this); // Tự hủy listener
                }
            }
        };
    }

    /**
     * (MỚI) Helper để khôi phục trạng thái
     */
    private void restoreState(GridNavigationState state, EmbyService embyService) {
        isRestoringState = true; // Đặt cờ
        loading.addListener(createRestoreStateListener());

        // Khôi phục các giá trị
        currentSortBy.set(state.getSortBy());
        currentSortOrder.set(state.getSortOrder());

        // Gọi hàm load internal tương ứng
        switch (state.getType()) {
            case FOLDER:
                loadPageInternal(state.getPageIndex(), state.getPrimaryParam(), state.getSelectedItemId());
                break;
            case SEARCH:
                loadSearchPageInternal(state.getPageIndex(), state.getPrimaryParam(), state.getSelectedItemId());
                break;
            case CHIP:
                loadItemsByTagChipInternal(state.getChipModel(), state.getChipType(), embyService, state.getSelectedItemId());
                break;
        }
    }

    /**
     * (SỬA ĐỔI) Hàm public được gọi từ MainController để quay lại.
     */
    public void navigateBack(EmbyService embyService) {
        if (navigationHistory.isEmpty()) return;

        // 1. Đẩy trạng thái hiện tại vào forward stack
        forwardHistory.push(createCurrentState());
        canGoForward.set(true);

        // 2. Lấy trạng thái cũ từ back stack
        GridNavigationState stateToLoad = navigationHistory.pop();
        canGoBack.set(!navigationHistory.isEmpty());

        // 3. Khôi phục trạng thái
        restoreState(stateToLoad, embyService);
    }

    /**
     * (MỚI) Hàm public được gọi từ MainController để tiến tới.
     */
    public void navigateForward(EmbyService embyService) {
        if (forwardHistory.isEmpty()) return;

        // 1. Đẩy trạng thái hiện tại vào back stack
        navigationHistory.push(createCurrentState());
        canGoBack.set(true);

        // 2. Lấy trạng thái từ forward stack
        GridNavigationState stateToLoad = forwardHistory.pop();
        canGoForward.set(!forwardHistory.isEmpty());

        // 3. Khôi phục trạng thái
        restoreState(stateToLoad, embyService);
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

    // (*** THÊM GETTER MỚI ***)
    public ReadOnlyBooleanProperty canGoBackProperty() {
        return canGoBack.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty canGoForwardProperty() {
        return canGoForward.getReadOnlyProperty();
    }
}