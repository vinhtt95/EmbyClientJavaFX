package com.example.embyapp.viewmodel.detail;

import com.example.embyapp.viewmodel.ItemDetailViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener; // (*** MỚI IMPORT ***)

import java.util.ArrayList; // (*** MỚI IMPORT ***)
import java.util.List; // (*** MỚI IMPORT ***)
import java.util.Map;
import java.util.Objects;

/**
 * (CẬP NHẬT 7)
 * - Thêm ListChangeListener để theo dõi ObservableList<TagModel> tagItems.
 * - Logic checkForChanges giờ cũng so sánh 2 List<TagModel>.
 */
public class ItemDetailDirtyTracker {

    private final ItemDetailViewModel viewModel; // Tham chiếu đến VM chính để lấy giá trị hiện tại

    // Trạng thái "Dirty"
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);

    // Snapshot các giá trị gốc
    private String originalTitle, originalOverview, originalReleaseDate, originalStudios, originalPeople;
    private List<TagModel> originalTagItems; // (*** MỚI ***)

    // Listener cho StringProperty
    private final ChangeListener<String> dirtyFlagListener = (obs, oldVal, newVal) -> checkForChanges();

    // (*** MỚI ***) Listener cho ObservableList<TagModel>
    private final ListChangeListener<TagModel> tagsListener = (c) -> checkForChanges();


    public ItemDetailDirtyTracker(ItemDetailViewModel viewModel) {
        this.viewModel = viewModel;
    }

    /**
     * Bắt đầu theo dõi. Được gọi sau khi tải dữ liệu xong.
     * @param originalStrings Map chứa các giá trị chuỗi gốc.
     */
    public void startTracking(Map<String, String> originalStrings) {
        // 1. Lưu snapshot gốc (cho Strings)
        updateOriginalStrings(originalStrings);

        // (*** MỚI ***) 1b. Lưu snapshot gốc (cho Tags)
        // Tạo một bản sao (copy) của danh sách
        this.originalTagItems = new ArrayList<>(viewModel.getTagItems());

        // 2. Gắn listener vào các property của VM
        viewModel.titleProperty().addListener(dirtyFlagListener);
        viewModel.overviewProperty().addListener(dirtyFlagListener);
        viewModel.releaseDateProperty().addListener(dirtyFlagListener);
        viewModel.studiosProperty().addListener(dirtyFlagListener);
        viewModel.peopleProperty().addListener(dirtyFlagListener);

        // (*** MỚI ***) 2b. Gắn listener vào List<TagModel>
        viewModel.getTagItems().addListener(tagsListener);

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
        viewModel.releaseDateProperty().removeListener(dirtyFlagListener);
        viewModel.studiosProperty().removeListener(dirtyFlagListener);
        viewModel.peopleProperty().removeListener(dirtyFlagListener);

        // (*** MỚI ***) 1b. Xóa listener khỏi List
        if (this.originalTagItems != null) { // Thêm kiểm tra null
            viewModel.getTagItems().removeListener(tagsListener);
        }

        // 2. Xóa snapshot
        this.originalTitle = null;
        this.originalOverview = null;
        this.originalReleaseDate = null;
        this.originalStudios = null;
        this.originalPeople = null;
        this.originalTagItems = null; // (*** MỚI ***)

        // 3. Reset trạng thái
        isDirty.set(false);
    }

    /**
     * (*** SỬA ĐỔI ***) Logic kiểm tra thay đổi.
     */
    private void checkForChanges() {
        if (originalTitle == null) return; // Chưa bắt đầu theo dõi

        // So sánh các String
        boolean stringChanges = !Objects.equals(viewModel.titleProperty().get(), originalTitle) ||
                !Objects.equals(viewModel.overviewProperty().get(), originalOverview) ||
                !Objects.equals(viewModel.releaseDateProperty().get(), originalReleaseDate) ||
                !Objects.equals(viewModel.studiosProperty().get(), originalStudios) ||
                !Objects.equals(viewModel.peopleProperty().get(), originalPeople);

        // (*** MỚI ***) So sánh List<TagModel>
        // Sử dụng hàm equals() của List, hàm này sẽ so sánh size và
        // gọi .equals() trên từng TagModel (đó là lý do ta implement equals/hashCode)
        boolean tagChanges = !Objects.equals(viewModel.getTagItems(), originalTagItems);

        isDirty.set(stringChanges || tagChanges);
    }

    /**
     * Cập nhật snapshot gốc từ một Map (thường là sau khi tải).
     */
    public void updateOriginalStrings(Map<String, String> originals) {
        this.originalTitle = originals.get("title");
        this.originalOverview = originals.get("overview");
        this.originalReleaseDate = originals.get("releaseDate");
        this.originalStudios = originals.get("studios");
        this.originalPeople = originals.get("people");
        // (Không cập nhật tags ở đây, đã làm trong startTracking)
    }

    /**
     * (*** SỬA ĐỔI ***)
     * Cập nhật snapshot gốc từ các giá trị HIỆN TẠI của UI (thường là sau khi lưu).
     */
    public void updateOriginalStringsFromCurrent() {
        this.originalTitle = viewModel.titleProperty().get();
        this.originalOverview = viewModel.overviewProperty().get();
        this.originalReleaseDate = viewModel.releaseDateProperty().get();
        this.originalStudios = viewModel.studiosProperty().get();
        this.originalPeople = viewModel.peopleProperty().get();

        // (*** MỚI ***) Cập nhật snapshot cho List<TagModel>
        this.originalTagItems = new ArrayList<>(viewModel.getTagItems());

        // Sau khi cập nhật, kiểm tra lại (sẽ set isDirty về false)
        checkForChanges();
    }

    // --- Getter cho Property ---
    public BooleanProperty isDirtyProperty() {
        return isDirty;
    }
}