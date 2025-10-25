package com.example.embyapp.service;

import embyclient.ApiException;
import embyclient.api.ImageServiceApi; // (CẬP NHẬT) Thêm import
import embyclient.api.ItemsServiceApi;
import embyclient.api.UserLibraryServiceApi; // (CẬP NHẬT) Thêm import
import embyclient.model.BaseItemDto;
import embyclient.model.ImageInfo; // (CẬP NHẬT) Thêm import
import embyclient.model.QueryResultBaseItemDto;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors; // (CẬP NHẬT) Thêm import (từ lần sửa trước)

/**
 * (SỬA ĐỔI) Repository để quản lý việc truy xuất BaseItemDto (Thư viện, Phim, Series...).
 * (CẬP NHẬT) Thêm logic để lấy Chi tiết Item đầy đủ và Danh sách Ảnh.
 * (CẬP NHẬT MỚI) Thêm phương thức phân trang getFullByParentIdPaginated.
 */
public class ItemRepository {

    private final EmbyService embyService;
    private ItemsServiceApi itemsService; // Cache API service

    // (CẬP NHẬT) Cache các API service mới
    private UserLibraryServiceApi userLibraryServiceApi;
    private ImageServiceApi imageServiceApi;

    // SỬA LỖI: Constructor rỗng, tự lấy Singleton
    public ItemRepository() {
        this.embyService = EmbyService.getInstance();
    }

    // --- Private Helper Getters cho API Services ---

    private ItemsServiceApi getItemsService() {
        if (itemsService == null && embyService.isLoggedIn()) {
            this.itemsService = embyService.getItemsServiceApi();
        }
        return this.itemsService;
    }

    // (CẬP NHẬT) Hàm helper mới
    private UserLibraryServiceApi getUserLibraryServiceApi() {
        if (userLibraryServiceApi == null && embyService.isLoggedIn()) {
            this.userLibraryServiceApi = new UserLibraryServiceApi(embyService.getApiClient());
        }
        return this.userLibraryServiceApi;
    }

    // (CẬP NHẬT) Hàm helper mới
    private ImageServiceApi getImageServiceApi() {
        if (imageServiceApi == null && embyService.isLoggedIn()) {
            this.imageServiceApi = new ImageServiceApi(embyService.getApiClient());
        }
        return this.imageServiceApi;
    }

    // --- Public API Methods ---

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
            // (CẬP NHẬT) Lọc chỉ-folder đã chuyển sang LibraryTreeViewModel
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
            // (CẬP NHẬT) Lọc chỉ-folder đã chuyển sang LibraryTreeViewModel
            return result.getItems();
        }
        return Collections.emptyList();
    }

    // XÓA HÀM CŨ: public List<BaseItemDto> getFullByParentId(String parentId) throws ApiException { ... }

    /**
     * (MỚI) Lấy các items con đầy đủ dựa trên parentId VỚI PHÂN TRANG.
     *
     * @param parentId ID của thư mục cha.
     * @param startIndex Vị trí bắt đầu.
     * @param limit Số lượng item muốn lấy.
     * @return QueryResultBaseItemDto chứa danh sách items và tổng số.
     * @throws ApiException Nếu API call thất bại.
     */
    public QueryResultBaseItemDto getFullByParentIdPaginated(String parentId, int startIndex, int limit) throws ApiException {
        if (!embyService.isLoggedIn()) {
            throw new IllegalStateException("Không thể lấy items khi chưa đăng nhập.");
        }

        ItemsServiceApi service = getItemsService();
        if (service == null) {
            throw new IllegalStateException("ItemsServiceApi is null.");
        }

        // Gọi hàm RequestEmby đã sửa đổi
        QueryResultBaseItemDto result = new RequestEmby().getQueryResultFullBaseItemDto(parentId, service, startIndex, limit);

        if (result != null) {
            return result;
        }

        // Trả về kết quả rỗng an toàn
        QueryResultBaseItemDto emptyResult = new QueryResultBaseItemDto();
        emptyResult.setItems(Collections.emptyList());
        emptyResult.setTotalRecordCount(0);
        return emptyResult;
    }


    /**
     * (CẬP NHẬT) HÀM MỚI
     * Lấy thông tin chi tiết đầy đủ của một item.
     *
     * @param userId ID của người dùng
     * @param itemId ID của item
     * @return BaseItemDto với đầy đủ thông tin chi tiết.
     * @throws ApiException Nếu API call thất bại.
     */
    public BaseItemDto getFullItemDetails(String userId, String itemId) throws ApiException {
        if (!embyService.isLoggedIn() || userId == null) {
            throw new IllegalStateException("Không thể lấy chi tiết item khi chưa đăng nhập.");
        }
        UserLibraryServiceApi service = getUserLibraryServiceApi();
        if (service == null) {
            throw new IllegalStateException("UserLibraryServiceApi is null.");
        }
        // Gọi API như code mẫu của bạn
        return service.getUsersByUseridItemsById(userId, itemId);
    }

    /**
     * (CẬP NHẬT) HÀM MỚI
     * Lấy danh sách ảnh (Backdrop, Primary, v.v.) của một item.
     *
     * @param itemId ID của item
     * @return Danh sách ImageInfo.
     * @throws ApiException Nếu API call thất bại.
     */
    public List<ImageInfo> getItemImages(String itemId) throws ApiException {
        if (!embyService.isLoggedIn()) {
            throw new IllegalStateException("Không thể lấy ảnh khi chưa đăng nhập.");
        }
        ImageServiceApi service = getImageServiceApi();
        if (service == null) {
            throw new IllegalStateException("ImageServiceApi is null.");
        }
        // Gọi API như code mẫu của bạn
        List<ImageInfo> images = service.getItemsByIdImages(itemId);
        if (images != null) {
            return images;
        }
        // Trả về danh sách rỗng an toàn
        return Collections.emptyList();
    }
}