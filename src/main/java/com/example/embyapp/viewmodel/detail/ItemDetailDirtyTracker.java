package com.example.embyapp.viewmodel.detail;

import com.example.embyapp.viewmodel.ItemDetailViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * (CẬP NHẬT 30) Thêm Genres.
 * - Thêm theo dõi Genres.
 * (CẬP NHẬT MỚI) Thêm CriticRating.
 */
public class ItemDetailDirtyTracker {

    private final ItemDetailViewModel viewModel;

    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);

    // Snapshot gốc
    private String originalTitle, originalOverview, originalReleaseDate, originalOriginalTitle;
    // (*** THÊM FIELD GỐC CHO RATING ***)
    private Float originalCriticRating;
    private List<TagModel> originalTagItems;
    private List<TagModel> originalStudioItems;
    private List<TagModel> originalPeopleItems;
    private List<TagModel> originalGenreItems; // (*** MỚI ***)

    // Listeners
    private final ChangeListener<String> dirtyFlagListener = (obs, oldVal, newVal) -> checkForChanges();
    // (*** THÊM LISTENER CHO RATING (Number) ***)
    private final ChangeListener<Number> ratingListener = (obs, oldVal, newVal) -> checkForChanges();
    private final ListChangeListener<TagModel> tagsListener = (c) -> checkForChanges();
    private final ListChangeListener<TagModel> studioItemsListener = (c) -> checkForChanges();
    private final ListChangeListener<TagModel> peopleItemsListener = (c) -> checkForChanges();
    private final ListChangeListener<TagModel> genreItemsListener = (c) -> checkForChanges(); // (*** MỚI ***)

    // State flags
    private boolean paused = false;
    private boolean importAcceptancePending = false; // <-- Cờ chờ accept


    public ItemDetailDirtyTracker(ItemDetailViewModel viewModel) {
        this.viewModel = viewModel;
    }

    /**
     * Bắt đầu theo dõi (sau khi tải item).
     */
    public void startTracking(Map<String, String> originalStrings) {
        updateOriginalStrings(originalStrings);
        this.originalOriginalTitle = viewModel.originalTitleProperty().get();
        // (*** THÊM DÒNG NÀY ĐỂ LẤY RATING GỐC ***)
        this.originalCriticRating = viewModel.criticRatingProperty().get();
        this.originalTagItems = new ArrayList<>(viewModel.getTagItems());
        this.originalStudioItems = new ArrayList<>(viewModel.getStudioItems());
        this.originalPeopleItems = new ArrayList<>(viewModel.getPeopleItems());
        this.originalGenreItems = new ArrayList<>(viewModel.getGenreItems()); // (*** MỚI ***)
        addListeners();
        paused = false;
        importAcceptancePending = false; // <-- Reset cờ
        isDirty.set(false);
    }

    /**
     * Dừng theo dõi (khi clear item).
     */
    public void stopTracking() {
        removeListeners();
        clearOriginals();
        paused = false;
        importAcceptancePending = false; // <-- Reset cờ
        isDirty.set(false);
    }

    // --- Import State Management ---
    // (Giữ nguyên logic startImport/endImport)

    /**
     * Được gọi bởi ImportHandler TRƯỚC KHI cập nhật UI.
     */
    public void startImport() {
        if (!importAcceptancePending) {
            // System.out.println("DirtyTracker: Entering importAcceptancePending state.");
            importAcceptancePending = true;
            isDirty.set(false); // Force disable save button
            pauseTracking(); // Tạm dừng listener
        }
    }

    /**
     * Được gọi bởi ImportHandler SAU KHI cập nhật UI xong.
     */
    public void endImport() {
        if (importAcceptancePending) { // Chỉ resume nếu thực sự đã startImport
            resumeTracking(); // Bật lại listener (sẽ gọi checkForChanges)
            // System.out.println("DirtyTracker: Resumed tracking after import UI update.");
            // checkForChanges sẽ chạy và set isDirty = false do importAcceptancePending = true
        }
    }

    // --- Pause / Resume Listeners ---

    public void pauseTracking() {
        if (!paused) {
            removeListeners();
            paused = true;
        }
    }

    public void resumeTracking() {
        if (paused) {
            addListeners();
            paused = false;
            // Quan trọng: Kiểm tra ngay sau khi resume
            checkForChanges();
        }
    }

    private void addListeners() {
        viewModel.titleProperty().addListener(dirtyFlagListener);
        // (*** THÊM LISTENER CHO RATING ***)
        viewModel.criticRatingProperty().addListener(ratingListener);
        viewModel.overviewProperty().addListener(dirtyFlagListener);
        viewModel.releaseDateProperty().addListener(dirtyFlagListener);
        viewModel.originalTitleProperty().addListener(dirtyFlagListener);

        if (viewModel.getTagItems() != null) {
            viewModel.getTagItems().addListener(tagsListener);
        }
        if (viewModel.getStudioItems() != null) {
            viewModel.getStudioItems().addListener(studioItemsListener);
        }
        if (viewModel.getPeopleItems() != null) {
            viewModel.getPeopleItems().addListener(peopleItemsListener);
        }
        if (viewModel.getGenreItems() != null) { // (*** MỚI ***)
            viewModel.getGenreItems().addListener(genreItemsListener);
        }
    }

    private void removeListeners() {
        viewModel.titleProperty().removeListener(dirtyFlagListener);
        // (*** XÓA LISTENER CHO RATING ***)
        viewModel.criticRatingProperty().removeListener(ratingListener);
        viewModel.overviewProperty().removeListener(dirtyFlagListener);
        viewModel.releaseDateProperty().removeListener(dirtyFlagListener);
        viewModel.originalTitleProperty().removeListener(dirtyFlagListener);

        if (viewModel.getTagItems() != null) {
            viewModel.getTagItems().removeListener(tagsListener);
        }
        if (viewModel.getStudioItems() != null) {
            viewModel.getStudioItems().removeListener(studioItemsListener);
        }
        if (viewModel.getPeopleItems() != null) {
            viewModel.getPeopleItems().removeListener(peopleItemsListener);
        }
        if (viewModel.getGenreItems() != null) { // (*** MỚI ***)
            viewModel.getGenreItems().removeListener(genreItemsListener);
        }
    }

    // --- Dirty Check Logic ---

    /**
     * Kiểm tra thay đổi, tôn trọng trạng thái importAcceptancePending.
     */
    private void checkForChanges() {
        if (paused) return; // Bỏ qua khi pause

        // Nếu đang chờ accept đầu tiên, luôn là false
        if (importAcceptancePending) {
            // System.out.println("DirtyTracker: Check skipped, pending first accept.");
            isDirty.set(false);
            return;
        }

        // Nếu không chờ accept, kiểm tra bình thường
        if (originalTitle == null && originalTagItems == null) {
            isDirty.set(false); // Chưa start
            return;
        }

        boolean stringChanges = !Objects.equals(viewModel.titleProperty().get(), originalTitle) ||
                !Objects.equals(viewModel.overviewProperty().get(), originalOverview) ||
                !Objects.equals(viewModel.releaseDateProperty().get(), originalReleaseDate) ||
                !Objects.equals(viewModel.originalTitleProperty().get(), originalOriginalTitle);

        // (*** THÊM CHECK THAY ĐỔI RATING ***)
        boolean ratingChanges = !Objects.equals(viewModel.criticRatingProperty().get(), originalCriticRating);

        boolean tagChanges = !Objects.equals(viewModel.getTagItems(), originalTagItems);

        boolean studioChanges = !Objects.equals(viewModel.getStudioItems(), originalStudioItems);
        boolean peopleChanges = !Objects.equals(viewModel.getPeopleItems(), originalPeopleItems);
        boolean genreChanges = !Objects.equals(viewModel.getGenreItems(), originalGenreItems); // (*** MỚI ***)

        // (*** THÊM RATING VÀO CHECK CUỐI CÙNG ***)
        isDirty.set(stringChanges || ratingChanges || tagChanges || studioChanges || peopleChanges || genreChanges);
        // System.out.println("DirtyTracker: isDirty = " + isDirty.get()); // Debug
    }

    /**
     * Bật cờ isDirty (thường do Accept hoặc Sửa thủ công).
     * Thoát khỏi trạng thái importAcceptancePending nếu đang ở đó.
     */
    public void forceDirty() { // <-- Đảm bảo phương thức này tồn tại
        if (paused) return; // Không force khi đang pause

        // Nếu đang chờ accept, đây là lần accept đầu tiên
        if (importAcceptancePending) {
            // System.out.println("DirtyTracker: Exiting importAcceptancePending state due to first accept.");
            importAcceptancePending = false; // Thoát trạng thái chờ
            isDirty.set(true); // Bật nút Save
        }
        // Nếu không chờ accept và chưa dirty (sửa thủ công)
        else if (!isDirty.get()) {
            // System.out.println("DirtyTracker: Forcing isDirty = true (manual edit detected).");
            isDirty.set(true); // Bật nút Save
        }
        // Nếu đã dirty rồi thì không cần làm gì
    }

    // --- Update Originals ---

    private void updateOriginalStrings(Map<String, String> originals) {
        this.originalTitle = originals.get("title");
        this.originalOverview = originals.get("overview");
        this.originalReleaseDate = originals.get("releaseDate");
    }

    /**
     * Cập nhật snapshot gốc SAU KHI LƯU THÀNH CÔNG.
     * Reset cả trạng thái import pending.
     */
    public void updateOriginalStringsFromCurrent() {
        // System.out.println("DirtyTracker: Updating originals from current UI after save.");
        this.originalTitle = viewModel.titleProperty().get();
        this.originalOriginalTitle = viewModel.originalTitleProperty().get();
        this.originalOverview = viewModel.overviewProperty().get();
        this.originalReleaseDate = viewModel.releaseDateProperty().get();
        this.originalCriticRating = viewModel.criticRatingProperty().get();

        this.originalTagItems = new ArrayList<>(viewModel.getTagItems());
        this.originalStudioItems = new ArrayList<>(viewModel.getStudioItems());
        this.originalPeopleItems = new ArrayList<>(viewModel.getPeopleItems());
        this.originalGenreItems = new ArrayList<>(viewModel.getGenreItems()); // (*** MỚI ***)

        importAcceptancePending = false; // <-- Reset cờ sau khi lưu

        // Kiểm tra lại (sẽ set isDirty về false nếu không paused)
        checkForChanges();
    }

    /**
     * Cập nhật snapshot gốc CHỈ CHO RATING sau khi lưu thành công (từ hàm saveCriticRatingImmediately).
     * Reset cờ dirty NẾU không còn thay đổi nào khác.
     */
    public void updateOriginalRating(Float newRating) {
        if (paused) return; // Không làm gì nếu đang pause

        // System.out.println("DirtyTracker: Updating original rating baseline to: " + newRating);
        this.originalCriticRating = newRating;

        // Reset cờ import nếu nó đang bật (mặc dù kịch bản này ít xảy ra)
        if (importAcceptancePending) {
            importAcceptancePending = false;
        }

        // Kiểm tra lại (sẽ set isDirty về false NẾU không còn thay đổi nào khác)
        checkForChanges();
    }

    /**
     * Xóa snapshot gốc.
     */
    private void clearOriginals() {
        this.originalTitle = null;
        this.originalOriginalTitle = null;
        this.originalOverview = null;
        this.originalReleaseDate = null;
        // (*** THÊM DÒNG NÀY ĐỂ XÓA RATING GỐC ***)
        this.originalCriticRating = null;

        this.originalTagItems = null;
        this.originalStudioItems = null;
        this.originalPeopleItems = null;
        this.originalGenreItems = null; // (*** MỚI ***)
    }

    // --- Getter ---
    public BooleanProperty isDirtyProperty() {
        return isDirty;
    }
}