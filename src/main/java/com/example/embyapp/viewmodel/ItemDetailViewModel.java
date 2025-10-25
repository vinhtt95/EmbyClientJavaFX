package com.example.embyapp.viewmodel;

import com.example.emby.modelEmby.BaseItemDto;
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.ItemRepository;
import com.example.embyapp.viewmodel.detail.ItemDetailDirtyTracker;
import com.example.embyapp.viewmodel.detail.ItemDetailImportHandler;
import com.example.embyapp.viewmodel.detail.ItemDetailLoader;
import com.example.embyapp.viewmodel.detail.ItemDetailSaver;
import com.example.embyapp.viewmodel.detail.TagModel; // (*** MỚI IMPORT ***)
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList; // (*** MỚI IMPORT ***)
import javafx.scene.image.Image;

import java.util.List; // (*** MỚI IMPORT ***)

/**
 * (CẬP NHẬT 7)
 * - Thay thế StringProperty tags bằng ObservableList<TagModel> tagItems.
 * - Thêm 2 phương thức addTag và removeTag.
 * - Sửa lỗi logic clearAllDetails và setItemToDisplay để đảm bảo saveChanges() hoạt động.
 */
public class ItemDetailViewModel {

    // --- Dependencies (Giữ lại) ---
    private final ItemRepository itemRepository;
    private final EmbyService embyService;

    // --- Lớp phụ trợ (MỚI) ---
    private final ItemDetailLoader loader;
    private final ItemDetailSaver saver;
    private final ItemDetailDirtyTracker dirtyTracker;
    private final ItemDetailImportHandler importHandler;

    // --- State (Giữ lại) ---
    private BaseItemDto originalItemDto; // DTO gốc từ server
    private String currentItemId;
    private String exportFileNameTitle; // Tên file gốc (OriginalTitle) để export

    // --- Properties cho UI Binding (*** SỬA ĐỔI TAGS ***) ---
    private final ReadOnlyBooleanWrapper loading = new ReadOnlyBooleanWrapper(false);

    // Các trường Form (Editable)
    private final StringProperty title = new SimpleStringProperty("");
    private final StringProperty overview = new SimpleStringProperty("");
    // private final StringProperty tags = new SimpleStringProperty(""); // (ĐÃ XÓA)
    private final ObservableList<TagModel> tagItems = FXCollections.observableArrayList(); // (*** MỚI ***)
    private final StringProperty releaseDate = new SimpleStringProperty("");
    private final StringProperty studios = new SimpleStringProperty("");
    private final StringProperty people = new SimpleStringProperty("");

    // Các trường thông tin (Read-only)
    private final ReadOnlyStringWrapper year = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper tagline = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper genres = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper runtime = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<Image> primaryImage = new ReadOnlyObjectWrapper<>(null);
    private final ObservableList<String> backdropImageUrls = FXCollections.observableArrayList();

    // Các trường hệ thống (Path, Folder)
    private final ReadOnlyStringWrapper itemPath = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper isFolder = new ReadOnlyBooleanWrapper(false);

    // Các trường trạng thái (Status)
    private final ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper("Vui lòng chọn một item từ danh sách...");
    private final ReadOnlyBooleanWrapper showStatusMessage = new ReadOnlyBooleanWrapper(true);
    private final ReadOnlyStringWrapper actionStatusMessage = new ReadOnlyStringWrapper("");

    // --- Constructor (ĐÃ SỬA ĐỔI) ---
    public ItemDetailViewModel(ItemRepository itemRepository, EmbyService embyService) {
        this.itemRepository = itemRepository;
        this.embyService = embyService;

        // Khởi tạo các lớp phụ trợ
        this.loader = new ItemDetailLoader(itemRepository, embyService);
        this.saver = new ItemDetailSaver(embyService);
        this.dirtyTracker = new ItemDetailDirtyTracker(this);
        this.importHandler = new ItemDetailImportHandler(this);
    }

