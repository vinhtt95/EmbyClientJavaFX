package com.example.embyapp.viewmodel;

import embyclient.ApiException;
import embyclient.api.ItemUpdateServiceApi;
import embyclient.model.*; // <-- Import tổng quát
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.ItemRepository;
import com.example.embyapp.viewmodel.detail.*; // <-- Import tổng quát
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.threeten.bp.OffsetDateTime;


/**
 * (CẬP NHẬT 19)
 * - Sửa setItemToDisplay để dùng result.getBackdropImages()
 */
public class ItemDetailViewModel {

    // --- Dependencies & Helpers ---
    private final ItemRepository itemRepository;
    private final EmbyService embyService;
    private final ItemDetailLoader loader;
    private final ItemDetailSaver saver;
    private final ItemDetailDirtyTracker dirtyTracker;
    private final ItemDetailImportHandler importHandler;
    private final ItemImageUpdater imageUpdater;
    private static final Gson gson = new Gson();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    // --- State ---
    private BaseItemDto originalItemDto;
    private String currentItemId;
    private String exportFileNameTitle;
    private final ObjectProperty<File> newPrimaryImageFile = new SimpleObjectProperty<>(null);

    // --- Properties ---
    private final ReadOnlyBooleanWrapper loading = new ReadOnlyBooleanWrapper(false);
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
    private final ObservableList<ImageInfo> backdropImages = FXCollections.observableArrayList();
    private final ReadOnlyStringWrapper itemPath = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper isFolder = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper("Vui lòng chọn một item từ danh sách...");
    private final ReadOnlyBooleanWrapper showStatusMessage = new ReadOnlyBooleanWrapper(true);
    private final ReadOnlyStringWrapper actionStatusMessage = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper primaryImageDirty = new ReadOnlyBooleanWrapper(false);


    // --- Constructor ---
    public ItemDetailViewModel(ItemRepository itemRepository, EmbyService embyService) {
        this.itemRepository = itemRepository;
        this.embyService = embyService;

        this.loader = new ItemDetailLoader(itemRepository, embyService);
        this.saver = new ItemDetailSaver(embyService);
        this.dirtyTracker = new ItemDetailDirtyTracker(this);
        this.importHandler = new ItemDetailImportHandler(this, this.dirtyTracker);
        this.imageUpdater = new ItemImageUpdater(embyService);

        newPrimaryImageFile.addListener((obs, oldVal, newVal) -> {
            primaryImageDirty.set(newVal != null);
        });
    }

