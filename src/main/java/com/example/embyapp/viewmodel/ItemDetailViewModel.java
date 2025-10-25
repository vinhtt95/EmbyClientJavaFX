package com.example.embyapp.viewmodel;

import com.example.emby.EmbyClient.ApiClient;
import com.example.emby.EmbyClient.ApiException;
import com.example.emby.EmbyClient.Java.ItemUpdateServiceApi; // (MỚI)
import com.example.emby.modelEmby.BaseItemDto;
import com.example.emby.modelEmby.ImageInfo;
import com.example.emby.modelEmby.BaseItemPerson;
import com.example.emby.modelEmby.NameLongIdPair;
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.ItemRepository;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.threeten.bp.OffsetDateTime; // (MỚI)

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap; // (MỚI)
import java.util.List;
import java.util.Map; // (MỚI)
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * (CẬP NHẬT LỚN) ViewModel cho ItemDetailView (Cột phải).
 * (CẬP NHẬT 4) Chuyển Title/Overview sang editable, thêm các trường mới.
 * (CẬP NHẬT 5) Sửa logic getStudios() để dùng NameLongIdPair và thêm error reporting.
 * (CẬP NHẬT 6) Thêm logic Import/Export/Review/Save.
 * (CẬP NHẬT 7) Sửa lỗi incompatible types (ThreeTen Instant vs Java Instant).
 * (CẬP NHẬT 8) Sửa lỗi logic clearDetails() khiến currentItemId bị null khi lưu.
 */
public class ItemDetailViewModel {

    // --- Dependencies ---
    private final ItemRepository itemRepository;
    private final EmbyService embyService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    // --- Properties cho UI Binding ---
    private final ReadOnlyBooleanWrapper loading = new ReadOnlyBooleanWrapper(false);

    // (MỚI) Thay đổi sang StringProperty để bind 2 chiều
    private final StringProperty title = new SimpleStringProperty("");
    private final StringProperty overview = new SimpleStringProperty("");

    private final ReadOnlyStringWrapper year = new ReadOnlyStringWrapper("");
    // Code đã sửa:
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

    // (MỚI) Properties cho các trường chung và chỉ-file
    private final ReadOnlyStringWrapper tags = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper releaseDate = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper studios = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper people = new ReadOnlyStringWrapper("");