    // --- Phương thức chính (*** SỬA LỖI LOGIC LƯU ***) ---

    /**
     * Phương thức chính được gọi khi item được chọn.
     * Ủy thác việc tải dữ liệu cho ItemDetailLoader.
     */
    public void setItemToDisplay(BaseItemDto item) {

        // (*** SỬA LỖI 1: Xử lý bỏ chọn item ***)
        if (item == null) {
            Platform.runLater(() -> {
                clearAllDetailsUI(); // Xóa UI

                // Xóa State thủ công
                this.currentItemId = null;
                this.originalItemDto = null;
                this.exportFileNameTitle = null;

                // Đặt lại Status
                statusMessage.set("Vui lòng chọn một item từ danh sách...");
                showStatusMessage.set(true);
                loading.set(false);
            });
            return;
        }

        // Lưu ID mới vào biến final tạm thời để luồng nền sử dụng
        final String newItemId = item.getId();

        Platform.runLater(() -> {
            clearAllDetailsUI(); // Xóa UI và listener cũ
            statusMessage.set("Đang tải chi tiết cho: " + item.getName() + "...");
            showStatusMessage.set(true);
            loading.set(true);
        });

        // Chạy tải nền, sử dụng lớp Loader
        new Thread(() -> {
            try {
                String userId = embyService.getCurrentUserId();
                if (userId == null) {
                    throw new IllegalStateException("Không thể lấy UserID. Vui lòng đăng nhập lại.");
                }

                // 1. Lớp Loader thực hiện tải (dùng newItemId)
                ItemDetailLoader.LoadResult result = loader.loadItemData(userId, newItemId);

                // (*** SỬA LỖI 2: Set State (ID và DTO) CÙNG NHAU ***)
                // 2. Lưu trữ state gốc
                this.originalItemDto = result.getFullDetails();
                this.currentItemId = newItemId; // <-- SET ID TẠI ĐÂY
                this.exportFileNameTitle = result.getOriginalTitleForExport();

                // 3. Cập nhật UI trên JavaFX Thread
                Platform.runLater(() -> {
                    // Cập nhật các trường editable
                    title.set(result.getTitleText());
                    overview.set(result.getOverviewText());

                    // (*** SỬA ĐỔI TAGS ***)
                    tagItems.setAll(result.getTagItems()); // (*** MỚI ***)
                    // (*** KẾT THÚC SỬA ĐỔI TAGS ***)

                    releaseDate.set(result.getReleaseDateText());
                    studios.set(result.getStudiosText());
                    people.set(result.getPeopleText());

                    // Cập nhật các trường read-only
                    year.set(result.getYearText());
                    tagline.set(result.getTaglineText());
                    genres.set(result.getGenresText());
                    runtime.set(result.getRuntimeText());
                    itemPath.set(result.getPathText());
                    isFolder.set(result.isFolder());
                    actionStatusMessage.set("");

                    // Cập nhật ảnh
                    if (result.getPrimaryImageUrl() != null) {
                        Image img = new Image(result.getPrimaryImageUrl(), true);
                        img.exceptionProperty().addListener((obs, oldEx, newEx) -> {
                            if (newEx != null) {
                                System.err.println("LỖI TẢI ẢNH: " + result.getPrimaryImageUrl());
                                newEx.printStackTrace();
                            }
                        });
                        primaryImage.set(img);
                    } else {
                        primaryImage.set(null);
                    }
                    backdropImageUrls.setAll(result.getBackdropUrls());

                    // 4. Khởi động Dirty Tracker
                    dirtyTracker.startTracking(result.getOriginalStrings());

                    // 5. Hoàn tất
                    importHandler.hideAllReviewButtons();
                    loading.set(false);
                    showStatusMessage.set(false);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    clearAllDetailsUI(); // Xóa UI
                    // Xóa State thủ công khi lỗi
                    this.currentItemId = null;
                    this.originalItemDto = null;
                    this.exportFileNameTitle = null;

                    statusMessage.set("Lỗi khi tải chi tiết: " + e.getMessage());
                    showStatusMessage.set(true);
                    loading.set(false);
                });
            }
        }).start();
    }

