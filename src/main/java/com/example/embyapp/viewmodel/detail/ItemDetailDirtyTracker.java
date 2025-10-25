// Đặt tại: src/main/java/com/example/embyapp/viewmodel/detail/ItemDetailDirtyTracker.java
package com.example.embyapp.viewmodel.detail;

import com.example.embyapp.viewmodel.ItemDetailViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

import java.util.Map;
import java.util.Objects;

/**
 * Lớp phụ trợ (Helper class) cho ItemDetailViewModel.
 * Chuyên trách việc Theo dõi (Track) các thay đổi (Dirty state) trên UI.
 */
public class ItemDetailDirtyTracker {

    private final ItemDetailViewModel viewModel; // Tham chiếu đến VM chính để lấy giá trị hiện tại

    // Trạng thái "Dirty"
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);

    // Snapshot các giá trị gốc
    private String originalTitle, originalOverview, originalTags, originalReleaseDate, originalStudios, originalPeople;

    // Listener để theo dõi thay đổi
    private final ChangeListener<String> dirtyFlagListener = (obs, oldVal, newVal) -> checkForChanges();

    public ItemDetailDirtyTracker(ItemDetailViewModel viewModel) {
        this.viewModel = viewModel;
    }

    /**
     * Bắt đầu theo dõi. Được gọi sau khi tải dữ liệu xong.
     * @param originalStrings Map chứa các giá trị chuỗi gốc.
     */
    public void startTracking(Map<String, String> originalStrings) {
        // 1. Lưu snapshot gốc
        updateOriginalStrings(originalStrings);

        // 2. Gắn listener vào các property của VM
        viewModel.titleProperty().addListener(dirtyFlagListener);
        viewModel.overviewProperty().addListener(dirtyFlagListener);
        viewModel.tagsProperty().addListener(dirtyFlagListener);
        viewModel.releaseDateProperty().addListener(dirtyFlagListener);
        viewModel.studiosProperty().addListener(dirtyFlagListener);
        viewModel.peopleProperty().addListener(dirtyFlagListener);

        // 3. Reset trạng thái
        isDirty.set(false);
    }

    /**
     * Dừng theo dõi. Được gọi khi clear item.
     */
    public void stopTracking() {
        // 1. Xóa listener
        viewModel.titleProperty().removeListener(dirtyFlagListener);
        viewModel.overviewProperty().removeListener(dirtyFlagListener);
        viewModel.tagsProperty().removeListener(dirtyFlagListener);
        viewModel.releaseDateProperty().removeListener(dirtyFlagListener);
        viewModel.studiosProperty().removeListener(dirtyFlagListener);
        viewModel.peopleProperty().removeListener(dirtyFlagListener);

        // 2. Xóa snapshot
        this.originalTitle = null;
        this.originalOverview = null;
        this.originalTags = null;
        this.originalReleaseDate = null;
        this.originalStudios = null;
        this.originalPeople = null;

        // 3. Reset trạng thái
        isDirty.set(false);
    }

    /**
     * Logic kiểm tra thay đổi.
     */
    private void checkForChanges() {
        if (originalTitle == null) return; // Chưa bắt đầu theo dõi

        boolean hasChanges = !Objects.equals(viewModel.titleProperty().get(), originalTitle) ||
                !Objects.equals(viewModel.overviewProperty().get(), originalOverview) ||
                !Objects.equals(viewModel.tagsProperty().get(), originalTags) ||
                !Objects.equals(viewModel.releaseDateProperty().get(), originalReleaseDate) ||
                !Objects.equals(viewModel.studiosProperty().get(), originalStudios) ||
                !Objects.equals(viewModel.peopleProperty().get(), originalPeople);

        isDirty.set(hasChanges);
    }

    /**
     * Cập nhật snapshot gốc từ một Map (thường là sau khi tải).
     */
    public void updateOriginalStrings(Map<String, String> originals) {
        this.originalTitle = originals.get("title");
        this.originalOverview = originals.get("overview");
        this.originalTags = originals.get("tags");
        this.originalReleaseDate = originals.get("releaseDate");
        this.originalStudios = originals.get("studios");
        this.originalPeople = originals.get("people");
    }

    /**
     * Cập nhật snapshot gốc từ các giá trị HIỆN TẠI của UI (thường là sau khi lưu).
     */
    public void updateOriginalStringsFromCurrent() {
        this.originalTitle = viewModel.titleProperty().get();
        this.originalOverview = viewModel.overviewProperty().get();
        this.originalTags = viewModel.tagsProperty().get();
        this.originalReleaseDate = viewModel.releaseDateProperty().get();
        this.originalStudios = viewModel.studiosProperty().get();
        this.originalPeople = viewModel.peopleProperty().get();

        // Sau khi cập nhật, kiểm tra lại (thường sẽ set isDirty về false)
        checkForChanges();
    }

    // --- Getter cho Property ---
    public BooleanProperty isDirtyProperty() {
        return isDirty;
    }
}