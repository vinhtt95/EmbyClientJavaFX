package com.example.embyapp.service;

import com.example.emby.EmbyClient.ApiException;
import com.example.emby.EmbyClient.Java.ItemsServiceApi;
import com.example.emby.modelEmby.BaseItemDto;
import com.example.emby.modelEmby.QueryResultBaseItemDto;

import java.util.Collections;
import java.util.List;

/**
 * (SỬA ĐỔI) Repository để quản lý việc truy xuất BaseItemDto (Thư viện, Phim, Series...).
 * Sửa lỗi tên hàm và logic lấy userId.
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
        QueryResultBaseItemDto result = service.getItems(
                userId, // userId
                null, // sortBy
                null, // sortOrder
                null, // includeItemTypes
                false, // recursive (Chỉ lấy thư mục gốc, không lấy con)
                null, // fields
                null, // startIndex
                null, // limit
                null, // excludeItemTypes
                null, // enableImages
                null, // imageTypeLimit
                null, // enableImageTypes
                null, // locationTypes
                null, // parentId (null để lấy gốc)
                null, // searchTerm
                null, // enableTotalRecordCount
                null, // enableUserData
                null, // imageTypes
                null, // mediaTypes
                null, // years
                null, // officialRatings
                null, // tags
                null, // genres
                null, // studios
                null, // artists
                null, // albums
                null, // ids
                null, // videoTypes
                null, // adjacentTo
                null, // minIndexNumber
                null, // minStartDate
                null, // maxStartDate
                null, // minEndDate
                null, // maxEndDate
                null, // minPlayers
                null, // maxPlayers
                null, // parentIndexNumber
                null, // hasThemeSong
                null, // hasThemeVideo
                null, // hasSubtitles
                null, // hasSpecialFeature
                null, // hasTrailer
                null, // isHD
                null, // is4K
                null, // isUnaired
                null, // isMissed
                null, // isNew
                null, // isPremiere
                null, // isRepeat
                null, // nameStartsWithOrGreater
                null, // nameStartsWith
                null, // nameLessThan
                null, // albumArtistStartsWithOrGreater
                null, // albumArtistStartsWith
                null, // artistStartsWithOrGreater
                null, // artistStartsWith
                null, // seriesStatus
                // seriesStatus
                null,null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null
        );

        if (result != null && result.getItems() != null) {
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

        // Gọi API
        QueryResultBaseItemDto result = service.getItems(
                userId, // userId
                null, // sortBy
                null, // sortOrder
                null, // includeItemTypes
                false, // recursive (Chỉ lấy con trực tiếp)
                null, // fields
                null, // startIndex
                null, // limit
                null, // excludeItemTypes
                null, // enableImages
                null, // imageTypeLimit
                null, // enableImageTypes
                null, // locationTypes
                Integer.parseInt(parentId), // parentId (Lấy con của ID này)
                null, // searchTerm
                null, // enableTotalRecordCount
                null, // enableUserData
                null, // imageTypes
                null, // mediaTypes
                null, // years
                null, // officialRatings
                null, // tags
                null, // genres
                null, // studios
                null, // artists
                null, // albums
                null, // ids
                null, // videoTypes
                null, // adjacentTo
                null, // minIndexNumber
                null, // minStartDate
                null, // maxStartDate
                null, // minEndDate
                null, // maxEndDate
                null, // minPlayers
                null, // maxPlayers
                null, // parentIndexNumber
                null, // hasThemeSong
                null, // hasThemeVideo
                null, // hasSubtitles
                null, // hasSpecialFeature
                null, // hasTrailer
                null, // isHD
                null, // is4K
                null, // isUnaired
                null, // isMissed
                null, // isNew
                null, // isPremiere
                null, // isRepeat
                null, // nameStartsWithOrGreater
                null, // nameStartsWith
                null, // nameLessThan
                null, // albumArtistStartsWithOrGreater
                null, // albumArtistStartsWith
                null, // artistStartsWithOrGreater
                null, // artistStartsWith
                null, // seriesStatus
                null, // seriesStatus
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null
        );

        if (result != null && result.getItems() != null) {
            return result.getItems();
        }
        return Collections.emptyList();
    }
}