    /**
     * (*** SỬA LỖI 3: Đổi tên và chức năng ***)
     * Xóa các trường UI và dừng listener, KHÔNG xóa state.
     */
    private void clearAllDetailsUI() {
        dirtyTracker.stopTracking(); // Dừng theo dõi

        // Xóa UI properties
        title.set("");
        year.set("");
        overview.set("");
        tagline.set("");
        genres.set("");
        runtime.set("");
        primaryImage.set(null);
        backdropImageUrls.clear();
        itemPath.set("");
        isFolder.set(false);
        actionStatusMessage.set("");

        // (*** SỬA ĐỔI TAGS ***)
        tagItems.clear(); // (*** MỚI ***)
        // (*** KẾT THÚC SỬA ĐỔI TAGS ***)

        releaseDate.set("");
        studios.set("");
        people.set("");

        // KHÔNG XÓA STATE (currentItemId, originalItemDto) ở đây

        // Xóa state của helper
        importHandler.clearState();
    }


    // --- Ủy thác cho Lớp phụ trợ (*** SỬA ĐỔI TAGS ***) ---

    /**
     * Ủy thác việc Lưu cho ItemDetailSaver.
     */
    public void saveChanges() {
        // Check này giờ sẽ pass
        if (originalItemDto == null || currentItemId == null) {
            reportActionError("Lỗi: Không có item nào đang được chọn để lưu.");
            return;
        }

        reportActionError("Đang lưu thay đổi lên server...");
        importHandler.hideAllReviewButtons();

        // 1. Tạo một "SaveRequest"
        // (*** SỬA ĐỔI TAGS ***)
        ItemDetailSaver.SaveRequest request = new ItemDetailSaver.SaveRequest(
                originalItemDto, // DTO gốc để cập nhật
                currentItemId,
                title.get(),
                overview.get(),
                List.copyOf(tagItems), // (*** MỚI: Gửi bản sao của List<TagModel> ***)
                releaseDate.get(),
                studios.get(),
                people.get()
        );
        // (*** KẾT THÚC SỬA ĐỔI TAGS ***)

        // 2. Chạy lưu nền, sử dụng lớp Saver
        new Thread(() -> {
            try {
                // 3. Lớp Saver thực hiện phân tích (parse) và gọi API
                saver.saveChanges(request);

                // 4. Báo thành công
                Platform.runLater(() -> {
                    reportActionError("Đã lưu thay đổi thành công!");
                    // Cập nhật lại snapshot gốc của DirtyTracker
                    dirtyTracker.updateOriginalStringsFromCurrent();
                });

            } catch (Exception e) {
                System.err.println("Lỗi khi lưu: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> reportActionError("Lỗi khi lưu: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Ủy thác việc Import cho ItemDetailImportHandler.
     */
    public void importAndPreview(BaseItemDto importedDto) {
        if (originalItemDto == null) return;
        importHandler.importAndPreview(importedDto);
    }

    /**
     * Ủy thác việc Chấp nhận (v) cho ItemDetailImportHandler.
     * (Đã sửa lỗi ở lần trước)
     */
    public void acceptImportField(String fieldName) {
        importHandler.acceptImportField(fieldName);
        // Không gọi updateOriginalStringsFromCurrent() ở đây
    }

    /**
     * Ủy thác việc Hủy bỏ (x) cho ItemDetailImportHandler.
     */
    public void rejectImportField(String fieldName) {
        importHandler.rejectImportField(fieldName);
    }

    // (*** MỚI: HÀM QUẢN LÝ TAGS ***)
    /**
     * Thêm một tag vào danh sách (được gọi từ Dialog).
     */
    public void addTag(TagModel newTag) {
        if (newTag != null) {
            tagItems.add(newTag);
            // ListChangeListener trong DirtyTracker sẽ tự động phát hiện thay đổi này.
        }
    }

    /**
     * Xóa một tag khỏi danh sách (được gọi từ TagView chip).
     */
    public void removeTag(TagModel tagToRemove) {
        if (tagToRemove != null) {
            tagItems.remove(tagToRemove);
            // ListChangeListener trong DirtyTracker sẽ tự động phát hiện thay đổi này.
        }
    }


    // --- Getters cho Export (Giữ lại) ---
    public BaseItemDto getItemForExport() {
        return this.originalItemDto;
    }

    public String getOriginalTitleForExport() {
        return this.exportFileNameTitle != null ? this.exportFileNameTitle : this.title.get();
    }


    // --- Getters/Setters cho Action Status (Giữ lại) ---
    public void reportActionError(String errorMessage) {
        Platform.runLater(() -> this.actionStatusMessage.set(errorMessage));
    }
    public void clearActionError() {
        Platform.runLater(() -> this.actionStatusMessage.set(""));
    }

    // --- Getters cho các Properties (UI Binding) ---

    // Dùng bởi các lớp phụ trợ
    public StringProperty titleProperty() { return title; }
    public StringProperty overviewProperty() { return overview; }
    // public StringProperty tagsProperty() { return tags; } // (ĐÃ XÓA)
    public ObservableList<TagModel> getTagItems() { return tagItems; } // (*** MỚI ***)
    public StringProperty releaseDateProperty() { return releaseDate; }
    public StringProperty studiosProperty() { return studios; }
    public StringProperty peopleProperty() { return people; }

    // Dùng bởi Controller (FXML)
    public ReadOnlyBooleanProperty loadingProperty() { return loading.getReadOnlyProperty(); }
    public ReadOnlyStringProperty yearProperty() { return year.getReadOnlyProperty(); }
    public ReadOnlyStringProperty statusMessageProperty() { return statusMessage.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showStatusMessageProperty() { return showStatusMessage.getReadOnlyProperty(); }
    public ReadOnlyStringProperty taglineProperty() { return tagline.getReadOnlyProperty(); }
    public ReadOnlyStringProperty genresProperty() { return genres.getReadOnlyProperty(); }
    public ReadOnlyStringProperty runtimeProperty() { return runtime.getReadOnlyProperty(); }
    public ReadOnlyObjectProperty<Image> primaryImageProperty() { return primaryImage.getReadOnlyProperty(); }
    public ObservableList<String> getBackdropImageUrls() { return backdropImageUrls; }
    public ReadOnlyStringProperty itemPathProperty() { return itemPath.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty isFolderProperty() { return isFolder.getReadOnlyProperty(); }
    public ReadOnlyStringProperty actionStatusMessageProperty() { return actionStatusMessage.getReadOnlyProperty(); }

    // Dirty property (từ tracker)
    public BooleanProperty isDirtyProperty() { return dirtyTracker.isDirtyProperty(); }

    // Review properties (từ import handler)
    public ReadOnlyBooleanProperty showTitleReviewProperty() { return importHandler.showTitleReviewProperty(); }
    public ReadOnlyBooleanProperty showOverviewReviewProperty() { return importHandler.showOverviewReviewProperty(); }
    // public ReadOnlyBooleanProperty showTagsReviewProperty() { return importHandler.showTagsReviewProperty(); } // (ĐÃ XÓA)
    public ReadOnlyBooleanProperty showReleaseDateReviewProperty() { return importHandler.showReleaseDateReviewProperty(); }
    public ReadOnlyBooleanProperty showStudiosReviewProperty() { return importHandler.showStudiosReviewProperty(); }
    public ReadOnlyBooleanProperty showPeopleReviewProperty() { return importHandler.showPeopleReviewProperty(); }
}