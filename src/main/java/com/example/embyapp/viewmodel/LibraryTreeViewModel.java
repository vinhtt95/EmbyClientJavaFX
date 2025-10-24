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
 * SỬA ĐỔI (Lần 2): Thêm logic "lazy loading" cho double-click.
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
                // (SỬA ĐỔI Lần 2) Luôn set item được chọn
                selectedLibraryItem.set(newVal.getValue());
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

                    // (SỬA ĐỔI Lần 2) Nếu item là thư mục, thêm 1 node "giả" (dummy)
                    // để JavaFX hiển thị mũi tên expand (>)
                    if (lib.isIsFolder() != null && lib.isIsFolder()) {
                        libNode.getChildren().add(new TreeItem<>(null)); // Dummy node
                    }

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

    // (HÀM MỚI - Lần 2) Tải các item con cho một TreeItem (dùng cho double-click)
    public void loadChildrenForItem(TreeItem<BaseItemDto> item) {
        // Kiểm tra điều kiện
        if (item == null || item.getValue() == null) {
            return;
        }
        // Kiểm tra xem đã load con chưa (nếu node con đầu tiên không phải là "giả")
        if (!item.isLeaf() && (item.getChildren().isEmpty() || item.getChildren().get(0).getValue() != null)) {
            // Đã load rồi, chỉ cần expand
            item.setExpanded(true);
            return;
        }

        // Bắt đầu chạy nền để tải
        new Thread(() -> {
            try {
                String parentId = item.getValue().getId();
                // Gọi Repository để lấy các item con
                List<BaseItemDto> children = itemRepository.getItemsByParentId(parentId);

                Platform.runLater(() -> {
                    // Xóa node "giả" (dummy node)
                    item.getChildren().clear();

                    // Thêm các node con thật
                    for (BaseItemDto childDto : children) {
                        TreeItem<BaseItemDto> childNode = new TreeItem<>(childDto);

                        // (QUAN TRỌNG) Thêm dummy node cho các node con này
                        // để chúng có thể được expand sau này
                        if (childDto.isIsFolder() != null && childDto.isIsFolder()) {
                            childNode.getChildren().add(new TreeItem<>(null));
                        }
                        item.getChildren().add(childNode);
                    }
                    // Tự động expand node cha sau khi tải xong
                    item.setExpanded(true);
                });

            } catch (ApiException e) {
                System.err.println("API Error loading children for tree: " + e.getMessage());
                // (Có thể thêm logic xóa dummy node và hiển thị lỗi)
                Platform.runLater(() -> {
                    item.getChildren().clear(); // Xóa dummy node khi lỗi
                    item.setExpanded(false);
                });
            } catch (Exception e) {
                System.err.println("Generic Error loading children for tree: " + e.getMessage());
                Platform.runLater(() -> {
                    item.getChildren().clear();
                    item.setExpanded(false);
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

