package com.example.embyapp.viewmodel;

import embyclient.ApiException;
import embyclient.api.ItemUpdateServiceApi;
import embyclient.model.*;
import com.example.embyapp.controller.AddTagDialogController; // <-- THÊM IMPORT
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.I18nManager;
import com.example.embyapp.service.ItemRepository;
import com.example.embyapp.service.ItemService; // Import ItemService
import com.example.embyapp.service.RequestEmby; // Import RequestEmby
import com.example.embyapp.viewmodel.detail.*;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.control.TreeItem; // Import TreeItem
import javafx.stage.Stage;
import com.google.gson.GsonBuilder;
import java.time.OffsetDateTime;
import embyclient.JSON.OffsetDateTimeTypeAdapter;

import javafx.beans.property.ObjectProperty;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
// (*** THÊM CÁC IMPORT CẦN THIẾT CHO LOGIC MỚI ***)
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * ViewModel for the Item Detail view.
 * (CẬP NHẬT 36) Thêm logic Sao chép nhanh (Quick Copy).
 */
public class ItemDetailViewModel {

    public enum CloneType {
        TAGS, STUDIOS, PEOPLE, GENRES
    }

    private final ItemRepository itemRepository;
    private final EmbyService embyService;
    private final ItemDetailLoader loader;
    private final ItemDetailSaver saver;
    private final ItemDetailDirtyTracker dirtyTracker;
    private final ItemDetailImportHandler importHandler;
    private final ItemImageUpdater imageUpdater;
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeTypeAdapter())
            .create();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private final I18nManager i18n;

    private BaseItemDto originalItemDto;
    private String currentItemId;
    private String exportFileNameTitle;
    private final ObjectProperty<File> newPrimaryImageFile = new SimpleObjectProperty<>(null);
    private final ObjectProperty<TreeItem<BaseItemDto>> selectedTreeItem = new SimpleObjectProperty<>(null);

    private final ReadOnlyBooleanWrapper loading = new ReadOnlyBooleanWrapper(false);
    private final StringProperty title = new SimpleStringProperty("");
    private final StringProperty originalTitle = new SimpleStringProperty("");
    private final ObjectProperty<Float> criticRating = new SimpleObjectProperty<>(null);
    private final StringProperty overview = new SimpleStringProperty("");
    private final ObservableList<TagModel> tagItems = FXCollections.observableArrayList();
    private final StringProperty releaseDate = new SimpleStringProperty("");
    private final ObservableList<TagModel> studiosItems = FXCollections.observableArrayList();
    private final ObservableList<TagModel> peopleItems = FXCollections.observableArrayList();
    private final ObservableList<TagModel> genresItems = FXCollections.observableArrayList();
    private final ReadOnlyStringWrapper year = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper tagline = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper genres = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<Image> primaryImage = new ReadOnlyObjectWrapper<>(null);
    private final ObservableList<ImageInfo> backdropImages = FXCollections.observableArrayList();
    private final ReadOnlyStringWrapper itemPath = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper isFolder = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyStringWrapper statusMessage;
    private final ReadOnlyBooleanWrapper showStatusMessage = new ReadOnlyBooleanWrapper(true);
    private final ReadOnlyStringWrapper actionStatusMessage = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper primaryImageDirty = new ReadOnlyBooleanWrapper(false);
    private final ObjectProperty<Boolean> popOutRequest = new SimpleObjectProperty<>(null);


    public ItemDetailViewModel(ItemRepository itemRepository, EmbyService embyService) {
        this.itemRepository = itemRepository;
        this.embyService = embyService;
        this.i18n = I18nManager.getInstance();

        this.loader = new ItemDetailLoader(itemRepository, embyService);
        this.saver = new ItemDetailSaver(embyService);
        this.dirtyTracker = new ItemDetailDirtyTracker(this);
        this.importHandler = new ItemDetailImportHandler(this, this.dirtyTracker);
        this.imageUpdater = new ItemImageUpdater(embyService);
        this.statusMessage = new ReadOnlyStringWrapper(i18n.getString("itemDetailViewModel", "statusDefault"));

        newPrimaryImageFile.addListener((obs, oldVal, newVal) -> {
            primaryImageDirty.set(newVal != null);
        });
    }

    public void setItemToDisplay(BaseItemDto item) {
        if (item == null) {
            Platform.runLater(this::clearAllDetailsUI);
            return;
        }
        final String newItemId = item.getId();
        Platform.runLater(() -> {
            clearAllDetailsUI();
            statusMessage.set(i18n.getString("itemDetailViewModel", "statusLoading", item.getName()));
            showStatusMessage.set(true);
            loading.set(true);
        });
        new Thread(() -> {
            try {
                String userId = embyService.getCurrentUserId();
                if (userId == null) {
                    throw new IllegalStateException(i18n.getString("itemDetailViewModel", "errorNoUser"));
                }
                ItemDetailLoader.LoadResult result = loader.loadItemData(userId, newItemId);

                BaseItemDto loadedDto = result.getFullDetails();
                String loadedItemId = newItemId;
                List<ImageInfo> backdrops = result.getBackdropImages();

                Platform.runLater(() -> {
                    this.originalItemDto = loadedDto;
                    this.currentItemId = loadedItemId;
                    this.exportFileNameTitle = result.getOriginalTitleForExport();

                    title.set(result.getTitleText());
                    originalTitle.set(result.getOriginalTitleForExport());
                    criticRating.set(result.getCriticRating());
                    overview.set(result.getOverviewText());
                    tagItems.setAll(result.getTagItems());
                    releaseDate.set(result.getReleaseDateText());
                    studiosItems.setAll(result.getStudioItems());
                    peopleItems.setAll(result.getPeopleItems());
                    genresItems.setAll(result.getGenreItems());
                    year.set(result.getYearText());
                    tagline.set(result.getTaglineText());
                    genres.set(result.getGenresText());
                    itemPath.set(result.getPathText());
                    isFolder.set(result.isFolder());
                    actionStatusMessage.set("");

                    if (result.getPrimaryImageUrl() != null) {
                        Image img = new Image(result.getPrimaryImageUrl(), true);
                        img.exceptionProperty().addListener((obs, oldEx, newEx) -> {
                            if (newEx != null) System.err.println("LỖI TẢI ẢNH: " + result.getPrimaryImageUrl());
                        });
                        primaryImage.set(img);
                    } else {
                        primaryImage.set(null);
                    }
                    backdropImages.setAll(backdrops);

                    dirtyTracker.startTracking(result.getOriginalStrings());
                    importHandler.hideAllReviewButtons();
                    loading.set(false);
                    showStatusMessage.set(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    clearAllDetailsUI();
                    statusMessage.set(i18n.getString("itemDetailViewModel", "errorLoad", e.getMessage()));
                    showStatusMessage.set(true);
                    loading.set(false);
                });
            }
        }).start();
    }

    private void clearAllDetailsUI() {
        dirtyTracker.stopTracking();
        title.set("");
        originalTitle.set("");
        criticRating.set(null);
        year.set("");
        overview.set("");
        tagline.set("");
        genres.set("");
        primaryImage.set(null);
        backdropImages.clear();
        itemPath.set("");
        isFolder.set(false);
        actionStatusMessage.set("");
        tagItems.clear();
        releaseDate.set("");
        studiosItems.clear();
        peopleItems.clear();
        genresItems.clear();
        importHandler.clearState();
        currentItemId = null;
        originalItemDto = null;
        exportFileNameTitle = null;
        newPrimaryImageFile.set(null);
    }

    public void saveChanges() {
        final BaseItemDto dtoAtSaveTime = this.originalItemDto;
        final String idAtSaveTime = this.currentItemId;

        if (dtoAtSaveTime == null || idAtSaveTime == null) {
            reportActionError(i18n.getString("itemDetailViewModel", "errorSave"));
            return;
        }
        reportActionError(i18n.getString("itemDetailViewModel", "statusSaving"));
        importHandler.hideAllReviewButtons();

        final String finalTitle = this.title.get();
        final String finalOriginalTitle = this.originalTitle.get();
        final Float finalCriticRating = this.criticRating.get();
        final String finalOverview = this.overview.get();
        final String finalReleaseDate = this.releaseDate.get();
        final List<TagModel> finalTagItems = List.copyOf(this.tagItems);
        final List<TagModel> finalStudiosItems = List.copyOf(this.studiosItems);
        final List<TagModel> finalPeopleItems = List.copyOf(this.peopleItems);
        final List<TagModel> finalGenresItems = List.copyOf(this.genresItems);

        final boolean isSavingAfterImport = importHandler.wasImportInProgress();
        final Set<String> acceptedFields = isSavingAfterImport ? importHandler.getAcceptedFields() : null;

        new Thread(() -> {
            try {
                BaseItemDto dtoToSendToApi;
                if (isSavingAfterImport) {
                    System.out.println(i18n.getString("itemDetailViewModel", "statusSavingImport"));
                    dtoToSendToApi = createDtoWithAcceptedChanges(dtoAtSaveTime, acceptedFields);
                } else {
                    System.out.println(i18n.getString("itemDetailViewModel", "statusSavingManual"));
                    ItemDetailSaver.SaveRequest manualSaveRequest = new ItemDetailSaver.SaveRequest(
                            dtoAtSaveTime, idAtSaveTime, finalTitle, finalOverview,
                            finalTagItems, finalReleaseDate, finalStudiosItems, finalPeopleItems,
                            finalGenresItems, finalCriticRating, finalOriginalTitle
                    );
                    dtoToSendToApi = saver.parseUiToDto(manualSaveRequest);

                    List<NameLongIdPair> genreItemsToSave = finalGenresItems.stream()
                            .map(tagModel -> {
                                NameLongIdPair pair = new NameLongIdPair();
                                pair.setName(tagModel.serialize());
                                pair.setId(null);
                                return pair;
                            })
                            .collect(Collectors.toList());
                    dtoToSendToApi.setGenreItems(genreItemsToSave);
                }

                ItemUpdateServiceApi itemUpdateServiceApi = embyService.getItemUpdateServiceApi();
                if (itemUpdateServiceApi == null) {
                    throw new IllegalStateException(i18n.getString("itemDetailViewModel", "errorApiUpdate"));
                }
                itemUpdateServiceApi.postItemsByItemid(dtoToSendToApi, idAtSaveTime);

                Platform.runLater(() -> {
                    reportActionError(i18n.getString("itemDetailViewModel", "statusSaveSuccess"));
                    if (idAtSaveTime.equals(this.currentItemId)) {
                        this.originalItemDto = gson.fromJson(gson.toJson(dtoToSendToApi), BaseItemDto.class);
                        dirtyTracker.updateOriginalStringsFromCurrent();
                    } else {
                        System.out.println("Save successful, but item changed during save. Not updating original DTO/dirty tracker baseline.");
                    }
                    importHandler.clearState();
                });
            } catch (ApiException e) {
                System.err.println("API Error saving item: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> reportActionError(i18n.getString("itemDetailViewModel", "errorApiSave", e.getCode(), e.getMessage())));
            } catch (Exception e) {
                System.err.println("Generic Error saving item: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> reportActionError(i18n.getString("itemDetailViewModel", "errorGenericSave", e.getMessage())));
            }
        }).start();
    }

    private BaseItemDto createDtoWithAcceptedChanges(BaseItemDto originalDtoAtSaveTime, Set<String> acceptedFields) {
        if (originalDtoAtSaveTime == null) {
            throw new RuntimeException("originalDtoAtSaveTime không được null khi tạo DTO thay đổi.");
        }
        BaseItemDto dtoCopy = gson.fromJson(gson.toJson(originalDtoAtSaveTime), BaseItemDto.class);
        System.out.println("Accepted fields to save: " + acceptedFields);

        if (acceptedFields.contains("title")) { dtoCopy.setName(title.get()); }
        if (acceptedFields.contains("originalTitle")) { dtoCopy.setOriginalTitle(originalTitle.get()); }
        if (acceptedFields.contains("criticRating")) { dtoCopy.setCriticRating(criticRating.get()); }
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
                Instant instant = Instant.ofEpochMilli(parsedDate.getTime());
                OffsetDateTime odt = OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
                dtoCopy.setPremiereDate(odt);
            } catch (ParseException e) {
                System.err.println("Không thể parse ngày (save accepted): " + releaseDate.get() + ". Sẽ giữ giá trị gốc.");
            }
        }
        if (acceptedFields.contains("studios")) {
            List<NameLongIdPair> studiosList = studiosItems.stream()
                    .map(tagModel -> {
                        NameLongIdPair pair = new NameLongIdPair();
                        pair.setName(tagModel.serialize());
                        pair.setId(null);
                        return pair;
                    })
                    .collect(Collectors.toList());
            dtoCopy.setStudios(studiosList);
        }
        if (acceptedFields.contains("people")) {
            List<BaseItemPerson> peopleList = peopleItems.stream()
                    .map(tagModel -> {
                        BaseItemPerson person = new BaseItemPerson();
                        person.setName(tagModel.serialize());
                        person.setType(PersonType.ACTOR);
                        return person;
                    })
                    .collect(Collectors.toList());
            dtoCopy.setPeople(peopleList);
        }
        if (acceptedFields.contains("genres")) {
            List<NameLongIdPair> genreItemsToSave = genresItems.stream()
                    .map(tagModel -> {
                        NameLongIdPair pair = new NameLongIdPair();
                        pair.setName(tagModel.serialize());
                        pair.setId(null);
                        return pair;
                    })
                    .collect(Collectors.toList());
            dtoCopy.setGenreItems(genreItemsToSave);
        }
        return dtoCopy;
    }

    private String dateToString(OffsetDateTime date) {
        if (date == null) return "";
        try {
            return dateFormat.format(new java.util.Date(date.toInstant().toEpochMilli()));
        } catch (Exception e) { return ""; }
    }

    /**
     * Được gọi từ Controller, dùng OriginalTitle để lấy ngày phát hành.
     */
    public void fetchReleaseDate() {
        final String code = originalTitle.get();
        if (code == null || code.trim().isEmpty()) {
            reportActionError("Tiêu đề gốc rỗng, không thể tìm ngày.");
            return;
        }
        reportActionError(i18n.getString("itemDetailView", "statusFetchingDate", code));
        new Thread(() -> {
            try {
                OffsetDateTime resultDate = itemRepository.fetchReleaseDateByCode(code);
                if (resultDate != null) {
                    String dateString = dateToString(resultDate);
                    Platform.runLater(() -> {
                        this.releaseDate.set(dateString);
                        reportActionError(i18n.getString("itemDetailView", "statusFetchDateSuccess"));
                    });
                } else {
                    Platform.runLater(() -> {
                        reportActionError(i18n.getString("itemDetailView", "statusFetchDateNotFound"));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    reportActionError(i18n.getString("itemDetailView", "errorFetchDate", e.getMessage()));
                });
            }
        }).start();
    }


    /**
     * Nhân bản một thuộc tính (Tags, Studios, v.v.) từ item hiện tại (detail)
     * sang TẤT CẢ các item con (recursive) của item đang chọn trong cây (tree).
     *
     * @param cloneType Loại thuộc tính cần nhân bản.
     */
    public void clonePropertiesToTreeChildren(CloneType cloneType) {
        final TreeItem<BaseItemDto> treeItem = selectedTreeItem.get();
        final BaseItemDto sourceDto = this.originalItemDto;
        final String userId = embyService.getCurrentUserId();

        if (treeItem == null || treeItem.getValue() == null) {
            reportActionError(i18n.getString("itemDetailView", "cloneErrorNoParent"));
            return;
        }
        if (sourceDto == null) {
            reportActionError(i18n.getString("itemDetailView", "cloneErrorNoSource"));
            return;
        }
        if (userId == null) {
            reportActionError(i18n.getString("itemDetailViewModel", "errorNoUser"));
            return;
        }

        final String parentID = treeItem.getValue().getId();
        final String sourceID = sourceDto.getId();
        final String typeName = cloneType.toString();

        reportActionError(i18n.getString("itemDetailView", "cloneStatusStart", typeName, "..."));

        new Thread(() -> {
            try {
                ItemService itemService = new ItemService(userId);
                RequestEmby requestEmby = new RequestEmby();
                int count = 0;

                switch (cloneType) {
                    case TAGS:
                        count = requestEmby.copyTags(itemService, sourceID, parentID);
                        break;
                    case STUDIOS:
                        count = requestEmby.copyStudio(itemService, sourceID, parentID);
                        break;
                    case PEOPLE:
                        count = requestEmby.copyPeople(itemService, sourceID, parentID);
                        break;
                    case GENRES:
                        count = requestEmby.copyGenres(itemService, sourceID, parentID);
                        break;
                }

                final int finalCount = count;
                Platform.runLater(() -> {
                    if (finalCount == 0) {
                        reportActionError(i18n.getString("itemDetailView", "cloneErrorNoChildren"));
                    } else {
                        reportActionError(i18n.getString("itemDetailView", "cloneStatusSuccess", typeName, finalCount));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> reportActionError(i18n.getString("itemDetailView", "cloneStatusError", e.getMessage())));
            }
        }).start();
    }

    // (*** BẮT ĐẦU PHƯƠNG THỨC MỚI CHO SAO CHÉP NHANH ***)
    /**
     * Lấy thuộc tính từ một item nguồn (theo ID) và merge (trộn) vào
     * item hiện tại đang hiển thị trên UI.
     *
     * @param sourceItemId ID của item nguồn (để sao chép TỪ)
     * @param context Loại thuộc tính cần sao chép (TAG, STUDIO, v.v.)
     */
    public void copyPropertiesFromItem(String sourceItemId, AddTagDialogController.SuggestionContext context) {
        final String userId = embyService.getCurrentUserId();
        if (userId == null) {
            reportActionError(i18n.getString("itemDetailViewModel", "errorNoUser"));
            return;
        }
        if (currentItemId == null) {
            reportActionError(i18n.getString("itemDetailViewModel", "errorSave"));
            return;
        }
        // Báo cáo trạng thái đang tải
        reportActionError(i18n.getString("addTagDialog", "copyStatusLoading", sourceItemId));

        new Thread(() -> {
            try {
                // 1. Lấy thông tin đầy đủ của item NGUỒN
                BaseItemDto sourceDto = itemRepository.getFullItemDetails(userId, sourceItemId);
                if (sourceDto == null) {
                    throw new Exception(i18n.getString("addTagDialog", "copyErrorNotFound"));
                }

                // 2. Phân tích (Parse) danh sách thuộc tính liên quan từ DTO nguồn
                List<TagModel> sourceTagsToCopy = new ArrayList<>();

                switch (context) {
                    case TAG:
                        if (sourceDto.getTagItems() != null) {
                            for (NameLongIdPair tagPair : sourceDto.getTagItems()) {
                                if (tagPair.getName() != null) {
                                    sourceTagsToCopy.add(TagModel.parse(tagPair.getName()));
                                }
                            }
                        }
                        break;
                    case STUDIO:
                        if (sourceDto.getStudios() != null) {
                            sourceTagsToCopy = sourceDto.getStudios().stream()
                                    .map(NameLongIdPair::getName)
                                    .filter(Objects::nonNull)
                                    .map(TagModel::parse)
                                    .collect(Collectors.toList());
                        }
                        break;
                    case PEOPLE:
                        if (sourceDto.getPeople() != null) {
                            sourceTagsToCopy = sourceDto.getPeople().stream()
                                    .map(BaseItemPerson::getName)
                                    .filter(Objects::nonNull)
                                    .map(TagModel::parse)
                                    .collect(Collectors.toList());
                        }
                        break;
                    case GENRE:
                        if (sourceDto.getGenres() != null) {
                            sourceTagsToCopy = sourceDto.getGenres().stream()
                                    .filter(Objects::nonNull)
                                    .map(TagModel::parse)
                                    .collect(Collectors.toList());
                        }
                        break;
                }

                final List<TagModel> finalSourceTags = sourceTagsToCopy; // Biến final để dùng trong lambda

                // 3. Quay lại UI Thread để merge (trộn)
                Platform.runLater(() -> {
                    ObservableList<TagModel> destinationList;
                    // Chọn đúng danh sách ĐÍCH (của item hiện tại đang sửa)
                    switch (context) {
                        case TAG: destinationList = tagItems; break;
                        case STUDIO: destinationList = studiosItems; break;
                        case PEOPLE: destinationList = peopleItems; break;
                        case GENRE: destinationList = genresItems; break;
                        default: return; // Không làm gì nếu context lạ
                    }

                    // Dùng Set để lọc trùng lặp
                    Set<TagModel> existingTags = new HashSet<>(destinationList);
                    int addedCount = 0;

                    for (TagModel newTag : finalSourceTags) {
                        // Phương thức .add() của Set trả về true nếu tag đó chưa tồn tại
                        if (existingTags.add(newTag)) {
                            destinationList.add(newTag); // Thêm vào danh sách UI
                            addedCount++;
                        }
                    }

                    // Báo cáo thành công
                    reportActionError(i18n.getString("addTagDialog", "copySuccessStatus", addedCount, sourceItemId));
                });

            } catch (Exception e) {
                e.printStackTrace();
                // Báo cáo lỗi
                Platform.runLater(() -> reportActionError(i18n.getString("addTagDialog", "copyErrorStatus", sourceItemId, e.getMessage())));
            }
        }).start();
    }
    // (*** KẾT THÚC PHƯƠNG THỨC MỚI ***)


    public void importAndPreview(BaseItemDto importedDto) { if (originalItemDto == null) return; importHandler.importAndPreview(importedDto); }
    public void acceptImportField(String fieldName) { importHandler.acceptImportField(fieldName); }
    public void rejectImportField(String fieldName) { importHandler.rejectImportField(fieldName); }
    public void markAsDirtyByAccept() { dirtyTracker.forceDirty(); }
    public void addTag(TagModel newTag) { if (newTag != null) { tagItems.add(newTag); } }
    public void removeTag(TagModel tagToRemove) { if (tagToRemove != null) { tagItems.remove(tagToRemove); } }
    public void addStudio(TagModel newStudio) { if (newStudio != null) { studiosItems.add(newStudio); } }
    public void removeStudio(TagModel studioToRemove) { if (studioToRemove != null) { studiosItems.remove(studioToRemove); } }
    public void addPerson(TagModel newPerson) { if (newPerson != null) { peopleItems.add(newPerson); } }
    public void removePerson(TagModel personToRemove) { if (personToRemove != null) { peopleItems.remove(personToRemove); } }
    public void addGenre(TagModel newGenre) { if (newGenre != null) { genresItems.add(newGenre); } }
    public void removeGenre(TagModel genreToRemove) { if (genreToRemove != null) { genresItems.remove(genreToRemove); } }
    public BaseItemDto getItemForExport() { return this.originalItemDto; }
    public String getOriginalTitleForExport() { return this.exportFileNameTitle != null ? this.exportFileNameTitle : this.title.get(); }
    public void reportActionError(String errorMessage) { Platform.runLater(() -> this.actionStatusMessage.set(errorMessage)); }
    public void clearActionError() { Platform.runLater(() -> this.actionStatusMessage.set("")); }


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
                reportActionError(i18n.getString("itemDetailViewModel", "errorImagePreview"));
            }
        }
    }

    /**
     * Uploads the selected primary image.
     */
    public void saveNewPrimaryImage() {
        File fileToSave = newPrimaryImageFile.get();
        if (fileToSave == null || currentItemId == null) {
            reportActionError(i18n.getString("itemDetailViewModel", "errorNoImageOrItem"));
            return;
        }
        reportActionError(i18n.getString("itemDetailViewModel", "statusUploadingPrimary"));
        new Thread(() -> {
            try {
                imageUpdater.uploadImage(currentItemId, ImageType.PRIMARY, fileToSave);
                Platform.runLater(() -> {
                    reportActionError(i18n.getString("itemDetailViewModel", "statusUploadPrimarySuccess"));
                    newPrimaryImageFile.set(null);
                    reloadPrimaryImage();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> reportActionError(i18n.getString("itemDetailViewModel", "errorUploadPrimary", e.getMessage())));
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
        reportActionError(i18n.getString("itemDetailViewModel", "statusDeletingBackdrop", backdrop.getImageIndex()));
        new Thread(() -> {
            try {
                imageUpdater.deleteImage(currentItemId, backdrop);
                Platform.runLater(() -> {
                    reportActionError(i18n.getString("itemDetailViewModel", "statusDeleteBackdropSuccess"));
                    reloadBackdrops();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> reportActionError(i18n.getString("itemDetailViewModel", "errorDeleteBackdrop", e.getMessage())));
            }
        }).start();
    }

    private void reloadPrimaryImage() {
        if (currentItemId == null) return;
        final BaseItemDto itemBeforeReload = this.originalItemDto;
        if (itemBeforeReload == null) return;
        new Thread(() -> {
            try {
                String userId = embyService.getCurrentUserId();
                if (userId == null) throw new IllegalStateException(i18n.getString("itemDetailViewModel", "errorLoadUserID"));
                BaseItemDto updatedDto = itemRepository.getFullItemDetails(userId, currentItemId);
                String serverUrl = embyService.getApiClient().getBasePath();
                String primaryImageUrl = null;
                if (updatedDto.getImageTags() != null && updatedDto.getImageTags().containsKey("Primary")) {
                    String tag = updatedDto.getImageTags().get("Primary");
                    primaryImageUrl = String.format("%s/Items/%s/Images/Primary?tag=%s&maxWidth=%d&quality=90",
                            serverUrl, currentItemId, tag, 600);
                }
                final String finalPrimaryImageUrl = primaryImageUrl;
                Platform.runLater(() -> {
                    if (currentItemId != null && currentItemId.equals(updatedDto.getId())) {
                        this.originalItemDto = updatedDto;
                    }
                    if (finalPrimaryImageUrl != null) {
                        Image img = new Image(finalPrimaryImageUrl, true);
                        primaryImage.set(img);
                    } else {
                        primaryImage.set(null);
                    }
                });
            } catch (Exception e) {
                System.err.println("Lỗi khi tải lại ảnh primary: " + e.getMessage());
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
                    reportActionError(i18n.getString("itemDetailViewModel", "statusReloadBackdrop"));
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> reportActionError(i18n.getString("itemDetailViewModel", "errorReloadBackdrop", e.getMessage())));
            }
        }).start();
    }

    /**
     * Uploads dropped backdrop files.
     */
    public void uploadDroppedBackdropFiles(List<File> files) {
        if (files == null || files.isEmpty() || currentItemId == null) return;
        reportActionError(i18n.getString("itemDetailViewModel", "statusUploadingBackdrops", files.size()));
        new Thread(() -> {
            try {
                for (int i = 0; i < files.size(); i++) {
                    File file = files.get(i);
                    final int current = i + 1;
                    Platform.runLater(() -> reportActionError(
                            i18n.getString("itemDetailViewModel", "statusUploadingBackdropProgress", current, files.size(), file.getName())
                    ));
                    imageUpdater.uploadImage(currentItemId, ImageType.BACKDROP, file);
                }
                Platform.runLater(() -> {
                    reportActionError(i18n.getString("itemDetailViewModel", "statusUploadBackdropSuccess", files.size()));
                    reloadBackdrops();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> reportActionError(i18n.getString("itemDetailViewModel", "errorUploadBackdrop", e.getMessage())));
            }
        }).start();
    }

    /**
     * Gửi tín hiệu yêu cầu MainController mở dialog pop-out.
     */
    public void requestPopOut() {
        popOutRequest.set(true);
    }

    public ObjectProperty<Boolean> popOutRequestProperty() {
        return popOutRequest;
    }

    public ObjectProperty<TreeItem<BaseItemDto>> selectedTreeItemProperty() {
        return selectedTreeItem;
    }

    // Getters for Controller/Properties
    public ObjectProperty<Float> criticRatingProperty() { return criticRating; }
    public ObservableList<TagModel> getGenreItems() { return genresItems; }
    public String getCurrentItemId() { return currentItemId; }
    public EmbyService getEmbyService() { return embyService; }
    public StringProperty titleProperty() { return title; }
    public StringProperty originalTitleProperty() { return originalTitle; }
    public StringProperty overviewProperty() { return overview; }
    public ObservableList<TagModel> getTagItems() { return tagItems; }
    public StringProperty releaseDateProperty() { return releaseDate; }
    public ObservableList<TagModel> getStudioItems() { return studiosItems; }
    public ObservableList<TagModel> getPeopleItems() { return peopleItems; }
    public ReadOnlyBooleanProperty loadingProperty() { return loading.getReadOnlyProperty(); }
    public ReadOnlyStringProperty yearProperty() { return year.getReadOnlyProperty(); }
    public ReadOnlyStringProperty statusMessageProperty() { return statusMessage.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showStatusMessageProperty() { return showStatusMessage.getReadOnlyProperty(); }
    public ReadOnlyStringProperty taglineProperty() { return tagline.getReadOnlyProperty(); }
    public ReadOnlyStringProperty genresProperty() { return genres.getReadOnlyProperty(); }
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
    public ReadOnlyBooleanProperty showOriginalTitleReviewProperty() { return importHandler.showOriginalTitleReviewProperty(); }
    public ReadOnlyBooleanProperty showStudiosReviewProperty() { return importHandler.showStudiosReviewProperty(); }
    public ReadOnlyBooleanProperty showPeopleReviewProperty() { return importHandler.showPeopleReviewProperty(); }
    public ReadOnlyBooleanProperty showGenresReviewProperty() { return importHandler.showGenresReviewProperty(); }
    public ReadOnlyBooleanProperty showTagsReviewProperty() { return importHandler.showTagsReviewProperty(); }
    public ReadOnlyBooleanProperty showCriticRatingReviewProperty() { return importHandler.showCriticRatingReviewProperty(); }
}