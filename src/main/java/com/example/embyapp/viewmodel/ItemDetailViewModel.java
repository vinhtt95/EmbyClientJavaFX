package com.example.embyapp.viewmodel;

import com.example.emby.EmbyClient.ApiException;
import com.example.emby.modelEmby.BaseItemDto;
import com.example.emby.modelEmby.ImageInfo;
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.ItemRepository;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
// SỬA ĐỔI: Xóa import stream
// import java.util.stream.Collectors;

/**
 * (CẬP NHẬT LỚN) ViewModel cho ItemDetailView (Cột phải).
 * ViewModel này giờ đây "chủ động" gọi API để tải thông tin chi tiết
 * khi một item được chọn.
 * (CẬP NHẬT 2) Expose danh sách URL thay vì ImageInfo DTO.
 * (CẬP NHẬT 3) Thêm logic đường dẫn (Path) và trạng thái (isFolder).
 */
public class ItemDetailViewModel {

    // --- Dependencies ---
    private final ItemRepository itemRepository;
    private final EmbyService embyService;

    // --- Properties cho UI Binding ---
    private final ReadOnlyBooleanWrapper loading = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyStringWrapper title = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper year = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper overview = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper("Vui lòng chọn một item từ danh sách...");
    private final ReadOnlyBooleanWrapper showStatusMessage = new ReadOnlyBooleanWrapper(true);

    // (MỚI) Properties cho thông tin chi tiết
    private final ReadOnlyStringWrapper tagline = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper genres = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper runtime = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<Image> primaryImage = new ReadOnlyObjectWrapper<>(null);

    // (CẬP NHẬT 2) Thay đổi từ ImageInfo -> String (URL)
    private final ObservableList<String> backdropImageUrls = FXCollections.observableArrayList();

    // (CẬP NHẬT 3) Properties cho đường dẫn và nút bấm
    private final ReadOnlyStringWrapper itemPath = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper isFolder = new ReadOnlyBooleanWrapper(false);
    // Property để giao tiếp lỗi/status của hành động (như nhấn nút Mở)
    private final ReadOnlyStringWrapper actionStatusMessage = new ReadOnlyStringWrapper("");


    /**
     * Constructor mới, yêu cầu inject Dependencies.
     * @param itemRepository Repository để lấy dữ liệu.
     * @param embyService Service để lấy trạng thái (UserId, ServerUrl).
     */
    public ItemDetailViewModel(ItemRepository itemRepository, EmbyService embyService) {
        this.itemRepository = itemRepository;
        this.embyService = embyService;
    }

