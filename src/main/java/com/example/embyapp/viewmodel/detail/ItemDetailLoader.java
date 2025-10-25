package com.example.embyapp.viewmodel.detail;

import embyclient.ApiException;
import embyclient.model.BaseItemDto;
import embyclient.model.BaseItemPerson;
import embyclient.model.ImageInfo;
import embyclient.model.ImageType; // <-- THÊM IMPORT
import embyclient.model.NameLongIdPair;
import com.example.embyapp.service.EmbyService;
import com.example.embyapp.service.ItemRepository;
// import org.threeten.bp.OffsetDateTime; // <-- XÓA IMPORT NÀY

// (*** THÊM IMPORT SỬA LỖI ***)
import java.time.OffsetDateTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * (CẬP NHẬT 19)
 * - Sửa lỗi: Lấy URL ảnh Primary từ BaseItemDto.getImageTags() thay vì ImageInfo.
 * - Sửa LoadResult để chỉ chứa List<ImageInfo> cho backdrops.
 * (CẬP NHẬT 21 - SỬA LỖI BIÊN DỊCH)
 * - Sửa lỗi org.threeten.bp.OffsetDateTime.
 * (CẬP NHẬT 27 - THÊM STUDIOS/PEOPLE DẠNG TAG)
 * - Thay đổi LoadResult để trả về List<TagModel> cho Studios/People.
 * - Thêm các hàm chuyển đổi Studios/People sang List<TagModel).
 */
public class ItemDetailLoader {

    private final ItemRepository itemRepository;
    private final EmbyService embyService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    public ItemDetailLoader(ItemRepository itemRepository, EmbyService embyService) {
        this.itemRepository = itemRepository;
        this.embyService = embyService;
    }

    public LoadResult loadItemData(String userId, String itemId) throws ApiException {
        String serverUrl = embyService.getApiClient().getBasePath();

        // 1. Tải dữ liệu thô
        BaseItemDto fullDetails = itemRepository.getFullItemDetails(userId, itemId);
        List<ImageInfo> images = itemRepository.getItemImages(itemId); // Tải tất cả info ảnh (chủ yếu cho backdrop)

        // 2. Định dạng dữ liệu
        LoadResult result = new LoadResult(fullDetails); // <-- Sửa constructor

        // ... (setText, setOverview, ... giữ nguyên) ...
        result.setTitleText(fullDetails.getName() != null ? fullDetails.getName() : "");
        result.setYearText(fullDetails.getProductionYear() != null ? String.valueOf(fullDetails.getProductionYear()) : "");
        result.setOverviewText(fullDetails.getOverview() != null ? fullDetails.getOverview() : "");
        result.setTaglineText((fullDetails.getTaglines() != null && !fullDetails.getTaglines().isEmpty()) ? fullDetails.getTaglines().get(0) : "");
        result.setGenresText((fullDetails.getGenres() != null) ? String.join(", ", fullDetails.getGenres()) : "");
        result.setRuntimeText(formatRuntime(fullDetails.getRunTimeTicks()));
        result.setPathText(fullDetails.getPath() != null ? fullDetails.getPath() : "Không có đường dẫn");
        result.setFolder(fullDetails.isIsFolder() != null && fullDetails.isIsFolder());

        // (*** LOGIC TAGS (giữ nguyên) ***)
        List<TagModel> parsedTags = new ArrayList<>();
        if (fullDetails.getTagItems() != null) {
            for (NameLongIdPair tagPair : fullDetails.getTagItems()) {
                if (tagPair.getName() != null) {
                    parsedTags.add(TagModel.parse(tagPair.getName()));
                }
            }
        }
        result.setTagItems(parsedTags);

        // (*** SỬA LỖI TẠI ĐÂY ***)
        // fullDetails.getPremiereDate() trả về java.time.OffsetDateTime
        // hàm dateToString giờ đây cũng chấp nhận java.time.OffsetDateTime
        result.setReleaseDateText(dateToString(fullDetails.getPremiereDate()));

        result.setStudioItems(studiosToTagModelList(fullDetails.getStudios())); // MODIFIED
        result.setPeopleItems(peopleToTagModelList(fullDetails.getPeople())); // MODIFIED

        // (*** LOGIC ẢNH (giữ nguyên) ***)
        String primaryImageUrl = null;
        if (fullDetails.getImageTags() != null && fullDetails.getImageTags().containsKey("Primary")) {
            String tag = fullDetails.getImageTags().get("Primary");
            primaryImageUrl = String.format("%s/Items/%s/Images/Primary?tag=%s&maxWidth=%d&quality=90",
                    serverUrl, itemId, tag, 600);
        }
        result.setPrimaryImageUrl(primaryImageUrl);
        List<ImageInfo> backdrops = images.stream()
                .filter(img -> ImageType.BACKDROP.equals(img.getImageType()))
                .collect(Collectors.toList());
        result.setBackdropImages(backdrops);

        // Lấy tên file export
        if (fullDetails.getOriginalTitle() != null && !fullDetails.getOriginalTitle().isEmpty()) {
            result.setOriginalTitleForExport(fullDetails.getOriginalTitle());
        } else {
            result.setOriginalTitleForExport(fullDetails.getName()); // Fallback
        }

        return result;
    }

