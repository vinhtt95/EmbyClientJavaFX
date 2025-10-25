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
 * (CẬP NHẬT 11)
 * - Tách biệt logic dirty: Import không tự động bật isDirty.
 * - Chỉ bật isDirty khi người dùng nhấn Accept (✓) lần đầu tiên.
 */
public class ItemDetailImportHandler {

    private final ItemDetailViewModel viewModel; // Tham chiếu đến VM chính
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    // Lưu trạng thái ngay trước khi import
    private final Map<String, Object> preImportState = new HashMap<>();

    // (*** MỚI: Cờ quản lý trạng thái Import ***)
    private boolean importInProgress = false;
    private boolean anyFieldAccepted = false; // Đã nhấn Accept ít nhất 1 lần chưa?

    // Các BooleanProperty cho nút (v/x)
    private final ReadOnlyBooleanWrapper showTitleReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showOverviewReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showReleaseDateReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showStudiosReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showPeopleReview = new ReadOnlyBooleanWrapper(false);

    public ItemDetailImportHandler(ItemDetailViewModel viewModel) {
        this.viewModel = viewModel;
    }

    /**
     * Nhận DTO từ file import và cập nhật UI để review.
     * (*** KHÔNG TỰ ĐỘNG BẬT isDirty NỮA ***)
     */
    public void importAndPreview(BaseItemDto importedDto) {
        if (importedDto == null) return;

        clearState(); // Xóa state cũ (sẽ reset cờ importInProgress)
        hideAllReviewButtons(); // Ẩn tất cả nút

        // (*** MỚI: Bắt đầu trạng thái import ***)
        this.importInProgress = true;
        this.anyFieldAccepted = false;

        // 1. Title
        preImportState.put("title", viewModel.titleProperty().get());
        viewModel.titleProperty().set(importedDto.getName() != null ? importedDto.getName() : "");
        showTitleReview.set(true);

        // 2. Overview
        preImportState.put("overview", viewModel.overviewProperty().get());
        viewModel.overviewProperty().set(importedDto.getOverview() != null ? importedDto.getOverview() : "");
        showOverviewReview.set(true);

        // 3. Tags
        preImportState.put("tags", new ArrayList<>(viewModel.getTagItems()));
        List<TagModel> importedTags = new ArrayList<>();
        if (importedDto.getTagItems() != null) {
            for (NameLongIdPair tagPair : importedDto.getTagItems()) {
                if (tagPair.getName() != null) {
                    importedTags.add(TagModel.parse(tagPair.getName()));
                }
            }
        }
        viewModel.getTagItems().setAll(importedTags);

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

        // (*** DÒNG NÀY ĐÃ BỊ XÓA ***)
        // viewModel.isDirtyProperty().set(true);
    }

    /**
     * Người dùng nhấn (v) - Chấp nhận thay đổi.
     * (*** SẼ BÁO CHO VIEWMODEL NẾU ĐÂY LÀ LẦN ACCEPT ĐẦU TIÊN ***)
     */
    public void acceptImportField(String fieldName) {
        // Ẩn nút (v/x) tương ứng
        switch (fieldName) {
            case "title": showTitleReview.set(false); break;
            case "overview": showOverviewReview.set(false); break;
            case "releaseDate": showReleaseDateReview.set(false); break;
            case "studios": showStudiosReview.set(false); break;
            case "people": showPeopleReview.set(false); break;
        }

        // (*** MỚI: Bật isDirty nếu là lần Accept đầu tiên sau Import ***)
        if (importInProgress && !anyFieldAccepted) {
            anyFieldAccepted = true;
            viewModel.markAsDirtyByAccept(); // Báo cho ViewModel
        }
    }

    /**
     * Người dùng nhấn (x) - Hủy bỏ thay đổi.
     * (Logic không đổi, DirtyTracker sẽ tự xử lý)
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
                viewModel.overviewProperty().set((String) preImportState.get("overview"));
                showOverviewReview.set(false);
                break;
            case "tags":
                List<TagModel> originalTags = (List<TagModel>) preImportState.get("tags");
                if (originalTags != null) {
                    viewModel.getTagItems().setAll(originalTags);
                }
                break; // Tags không có nút (v/x) riêng nhưng vẫn cần logic revert
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
        // Listener của DirtyTracker sẽ tự động kiểm tra lại isDirty
    }

    public void hideAllReviewButtons() {
        showTitleReview.set(false);
        showOverviewReview.set(false);
        showReleaseDateReview.set(false);
        showStudiosReview.set(false);
        showPeopleReview.set(false);
    }

    /**
     * Xóa trạng thái và reset cờ import.
     */
    public void clearState() {
        preImportState.clear();
        hideAllReviewButtons();
        // (*** MỚI: Reset cờ ***)
        importInProgress = false;
        anyFieldAccepted = false;
    }

    // --- Hàm helper định dạng ---
    // ... (dateToString, studiosToString, peopleToString) ...
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
    public ReadOnlyBooleanProperty showReleaseDateReviewProperty() { return showReleaseDateReview.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showStudiosReviewProperty() { return showStudiosReview.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty showPeopleReviewProperty() { return showPeopleReview.getReadOnlyProperty(); }
}