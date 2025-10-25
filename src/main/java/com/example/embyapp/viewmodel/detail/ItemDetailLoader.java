// Đặt tại: src/main/java/com/example/embyapp/viewmodel/detail/ItemDetailLoader.java
package com.example.embyapp.viewmodel.detail;

import com.example.emby.EmbyClient.ApiException;
import com.example.emby.modelEmby.BaseItemDto;
import com.example.emby.modelEmby.BaseItemPerson;
import com.example.emby.modelEmby.ImageInfo;
import com.example.emby.modelEmby.NameLongIdPair;
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.ItemRepository;
import org.threeten.bp.OffsetDateTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Lớp phụ trợ (Helper class) cho ItemDetailViewModel.
 * Chuyên trách việc Tải (Load) và Định dạng (Format) dữ liệu từ Repository
 * để chuẩn bị cho UI binding.
 */
public class ItemDetailLoader {

    private final ItemRepository itemRepository;
    private final EmbyService embyService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    public ItemDetailLoader(ItemRepository itemRepository, EmbyService embyService) {
        this.itemRepository = itemRepository;
        this.embyService = embyService;
    }

    /**
     * Tải tất cả dữ liệu cần thiết cho một item.
     * Hàm này được gọi từ luồng nền (background thread).
     *
     * @param userId ID người dùng
     * @param itemId ID item
     * @return một đối tượng LoadResult chứa tất cả dữ liệu đã định dạng.
     * @throws ApiException nếu API call thất bại
     */
    public LoadResult loadItemData(String userId, String itemId) throws ApiException {
        String serverUrl = embyService.getApiClient().getBasePath();

        // 1. Tải dữ liệu thô
        BaseItemDto fullDetails = itemRepository.getFullItemDetails(userId, itemId);
        List<ImageInfo> images = itemRepository.getItemImages(itemId);

        // 2. Định dạng dữ liệu
        LoadResult result = new LoadResult(fullDetails);

        result.setTitleText(fullDetails.getName() != null ? fullDetails.getName() : "");
        result.setYearText(fullDetails.getProductionYear() != null ? String.valueOf(fullDetails.getProductionYear()) : "");
        result.setOverviewText(fullDetails.getOverview() != null ? fullDetails.getOverview() : "");
        result.setTaglineText((fullDetails.getTaglines() != null && !fullDetails.getTaglines().isEmpty()) ? fullDetails.getTaglines().get(0) : "");
        result.setGenresText((fullDetails.getGenres() != null) ? String.join(", ", fullDetails.getGenres()) : "");
        result.setRuntimeText(formatRuntime(fullDetails.getRunTimeTicks()));
        result.setPathText(fullDetails.getPath() != null ? fullDetails.getPath() : "Không có đường dẫn");
        result.setFolder(fullDetails.isIsFolder() != null && fullDetails.isIsFolder());

        // Định dạng các trường editable
        result.setTagsText(listToString(fullDetails.getTags()));
        result.setReleaseDateText(dateToString(fullDetails.getPremiereDate()));
        result.setStudiosText(studiosToString(fullDetails.getStudios()));
        result.setPeopleText(peopleToString(fullDetails.getPeople()));

        // Định dạng ảnh
        result.setPrimaryImageUrl(findImageUrl(images, "Primary", serverUrl, itemId));
        result.setBackdropUrls(buildBackdropUrls(images, serverUrl, itemId));

        // Lấy tên file export
        if (fullDetails.getOriginalTitle() != null && !fullDetails.getOriginalTitle().isEmpty()) {
            result.setOriginalTitleForExport(fullDetails.getOriginalTitle());
        } else {
            result.setOriginalTitleForExport(fullDetails.getName()); // Fallback
        }

        return result;
    }

    // --- Các hàm helper định dạng (Đã chuyển từ ViewModel cũ) ---

    private String listToString(List<String> list) {
        return (list != null) ? String.join(", ", list) : "";
    }

