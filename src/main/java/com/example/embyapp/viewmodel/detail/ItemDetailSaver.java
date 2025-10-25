package com.example.embyapp.viewmodel.detail;

import embyclient.ApiException;
import embyclient.api.ItemUpdateServiceApi;
import embyclient.model.BaseItemDto;
import embyclient.model.BaseItemPerson;
import embyclient.model.NameLongIdPair; // (*** QUAN TRỌNG ***)
import embyclient.model.PersonType;
import com.example.embyapp.service.EmbyService;
// import org.threeten.bp.OffsetDateTime; // <-- XÓA IMPORT NÀY

// (*** THÊM CÁC IMPORT java.time ***)
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * (CẬP NHẬT 30) Thêm Genres.
 * - Cập nhật SaveRequest để chứa Genres.
 */
public class ItemDetailSaver {

    private final EmbyService embyService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    public ItemDetailSaver(EmbyService embyService) {
        this.embyService = embyService;
    }

    /**
     * Phân tích, cập nhật DTO và gọi API lưu.
     * Hàm này được gọi từ luồng nền (background thread).
     *
     * @param request Đối tượng chứa DTO gốc và các chuỗi mới từ UI.
     * @throws ApiException nếu API call thất bại.
     */
    public void saveChanges(SaveRequest request) throws ApiException {
        // 1. Parse dữ liệu từ String (UI) vào DTO
        BaseItemDto dtoToSave = parseUiToDto(request); // Đổi tên hàm

        // 2. Gọi API
        ItemUpdateServiceApi itemUpdateServiceApi = embyService.getItemUpdateServiceApi();
        if (itemUpdateServiceApi == null) {
            throw new IllegalStateException("Không thể lấy ItemUpdateServiceApi. Vui lòng đăng nhập lại.");
        }

        itemUpdateServiceApi.postItemsByItemid(dtoToSave, request.getItemId());
    }

    /**
     * (*** SỬA ĐỔI LOGIC LƯU TAGS ***)
     * Logic phân tích (parse) các chuỗi từ UI về lại DTO.
     */
    public BaseItemDto parseUiToDto(SaveRequest request) { // Đổi tên hàm
        BaseItemDto dto = request.getOriginalDto();

        dto.setName(request.getTitle());
        dto.setOverview(request.getOverview());

        // (*** SỬA ĐỔI TAGS ***)
        // Parse Tags (List<TagModel> -> List<NameLongIdPair>)
        List<NameLongIdPair> tagItemsToSave = request.getTagItems().stream()
                .map(tagModel -> {
                    NameLongIdPair pair = new NameLongIdPair();
                    // Lấy chuỗi đã serialize (JSON hoặc thường) làm Name
                    pair.setName(tagModel.serialize());
                    // ID có thể để null, server sẽ tự xử lý
                    pair.setId(null);
                    return pair;
                })
                .collect(Collectors.toList());
        // Ghi vào trường "TagItems" thay vì "Tags"
        dto.setTagItems(tagItemsToSave);
        // Có thể xóa trường Tags cũ để tránh xung đột (tùy chọn, tùy server xử lý)
        // dto.setTags(null);
        // (*** KẾT THÚC SỬA ĐỔI TAGS ***)


        // Parse Studios (List<TagModel> -> List<NameLongIdPair>) // MODIFIED
        List<NameLongIdPair> studiosList = request.getStudioItems().stream()
                .map(tagModel -> {
                    NameLongIdPair pair = new NameLongIdPair();
                    pair.setName(tagModel.serialize()); // Serialize TagModel về chuỗi
                    pair.setId(null);
                    return pair;
                })
                .collect(Collectors.toList());
        dto.setStudios(studiosList);

        // Parse People (List<TagModel> -> List<BaseItemPerson>) // MODIFIED
        List<BaseItemPerson> peopleList = request.getPeopleItems().stream()
                .map(tagModel -> {
                    // Sửa lỗi: Sử dụng constructor rỗng và setter
                    BaseItemPerson person = new BaseItemPerson();
                    person.setName(tagModel.serialize()); // Serialize TagModel về chuỗi
                    person.setType(PersonType.ACTOR); // Gán type mặc định
                    return person;
                })
                .collect(Collectors.toList());
        dto.setPeople(peopleList);

        // (*** Genres không được xử lý ở đây. Nó sẽ được xử lý trong ItemDetailViewModel ***)
        // Lý do: Genres được lưu dưới dạng List<String>, không phải List<NameLongIdPair> hay List<BaseItemPerson>

        // (*** SỬA LỖI 3: DATE ***)
        // Parse Date (String -> java.time.OffsetDateTime)
        try {
            Date parsedDate = dateFormat.parse(request.getReleaseDate());

            // Sửa lỗi: Chuyển sang java.time.Instant và java.time.OffsetDateTime
            Instant instant = Instant.ofEpochMilli(parsedDate.getTime());
            OffsetDateTime odt = OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());

            dto.setPremiereDate(odt);
        } catch (ParseException e) {
            System.err.println("Không thể parse ngày: " + request.getReleaseDate() + ". Sẽ set thành null.");
            dto.setPremiereDate(null); // Set null nếu định dạng sai
        }

        return dto;
    }


    /**
     * Lớp POJO nội bộ để chứa yêu cầu lưu (SaveRequest).
     */
    public static class SaveRequest {
        private final BaseItemDto originalDto;
        private final String itemId;
        private final String title;
        private final String overview;
        private final List<TagModel> tagItems;
        private final String releaseDate;
        private final List<TagModel> studioItems; // MODIFIED
        private final List<TagModel> peopleItems; // MODIFIED
        private final List<TagModel> genreItems; // (*** MỚI ***)

        public SaveRequest(BaseItemDto originalDto, String itemId, String title, String overview,
                           List<TagModel> tagItems,
                           String releaseDate, List<TagModel> studioItems, List<TagModel> peopleItems,
                           List<TagModel> genreItems) { // (*** THÊM GENRE ***)
            this.originalDto = originalDto;
            this.itemId = itemId;
            this.title = title;
            this.overview = overview;
            this.tagItems = tagItems;
            this.releaseDate = releaseDate;
            this.studioItems = studioItems;
            this.peopleItems = peopleItems;
            this.genreItems = genreItems; // (*** MỚI ***)
        }

        // --- Getters ---
        public BaseItemDto getOriginalDto() { return originalDto; }
        public String getItemId() { return itemId; }
        public String getTitle() { return title; }
        public String getOverview() { return overview; }
        public List<TagModel> getTagItems() { return tagItems; }
        public String getReleaseDate() { return releaseDate; }
        public List<TagModel> getStudioItems() { return studioItems; }
        public List<TagModel> getPeopleItems() { return peopleItems; }
        public List<TagModel> getGenreItems() { return genreItems; } // (*** MỚI ***)
    }
}