    /**
     * Được gọi bởi MainController khi item trên Grid thay đổi.
     * @param item Item (tóm tắt) được chọn (hoặc null để xóa).
     */
    public void setItemToDisplay(BaseItemDto item) {
        // Trường hợp 1: Bỏ chọn (Clear)
        if (item == null) {
            Platform.runLater(() -> {
                clearDetails();
                statusMessage.set("Vui lòng chọn một item từ danh sách...");
                showStatusMessage.set(true);
                loading.set(false); // Không loading
            });
            return;
        }

        // Trường hợp 2: Chọn item mới -> Bắt đầu tải
        Platform.runLater(() -> {
            clearDetails();
            statusMessage.set("Đang tải chi tiết cho: " + item.getName() + "...");
            showStatusMessage.set(true); // Hiển thị status "Đang tải..."
            loading.set(true); // Hiển thị vòng xoay
        });

        // Khởi chạy luồng nền để gọi API
        new Thread(() -> {
            try {
                // Lấy thông tin cần thiết từ service
                String userId = embyService.getCurrentUserId();
                String itemId = item.getId();
                String serverUrl = embyService.getApiClient().getBasePath();

                if (userId == null) {
                    throw new IllegalStateException("Không thể lấy UserID. Vui lòng đăng nhập lại.");
                }

                // Gọi API (như kế hoạch)
                BaseItemDto fullDetails = itemRepository.getFullItemDetails(userId, itemId);
                List<ImageInfo> images = itemRepository.getItemImages(itemId);
                System.out.println("[DEBUG] Tìm thấy " + images.size() + " ảnh cho item ID: " + itemId);

                // Xử lý dữ liệu (vẫn ở luồng nền)
                String titleText = fullDetails.getName() != null ? fullDetails.getName() : "Không có tiêu đề";
                String yearText = fullDetails.getProductionYear() != null ? String.valueOf(fullDetails.getProductionYear()) : "";
                String overviewText = fullDetails.getOverview() != null ? fullDetails.getOverview() : "Không có mô tả.";
                String taglineText = (fullDetails.getTaglines() != null && !fullDetails.getTaglines().isEmpty()) ? fullDetails.getTaglines().get(0) : "";
                String genresText = (fullDetails.getGenres() != null) ? String.join(", ", fullDetails.getGenres()) : "";
                String runtimeText = formatRuntime(fullDetails.getRunTimeTicks());

                // (MỚI) Lấy thông tin đường dẫn và loại item
                final String path = fullDetails.getPath();
                final boolean folder = fullDetails.isIsFolder() != null && fullDetails.isIsFolder();

                // SỬA ĐỔI: Gọi các hàm helper (đã được viết lại)
                String primaryImageUrl = findImageUrl(images, "Primary", serverUrl, itemId);
                List<String> backdropUrls = buildBackdropUrls(images, serverUrl, itemId);


                // Cập nhật UI trên JavaFX Thread
                Platform.runLater(() -> {
                    title.set(titleText);
                    year.set(yearText);
                    overview.set(overviewText);
                    tagline.set(taglineText);
                    genres.set(genresText);
                    runtime.set(runtimeText);

                    // (MỚI) Cập nhật UI properties cho đường dẫn
                    itemPath.set(path != null ? path : "Không có đường dẫn");
                    isFolder.set(folder);
                    actionStatusMessage.set(""); // Xóa lỗi cũ (nếu có)

                    if (primaryImageUrl != null) {
                        // Giữ nguyên logic debug và listener lỗi
                        System.out.println("[DEBUG] Attempting to load Primary Image from URL: " + primaryImageUrl);
                        Image img = new Image(primaryImageUrl, true); // true = tải nền

                        img.exceptionProperty().addListener((obs, oldEx, newEx) -> {
                            if (newEx != null) {
                                System.err.println("!!! LỖI KHI TẢI ẢNH (Primary Image) !!!");
                                System.err.println("URL: " + primaryImageUrl);
                                newEx.printStackTrace();
                            }
                        });
                        primaryImage.set(img);
                    } else {
                        System.out.println("[DEBUG] No Primary Image URL found for item: " + titleText);
                    }

                    backdropImageUrls.setAll(backdropUrls);

                    loading.set(false); // Ẩn vòng xoay
                    showStatusMessage.set(false); // Ẩn status, hiển thị nội dung
                });

            } catch (Exception e) {
                e.printStackTrace();
                // Xử lý lỗi -> Hiển thị trên UI
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
     * Helper: Xóa tất cả dữ liệu chi tiết (khi đổi item hoặc bỏ chọn).
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

        // (MỚI)
        itemPath.set("");
        isFolder.set(false);
        actionStatusMessage.set("");
    }

    /**
     * Helper: Chuyển đổi RunTimeTicks (100-nanoseconds) sang định dạng "1h 57m".
     */
    private String formatRuntime(Long runTimeTicks) {
        if (runTimeTicks == null || runTimeTicks == 0) return "";
        try {
            // 1 tick = 100 nanoseconds.
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
            System.err.println("Lỗi format runtime: " + e.getMessage());
            return "";
        }
    }

    /**
     * SỬA ĐỔI: Helper: Tìm URL cho một loại ảnh cụ thể (ví dụ: "Primary").
     * Đã viết lại bằng vòng lặp 'for' và thêm log.
     */
    private String findImageUrl(List<ImageInfo> images, String imageType, String serverUrl, String itemId) {
        System.out.println("[DEBUG] Searching for imageType: " + imageType);
        for (ImageInfo image : images) {
            if (imageType.equals(image.getImageType().getValue())) {
                // Định dạng URL: {Server}/Items/{ItemId}/Images/{ImageType}/{ImageIndex}
                String url = String.format("%s/Items/%s/Images/%s/%d",
                        serverUrl, itemId, imageType, 0);

                // SỬA ĐỔI: Thêm log để bạn thấy URL
                System.out.println("[DEBUG] Generated URL for '" + imageType + "': " + url);
                return url; // Trả về URL ngay khi tìm thấy
            }
        }
        System.out.println("[DEBUG] No image found for imageType: " + imageType);
        return null; // Không tìm thấy
    }

    /**
     * SỬA ĐỔI: Helper: Xây dựng danh sách URL cho ảnh backdrop thumbnail.
     * Đã viết lại bằng vòng lặp 'for' và thêm log.
     */
    private List<String> buildBackdropUrls(List<ImageInfo> images, String serverUrl, String itemId) {
        List<String> urls = new ArrayList<>();
        System.out.println("[DEBUG] Building Backdrop URLs...");
        for (ImageInfo imgInfo : images) {
            if ("Backdrop".equals(imgInfo.getImageType().getValue()) && imgInfo.getImageIndex() != null) {
                // Định dạng URL: {Server}/Items/{ItemId}/Images/Backdrop/{ImageIndex}?maxWidth=400
                String url = String.format("%s/Items/%s/Images/Backdrop/%d?maxWidth=400",
                        serverUrl,
                        itemId,
                        imgInfo.getImageIndex());

                // SỬA ĐỔI: Thêm log để bạn thấy URL
                System.out.println("[DEBUG] Generated URL for 'Backdrop': " + url);
                urls.add(url);
            }
        }
        System.out.println("[DEBUG] Found " + urls.size() + " Backdrop URLs.");
        return urls;
    }


    // --- Getters cho Properties (Dùng bởi Controller) ---

    public ReadOnlyBooleanProperty loadingProperty() {
        return loading.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty titleProperty() {
        return title.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty yearProperty() {
        return year.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty overviewProperty() {
        return overview.getReadOnlyProperty();
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

    // (MỚI) Getters cho Path và Action
    public ReadOnlyStringProperty itemPathProperty() { return itemPath.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty isFolderProperty() { return isFolder.getReadOnlyProperty(); }
    public ReadOnlyStringProperty actionStatusMessageProperty() { return actionStatusMessage.getReadOnlyProperty(); }

    // (MỚI) Setter cho Action Status (để Controller có thể set lỗi)
    // Cần làm cho nó có thể ghi được từ bên ngoài, nên đổi ReadOnlyStringWrapper -> StringProperty
    // Hoặc cung cấp một hàm public để set. Hãy dùng cách 2:
    public void setActionStatusMessage(String message) {
        // Đảm bảo chạy trên UI thread nếu cần, nhưng Controller gọi
        // từ Platform.runLater nên có thể an toàn
        this.actionStatusMessage.set(message);
    }

    // Tuy nhiên, vì Controller cũng cần bind vào nó,
    // chúng ta nên dùng StringProperty thay vì ReadOnlyStringWrapper
    // Hãy quay lại sửa ở trên...

    // Sửa lại:
    // private final StringProperty actionStatusMessage = new SimpleStringProperty("");
    // public StringProperty actionStatusMessageProperty() { return actionStatusMessage; }

    // ... Nhưng kế hoạch ban đầu là dùng ReadOnlyStringWrapper.
    // Hãy giữ nguyên kế hoạch gốc. Controller sẽ bind vào ReadOnlyProperty,
    // và ViewModel sẽ cung cấp 1 hàm public để set nó.

    /* (Đã sửa lại ở trên, giữ nguyên ReadOnlyStringWrapper và dùng getter
     * actionStatusMessageProperty() cho cả bind và set)
     * ...
     * private final ReadOnlyStringWrapper actionStatusMessage = new ReadOnlyStringWrapper("");
     * public ReadOnlyStringProperty actionStatusMessageProperty() { return actionStatusMessage.getReadOnlyProperty(); }
     *
     * Hàm setItemToDisplay đã set nó.
     * Hàm clearDetails đã set nó.
     * Controller sẽ cần gọi hàm public để set lỗi.
     * -> Thêm hàm này:
     */
    public void reportActionError(String errorMessage) {
        Platform.runLater(() -> {
            this.actionStatusMessage.set(errorMessage);
        });
    }
    public void clearActionError() {
        Platform.runLater(() -> {
            this.actionStatusMessage.set("");
        });
    }
    // ...
    // Quyết định cuối: Để đơn giản cho Controller,
    // ta sẽ expose ReadOnlyProperty,
    // và Controller sẽ gọi hàm `reportActionError` hoặc `clearActionError`
    // (Đã thêm 2 hàm trên)
}