    // (MỚI) Các trường quản lý trạng thái Import/Save
    private BaseItemDto originalItemDto; // Lưu trữ DTO gốc tải từ server
    private final Map<String, Object> preImportState = new HashMap<>(); // Lưu trạng thái ngay trước khi import
    private String currentItemId; // (MỚI) Lưu ID của item hiện tại

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
                this.currentItemId = null; // (MỚI) <-- Đặt ở đây là ĐÚNG
                this.originalItemDto = null; // (MỚI) <-- Đặt ở đây là ĐÚNG
                statusMessage.set("Vui lòng chọn một item từ danh sách...");
                showStatusMessage.set(true);
                loading.set(false);
            });
            return;
        }

        this.currentItemId = item.getId(); // (MỚI) Lưu ID

        Platform.runLater(() -> {
            clearDetails(); // <-- Hàm này giờ đã AN TOÀN, không xóa currentItemId
            statusMessage.set("Đang tải chi tiết cho: " + item.getName() + "...");
            showStatusMessage.set(true);
            loading.set(true);
        });

        new Thread(() -> {
            try {
                String userId = embyService.getCurrentUserId();
                String itemId = item.getId(); // Lấy ID đã lưu (an toàn)
                String serverUrl = embyService.getApiClient().getBasePath();

                if (userId == null) {
                    throw new IllegalStateException("Không thể lấy UserID. Vui lòng đăng nhập lại.");
                }

                BaseItemDto fullDetails = itemRepository.getFullItemDetails(userId, itemId);

                // (MỚI) Lưu DTO gốc
                this.originalItemDto = fullDetails;

                List<ImageInfo> images = itemRepository.getItemImages(itemId);

                // --- Xử lý dữ liệu (luồng nền) ---
                String titleText = fullDetails.getName() != null ? fullDetails.getName() : "Không có tiêu đề";
                String yearText = fullDetails.getProductionYear() != null ? String.valueOf(fullDetails.getProductionYear()) : "";
                String overviewText = fullDetails.getOverview() != null ? fullDetails.getOverview() : "";
                String taglineText = (fullDetails.getTaglines() != null && !fullDetails.getTaglines().isEmpty()) ? fullDetails.getTaglines().get(0) : "";
                String genresText = (fullDetails.getGenres() != null) ? String.join(", ", fullDetails.getGenres()) : "";
                String runtimeText = formatRuntime(fullDetails.getRunTimeTicks());

                final String path = fullDetails.getPath();
                final boolean folder = fullDetails.isIsFolder() != null && fullDetails.isIsFolder();

                // (MỚI) Trích xuất dữ liệu mới
                String tagsText = (fullDetails.getTags() != null) ? String.join(", ", fullDetails.getTags()) : "";

                String releaseDateText = "";
                if (fullDetails.getPremiereDate() != null) {
                    try {
                        // (SỬA LỖI) Dùng hàm helper mới
                        releaseDateText = dateToString(fullDetails.getPremiereDate());
                    } catch (Exception e) {
                        System.err.println("Không thể format PremiereDate: " + e.getMessage());
                    }
                }

                // (SỬA ĐỔI) Dùng NameLongIdPair::getName
                String studiosText = (fullDetails.getStudios() != null) ?
                        fullDetails.getStudios().stream()
                                .map(NameLongIdPair::getName)
                                .collect(Collectors.joining(", ")) : "";

                String peopleText = (fullDetails.getPeople() != null) ?
                        fullDetails.getPeople().stream()
                                .map(BaseItemPerson::getName)
                                .collect(Collectors.joining(", ")) : "";

                String primaryImageUrl = findImageUrl(images, "Primary", serverUrl, itemId);
                List<String> backdropUrls = buildBackdropUrls(images, serverUrl, itemId);


                // --- Cập nhật UI (JavaFX Thread) ---
                String finalReleaseDateText = releaseDateText;
                Platform.runLater(() -> {
                    title.set(titleText);
                    year.set(yearText);
                    overview.set(overviewText);
                    tagline.set(taglineText);
                    genres.set(genresText);
                    runtime.set(runtimeText);

                    itemPath.set(path != null ? path : "Không có đường dẫn");
                    isFolder.set(folder);
                    actionStatusMessage.set("");

                    // (MỚI) Cập nhật các trường mới
                    tags.set(tagsText);
                    releaseDate.set(finalReleaseDateText);
                    studios.set(studiosText);
                    people.set(peopleText);

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

                    // (MỚI) Reset tất cả các nút review
                    hideAllReviewButtons();
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

        // (MỚI)
        tags.set("");
        releaseDate.set("");
        studios.set("");
        people.set("");

        // (MỚI)
        // this.originalItemDto = null; // <<<--- ĐÃ XÓA
        // this.currentItemId = null; // <<<--- ĐÃ XÓA
        this.preImportState.clear();
        hideAllReviewButtons();
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

    // (MỚI) Các hàm helper chuyển đổi
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
        return (studios != null) ? studios.stream().map(NameLongIdPair::getName).collect(Collectors.joining(", ")) : "";
    }
    private String peopleToString(List<BaseItemPerson> people) {
        return (people != null) ? people.stream().map(BaseItemPerson::getName).collect(Collectors.joining(", ")) : "";
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
     * (MỚI) Nhận DTO từ file import và cập nhật UI để review.
     */
    public void importAndPreview(BaseItemDto importedDto) {
        if (originalItemDto == null || importedDto == null) return;

        Platform.runLater(() -> {
            preImportState.clear(); // Xóa state cũ
            hideAllReviewButtons(); // Ẩn tất cả nút

            // 1. Title (Editable)
            preImportState.put("title", title.get()); // Lưu giá trị HIỆN TẠI (có thể đã edit)
            title.set(importedDto.getName() != null ? importedDto.getName() : "");
            showTitleReview.set(true);

            // 2. Overview (Editable)
            preImportState.put("overview", overview.get());
            overview.set(importedDto.getOverview() != null ? importedDto.getOverview() : "");
            showOverviewReview.set(true);

            // 3. Tags (Read-only field)
            preImportState.put("tags_string", tags.get()); // Lưu string UI
            preImportState.put("tags_data", originalItemDto.getTags()); // Lưu data thật
            List<String> importTags = importedDto.getTags() != null ? importedDto.getTags() : Collections.emptyList();
            tags.set(listToString(importTags)); // Cập nhật UI
            originalItemDto.setTags(importTags); // Cập nhật data thật
            showTagsReview.set(true);

            // 4. Release Date (Read-only field)
            preImportState.put("releaseDate_string", releaseDate.get());
            preImportState.put("releaseDate_data", originalItemDto.getPremiereDate());
            releaseDate.set(dateToString(importedDto.getPremiereDate()));
            originalItemDto.setPremiereDate(importedDto.getPremiereDate());
            showReleaseDateReview.set(true);

            // 5. Studios (Read-only field)
            preImportState.put("studios_string", studios.get());
            preImportState.put("studios_data", originalItemDto.getStudios());
            List<NameLongIdPair> importStudios = importedDto.getStudios() != null ? importedDto.getStudios() : Collections.emptyList();
            studios.set(studiosToString(importStudios));
            originalItemDto.setStudios(importStudios);
            showStudiosReview.set(true);

            // 6. People (Read-only field)
            preImportState.put("people_string", people.get());
            preImportState.put("people_data", originalItemDto.getPeople());
            List<BaseItemPerson> importPeople = importedDto.getPeople() != null ? importedDto.getPeople() : Collections.emptyList();
            people.set(peopleToString(importPeople));
            originalItemDto.setPeople(importPeople);
            showPeopleReview.set(true);
        });
    }

    /**
     * (MỚI) Người dùng nhấn (v) - Chấp nhận thay đổi.
     */
    public void acceptImportField(String fieldName) {
        switch (fieldName) {
            case "title": showTitleReview.set(false); break;
            case "overview": showOverviewReview.set(false); break;
            case "tags": showTagsReview.set(false); break;
            case "releaseDate": showReleaseDateReview.set(false); break;
            case "studios": showStudiosReview.set(false); break;
            case "people": showPeopleReview.set(false); break;
        }
    }

    /**
     * (MỚI) Người dùng nhấn (x) - Hủy bỏ thay đổi.
     */
    @SuppressWarnings("unchecked")
    public void rejectImportField(String fieldName) {
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
                originalItemDto.setTags((List<String>) preImportState.get("tags_data"));
                showTagsReview.set(false);
                break;
            case "releaseDate":
                releaseDate.set((String) preImportState.get("releaseDate_string"));
                originalItemDto.setPremiereDate((OffsetDateTime) preImportState.get("releaseDate_data"));
                showReleaseDateReview.set(false);
                break;
            case "studios":
                studios.set((String) preImportState.get("studios_string"));
                originalItemDto.setStudios((List<NameLongIdPair>) preImportState.get("studios_data"));
                showStudiosReview.set(false);
                break;
            case "people":
                people.set((String) preImportState.get("people_string"));
                originalItemDto.setPeople((List<BaseItemPerson>) preImportState.get("people_data"));
                showPeopleReview.set(false);
                break;
        }
    }

    /**
     * (MỚI) Được gọi bởi Controller khi nhấn nút "Lưu thay đổi".
     */
    public void saveChanges() {
        if (originalItemDto == null || currentItemId == null) {
            reportActionError("Lỗi: Không có item nào đang được chọn để lưu.");
            return;
        }

        // 1. Cập nhật các trường editable (Title, Overview) vào DTO
        originalItemDto.setName(title.get());
        originalItemDto.setOverview(overview.get());
        // Các trường (Tags, Studios, People...) ĐÃ ĐƯỢC CẬP NHẬT trong DTO
        // bởi logic `importAndPreview` và `rejectImportField`.

        reportActionError("Đang lưu thay đổi lên server...");
        hideAllReviewButtons(); // Ẩn các nút (v/x) sau khi lưu

        // 2. Gọi API trong luồng nền
        new Thread(() -> {
            try {
                // (MỚI) Lấy API service từ EmbyService
                ItemUpdateServiceApi itemUpdateServiceApi = embyService.getItemUpdateServiceApi();
                if (itemUpdateServiceApi == null) {
                    throw new IllegalStateException("Không thể lấy ItemUpdateServiceApi. Vui lòng đăng nhập lại.");
                }

                // Gọi API (theo code mẫu của bạn)
                itemUpdateServiceApi.postItemsByItemid(originalItemDto, currentItemId);

                // Báo thành công
                Platform.runLater(() -> {
                    reportActionError("Đã lưu thay đổi thành công!");
                });

            } catch (ApiException e) {
                System.err.println("API Error saving item: " + e.getMessage());
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

    // (MỚI) Getters cho các trường mới
    public ReadOnlyStringProperty tagsProperty() { return tags.getReadOnlyProperty(); }
    public ReadOnlyStringProperty releaseDateProperty() { return releaseDate.getReadOnlyProperty(); }
    public ReadOnlyStringProperty studiosProperty() { return studios.getReadOnlyProperty(); }
    public ReadOnlyStringProperty peopleProperty() { return people.getReadOnlyProperty(); }


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