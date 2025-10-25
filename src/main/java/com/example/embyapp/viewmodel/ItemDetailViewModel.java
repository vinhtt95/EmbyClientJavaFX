package com.example.embyapp.viewmodel;

import com.example.emby.EmbyClient.ApiException; // <-- MỚI IMPORT
import com.example.emby.EmbyClient.Java.ItemUpdateServiceApi; // <-- MỚI IMPORT
import com.example.emby.modelEmby.BaseItemDto;
import com.example.emby.modelEmby.BaseItemPerson; // <-- MỚI IMPORT
import com.example.emby.modelEmby.NameLongIdPair; // <-- MỚI IMPORT
import com.example.emby.modelEmby.PersonType; // <-- MỚI IMPORT
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.ItemRepository;
import com.example.embyapp.viewmodel.detail.ItemDetailDirtyTracker;
import com.example.embyapp.viewmodel.detail.ItemDetailImportHandler;
import com.example.embyapp.viewmodel.detail.ItemDetailLoader;
import com.example.embyapp.viewmodel.detail.ItemDetailSaver; // Vẫn cần cho manual edit
import com.example.embyapp.viewmodel.detail.TagModel;
import com.google.gson.Gson; // <-- MỚI IMPORT (cho deep copy)
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.text.ParseException; // <-- MỚI IMPORT
import java.text.SimpleDateFormat; // <-- MỚI IMPORT
import java.util.Arrays; // <-- MỚI IMPORT
import java.util.Date; // <-- MỚI IMPORT
import java.util.List;
import java.util.Set; // <-- MỚI IMPORT
import java.util.stream.Collectors; // <-- MỚI IMPORT
import org.threeten.bp.OffsetDateTime; // <-- MỚI IMPORT


/**
 * (CẬP NHẬT 17)
 * - Sửa saveChanges() để xử lý riêng trường hợp lưu sau import.
 * - Thêm createDtoWithAcceptedChanges() và logic parse tương ứng.
 */
public class ItemDetailViewModel {

    // --- Dependencies & Helpers ---
    private final ItemRepository itemRepository;
    private final EmbyService embyService;
    private final ItemDetailLoader loader;
    private final ItemDetailSaver saver; // Vẫn dùng cho manual edit
    private final ItemDetailDirtyTracker dirtyTracker;
    private final ItemDetailImportHandler importHandler;
    private static final Gson gson = new Gson(); // Dùng cho deep copy DTO
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy"); // Dùng cho parse date

    // --- State ---
    private BaseItemDto originalItemDto; // DTO gốc từ server
    private String currentItemId;
    private String exportFileNameTitle;

    // --- Properties ---
    private final ReadOnlyBooleanWrapper loading = new ReadOnlyBooleanWrapper(false);
    // ... (Các properties còn lại: title, overview, tagItems, v.v...) ...
    private final StringProperty title = new SimpleStringProperty("");
    private final StringProperty overview = new SimpleStringProperty("");
    private final ObservableList<TagModel> tagItems = FXCollections.observableArrayList();
    private final StringProperty releaseDate = new SimpleStringProperty("");
    private final StringProperty studios = new SimpleStringProperty("");
    private final StringProperty people = new SimpleStringProperty("");
    private final ReadOnlyStringWrapper year = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper tagline = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper genres = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper runtime = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<Image> primaryImage = new ReadOnlyObjectWrapper<>(null);
    private final ObservableList<String> backdropImageUrls = FXCollections.observableArrayList();
    private final ReadOnlyStringWrapper itemPath = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper isFolder = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper("Vui lòng chọn một item từ danh sách...");
    private final ReadOnlyBooleanWrapper showStatusMessage = new ReadOnlyBooleanWrapper(true);
    private final ReadOnlyStringWrapper actionStatusMessage = new ReadOnlyStringWrapper("");


    // --- Constructor ---
    public ItemDetailViewModel(ItemRepository itemRepository, EmbyService embyService) {
        this.itemRepository = itemRepository;
        this.embyService = embyService;

        this.loader = new ItemDetailLoader(itemRepository, embyService);
        this.saver = new ItemDetailSaver(embyService); // Khởi tạo saver
        this.dirtyTracker = new ItemDetailDirtyTracker(this);
        this.importHandler = new ItemDetailImportHandler(this, this.dirtyTracker);
    }

