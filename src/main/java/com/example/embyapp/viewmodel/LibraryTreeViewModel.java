package com.example.embyapp.viewmodel;

import com.example.emby.EmbyClient.ApiException;
import com.example.emby.modelEmby.BaseItemDto;
import com.example.embyapp.service.ItemRepository;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.control.TreeItem;

import java.util.List;

/**
 * (SỬA ĐỔI) ViewModel cho LibraryTreeView.
 * Sửa lỗi: Sử dụng ReadOnly...Wrapper để expose ReadOnlyProperty.
 */
public class LibraryTreeViewModel {

    private final ItemRepository itemRepository;

    // SỬA LỖI: Dùng Wrapper để có thể expose ReadOnlyProperty ra bên ngoài
    // Controller/VM khác (như MainController) chỉ có thể Read, không thể Set.
    private final ReadOnlyBooleanWrapper loading = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyObjectWrapper<TreeItem<BaseItemDto>> rootItem = new ReadOnlyObjectWrapper<>();

    // Property này KHÔNG ReadOnly, vì nó được binding 2 chiều với TreeView
    private final ObjectProperty<TreeItem<BaseItemDto>> selectedTreeItem = new SimpleObjectProperty<>();

    // Property này KHÔNG ReadOnly, vì nó là kết quả nội bộ
    private final ObjectProperty<BaseItemDto> selectedLibraryItem = new SimpleObjectProperty<>();


    public LibraryTreeViewModel(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;

        // Xử lý logic khi selectedTreeItem thay đổi
        selectedTreeItem.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                // Chỉ cập nhật nếu nó là "thư viện" (thư mục gốc),
                // sau này có thể mở rộng để kiểm tra sâu hơn
                if (newVal.getParent() == rootItem.get()) {
                    selectedLibraryItem.set(newVal.getValue());
                } else {
                    // Nếu user chọn 1 item con (ví dụ: đang expand tree),
                    // chúng ta không muốn cập nhật Grid,
                    // nên set selectedLibraryItem về null.
                    // (Hoặc có thể tìm parent gốc của nó, tùy logic)
                    selectedLibraryItem.set(null); // Tạm thời set null
                }
            } else {
                selectedLibraryItem.set(null);
            }
        });
    }

    /**
     * Bắt đầu quá trình tải thư viện (chạy nền).
     */
    public void loadLibraries() {
        loading.set(true);

        new Thread(() -> {
            try {
                // Gọi hàm đã sửa tên trong Repository
                List<BaseItemDto> libraries = itemRepository.getRootViews();

                // Tạo root TreeItem ảo (không hiển thị)
                TreeItem<BaseItemDto> root = new TreeItem<>();
                root.setExpanded(true);

                // Thêm các thư viện làm con của root ảo
                for (BaseItemDto lib : libraries) {
                    TreeItem<BaseItemDto> libNode = new TreeItem<>(lib);
                    // (Sau này có thể thêm logic tải con cháu ở đây nếu cần expand)
                    root.getChildren().add(libNode);
                }

                // Cập nhật UI trên JavaFX thread
                Platform.runLater(() -> {
                    rootItem.set(root);
                    loading.set(false);
                });

            } catch (ApiException e) {
                System.err.println("API Error loading libraries: " + e.getMessage());
                Platform.runLater(() -> {
                    // Có thể tạo 1 TreeItem lỗi để hiển thị
                    loading.set(false);
                });
            } catch (Exception e) {
                System.err.println("Generic Error loading libraries: " + e.getMessage());
                Platform.runLater(() -> {
                    loading.set(false);
                });
            }
        }).start();
    }


    // --- Getters cho Properties (Dùng bởi Controller) ---

    // SỬA LỖI: Giờ hàm này đã hợp lệ
    public ReadOnlyBooleanProperty loadingProperty() {
        return loading.getReadOnlyProperty();
    }

    // SỬA LỖI: Giờ hàm này đã hợp lệ
    public ReadOnlyObjectProperty<TreeItem<BaseItemDto>> rootItemProperty() {
        return rootItem.getReadOnlyProperty();
    }

    public ObjectProperty<TreeItem<BaseItemDto>> selectedTreeItemProperty() {
        return selectedTreeItem;
    }

    public ReadOnlyObjectProperty<BaseItemDto> selectedLibraryItemProperty() {
        // Expose ra ngoài dưới dạng ReadOnly, vì chỉ ViewModel này được set nó
        return selectedLibraryItem;
    }
}

