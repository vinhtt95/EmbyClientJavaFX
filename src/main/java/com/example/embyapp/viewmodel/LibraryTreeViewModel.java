package com.example.embyapp.viewmodel;

import embyclient.ApiException;
import embyclient.model.BaseItemDto;
import com.example.embyapp.service.ItemRepository;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.control.TreeItem;

import java.util.List;
import java.util.stream.Collectors; // (CẬP NHẬT) Thêm import

/**
 * (SỬA ĐỔI) ViewModel cho LibraryTreeView.
 * Sửa lỗi: Sử dụng ReadOnly...Wrapper để expose ReadOnlyProperty.
 * SỬA ĐỔI (Lần 2): Thêm logic "lazy loading" cho double-click.
 * (CẬP NHẬT 3): Lọc danh sách item, chỉ hiển thị folder trong cây.
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
                // 1. Lấy TẤT CẢ item gốc từ Repository
                List<BaseItemDto> allRootItems = itemRepository.getRootViews();

                // 2. (CẬP NHẬT) Lọc danh sách, chỉ giữ lại FOLDER
                List<BaseItemDto> libraries = allRootItems.stream()
                        .filter(item -> item.isIsFolder() != null && item.isIsFolder())
                        .collect(Collectors.toList());

                // Tạo root TreeItem ảo (không hiển thị)
                TreeItem<BaseItemDto> root = new TreeItem<>();
                root.setExpanded(true);

                // 3. Thêm các thư viện (đã lọc) làm con của root ảo
                for (BaseItemDto lib : libraries) {
                    TreeItem<BaseItemDto> libNode = new TreeItem<>(lib);

                    // (SỬA ĐỔI Lần 2) Nếu item là thư mục, thêm 1 node "giả" (dummy)
                    // Chúng ta đã biết đây là folder, nên luôn thêm dummy node
                    libNode.getChildren().add(new TreeItem<>(null)); // Dummy node

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
                // 1. (CẬP NHẬT) Gọi Repository để lấy TẤT CẢ các item con
                List<BaseItemDto> allChildren = itemRepository.getItemsByParentId(parentId);

                // 2. (CẬP NHẬT) Lọc danh sách, chỉ giữ lại FOLDER
                List<BaseItemDto> folderChildren = allChildren.stream()
                        .filter(child -> child.isIsFolder() != null && child.isIsFolder())
                        .collect(Collectors.toList());

                Platform.runLater(() -> {
                    // Xóa node "giả" (dummy node)
                    item.getChildren().clear();

                    // 3. (CẬP NHẬT) Thêm các node con (chỉ folder)
                    for (BaseItemDto childDto : folderChildren) {
                        TreeItem<BaseItemDto> childNode = new TreeItem<>(childDto);

                        // (QUAN TRỌNG) Thêm dummy node cho các node con này
                        // vì chúng ta biết chúng là folder
                        childNode.getChildren().add(new TreeItem<>(null));
                        item.getChildren().add(childNode);
                    }
                    // Tự động expand node cha sau khi tải xong
                    // (Nếu folderChildren rỗng, nó sẽ clear dummy node và không expand,
                    // điều này sẽ làm mũi tên biến mất, đúng như ý bạn)
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

