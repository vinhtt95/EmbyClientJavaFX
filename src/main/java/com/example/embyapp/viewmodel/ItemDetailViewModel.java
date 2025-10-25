package com.example.embyapp.viewmodel;

import com.example.emby.EmbyClient.ApiException;
import com.example.emby.EmbyClient.Java.ItemUpdateServiceApi;
import com.example.emby.modelEmby.BaseItemDto;
import com.example.emby.modelEmby.ImageInfo;
import com.example.emby.modelEmby.BaseItemPerson;
import com.example.emby.modelEmby.NameLongIdPair;
import com.example.emby.modelEmby.PersonType; // (MỚI) Import PersonType
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.ItemRepository;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneId;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * (CẬP NHẬT 10)
 * - Sửa lỗi constructor BaseItemPerson (thêm PersonType.ACTOR).
 * (CẬP NHẬT 11)
 * - Sửa logic export: Thêm trường exportFileNameTitle để lưu OriginalTitle (tên gốc)
 * thay vì Name (tên hiển thị).
 */
public class ItemDetailViewModel {

    // --- Dependencies ---
    private final ItemRepository itemRepository;
    private final EmbyService embyService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    // --- Properties cho UI Binding ---
    private final ReadOnlyBooleanWrapper loading = new ReadOnlyBooleanWrapper(false);

    // (SỬA ĐỔI) Tất cả các trường form giờ là StringProperty
    private final StringProperty title = new SimpleStringProperty("");
    private final StringProperty overview = new SimpleStringProperty("");
    private final StringProperty tags = new SimpleStringProperty("");
    private final StringProperty releaseDate = new SimpleStringProperty("");
    private final StringProperty studios = new SimpleStringProperty("");
    private final StringProperty people = new SimpleStringProperty("");

    private final ReadOnlyStringWrapper year = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper("Vui lòng chọn một item từ danh sách...");
    private final ReadOnlyBooleanWrapper showStatusMessage = new ReadOnlyBooleanWrapper(true);

    // Properties cho thông tin chi tiết
    private final ReadOnlyStringWrapper tagline = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper genres = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper runtime = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<Image> primaryImage = new ReadOnlyObjectWrapper<>(null);

    private final ObservableList<String> backdropImageUrls = FXCollections.observableArrayList();

    // Properties cho đường dẫn và nút bấm
    private final ReadOnlyStringWrapper itemPath = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper isFolder = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyStringWrapper actionStatusMessage = new ReadOnlyStringWrapper(""); // Dùng cho lỗi Mở/Phát

