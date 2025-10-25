package com.example.embyapp.viewmodel.detail;

import com.example.emby.modelEmby.BaseItemDto;
import com.example.emby.modelEmby.BaseItemPerson;
import com.example.emby.modelEmby.NameLongIdPair;
import com.example.embyapp.viewmodel.ItemDetailViewModel;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.threeten.bp.OffsetDateTime;


/**
 * (CẬP NHẬT 7)
 * - Sửa logic import/preview cho tags để đọc TagItems và cập nhật ObservableList<TagModel>.
 * - Xóa (v/x) cho tags.
 */
public class ItemDetailImportHandler {

    private final ItemDetailViewModel viewModel; // Tham chiếu đến VM chính để set giá trị UI
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");


    // Lưu trạng thái ngay trước khi import
    private final Map<String, Object> preImportState = new HashMap<>();

    // Các BooleanProperty cho 6 cặp nút (v/x)
    private final ReadOnlyBooleanWrapper showTitleReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showOverviewReview = new ReadOnlyBooleanWrapper(false);
    // private final ReadOnlyBooleanWrapper showTagsReview = new ReadOnlyBooleanWrapper(false); // (ĐÃ XÓA)
    private final ReadOnlyBooleanWrapper showReleaseDateReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showStudiosReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showPeopleReview = new ReadOnlyBooleanWrapper(false);

    public ItemDetailImportHandler(ItemDetailViewModel viewModel) {
        this.viewModel = viewModel;
    }

    /**
     * (*** SỬA ĐỔI TAGS ***)
     * Nhận DTO từ file import và cập nhật UI để review.
     */
    public void importAndPreview(BaseItemDto importedDto) {
        if (importedDto == null) return;

        clearState(); // Xóa state cũ
        hideAllReviewButtons(); // Ẩn tất cả nút

        // 1. Title
        preImportState.put("title", viewModel.titleProperty().get()); // Lưu giá trị HIỆN TẠI
        viewModel.titleProperty().set(importedDto.getName() != null ? importedDto.getName() : "");
        showTitleReview.set(true);

        // 2. Overview
        preImportState.put("overview", viewModel.overviewProperty().get());
        viewModel.overviewProperty().set(importedDto.getOverview() != null ? importedDto.getOverview() : "");
        showOverviewReview.set(true);

        // (*** SỬA ĐỔI TAGS ***)
        // 3. Tags
        // Lưu trạng thái List<TagModel> HIỆN TẠI
        preImportState.put("tags", new ArrayList<>(viewModel.getTagItems()));

        // Phân tích TagItems từ DTO đã import
        List<TagModel> importedTags = new ArrayList<>();
        if (importedDto.getTagItems() != null) {
            for (NameLongIdPair tagPair : importedDto.getTagItems()) {
                if (tagPair.getName() != null) {
                    importedTags.add(TagModel.parse(tagPair.getName()));
                }
            }
        }
        // Cập nhật ViewModel
        viewModel.getTagItems().setAll(importedTags);
        // (Không còn nút review (v/x) cho tags)
        // showTagsReview.set(true); // (ĐÃ XÓA)
        // (*** KẾT THÚC SỬA ĐỔI TAGS ***)


        // 4. Release Date
        preImportState.put("releaseDate", viewModel.releaseDateProperty().get());
        viewModel.releaseDateProperty().set(dateToString(importedDto.getPremiereDate()));
        showReleaseDateReview.set(true);

        // 5. Studios
        preImportState.put("studios", viewModel.studiosProperty().get());
        List<NameLongIdPair> importStudios = importedDto.getStudios() != null ? importedDto.getStudios() : Collections.emptyList();
        viewModel.studiosProperty().set(studiosToString(importStudios));
        showStudiosReview.set(true);

        // 6. People
        preImportState.put("people", viewModel.peopleProperty().get());
        List<BaseItemPerson> importPeople = importedDto.getPeople() != null ? importedDto.getPeople() : Collections.emptyList();
        viewModel.peopleProperty().set(peopleToString(importPeople));
        showPeopleReview.set(true);

        // Import là một thay đổi, bật nút Save (thông qua dirty tracker)
        viewModel.isDirtyProperty().set(true);
    }

