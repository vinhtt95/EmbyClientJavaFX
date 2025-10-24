package com.example.embyapp.service;

import com.example.emby.EmbyClient.ApiException;
import com.example.emby.EmbyClient.Java.ItemsServiceApi;
import com.example.emby.modelEmby.BaseItemDto;
import com.example.emby.modelEmby.QueryResultBaseItemDto;

import java.util.Collections;
import java.util.List;
// (CẬP NHẬT) Xóa import stream, vì chúng ta không lọc ở đây nữa
// import java.util.stream.Collectors;

/**
 * (SỬA ĐỔI) Repository để quản lý việc truy xuất BaseItemDto (Thư viện, Phim, Series...).
 * Sửa lỗi tên hàm và logic lấy userId.
 * SỬA ĐỔI (Lần 2): Sửa lỗi bug Integer.parseInt(parentId).
 * (CẬP NHẬT 3): ĐÃ HOÀN TÁC - Trả về tất cả item, không lọc.
 */
public class ItemRepository {

    private final EmbyService embyService;
    private ItemsServiceApi itemsService; // Cache API service

    // SỬA LỖI: Constructor rỗng, tự lấy Singleton
    public ItemRepository() {
        this.embyService = EmbyService.getInstance();
    }

    private ItemsServiceApi getItemsService() {
        if (itemsService == null && embyService.isLoggedIn()) {
            this.itemsService = embyService.getItemsServiceApi();
        }
        return this.itemsService;
    }

    /**
     * SỬA LỖI: Đổi tên hàm thành getRootViews() (như ViewModel đang gọi)
     * SỬA LỖI: Tự lấy userId từ EmbyService, không nhận tham số.
     *
     * Lấy các thư viện gốc (Views) của user hiện tại.
     *
     * @return Danh sách BaseItemDto là các thư mục gốc.
     * @throws ApiException Nếu API call thất bại.
     */
    public List<BaseItemDto> getRootViews() throws ApiException {
        if (!embyService.isLoggedIn()) {
            throw new IllegalStateException("Không thể lấy items khi chưa đăng nhập.");
        }
        // SỬA LỖI: Tự lấy userId
        String userId = embyService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Không thể lấy UserID từ EmbyService.");
        }

        ItemsServiceApi service = getItemsService();
        if (service == null) {
            throw new IllegalStateException("ItemsServiceApi is null.");
        }

        // Gọi API để lấy các mục gốc (User Views)
        QueryResultBaseItemDto result;

        result = new RequestEmby().getUsersByUseridItems(userId, service);

        if (result != null && result.getItems() != null) {
            // (CẬP NHẬT) Đã hoàn tác. Trả về mọi thứ.
            return result.getItems();
        }
        return Collections.emptyList();
    }


    /**
     * Lấy các items con dựa trên parentId.
     *
     * @param parentId ID của thư mục cha.
     * @return Danh sách BaseItemDto là con.
     * @throws ApiException Nếu API call thất bại.
     */
    public List<BaseItemDto> getItemsByParentId(String parentId) throws ApiException {
        if (!embyService.isLoggedIn()) {
            throw new IllegalStateException("Không thể lấy items khi chưa đăng nhập.");
        }
        String userId = embyService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Không thể lấy UserID từ EmbyService.");
        }

        ItemsServiceApi service = getItemsService();
        if (service == null) {
            throw new IllegalStateException("ItemsServiceApi is null.");
        }

        QueryResultBaseItemDto result = new RequestEmby().getQueryResultBaseItemDto(parentId, service);

        if (result != null && result.getItems() != null) {
            // (CẬP NHẬT) Đã hoàn tác. Trả về mọi thứ.
            return result.getItems();
        }
        return Collections.emptyList();
    }

}

