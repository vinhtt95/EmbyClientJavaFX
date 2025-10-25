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
 * (CẬP NHẬT 7)
 * - Thêm ListChangeListener để theo dõi ObservableList<TagModel> tagItems.
 * - Logic checkForChanges giờ cũng so sánh 2 List<TagModel>.
 * (CẬP NHẬT 11)
 * - Thêm hàm forceDirty() để ViewModel có thể chủ động bật cờ isDirty.
 */
public class ItemDetailDirtyTracker {

    private final ItemDetailViewModel viewModel; // Tham chiếu đến VM chính

    // Trạng thái "Dirty"
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);

    // Snapshot các giá trị gốc
    private String originalTitle, originalOverview, originalReleaseDate, originalStudios, originalPeople;
    private List<TagModel> originalTagItems;

    // Listener cho StringProperty
    private final ChangeListener<String> dirtyFlagListener = (obs, oldVal, newVal) -> checkForChanges();

    // Listener cho ObservableList<TagModel>
    private final ListChangeListener<TagModel> tagsListener = (c) -> checkForChanges();


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
        this.originalTagItems = new ArrayList<>(viewModel.getTagItems());

        // 2. Gắn listener
        viewModel.titleProperty().addListener(dirtyFlagListener);
        viewModel.overviewProperty().addListener(dirtyFlagListener);
        viewModel.releaseDateProperty().addListener(dirtyFlagListener);
        viewModel.studiosProperty().addListener(dirtyFlagListener);
        viewModel.peopleProperty().addListener(dirtyFlagListener);
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
        // Quan trọng: Chỉ remove listener nếu list gốc đã được khởi tạo
        if (this.originalTagItems != null) {
            viewModel.getTagItems().removeListener(tagsListener);
        }

        // 2. Xóa snapshot
        this.originalTitle = null;
        this.originalOverview = null;
        this.originalReleaseDate = null;
        this.originalStudios = null;
        this.originalPeople = null;
        this.originalTagItems = null;

        // 3. Reset trạng thái
        isDirty.set(false);
    }

    /**
     * Logic kiểm tra thay đổi (so sánh UI hiện tại với snapshot gốc).
     */
    private void checkForChanges() {
        // Kiểm tra xem đã bắt đầu theo dõi chưa (ít nhất một snapshot phải khác null)
        if (originalTitle == null && originalTagItems == null) return;

        // So sánh các String
        boolean stringChanges = !Objects.equals(viewModel.titleProperty().get(), originalTitle) ||
                !Objects.equals(viewModel.overviewProperty().get(), originalOverview) ||
                !Objects.equals(viewModel.releaseDateProperty().get(), originalReleaseDate) ||
                !Objects.equals(viewModel.studiosProperty().get(), originalStudios) ||
                !Objects.equals(viewModel.peopleProperty().get(), originalPeople);

        // So sánh List<TagModel>
        boolean tagChanges = !Objects.equals(viewModel.getTagItems(), originalTagItems);

        isDirty.set(stringChanges || tagChanges);
    }

    /**
     * (*** MỚI: HÀM ĐỂ VIEWMODEL GỌI ***)
     * Chủ động đặt trạng thái là dirty.
     * Thường được gọi sau khi Accept một trường import.
     */
    public void forceDirty() {
        if (!isDirty.get()) { // Chỉ set nếu nó chưa dirty
            System.out.println("DirtyTracker: Forcing isDirty = true due to import accept.");
            isDirty.set(true);
        }
    }


    /**
     * Cập nhật snapshot gốc từ một Map (sau khi tải).
     */
    public void updateOriginalStrings(Map<String, String> originals) {
        this.originalTitle = originals.get("title");
        this.originalOverview = originals.get("overview");
        this.originalReleaseDate = originals.get("releaseDate");
        this.originalStudios = originals.get("studios");
        this.originalPeople = originals.get("people");
        // Snapshot cho tags được cập nhật riêng trong startTracking và updateOriginalStringsFromCurrent
    }

    /**
     * Cập nhật snapshot gốc từ các giá trị HIỆN TẠI của UI (sau khi lưu).
     */
    public void updateOriginalStringsFromCurrent() {
        this.originalTitle = viewModel.titleProperty().get();
        this.originalOverview = viewModel.overviewProperty().get();
        this.originalReleaseDate = viewModel.releaseDateProperty().get();
        this.originalStudios = viewModel.studiosProperty().get();
        this.originalPeople = viewModel.peopleProperty().get();
        this.originalTagItems = new ArrayList<>(viewModel.getTagItems());

        // Sau khi cập nhật, kiểm tra lại (sẽ set isDirty về false)
        checkForChanges();
    }

    // --- Getter cho Property ---
    public BooleanProperty isDirtyProperty() {
        // Trả về chính property để ViewModel có thể bind vào
        return isDirty;
    }
}