    /**
     * Người dùng nhấn (v) - Chấp nhận thay đổi.
     */
    public void acceptImportField(String fieldName) {
        // Chỉ cần ẩn nút (v/x), dữ liệu đã ở trong textfield
        switch (fieldName) {
            case "title": showTitleReview.set(false); break;
            case "overview": showOverviewReview.set(false); break;
            // case "tags": showTagsReview.set(false); break; // (ĐÃ XÓA)
            case "releaseDate": showReleaseDateReview.set(false); break;
            case "studios": showStudiosReview.set(false); break;
            case "people": showPeopleReview.set(false); break;
        }
    }

    /**
     * (*** SỬA ĐỔI TAGS ***)
     * Người dùng nhấn (x) - Hủy bỏ thay đổi.
     */
    @SuppressWarnings("unchecked")
    public void rejectImportField(String fieldName) {
        // Khôi phục giá trị UI từ preImportState
        switch (fieldName) {
            case "title":
                viewModel.titleProperty().set((String) preImportState.get("title"));
                showTitleReview.set(false);
                break;
            case "overview":
                // (*** ĐÂY LÀ DÒNG ĐÃ SỬA LỖI ***)
                viewModel.overviewProperty().set((String) preImportState.get("overview"));
                showOverviewReview.set(false);
                break;

            // (*** MỚI: Logic Hủy bỏ cho Tags ***)
            case "tags":
                // Khôi phục List<TagModel>
                List<TagModel> originalTags = (List<TagModel>) preImportState.get("tags");
                if (originalTags != null) {
                    viewModel.getTagItems().setAll(originalTags);
                }
                // showTagsReview.set(false); // (Đã xóa)
                break;

            case "releaseDate":
                viewModel.releaseDateProperty().set((String) preImportState.get("releaseDate"));
                showReleaseDateReview.set(false);
                break;
            case "studios":
                viewModel.studiosProperty().set((String) preImportState.get("studios"));
                showStudiosReview.set(false);
                break;
            case "people":
                viewModel.peopleProperty().set((String) preImportState.get("people"));
                showPeopleReview.set(false);
                break;
        }
    }

    public void hideAllReviewButtons() {
        showTitleReview.set(false);
        showOverviewReview.set(false);
        // showTagsReview.set(false); // (ĐÃ XÓA)
        showReleaseDateReview.set(false);
        showStudiosReview.set(false);
        showPeopleReview.set(false);
    }

    public void clearState() {
        preImportState.clear();
        hideAllReviewButtons();
    }

    // --- Hàm helper định dạng (sao chép từ Loader để dùng nội bộ) ---
    private String listToString(List<String> list) {
        return (list != null) ? String.join(", ", list) : "";
    }
    private String dateToString(OffsetDateTime date) {
        if (date == null) return "";
        try {
            return dateFormat.format(new java.util.Date(date.toInstant().toEpochMilli()));
        } catch (Exception e) { return ""; }
    }
    private String studiosToString(List<NameLongIdPair> studios) {
        return (studios != null) ? studios.stream().map(NameLongIdPair::getName).filter(Objects::nonNull).collect(Collectors.joining(", ")) : "";
    }
    private String peopleToString(List<BaseItemPerson> people) {
        return (people != null) ? people.stream().map(BaseItemPerson::getName).filter(Objects::nonNull).collect(Collectors.joining(", ")) : "";
    }


    // --- Getters cho các BooleanProperty (v/x) ---
    public ReadOnlyBooleanProperty showTitleReviewProperty() { return showTitleReview.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showOverviewReviewProperty() { return showOverviewReview.getReadOnlyProperty(); }
    // public ReadOnlyBooleanProperty showTagsReviewProperty() { return showTagsReview.getReadOnlyProperty(); } // (ĐÃ XÓA)
    public ReadOnlyBooleanProperty showReleaseDateReviewProperty() { return showReleaseDateReview.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showStudiosReviewProperty() { return showStudiosReview.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showPeopleReviewProperty() { return showPeopleReview.getReadOnlyProperty(); }
}