    // (MỚI) Quản lý trạng thái "Dirty"
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);
    // (MỚI) Lưu trạng thái gốc của các string (dùng cho dirty check)
    private String originalTitle, originalOverview, originalTags, originalReleaseDate, originalStudios, originalPeople;
    // (MỚI) Listener để theo dõi thay đổi
    private final ChangeListener<String> dirtyFlagListener = (obs, oldVal, newVal) -> checkForChanges();

    // (MỚI) Các trường quản lý trạng thái Import/Save
    private BaseItemDto originalItemDto; // Lưu trữ DTO gốc tải từ server
    private final Map<String, Object> preImportState = new HashMap<>(); // Lưu trạng thái ngay trước khi import
    private String currentItemId;
    private String exportFileNameTitle; // (MỚI) Tên file gốc (OriginalTitle) để export


    // (MỚI) Thêm các BooleanProperty cho 6 cặp nút (v/x)
    private final ReadOnlyBooleanWrapper showTitleReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showOverviewReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showTagsReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showReleaseDateReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showStudiosReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showPeopleReview = new ReadOnlyBooleanWrapper(false);


    public ItemDetailViewModel(ItemRepository itemRepository, EmbyService embyService) {
        this.itemRepository = itemRepository;
        this.embyService = embyService;
    }

    public void setItemToDisplay(BaseItemDto item) {
        if (item == null) {
            Platform.runLater(() -> {
                clearDetails();
                this.currentItemId = null;
                this.originalItemDto = null;
                statusMessage.set("Vui lòng chọn một item từ danh sách...");
                showStatusMessage.set(true);
                loading.set(false);
            });
            return;
        }

        this.currentItemId = item.getId();

        Platform.runLater(() -> {
            clearDetails(); // Xóa UI và listener cũ
            statusMessage.set("Đang tải chi tiết cho: " + item.getName() + "...");
            showStatusMessage.set(true);
            loading.set(true);
        });

        new Thread(() -> {
            try {
                String userId = embyService.getCurrentUserId();
                String itemId = this.currentItemId;
                String serverUrl = embyService.getApiClient().getBasePath();

                if (userId == null) {
                    throw new IllegalStateException("Không thể lấy UserID. Vui lòng đăng nhập lại.");
                }

                BaseItemDto fullDetails = itemRepository.getFullItemDetails(userId, itemId);
                this.originalItemDto = fullDetails;

                // (MỚI) Lấy Tên Gốc (OriginalTitle) để dùng làm tên file export
                // Fallback về Name nếu OriginalTitle không có
                final String originalTitleForExport;
                if (fullDetails.getOriginalTitle() != null && !fullDetails.getOriginalTitle().isEmpty()) {
                    originalTitleForExport = fullDetails.getOriginalTitle();
                } else {
                    originalTitleForExport = fullDetails.getName(); // Fallback
                }


                List<ImageInfo> images = itemRepository.getItemImages(itemId);

                // --- Xử lý dữ liệu (luồng nền) ---
                String titleText = fullDetails.getName() != null ? fullDetails.getName() : "";
                String yearText = fullDetails.getProductionYear() != null ? String.valueOf(fullDetails.getProductionYear()) : "";
                String overviewText = fullDetails.getOverview() != null ? fullDetails.getOverview() : "";
                String taglineText = (fullDetails.getTaglines() != null && !fullDetails.getTaglines().isEmpty()) ? fullDetails.getTaglines().get(0) : "";
                String genresText = (fullDetails.getGenres() != null) ? String.join(", ", fullDetails.getGenres()) : "";
                String runtimeText = formatRuntime(fullDetails.getRunTimeTicks());
                final String path = fullDetails.getPath();
                final boolean folder = fullDetails.isIsFolder() != null && fullDetails.isIsFolder();

                // (SỬA ĐỔI) Dùng helper để lấy text
                String tagsText = listToString(fullDetails.getTags());
                String releaseDateText = dateToString(fullDetails.getPremiereDate());
                String studiosText = studiosToString(fullDetails.getStudios());
                String peopleText = peopleToString(fullDetails.getPeople());

                String primaryImageUrl = findImageUrl(images, "Primary", serverUrl, itemId);
                List<String> backdropUrls = buildBackdropUrls(images, serverUrl, itemId);


                // --- Cập nhật UI (JavaFX Thread) ---
                Platform.runLater(() -> {
                    // (MỚI) Lưu tên file export
                    this.exportFileNameTitle = originalTitleForExport;

                    // (MỚI) Cập nhật snapshot chuỗi gốc (dùng titleText, là Name)
                    updateOriginalStrings(titleText, overviewText, tagsText, releaseDateText, studiosText, peopleText);

                    // Cập nhật UI
                    title.set(titleText);
                    overview.set(overviewText);
                    tags.set(tagsText);
                    releaseDate.set(releaseDateText);
                    studios.set(studiosText);
                    people.set(peopleText);

                    year.set(yearText);
                    tagline.set(taglineText);
                    genres.set(genresText);
                    runtime.set(runtimeText);
                    itemPath.set(path != null ? path : "Không có đường dẫn");
                    isFolder.set(folder);
                    actionStatusMessage.set("");

                    // (MỚI) Thêm listener để theo dõi
                    addDirtyListeners();

                    if (primaryImageUrl != null) {
                        Image img = new Image(primaryImageUrl, true);
                        img.exceptionProperty().addListener((obs, oldEx, newEx) -> {
                            if (newEx != null) {
                                System.err.println("LỖI TẢI ẢNH: " + primaryImageUrl);
                                newEx.printStackTrace();
                            }
                        });
                        primaryImage.set(img);
                    } else {
                        primaryImage.set(null);
                    }

                    backdropImageUrls.setAll(backdropUrls);
                    hideAllReviewButtons();
                    isDirty.set(false); // (MỚI) Reset trạng thái dirty
                    loading.set(false);
                    showStatusMessage.set(false);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    clearDetails();
                    statusMessage.set("Lỗi khi tải chi tiết: " + e.getMessage());
                    showStatusMessage.set(true);
                    loading.set(false);
                });
            }
        }).start();
    }

    /**
     * (*** SỬA LỖI ***)
     * Hàm này chỉ xóa các trường UI, KHÔNG xóa state (currentItemId, originalItemDto).
     */
    private void clearDetails() {
        // (MỚI) Xóa listener
        removeDirtyListeners();

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

        tags.set("");
        releaseDate.set("");
        studios.set("");
        people.set("");

        this.preImportState.clear();
        hideAllReviewButtons();
        isDirty.set(false); // (MỚI)
        this.exportFileNameTitle = null; // (MỚI) Xóa tên file export
    }

    // (MỚI) Thêm hàm ẩn tất cả nút review
    private void hideAllReviewButtons() {
        showTitleReview.set(false);
        showOverviewReview.set(false);
        showTagsReview.set(false);
        showReleaseDateReview.set(false);
        showStudiosReview.set(false);
        showPeopleReview.set(false);
    }

    // --- (MỚI) Logic theo dõi thay đổi (Dirty) ---

    /** (MỚI) Cập nhật snapshot chuỗi gốc */
    private void updateOriginalStrings(String title, String overview, String tags, String releaseDate, String studios, String people) {
        this.originalTitle = title;
        this.originalOverview = overview;
        this.originalTags = tags;
        this.originalReleaseDate = releaseDate;
        this.originalStudios = studios;
        this.originalPeople = people;
    }

    /** (MỚI) Hàm helper để cập nhật snapshot sau khi LƯU THÀNH CÔNG */
    private void updateOriginalStringsFromCurrent() {
        updateOriginalStrings(title.get(), overview.get(), tags.get(), releaseDate.get(), studios.get(), people.get());
    }

    /** (MỚI) Thêm listener theo dõi */
    private void addDirtyListeners() {
        title.addListener(dirtyFlagListener);
        overview.addListener(dirtyFlagListener);
        tags.addListener(dirtyFlagListener);
        releaseDate.addListener(dirtyFlagListener);
        studios.addListener(dirtyFlagListener);
        people.addListener(dirtyFlagListener);
    }

    /** (MỚI) Xóa listener theo dõi */
    private void removeDirtyListeners() {
        title.removeListener(dirtyFlagListener);
        overview.removeListener(dirtyFlagListener);
        tags.removeListener(dirtyFlagListener);
        releaseDate.removeListener(dirtyFlagListener);
        studios.removeListener(dirtyFlagListener);
        people.removeListener(dirtyFlagListener);
    }

    /** (MỚI) Kiểm tra xem có thay đổi không */
    private void checkForChanges() {
        if (originalItemDto == null) return; // Chưa tải xong

        boolean hasChanges = !Objects.equals(title.get(), originalTitle) ||
                !Objects.equals(overview.get(), originalOverview) ||
                !Objects.equals(tags.get(), originalTags) ||
                !Objects.equals(releaseDate.get(), originalReleaseDate) ||
                !Objects.equals(studios.get(), originalStudios) ||
                !Objects.equals(people.get(), originalPeople);

        isDirty.set(hasChanges);
    }

    // --- (MỚI) Các hàm helper chuyển đổi ---
    private String listToString(List<String> list) {
        return (list != null) ? String.join(", ", list) : "";
    }

    private String dateToString(OffsetDateTime date) {
        if (date == null) return "";
        try {
            // *** SỬA LỖI (FIX) ***
            // Chuyển OffsetDateTime (threetenbp) sang java.util.Date
            // bằng cách lấy epochMilli, vì toInstant() trả về org.threeten.bp.Instant
            return dateFormat.format(new java.util.Date(date.toInstant().toEpochMilli()));
        } catch (Exception e) {
            System.err.println("Lỗi format dateToString: " + e.getMessage());
            return "";
        }
    }

    private String studiosToString(List<NameLongIdPair> studios) {
        return (studios != null) ? studios.stream().map(NameLongIdPair::getName).filter(Objects::nonNull).collect(Collectors.joining(", ")) : "";
    }
    private String peopleToString(List<BaseItemPerson> people) {
        return (people != null) ? people.stream().map(BaseItemPerson::getName).filter(Objects::nonNull).collect(Collectors.joining(", ")) : "";
    }

    private String formatRuntime(Long runTimeTicks) {
        if (runTimeTicks == null || runTimeTicks == 0) return "";
        try {
            long totalMinutes = TimeUnit.NANOSECONDS.toMinutes(runTimeTicks * 100);
            if (totalMinutes == 0) return "";
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            if (hours > 0) {
                return String.format("%dh %dm", hours, minutes);
            } else {
                return String.format("%dm", minutes);
            }
        } catch (Exception e) {
            return "";
        }
    }

    private String findImageUrl(List<ImageInfo> images, String imageType, String serverUrl, String itemId) {
        if (images == null) return null; // (MỚI) Kiểm tra null
        for (ImageInfo image : images) {
            if (imageType.equals(image.getImageType().getValue())) {
                return String.format("%s/Items/%s/Images/%s/%d", serverUrl, itemId, imageType, 0);
            }
        }
        return null;
    }

    private List<String> buildBackdropUrls(List<ImageInfo> images, String serverUrl, String itemId) {
        List<String> urls = new ArrayList<>();
        if (images == null) return urls; // (MỚI) Kiểm tra null
        for (ImageInfo imgInfo : images) {
            if ("Backdrop".equals(imgInfo.getImageType().getValue()) && imgInfo.getImageIndex() != null) {
                String url = String.format("%s/Items/%s/Images/Backdrop/%d?maxWidth=400",
                        serverUrl, itemId, imgInfo.getImageIndex());
                urls.add(url);
            }
        }
        return urls;
    }


    // --- (MỚI) LOGIC IMPORT/EXPORT/SAVE ---

    /**
     * (MỚI) Lấy DTO gốc để export
     */
    public BaseItemDto getItemForExport() {
        return this.originalItemDto;
    }

    /**
     * (SỬA ĐỔI) Lấy Tiêu đề GỐC (OriginalTitle) để làm tên file export
     */
    public String getOriginalTitleForExport() {
        // Trả về tên đã lưu (OriginalTitle), fallback về title hiện tại nếu bị null
        return this.exportFileNameTitle != null ? this.exportFileNameTitle : this.title.get();
    }

    /**
     * (MỚI) Nhận DTO từ file import và cập nhật UI để review.
     */
    public void importAndPreview(BaseItemDto importedDto) {
        if (originalItemDto == null || importedDto == null) return;

        Platform.runLater(() -> {
            preImportState.clear(); // Xóa state cũ
            hideAllReviewButtons(); // Ẩn tất cả nút

            // 1. Title (Editable)
            preImportState.put("title", title.get()); // Lưu giá trị HIỆN TẠI
            title.set(importedDto.getName() != null ? importedDto.getName() : "");
            showTitleReview.set(true);

            // 2. Overview (Editable)
            preImportState.put("overview", overview.get());
            overview.set(importedDto.getOverview() != null ? importedDto.getOverview() : "");
            showOverviewReview.set(true);

            // 3. Tags (Editable)
            preImportState.put("tags_string", tags.get());
            List<String> importTags = importedDto.getTags() != null ? importedDto.getTags() : Collections.emptyList();
            tags.set(listToString(importTags));
            showTagsReview.set(true);

            // 4. Release Date (Editable)
            preImportState.put("releaseDate_string", releaseDate.get());
            releaseDate.set(dateToString(importedDto.getPremiereDate()));
            showReleaseDateReview.set(true);

            // 5. Studios (Editable)
            preImportState.put("studios_string", studios.get());
            List<NameLongIdPair> importStudios = importedDto.getStudios() != null ? importedDto.getStudios() : Collections.emptyList();
            studios.set(studiosToString(importStudios));
            showStudiosReview.set(true);

            // 6. People (Editable)
            preImportState.put("people_string", people.get());
            List<BaseItemPerson> importPeople = importedDto.getPeople() != null ? importedDto.getPeople() : Collections.emptyList();
            people.set(peopleToString(importPeople));
            showPeopleReview.set(true);

            // (MỚI) Import là một thay đổi, bật nút Save
            isDirty.set(true);
        });
    }

    /**
     * (MỚI) Người dùng nhấn (v) - Chấp nhận thay đổi.
     */
    public void acceptImportField(String fieldName) {
        // Chỉ cần ẩn nút (v/x), dữ liệu đã ở trong textfield
        switch (fieldName) {
            case "title": showTitleReview.set(false); break;
            case "overview": showOverviewReview.set(false); break;
            case "tags": showTagsReview.set(false); break;
            case "releaseDate": showReleaseDateReview.set(false); break;
            case "studios": showStudiosReview.set(false); break;
            case "people": showPeopleReview.set(false); break;
        }
        // Cập nhật snapshot gốc để (v) có nghĩa là "chấp nhận"
        updateOriginalStringsFromCurrent();
        // Kiểm tra lại (có thể các trường khác vẫn dirty)
        checkForChanges();
    }

    /**
     * (MỚI) Người dùng nhấn (x) - Hủy bỏ thay đổi.
     */
    @SuppressWarnings("unchecked")
    public void rejectImportField(String fieldName) {
        // (SỬA ĐỔI) Chỉ khôi phục UI. DTO gốc sẽ được parse khi lưu.
        switch (fieldName) {
            case "title":
                title.set((String) preImportState.get("title"));
                showTitleReview.set(false);
                break;
            case "overview":
                overview.set((String) preImportState.get("overview"));
                showOverviewReview.set(false);
                break;
            case "tags":
                tags.set((String) preImportState.get("tags_string"));
                showTagsReview.set(false);
                break;
            case "releaseDate":
                releaseDate.set((String) preImportState.get("releaseDate_string"));
                showReleaseDateReview.set(false);
                break;
            case "studios":
                studios.set((String) preImportState.get("studios_string"));
                showStudiosReview.set(false);
                break;
            case "people":
                people.set((String) preImportState.get("people_string"));
                showPeopleReview.set(false);
                break;
        }
        // Kiểm tra lại, vì hành động reject có thể làm nó hết dirty
        checkForChanges();
    }

    /**
     * (SỬA ĐỔI) Được gọi bởi Controller khi nhấn nút "Lưu thay đổi".
     * Sẽ parse dữ liệu từ UI (String) về DTO trước khi gửi.
     */
    public void saveChanges() {
        if (originalItemDto == null || currentItemId == null) {
            reportActionError("Lỗi: Không có item nào đang được chọn để lưu.");
            return;
        }

        // 1. (SỬA ĐỔI) Parse dữ liệu từ UI (String) vào DTO
        originalItemDto.setName(title.get());
        originalItemDto.setOverview(overview.get());

        // Parse Tags (String -> List<String>)
        List<String> tagsList = Arrays.stream(tags.get().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        originalItemDto.setTags(tagsList);

        // Parse Studios (String -> List<NameLongIdPair>)
        List<NameLongIdPair> studiosList = Arrays.stream(studios.get().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(name -> {
                    NameLongIdPair pair = new NameLongIdPair();
                    pair.setName(name);
                    // Lưu ý: ID sẽ bị null. Emby server thường sẽ tự khớp tên.
                    return pair;
                })
                .collect(Collectors.toList());
        originalItemDto.setStudios(studiosList);

        // Parse People (String -> List<BaseItemPerson>)
        List<BaseItemPerson> peopleList = Arrays.stream(people.get().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(name -> {
                    // *** SỬA LỖI (FIX) ***
                    // Phải cung cấp Name (String) và Type (PersonType)
                    // Chúng ta mặc định là Actor
                    return new BaseItemPerson(name, PersonType.ACTOR);
                })
                .collect(Collectors.toList());
        originalItemDto.setPeople(peopleList);

        // Parse Date (String -> OffsetDateTime)
        try {
            Date parsedDate = dateFormat.parse(releaseDate.get());
            // Dùng time và ZoneId của threetenbp
            org.threeten.bp.Instant threetenInstant = org.threeten.bp.Instant.ofEpochMilli(parsedDate.getTime());
            OffsetDateTime odt = OffsetDateTime.ofInstant(threetenInstant, org.threeten.bp.ZoneId.systemDefault());
            originalItemDto.setPremiereDate(odt);
        } catch (Exception e) {
            System.err.println("Không thể parse ngày: " + releaseDate.get() + ". Sẽ set thành null.");
            originalItemDto.setPremiereDate(null); // Set null nếu định dạng sai
        }

        reportActionError("Đang lưu thay đổi lên server...");
        hideAllReviewButtons();

        // 2. Gọi API trong luồng nền
        new Thread(() -> {
            try {
                ItemUpdateServiceApi itemUpdateServiceApi = embyService.getItemUpdateServiceApi();
                if (itemUpdateServiceApi == null) {
                    throw new IllegalStateException("Không thể lấy ItemUpdateServiceApi. Vui lòng đăng nhập lại.");
                }

                itemUpdateServiceApi.postItemsByItemid(originalItemDto, currentItemId);

                // Báo thành công
                Platform.runLater(() -> {
                    reportActionError("Đã lưu thay đổi thành công!");
                    isDirty.set(false); // (MỚI) Tắt nút Lưu
                    updateOriginalStringsFromCurrent(); // (MỚI) Cập nhật snapshot gốc
                });

            } catch (ApiException e) {
                System.err.println("API Error saving item: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> reportActionError("Lỗi API khi lưu: " + e.getMessage()));
            } catch (Exception e) {
                System.err.println("Generic Error saving item: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> reportActionError("Lỗi không xác định khi lưu: " + e.getMessage()));
            }
        }).start();
    }


    // --- (MỚI) Getters cho các BooleanProperty (v/x) ---
    public ReadOnlyBooleanProperty showTitleReviewProperty() { return showTitleReview.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showOverviewReviewProperty() { return showOverviewReview.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showTagsReviewProperty() { return showTagsReview.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showReleaseDateReviewProperty() { return showReleaseDateReview.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showStudiosReviewProperty() { return showStudiosReview.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showPeopleReviewProperty() { return showPeopleReview.getReadOnlyProperty(); }


    // --- Getters cho Properties ---

    // (MỚI)
    public BooleanProperty isDirtyProperty() { return isDirty; }

    public ReadOnlyBooleanProperty loadingProperty() {
        return loading.getReadOnlyProperty();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public ReadOnlyStringProperty yearProperty() {
        return year.getReadOnlyProperty();
    }

    public StringProperty overviewProperty() {
        return overview;
    }

    public ReadOnlyStringProperty statusMessageProperty() {
        return statusMessage.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty showStatusMessageProperty() {
        return showStatusMessage.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty taglineProperty() {
        return tagline.getReadOnlyProperty();
    }
    public ReadOnlyStringProperty genresProperty() {
        return genres.getReadOnlyProperty();
    }
    public ReadOnlyStringProperty runtimeProperty() {
        return runtime.getReadOnlyProperty();
    }
    public ReadOnlyObjectProperty<Image> primaryImageProperty() {
        return primaryImage.getReadOnlyProperty();
    }
    public ObservableList<String> getBackdropImageUrls() {
        return backdropImageUrls;
    }

    // Getters cho Path
    public ReadOnlyStringProperty itemPathProperty() { return itemPath.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty isFolderProperty() { return isFolder.getReadOnlyProperty(); }
    public ReadOnlyStringProperty actionStatusMessageProperty() { return actionStatusMessage.getReadOnlyProperty(); }

    // (SỬA ĐỔI) Getters cho các trường mới (không còn ReadOnly)
    public StringProperty tagsProperty() { return tags; }
    public StringProperty releaseDateProperty() { return releaseDate; }
    public StringProperty studiosProperty() { return studios; }
    public StringProperty peopleProperty() { return people; }


    // --- Các hàm Setter/Reporter cho Action Error (MỚI) ---
    // Được ItemGridController và ItemDetailController gọi để báo cáo lỗi/status

    /**
     * Báo cáo lỗi hoặc thông báo lên thanh status phụ (dưới nút Mở/Phát).
     */
    public void reportActionError(String errorMessage) {
        Platform.runLater(() -> this.actionStatusMessage.set(errorMessage));
    }

    /**
     * Xóa thông báo lỗi/status hành động.
     */
    public void clearActionError() {
        Platform.runLater(() -> this.actionStatusMessage.set(""));
    }
}