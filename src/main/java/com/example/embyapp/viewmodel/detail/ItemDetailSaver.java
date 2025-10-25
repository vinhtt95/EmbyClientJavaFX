// Đặt tại: src/main/java/com/example/embyapp/viewmodel/detail/ItemDetailSaver.java
package com.example.embyapp.viewmodel.detail;

import com.example.emby.EmbyClient.ApiException;
import com.example.emby.EmbyClient.Java.ItemUpdateServiceApi;
import com.example.emby.modelEmby.BaseItemDto;
import com.example.emby.modelEmby.BaseItemPerson;
import com.example.emby.modelEmby.NameLongIdPair;
import com.example.emby.modelEmby.PersonType;
import com.example.embyapp.service.EmbyService;
import org.threeten.bp.OffsetDateTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lớp phụ trợ (Helper class) cho ItemDetailViewModel.
 * Chuyên trách việc Phân tích (Parse) dữ liệu từ UI (String)
 * và Lưu (Save) thay đổi về server.
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
        BaseItemDto dtoToSave = parseUiStringsToDto(request);

        // 2. Gọi API
        ItemUpdateServiceApi itemUpdateServiceApi = embyService.getItemUpdateServiceApi();
        if (itemUpdateServiceApi == null) {
            throw new IllegalStateException("Không thể lấy ItemUpdateServiceApi. Vui lòng đăng nhập lại.");
        }

        itemUpdateServiceApi.postItemsByItemid(dtoToSave, request.getItemId());
    }

    /**
     * Logic phân tích (parse) các chuỗi từ UI về lại DTO.
     */
    private BaseItemDto parseUiStringsToDto(SaveRequest request) {
        BaseItemDto dto = request.getOriginalDto();

        dto.setName(request.getTitle());
        dto.setOverview(request.getOverview());

        // Parse Tags (String -> List<String>)
        List<String> tagsList = Arrays.stream(request.getTags().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        dto.setTags(tagsList);

        // Parse Studios (String -> List<NameLongIdPair>)
        List<NameLongIdPair> studiosList = Arrays.stream(request.getStudios().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(name -> {
                    NameLongIdPair pair = new NameLongIdPair();
                    pair.setName(name);
                    return pair;
                })
                .collect(Collectors.toList());
        dto.setStudios(studiosList);

        // Parse People (String -> List<BaseItemPerson>)
        List<BaseItemPerson> peopleList = Arrays.stream(request.getPeople().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(name -> new BaseItemPerson(name, PersonType.ACTOR)) // Mặc định là Actor
                .collect(Collectors.toList());
        dto.setPeople(peopleList);

        // Parse Date (String -> OffsetDateTime)
        try {
            Date parsedDate = dateFormat.parse(request.getReleaseDate());
            org.threeten.bp.Instant threetenInstant = org.threeten.bp.Instant.ofEpochMilli(parsedDate.getTime());
            OffsetDateTime odt = OffsetDateTime.ofInstant(threetenInstant, org.threeten.bp.ZoneId.systemDefault());
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
        private final String tags;
        private final String releaseDate;
        private final String studios;
        private final String people;

        public SaveRequest(BaseItemDto originalDto, String itemId, String title, String overview, String tags, String releaseDate, String studios, String people) {
            this.originalDto = originalDto;
            this.itemId = itemId;
            this.title = title;
            this.overview = overview;
            this.tags = tags;
            this.releaseDate = releaseDate;
            this.studios = studios;
            this.people = people;
        }

        // --- Getters ---
        public BaseItemDto getOriginalDto() { return originalDto; }
        public String getItemId() { return itemId; }
        public String getTitle() { return title; }
        public String getOverview() { return overview; }
        public String getTags() { return tags; }
        public String getReleaseDate() { return releaseDate; }
        public String getStudios() { return studios; }
        public String getPeople() { return people; }
    }
}