    // --- Load Item ---
    // ... (setItemToDisplay, clearAllDetailsUI không đổi) ...
    public void setItemToDisplay(BaseItemDto item) {
        if (item == null) {
            Platform.runLater(() -> {
                clearAllDetailsUI();
                this.currentItemId = null;
                this.originalItemDto = null;
                this.exportFileNameTitle = null;
                statusMessage.set("Vui lòng chọn một item từ danh sách...");
                showStatusMessage.set(true);
                loading.set(false);
            });
            return;
        }
        final String newItemId = item.getId();
        Platform.runLater(() -> {
            clearAllDetailsUI();
            statusMessage.set("Đang tải chi tiết cho: " + item.getName() + "...");
            showStatusMessage.set(true);
            loading.set(true);
        });
        new Thread(() -> {
            try {
                String userId = embyService.getCurrentUserId();
                if (userId == null) {
                    throw new IllegalStateException("Không thể lấy UserID. Vui lòng đăng nhập lại.");
                }
                ItemDetailLoader.LoadResult result = loader.loadItemData(userId, newItemId);
                this.originalItemDto = result.getFullDetails();
                this.currentItemId = newItemId;
                this.exportFileNameTitle = result.getOriginalTitleForExport();
                Platform.runLater(() -> {
                    title.set(result.getTitleText());
                    overview.set(result.getOverviewText());
                    tagItems.setAll(result.getTagItems());
                    releaseDate.set(result.getReleaseDateText());
                    studios.set(result.getStudiosText());
                    people.set(result.getPeopleText());
                    year.set(result.getYearText());
                    tagline.set(result.getTaglineText());
                    genres.set(result.getGenresText());
                    runtime.set(result.getRuntimeText());
                    itemPath.set(result.getPathText());
                    isFolder.set(result.isFolder());
                    actionStatusMessage.set("");
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
                    dirtyTracker.startTracking(result.getOriginalStrings());
                    importHandler.hideAllReviewButtons();
                    loading.set(false);
                    showStatusMessage.set(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    clearAllDetailsUI();
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
    private void clearAllDetailsUI() {
        dirtyTracker.stopTracking();
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
        tagItems.clear();
        releaseDate.set("");
        studios.set("");
        people.set("");
        importHandler.clearState();
    }


    // --- Save Logic (*** ĐÃ SỬA ĐỔI HOÀN TOÀN ***) ---
    /**
     * Xử lý lưu thay đổi, phân biệt giữa lưu sau Import và lưu sau Sửa thủ công.
     */
    public void saveChanges() {
        if (originalItemDto == null || currentItemId == null) {
            reportActionError("Lỗi: Không có item nào đang được chọn để lưu.");
            return;
        }

        reportActionError("Đang lưu thay đổi lên server...");
        importHandler.hideAllReviewButtons(); // Ẩn nút (v/x) nếu còn

        // Xác định xem có phải lưu sau import không
        final boolean isSavingAfterImport = importHandler.wasImportInProgress();
        final Set<String> acceptedFields = isSavingAfterImport ? importHandler.getAcceptedFields() : null;

        // Chạy lưu trong luồng nền
        new Thread(() -> {
            try {
                BaseItemDto dtoToSave;

                // 1. Chuẩn bị DTO để lưu
                if (isSavingAfterImport) {
                    System.out.println("Saving accepted fields after import...");
                    dtoToSave = createDtoWithAcceptedChanges(acceptedFields);
                } else {
                    System.out.println("Saving manual edits...");
                    // Dùng logic cũ: tạo SaveRequest từ UI hiện tại và để Saver parse
                    ItemDetailSaver.SaveRequest manualSaveRequest = new ItemDetailSaver.SaveRequest(
                            originalItemDto, currentItemId, title.get(), overview.get(),
                            List.copyOf(tagItems), releaseDate.get(), studios.get(), people.get()
                    );
                    // Nhờ Saver parse và trả về DTO đã cập nhật
                    // (Tái sử dụng logic parse của Saver)
                    dtoToSave = saver.parseUiToDto(manualSaveRequest); // Gọi hàm parse mới
                }

                // 2. Gọi API để lưu
                ItemUpdateServiceApi itemUpdateServiceApi = embyService.getItemUpdateServiceApi();
                if (itemUpdateServiceApi == null) {
                    throw new IllegalStateException("Không thể lấy ItemUpdateServiceApi.");
                }
                itemUpdateServiceApi.postItemsByItemid(dtoToSave, currentItemId);

                // 3. Xử lý thành công (trên UI thread)
                Platform.runLater(() -> {
                    reportActionError("Đã lưu thay đổi thành công!");
                    // Cập nhật lại originalItemDto bằng cái vừa lưu thành công
                    // (Quan trọng để lần lưu sau đúng)
                    // Dùng deep copy để tránh tham chiếu lẫn lộn
                    this.originalItemDto = gson.fromJson(gson.toJson(dtoToSave), BaseItemDto.class);

                    // Cập nhật snapshot gốc của DirtyTracker và reset import state
                    dirtyTracker.updateOriginalStringsFromCurrent();

                    // Xóa trạng thái import sau khi lưu thành công
                    importHandler.clearState();
                });

            } catch (ApiException e) {
                System.err.println("API Error saving item: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> reportActionError("Lỗi API khi lưu: " + e.getCode() + " - " + e.getMessage()));
            } catch (Exception e) {
                System.err.println("Generic Error saving item: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> reportActionError("Lỗi không xác định khi lưu: " + e.getMessage()));
            }
        }).start();
    }

    // --- (*** MỚI: Helper tạo DTO chỉ với các thay đổi được chấp nhận ***) ---
    /**
     * Tạo một bản sao DTO chỉ chứa các thay đổi đã được chấp nhận từ import.
     * @param acceptedFields Set chứa tên các trường đã nhấn (✓).
     * @return BaseItemDto đã được cập nhật chọn lọc.
     * @throws RuntimeException nếu originalItemDto bị null.
     */
    private BaseItemDto createDtoWithAcceptedChanges(Set<String> acceptedFields) {
        if (originalItemDto == null) {
            throw new RuntimeException("originalItemDto không được null khi tạo DTO thay đổi.");
        }
        // Tạo bản sao sâu (deep copy) để không ảnh hưởng DTO gốc
        BaseItemDto dtoCopy = gson.fromJson(gson.toJson(originalItemDto), BaseItemDto.class);

        System.out.println("Accepted fields to save: " + acceptedFields);

        // Áp dụng các thay đổi được chấp nhận từ UI hiện tại vào bản sao
        if (acceptedFields.contains("title")) {
            dtoCopy.setName(title.get());
        }
        if (acceptedFields.contains("overview")) {
            dtoCopy.setOverview(overview.get());
        }
        if (acceptedFields.contains("tags")) { // Tags luôn được accept ngầm khi import
            List<NameLongIdPair> tagItemsToSave = tagItems.stream()
                    .map(tagModel -> {
                        NameLongIdPair pair = new NameLongIdPair();
                        pair.setName(tagModel.serialize());
                        pair.setId(null);
                        return pair;
                    })
                    .collect(Collectors.toList());
            dtoCopy.setTagItems(tagItemsToSave);
            // dtoCopy.setTags(null); // Optional clear old field
        }
        if (acceptedFields.contains("releaseDate")) {
            try {
                Date parsedDate = dateFormat.parse(releaseDate.get());
                org.threeten.bp.Instant threetenInstant = org.threeten.bp.Instant.ofEpochMilli(parsedDate.getTime());
                OffsetDateTime odt = OffsetDateTime.ofInstant(threetenInstant, org.threeten.bp.ZoneId.systemDefault());
                dtoCopy.setPremiereDate(odt);
            } catch (ParseException e) {
                System.err.println("Không thể parse ngày (save accepted): " + releaseDate.get() + ". Sẽ giữ giá trị gốc.");
                // Giữ giá trị gốc nếu parse lỗi
            }
        }
        if (acceptedFields.contains("studios")) {
            List<NameLongIdPair> studiosList = Arrays.stream(studios.get().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(name -> {
                        NameLongIdPair pair = new NameLongIdPair();
                        pair.setName(name);
                        return pair;
                    })
                    .collect(Collectors.toList());
            dtoCopy.setStudios(studiosList);
        }
        if (acceptedFields.contains("people")) {
            List<BaseItemPerson> peopleList = Arrays.stream(people.get().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(name -> new BaseItemPerson(name, PersonType.ACTOR))
                    .collect(Collectors.toList());
            dtoCopy.setPeople(peopleList);
        }

        return dtoCopy;
    }


    // --- Import / Accept / Reject ---
    public void importAndPreview(BaseItemDto importedDto) {
        if (originalItemDto == null) return;
        importHandler.importAndPreview(importedDto);
    }
    public void acceptImportField(String fieldName) {
        importHandler.acceptImportField(fieldName);
    }
    public void rejectImportField(String fieldName) {
        importHandler.rejectImportField(fieldName);
    }
    public void markAsDirtyByAccept() {
        dirtyTracker.forceDirty();
    }


    // --- Tag Management ---
    public void addTag(TagModel newTag) {
        if (newTag != null) {
            tagItems.add(newTag);
        }
    }
    public void removeTag(TagModel tagToRemove) {
        if (tagToRemove != null) {
            tagItems.remove(tagToRemove);
        }
    }


    // --- Getters cho Export ---
    public BaseItemDto getItemForExport() { return this.originalItemDto; }
    public String getOriginalTitleForExport() { return this.exportFileNameTitle != null ? this.exportFileNameTitle : this.title.get(); }


    // --- Action Status ---
    public void reportActionError(String errorMessage) { Platform.runLater(() -> this.actionStatusMessage.set(errorMessage)); }
    public void clearActionError() { Platform.runLater(() -> this.actionStatusMessage.set("")); }


    // --- Getters for Properties ---
    // Dùng bởi các lớp phụ trợ
    public StringProperty titleProperty() { return title; }
    public StringProperty overviewProperty() { return overview; }
    public ObservableList<TagModel> getTagItems() { return tagItems; }
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
    public ReadOnlyBooleanProperty showReleaseDateReviewProperty() { return importHandler.showReleaseDateReviewProperty(); }
    public ReadOnlyBooleanProperty showStudiosReviewProperty() { return importHandler.showStudiosReviewProperty(); }
    public ReadOnlyBooleanProperty showPeopleReviewProperty() { return importHandler.showPeopleReviewProperty(); }
}