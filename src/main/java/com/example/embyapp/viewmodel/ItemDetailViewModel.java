package com.example.embyapp.viewmodel;

import com.example.emby.EmbyClient.ApiException;
import com.example.emby.modelEmby.BaseItemDto;
import com.example.emby.modelEmby.ImageInfo;
import com.example.emby.modelEmby.BaseItemPerson; // Giả định SDK của bạn có class này
import com.example.emby.modelEmby.NameLongIdPair; // (MỚI) Import chính xác
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.ItemRepository;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * (CẬP NHẬT LỚN) ViewModel cho ItemDetailView (Cột phải).
 * (CẬP NHẬT 4) Chuyển Title/Overview sang editable, thêm các trường mới.
 * (CẬP NHẬT 5) Sửa logic getStudios() để dùng NameLongIdPair và thêm error reporting.
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


    public ItemDetailViewModel(ItemRepository itemRepository, EmbyService embyService) {
        this.itemRepository = itemRepository;
        this.embyService = embyService;
    }

    public void setItemToDisplay(BaseItemDto item) {
        if (item == null) {
            Platform.runLater(() -> {
                clearDetails();
                statusMessage.set("Vui lòng chọn một item từ danh sách...");
                showStatusMessage.set(true);
                loading.set(false);
            });
            return;
        }

        Platform.runLater(() -> {
            clearDetails();
            statusMessage.set("Đang tải chi tiết cho: " + item.getName() + "...");
            showStatusMessage.set(true);
            loading.set(true);
        });

        new Thread(() -> {
            try {
                String userId = embyService.getCurrentUserId();
                String itemId = item.getId();
                String serverUrl = embyService.getApiClient().getBasePath();

                if (userId == null) {
                    throw new IllegalStateException("Không thể lấy UserID. Vui lòng đăng nhập lại.");
                }

                BaseItemDto fullDetails = itemRepository.getFullItemDetails(userId, itemId);
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
                        releaseDateText = fullDetails.getPremiereDate().toString();
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
        for (ImageInfo image : images) {
            if (imageType.equals(image.getImageType().getValue())) {
                return String.format("%s/Items/%s/Images/%s/%d", serverUrl, itemId, imageType, 0);
            }
        }
        return null;
    }

    private List<String> buildBackdropUrls(List<ImageInfo> images, String serverUrl, String itemId) {
        List<String> urls = new ArrayList<>();
        for (ImageInfo imgInfo : images) {
            if ("Backdrop".equals(imgInfo.getImageType().getValue()) && imgInfo.getImageIndex() != null) {
                String url = String.format("%s/Items/%s/Images/Backdrop/%d?maxWidth=400",
                        serverUrl, itemId, imgInfo.getImageIndex());
                urls.add(url);
            }
        }
        return urls;
    }


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