    private String dateToString(OffsetDateTime date) {
        if (date == null) return "";
        try {
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
        if (images == null) return null;
        for (ImageInfo image : images) {
            if (imageType.equals(image.getImageType().getValue())) {
                return String.format("%s/Items/%s/Images/%s/%d", serverUrl, itemId, imageType, 0);
            }
        }
        return null;
    }

    private List<String> buildBackdropUrls(List<ImageInfo> images, String serverUrl, String itemId) {
        List<String> urls = new ArrayList<>();
        if (images == null) return urls;
        for (ImageInfo imgInfo : images) {
            if ("Backdrop".equals(imgInfo.getImageType().getValue()) && imgInfo.getImageIndex() != null) {
                String url = String.format("%s/Items/%s/Images/Backdrop/%d?maxWidth=400",
                        serverUrl, itemId, imgInfo.getImageIndex());
                urls.add(url);
            }
        }
        return urls;
    }

    /**
     * Lớp POJO nội bộ để chứa kết quả tải và định dạng.
     */
    public static class LoadResult {
        private final BaseItemDto fullDetails;
        private String titleText, overviewText, tagsText, releaseDateText, studiosText, peopleText;
        private String yearText, taglineText, genresText, runtimeText, pathText;
        private String primaryImageUrl, originalTitleForExport;
        private boolean isFolder;
        private List<String> backdropUrls;

        public LoadResult(BaseItemDto fullDetails) {
            this.fullDetails = fullDetails;
        }

        /**
         * Lấy snapshot các giá trị gốc để dùng cho DirtyTracker.
         */
        public Map<String, String> getOriginalStrings() {
            Map<String, String> originals = new HashMap<>();
            originals.put("title", titleText);
            originals.put("overview", overviewText);
            originals.put("tags", tagsText);
            originals.put("releaseDate", releaseDateText);
            originals.put("studios", studiosText);
            originals.put("people", peopleText);
            return originals;
        }

        // --- Getters & Setters ---
        public BaseItemDto getFullDetails() { return fullDetails; }
        public String getTitleText() { return titleText; }
        public void setTitleText(String titleText) { this.titleText = titleText; }
        public String getOverviewText() { return overviewText; }
        public void setOverviewText(String overviewText) { this.overviewText = overviewText; }
        public String getTagsText() { return tagsText; }
        public void setTagsText(String tagsText) { this.tagsText = tagsText; }
        public String getReleaseDateText() { return releaseDateText; }
        public void setReleaseDateText(String releaseDateText) { this.releaseDateText = releaseDateText; }
        public String getStudiosText() { return studiosText; }
        public void setStudiosText(String studiosText) { this.studiosText = studiosText; }
        public String getPeopleText() { return peopleText; }
        public void setPeopleText(String peopleText) { this.peopleText = peopleText; }
        public String getYearText() { return yearText; }
        public void setYearText(String yearText) { this.yearText = yearText; }
        public String getTaglineText() { return taglineText; }
        public void setTaglineText(String taglineText) { this.taglineText = taglineText; }
        public String getGenresText() { return genresText; }
        public void setGenresText(String genresText) { this.genresText = genresText; }
        public String getRuntimeText() { return runtimeText; }
        public void setRuntimeText(String runtimeText) { this.runtimeText = runtimeText; }
        public String getPathText() { return pathText; }
        public void setPathText(String pathText) { this.pathText = pathText; }
        public String getPrimaryImageUrl() { return primaryImageUrl; }
        public void setPrimaryImageUrl(String primaryImageUrl) { this.primaryImageUrl = primaryImageUrl; }
        public String getOriginalTitleForExport() { return originalTitleForExport; }
        public void setOriginalTitleForExport(String originalTitleForExport) { this.originalTitleForExport = originalTitleForExport; }
        public boolean isFolder() { return isFolder; }
        public void setFolder(boolean folder) { isFolder = folder; }
        public List<String> getBackdropUrls() { return backdropUrls; }
        public void setBackdropUrls(List<String> backdropUrls) { this.backdropUrls = backdropUrls; }
    }
}