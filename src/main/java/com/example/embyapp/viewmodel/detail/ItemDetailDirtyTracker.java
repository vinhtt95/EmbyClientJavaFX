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
 * (CẬP NHẬT 16)
 * - Thêm pauseTracking() và resumeTracking() để tạm dừng listener khi import.
 * (CẬP NHẬT 17)
 * - Thêm importAcceptancePending state.
 * - forceDirty() now transitions out of pending state.
 * - checkForChanges() respects pending state.
 */
public class ItemDetailDirtyTracker {

    private final ItemDetailViewModel viewModel;

    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);

    // Snapshot gốc
    private String originalTitle, originalOverview, originalReleaseDate, originalStudios, originalPeople;
    private List<TagModel> originalTagItems;

    // Listeners
    private final ChangeListener<String> dirtyFlagListener = (obs, oldVal, newVal) -> checkForChanges();
    private final ListChangeListener<TagModel> tagsListener = (c) -> checkForChanges();

    // State flags
    private boolean paused = false;
    private boolean importAcceptancePending = false; // <-- MỚI: Cờ chờ accept


    public ItemDetailDirtyTracker(ItemDetailViewModel viewModel) {
        this.viewModel = viewModel;
    }

    /**
     * Bắt đầu theo dõi (sau khi tải item).
     */
    public void startTracking(Map<String, String> originalStrings) {
        updateOriginalStrings(originalStrings);
        this.originalTagItems = new ArrayList<>(viewModel.getTagItems());
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

    /**
     * Được gọi bởi ImportHandler TRƯỚC KHI cập nhật UI.
     */
    public void startImport() {
        if (!importAcceptancePending) {
            System.out.println("DirtyTracker: Entering importAcceptancePending state.");
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
            System.out.println("DirtyTracker: Resumed tracking after import UI update.");
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
        viewModel.overviewProperty().addListener(dirtyFlagListener);
        viewModel.releaseDateProperty().addListener(dirtyFlagListener);
        viewModel.studiosProperty().addListener(dirtyFlagListener);
        viewModel.peopleProperty().addListener(dirtyFlagListener);
        if (viewModel.getTagItems() != null) {
            viewModel.getTagItems().addListener(tagsListener);
        }
    }

    private void removeListeners() {
        viewModel.titleProperty().removeListener(dirtyFlagListener);
        viewModel.overviewProperty().removeListener(dirtyFlagListener);
        viewModel.releaseDateProperty().removeListener(dirtyFlagListener);
        viewModel.studiosProperty().removeListener(dirtyFlagListener);
        viewModel.peopleProperty().removeListener(dirtyFlagListener);
        if (viewModel.getTagItems() != null) {
            viewModel.getTagItems().removeListener(tagsListener);
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
                !Objects.equals(viewModel.studiosProperty().get(), originalStudios) ||
                !Objects.equals(viewModel.peopleProperty().get(), originalPeople);

        boolean tagChanges = !Objects.equals(viewModel.getTagItems(), originalTagItems);

        isDirty.set(stringChanges || tagChanges);
        // System.out.println("DirtyTracker: isDirty = " + isDirty.get()); // Debug
    }

    /**
     * Bật cờ isDirty (thường do Accept hoặc Sửa thủ công).
     * Thoát khỏi trạng thái importAcceptancePending nếu đang ở đó.
     */
    public void forceDirty() {
        if (paused) return; // Không force khi đang pause

        // Nếu đang chờ accept, đây là lần accept đầu tiên
        if (importAcceptancePending) {
            System.out.println("DirtyTracker: Exiting importAcceptancePending state due to first accept.");
            importAcceptancePending = false; // Thoát trạng thái chờ
            isDirty.set(true); // Bật nút Save
        }
        // Nếu không chờ accept và chưa dirty (sửa thủ công)
        else if (!isDirty.get()) {
            System.out.println("DirtyTracker: Forcing isDirty = true (manual edit detected).");
            isDirty.set(true); // Bật nút Save
        }
        // Nếu đã dirty rồi thì không cần làm gì
    }

    // --- Update Originals ---

    private void updateOriginalStrings(Map<String, String> originals) {
        this.originalTitle = originals.get("title");
        this.originalOverview = originals.get("overview");
        this.originalReleaseDate = originals.get("releaseDate");
        this.originalStudios = originals.get("studios");
        this.originalPeople = originals.get("people");
    }

    /**
     * Cập nhật snapshot gốc SAU KHI LƯU THÀNH CÔNG.
     * Reset cả trạng thái import pending.
     */
    public void updateOriginalStringsFromCurrent() {
        System.out.println("DirtyTracker: Updating originals from current UI after save.");
        this.originalTitle = viewModel.titleProperty().get();
        this.originalOverview = viewModel.overviewProperty().get();
        this.originalReleaseDate = viewModel.releaseDateProperty().get();
        this.originalStudios = viewModel.studiosProperty().get();
        this.originalPeople = viewModel.peopleProperty().get();
        this.originalTagItems = new ArrayList<>(viewModel.getTagItems());

        importAcceptancePending = false; // <-- Reset cờ sau khi lưu

        // Kiểm tra lại (sẽ set isDirty về false nếu không paused)
        checkForChanges();
    }

    /**
     * Xóa snapshot gốc.
     */
    private void clearOriginals() {
        this.originalTitle = null;
        this.originalOverview = null;
        this.originalReleaseDate = null;
        this.originalStudios = null;
        this.originalPeople = null;
        this.originalTagItems = null;
    }

    // --- Getter ---
    public BooleanProperty isDirtyProperty() {
        return isDirty;
    }
}