    // --- Load Item ---
    public void setItemToDisplay(BaseItemDto item) {
        if (item == null) {
            Platform.runLater(this::clearAllDetailsUI);
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

                // (*** SỬA ĐỔI: Lấy trực tiếp backdrops đã lọc ***)
                List<ImageInfo> backdrops = result.getBackdropImages();

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

                    // Set ảnh Primary (Logic này không đổi)
                    if (result.getPrimaryImageUrl() != null) {
                        Image img = new Image(result.getPrimaryImageUrl(), true);
                        img.exceptionProperty().addListener((obs, oldEx, newEx) -> {
                            if (newEx != null) System.err.println("LỖI TẢI ẢNH: " + result.getPrimaryImageUrl());
                        });
                        primaryImage.set(img);
                    } else {
                        primaryImage.set(null);
                    }

                    // Set Backdrops
                    backdropImages.setAll(backdrops); // <-- Gán trực tiếp

                    dirtyTracker.startTracking(result.getOriginalStrings());
                    importHandler.hideAllReviewButtons();
                    loading.set(false);
                    showStatusMessage.set(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    clearAllDetailsUI();
                    statusMessage.set("Lỗi khi tải chi tiết: " + e.getMessage());
                    showStatusMessage.set(true);
                    loading.set(false);
                });
            }
        }).start();
    }

    // (clearAllDetailsUI giữ nguyên)
    private void clearAllDetailsUI() {
        dirtyTracker.stopTracking();
        title.set("");
        year.set("");
        overview.set("");
        tagline.set("");
        genres.set("");
        runtime.set("");
        primaryImage.set(null);
        backdropImages.clear();
        itemPath.set("");
        isFolder.set(false);
        actionStatusMessage.set("");
        tagItems.clear();
        releaseDate.set("");
        studios.set("");
        people.set("");
        importHandler.clearState();
        currentItemId = null;
        originalItemDto = null;
        exportFileNameTitle = null;
        newPrimaryImageFile.set(null);
    }

    // (saveChanges giữ nguyên)
    public void saveChanges() {
        if (originalItemDto == null || currentItemId == null) {
            reportActionError("Lỗi: Không có item nào đang được chọn để lưu.");
            return;
        }
        reportActionError("Đang lưu thay đổi lên server...");
        importHandler.hideAllReviewButtons();
        final boolean isSavingAfterImport = importHandler.wasImportInProgress();
        final Set<String> acceptedFields = isSavingAfterImport ? importHandler.getAcceptedFields() : null;
        new Thread(() -> {
            try {
                BaseItemDto dtoToSave;
                if (isSavingAfterImport) {
                    System.out.println("Saving accepted fields after import...");
                    dtoToSave = createDtoWithAcceptedChanges(acceptedFields);
                } else {
                    System.out.println("Saving manual edits...");
                    ItemDetailSaver.SaveRequest manualSaveRequest = new ItemDetailSaver.SaveRequest(
                            originalItemDto, currentItemId, title.get(), overview.get(),
                            List.copyOf(tagItems), releaseDate.get(), studios.get(), people.get()
                    );
                    dtoToSave = saver.parseUiToDto(manualSaveRequest);
                }
                ItemUpdateServiceApi itemUpdateServiceApi = embyService.getItemUpdateServiceApi();
                if (itemUpdateServiceApi == null) {
                    throw new IllegalStateException("Không thể lấy ItemUpdateServiceApi.");
                }
                itemUpdateServiceApi.postItemsByItemid(dtoToSave, currentItemId);
                Platform.runLater(() -> {
                    reportActionError("Đã lưu thay đổi thành công!");
                    this.originalItemDto = gson.fromJson(gson.toJson(dtoToSave), BaseItemDto.class);
                    dirtyTracker.updateOriginalStringsFromCurrent();
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

    // (createDtoWithAcceptedChanges giữ nguyên)
    private BaseItemDto createDtoWithAcceptedChanges(Set<String> acceptedFields) {
        if (originalItemDto == null) {
            throw new RuntimeException("originalItemDto không được null khi tạo DTO thay đổi.");
        }
        BaseItemDto dtoCopy = gson.fromJson(gson.toJson(originalItemDto), BaseItemDto.class);
        System.out.println("Accepted fields to save: " + acceptedFields);
        if (acceptedFields.contains("title")) { dtoCopy.setName(title.get()); }
        if (acceptedFields.contains("overview")) { dtoCopy.setOverview(overview.get()); }
        if (acceptedFields.contains("tags")) {
            List<NameLongIdPair> tagItemsToSave = tagItems.stream()
                    .map(tagModel -> {
                        NameLongIdPair pair = new NameLongIdPair();
                        pair.setName(tagModel.serialize());
                        pair.setId(null);
                        return pair;
                    })
                    .collect(Collectors.toList());
            dtoCopy.setTagItems(tagItemsToSave);
        }
        if (acceptedFields.contains("releaseDate")) {
            try {
                Date parsedDate = dateFormat.parse(releaseDate.get());
                org.threeten.bp.Instant threetenInstant = org.threeten.bp.Instant.ofEpochMilli(parsedDate.getTime());
                OffsetDateTime odt = OffsetDateTime.ofInstant(threetenInstant, org.threeten.bp.ZoneId.systemDefault());
                dtoCopy.setPremiereDate(odt);
            } catch (ParseException e) {
                System.err.println("Không thể parse ngày (save accepted): " + releaseDate.get() + ". Sẽ giữ giá trị gốc.");
            }
        }
        if (acceptedFields.contains("studios")) {
            List<NameLongIdPair> studiosList = Arrays.stream(studios.get().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
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
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(name -> new BaseItemPerson(name, PersonType.ACTOR))
                    .collect(Collectors.toList());
            dtoCopy.setPeople(peopleList);
        }
        return dtoCopy;
    }

    // (Các hàm Import/Accept/Reject, Tag, Export, Action... giữ nguyên)
    public void importAndPreview(BaseItemDto importedDto) { if (originalItemDto == null) return; importHandler.importAndPreview(importedDto); }
    public void acceptImportField(String fieldName) { importHandler.acceptImportField(fieldName); }
    public void rejectImportField(String fieldName) { importHandler.rejectImportField(fieldName); }
    public void markAsDirtyByAccept() { dirtyTracker.forceDirty(); }
    public void addTag(TagModel newTag) { if (newTag != null) { tagItems.add(newTag); } }
    public void removeTag(TagModel tagToRemove) { if (tagToRemove != null) { tagItems.remove(tagToRemove); } }
    public BaseItemDto getItemForExport() { return this.originalItemDto; }
    public String getOriginalTitleForExport() { return this.exportFileNameTitle != null ? this.exportFileNameTitle : this.title.get(); }
    public void reportActionError(String errorMessage) { Platform.runLater(() -> this.actionStatusMessage.set(errorMessage)); }
    public void clearActionError() { Platform.runLater(() -> this.actionStatusMessage.set("")); }


    // (Các hàm xử lý ảnh: selectNewPrimaryImage, saveNewPrimaryImage, selectNewBackdrops, deleteBackdrop, reloadBackdrops, uploadDroppedBackdropFiles... giữ nguyên)
    public void selectNewPrimaryImage(Stage ownerStage) {
        if (currentItemId == null) return;
        List<File> files = imageUpdater.chooseImages(ownerStage, false);
        if (files != null && !files.isEmpty()) {
            File selectedFile = files.get(0);
            newPrimaryImageFile.set(selectedFile);
            try {
                Image localImage = new Image(selectedFile.toURI().toString());
                primaryImage.set(localImage);
            } catch (Exception e) {
                reportActionError("Lỗi: Không thể preview ảnh local.");
            }
        }
    }
    public void saveNewPrimaryImage() {
        File fileToSave = newPrimaryImageFile.get();
        if (fileToSave == null || currentItemId == null) {
            reportActionError("Lỗi: Không có ảnh mới hoặc item ID.");
            return;
        }
        reportActionError("Đang upload ảnh Primary...");
        new Thread(() -> {
            try {
                imageUpdater.uploadImage(currentItemId, ImageType.PRIMARY, fileToSave);
                Platform.runLater(() -> {
                    reportActionError("Upload ảnh Primary thành công!");
                    newPrimaryImageFile.set(null);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> reportActionError("Lỗi upload ảnh: " + e.getMessage()));
            }
        }).start();
    }
    public void selectNewBackdrops(Stage ownerStage) {
        if (currentItemId == null) return;
        List<File> files = imageUpdater.chooseImages(ownerStage, true);
        uploadDroppedBackdropFiles(files);
    }
    public void deleteBackdrop(ImageInfo backdrop) {
        if (backdrop == null || currentItemId == null) return;
        reportActionError("Đang xóa backdrop index " + backdrop.getImageIndex() + "...");
        new Thread(() -> {
            try {
                imageUpdater.deleteImage(currentItemId, backdrop);
                Platform.runLater(() -> {
                    reportActionError("Xóa backdrop thành công.");
                    backdropImages.remove(backdrop);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> reportActionError("Lỗi xóa backdrop: " + e.getMessage()));
            }
        }).start();
    }
    private void reloadBackdrops() {
        if (currentItemId == null) return;
        new Thread(() -> {
            try {
                List<ImageInfo> images = itemRepository.getItemImages(currentItemId);
                List<ImageInfo> backdrops = images.stream()
                        .filter(img -> ImageType.BACKDROP.equals(img.getImageType()))
                        .collect(Collectors.toList());
                Platform.runLater(() -> {
                    backdropImages.setAll(backdrops);
                    reportActionError("Đã tải lại gallery backdrop.");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> reportActionError("Lỗi tải lại gallery: " + e.getMessage()));
            }
        }).start();
    }
    public void uploadDroppedBackdropFiles(List<File> files) {
        if (files == null || files.isEmpty() || currentItemId == null) return;

        reportActionError("Đang upload " + files.size() + " ảnh backdrop...");
        new Thread(() -> {
            try {
                for (File file : files) {
                    imageUpdater.uploadImage(currentItemId, ImageType.BACKDROP, file);
                }
                Platform.runLater(() -> {
                    reportActionError("Upload " + files.size() + " ảnh thành công. Đang tải lại gallery...");
                    reloadBackdrops();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> reportActionError("Lỗi upload backdrop: " + e.getMessage()));
            }
        }).start();
    }

    // (Getters cho Controller/Properties giữ nguyên)
    public String getCurrentItemId() { return currentItemId; }
    public EmbyService getEmbyService() { return embyService; }
    public StringProperty titleProperty() { return title; }
    public StringProperty overviewProperty() { return overview; }
    public ObservableList<TagModel> getTagItems() { return tagItems; }
    public StringProperty releaseDateProperty() { return releaseDate; }
    public StringProperty studiosProperty() { return studios; }
    public StringProperty peopleProperty() { return people; }
    public ReadOnlyBooleanProperty loadingProperty() { return loading.getReadOnlyProperty(); }
    public ReadOnlyStringProperty yearProperty() { return year.getReadOnlyProperty(); }
    public ReadOnlyStringProperty statusMessageProperty() { return statusMessage.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showStatusMessageProperty() { return showStatusMessage.getReadOnlyProperty(); }
    public ReadOnlyStringProperty taglineProperty() { return tagline.getReadOnlyProperty(); }
    public ReadOnlyStringProperty genresProperty() { return genres.getReadOnlyProperty(); }
    public ReadOnlyStringProperty runtimeProperty() { return runtime.getReadOnlyProperty(); }
    public ReadOnlyObjectProperty<Image> primaryImageProperty() { return primaryImage.getReadOnlyProperty(); }
    public ObservableList<ImageInfo> getBackdropImages() { return backdropImages; }
    public ReadOnlyStringProperty itemPathProperty() { return itemPath.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty isFolderProperty() { return isFolder.getReadOnlyProperty(); }
    public ReadOnlyStringProperty actionStatusMessageProperty() { return actionStatusMessage.getReadOnlyProperty(); }
    public BooleanProperty isDirtyProperty() { return dirtyTracker.isDirtyProperty(); }
    public ReadOnlyBooleanProperty primaryImageDirtyProperty() { return primaryImageDirty.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showTitleReviewProperty() { return importHandler.showTitleReviewProperty(); }
    public ReadOnlyBooleanProperty showOverviewReviewProperty() { return importHandler.showOverviewReviewProperty(); }
    public ReadOnlyBooleanProperty showReleaseDateReviewProperty() { return importHandler.showReleaseDateReviewProperty(); }
    public ReadOnlyBooleanProperty showStudiosReviewProperty() { return importHandler.showStudiosReviewProperty(); }
    public ReadOnlyBooleanProperty showPeopleReviewProperty() { return importHandler.showPeopleReviewProperty(); }
}