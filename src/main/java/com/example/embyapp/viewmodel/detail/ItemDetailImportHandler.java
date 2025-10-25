package com.example.embyapp.viewmodel.detail;

import embyclient.model.BaseItemDto;
import embyclient.model.BaseItemPerson;
import embyclient.model.NameLongIdPair;
import com.example.embyapp.viewmodel.ItemDetailViewModel;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet; // <-- MỚI
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set; // <-- MỚI
import java.util.stream.Collectors;
// import org.threeten.bp.OffsetDateTime; // <-- XÓA IMPORT NÀY

// (*** THÊM IMPORT SỬA LỖI ***)
import java.time.OffsetDateTime;


/**
 * (CẬP NHẬT 17)
 * - Gọi startImport/endImport của DirtyTracker.
 * - Lưu importedDto.
 * - Theo dõi acceptedFields.
 * - Thêm getAcceptedFields() và wasImportInProgress().
 * (CẬP NHẬT 22 - SỬA LỖI BIÊN DỊCH)
 * - Sửa lỗi org.threeten.bp.OffsetDateTime.
 */
public class ItemDetailImportHandler {

    private final ItemDetailViewModel viewModel;
    private final ItemDetailDirtyTracker dirtyTracker; // Giữ tham chiếu
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    // Lưu trạng thái trước import
    private final Map<String, Object> preImportState = new HashMap<>();

    // (*** MỚI: Lưu DTO đã import và các trường đã accept ***)
    private BaseItemDto importedDto = null; // DTO gốc từ file JSON
    private final Set<String> acceptedFields = new HashSet<>(); // Tên các trường đã nhấn (✓)

    // Nút (v/x)
    private final ReadOnlyBooleanWrapper showTitleReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showOverviewReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showReleaseDateReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showStudiosReview = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper showPeopleReview = new ReadOnlyBooleanWrapper(false);

    public ItemDetailImportHandler(ItemDetailViewModel viewModel, ItemDetailDirtyTracker dirtyTracker) {
        this.viewModel = viewModel;
        this.dirtyTracker = dirtyTracker;
    }

    /**
     * Nhận DTO, cập nhật UI, báo cho DirtyTracker.
     */
    public void importAndPreview(BaseItemDto importedDto) {
        if (importedDto == null) return;

        this.importedDto = importedDto; // <-- Lưu DTO đã import

        dirtyTracker.startImport(); // <-- Báo cho Tracker bắt đầu import state

        try {
            clearStateExceptImportedDto(); // Xóa state cũ (trừ importedDto)
            hideAllReviewButtons();

            // acceptedFields đã được clear trong clearState

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
            // Tags không có nút accept riêng, coi như được accept ngầm khi import
            acceptedFields.add("tags"); // <-- Tự động accept tags

            // 4. Release Date (*** SỬA LỖI TẠI ĐÂY ***)
            preImportState.put("releaseDate", viewModel.releaseDateProperty().get());
            // importedDto.getPremiereDate() là java.time
            // hàm dateToString giờ cũng nhận java.time
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

        } finally {
            dirtyTracker.endImport(); // <-- Báo cho Tracker kết thúc cập nhật UI
        }
    }

    /**
     * Nhấn (v) - Chấp nhận.
     */
    public void acceptImportField(String fieldName) {
        // Ẩn nút (v/x)
        hideReviewButton(fieldName);

        // (*** MỚI: Thêm vào set acceptedFields ***)
        acceptedFields.add(fieldName);

        // Báo cho ViewModel (ViewModel sẽ gọi forceDirty của Tracker)
        viewModel.markAsDirtyByAccept();
    }

    /**
     * Nhấn (x) - Hủy bỏ.
     */
    @SuppressWarnings("unchecked")
    public void rejectImportField(String fieldName) {
        // (*** MỚI: Xóa khỏi set acceptedFields ***)
        acceptedFields.remove(fieldName);

        dirtyTracker.pauseTracking(); // Tạm dừng tracker khi revert UI
        try {
            // Khôi phục giá trị UI
            switch (fieldName) {
                case "title":
                    viewModel.titleProperty().set((String) preImportState.get("title"));
                    break;
                case "overview":
                    viewModel.overviewProperty().set((String) preImportState.get("overview"));
                    break;
                case "tags": // Tags không có nút reject riêng, nhưng logic vẫn cần
                    List<TagModel> originalTags = (List<TagModel>) preImportState.get("tags");
                    if (originalTags != null) {
                        viewModel.getTagItems().setAll(originalTags);
                    }
                    break;
                case "releaseDate":
                    viewModel.releaseDateProperty().set((String) preImportState.get("releaseDate"));
                    break;
                case "studios":
                    viewModel.studiosProperty().set((String) preImportState.get("studios"));
                    break;
                case "people":
                    viewModel.peopleProperty().set((String) preImportState.get("people"));
                    break;
            }
            // Ẩn nút (v/x) sau khi revert
            hideReviewButton(fieldName);
        } finally {
            dirtyTracker.resumeTracking(); // Bật lại tracker
            // Tracker sẽ tự kiểm tra lại isDirty
        }
    }

    private void hideReviewButton(String fieldName) {
        switch (fieldName) {
            case "title": showTitleReview.set(false); break;
            case "overview": showOverviewReview.set(false); break;
            // Tags không có nút
            case "releaseDate": showReleaseDateReview.set(false); break;
            case "studios": showStudiosReview.set(false); break;
            case "people": showPeopleReview.set(false); break;
        }
    }

    public void hideAllReviewButtons() {
        showTitleReview.set(false);
        showOverviewReview.set(false);
        showReleaseDateReview.set(false);
        showStudiosReview.set(false);
        showPeopleReview.set(false);
    }

    /**
     * Xóa state, reset cờ import, xóa accepted fields.
     */
    public void clearState() {
        preImportState.clear();
        hideAllReviewButtons();
        importedDto = null; // <-- Reset DTO đã import
        acceptedFields.clear(); // <-- Reset accepted fields
    }

    /**
     * Xóa state nhưng giữ lại importedDto (dùng trong importAndPreview).
     */
    private void clearStateExceptImportedDto() {
        preImportState.clear();
        hideAllReviewButtons();
        // Không reset importedDto
        acceptedFields.clear();
    }

    // --- (*** MỚI: Getters cho ViewModel ***) ---
    public boolean wasImportInProgress() {
        return importedDto != null; // Có DTO import nghĩa là đang trong quá trình import
    }

    public Set<String> getAcceptedFields() {
        return acceptedFields;
    }

    public BaseItemDto getImportedDto() {
        return importedDto;
    }


    // --- Hàm helper định dạng ---

    // (*** SỬA LỖI TẠI ĐÂY: Thay đổi kiểu tham số ***)
    private String dateToString(OffsetDateTime date) { // <-- Đổi từ org.threeten.bp sang java.time
        if (date == null) return "";
        try {
            // Logic bên trong (toInstant().toEpochMilli()) hoạt động cho cả hai
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