    // --- Các hàm helper định dạng ---
    private String listToString(List<String> list) {
        return (list != null) ? String.join(", ", list) : "";
    }

    // (*** SỬA LỖI TẠI ĐÂY: Thay đổi kiểu tham số ***)
    private String dateToString(OffsetDateTime date) { // <-- Đổi từ org.threeten.bp sang java.time
        if (date == null) return "";
        try {
            // Logic bên trong hàm này (toInstant().toEpochMilli())
            // hoạt động đúng cho cả hai thư viện.
            return dateFormat.format(new java.util.Date(date.toInstant().toEpochMilli()));
        } catch (Exception e) {
            System.err.println("Lỗi format dateToString: " + e.getMessage());
            return "";
        }
    }

    // MODIFIED: Chuyển Studios sang List<TagModel>
    private List<TagModel> studiosToTagModelList(List<NameLongIdPair> studios) {
        return (studios != null) ? studios.stream()
                .map(NameLongIdPair::getName)
                .filter(Objects::nonNull)
                .map(TagModel::parse)
                .collect(Collectors.toList()) : new ArrayList<>();
    }

    // MODIFIED: Chuyển People sang List<TagModel>
    private List<TagModel> peopleToTagModelList(List<BaseItemPerson> people) {
        return (people != null) ? people.stream()
                .map(BaseItemPerson::getName)
                .filter(Objects::nonNull)
                .map(TagModel::parse)
                .collect(Collectors.toList()) : new ArrayList<>();
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

    /**
     * Lớp POJO nội bộ.
     */
    public static class LoadResult {
        private final BaseItemDto fullDetails;
        private String titleText, overviewText, releaseDateText;
        private List<TagModel> tagItems, studioItems, peopleItems; // MODIFIED
        private String yearText, taglineText, genresText, runtimeText, pathText;
        private String primaryImageUrl, originalTitleForExport;
        private boolean isFolder;
        private List<ImageInfo> backdropImages;

        public LoadResult(BaseItemDto fullDetails) {
            this.fullDetails = fullDetails;
        }

        public Map<String, String> getOriginalStrings() {
            Map<String, String> originals = new HashMap<>();
            originals.put("title", titleText);
            originals.put("overview", overviewText);
            originals.put("releaseDate", releaseDateText);
            // REMOVED: Không lưu studios/people dạng String vào đây nữa
            return originals;
        }

        // --- Getters & Setters ---
        public BaseItemDto getFullDetails() { return fullDetails; }
        public String getTitleText() { return titleText; }
        public void setTitleText(String titleText) { this.titleText = titleText; }
        public String getOverviewText() { return overviewText; }
        public void setOverviewText(String overviewText) { this.overviewText = overviewText; }
        public List<TagModel> getTagItems() { return tagItems; }
        public void setTagItems(List<TagModel> tagItems) { this.tagItems = tagItems; }
        public String getReleaseDateText() { return releaseDateText; }
        public void setReleaseDateText(String releaseDateText) { this.releaseDateText = releaseDateText; }
        // REMOVED: public String getStudiosText() { return studiosText; }
        // REMOVED: public void setStudiosText(String studiosText) { this.studiosText = studiosText; }
        // REMOVED: public String getPeopleText() { return peopleText; }
        // REMOVED: public void setPeopleText(String peopleText) { this.peopleText = peopleText; }
        public List<TagModel> getStudioItems() { return studioItems; } // ADDED
        public void setStudioItems(List<TagModel> studioItems) { this.studioItems = studioItems; } // ADDED
        public List<TagModel> getPeopleItems() { return peopleItems; } // ADDED
        public void setPeopleItems(List<TagModel> peopleItems) { this.peopleItems = peopleItems; } // ADDED

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
        public List<ImageInfo> getBackdropImages() { return backdropImages; }
        public void setBackdropImages(List<ImageInfo> backdropImages) { this.backdropImages = backdropImages; }
    }
}