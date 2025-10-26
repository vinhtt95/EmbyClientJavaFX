package com.example.embyapp.service;

import embyclient.ApiClient;
import embyclient.ApiException;
import embyclient.api.*;
import embyclient.model.BaseItemDto;
import embyclient.model.QueryResultBaseItemDto;
import embyclient.model.QueryResultUserLibraryTagItem;
import embyclient.model.UserLibraryTagItem;
import com.example.embyapp.viewmodel.detail.SuggestionItemModel;

import java.util.Collections;
import java.util.List;

public class RequestEmby {


    /**
     * Lấy danh sách các Item con theo parentID
     *
     * @param parentID
     * @param itemsServiceApi
     * @return
     * @throws ApiException
     */
    public QueryResultBaseItemDto getQueryResultBaseItemDto(String parentID, ItemsServiceApi itemsServiceApi) {
        QueryResultBaseItemDto result = null;
        try {
            result = itemsServiceApi.getItems(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "Ascending", parentID, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "ProductionYear,PremiereDate,SortName", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        } catch (ApiException e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

    /**
     * Lấy danh sách các Item con theo parentID
     *
     * @param parentID
     * @param itemsServiceApi
     * @param startIndex
     * @param limit
     * @return
     * @throws ApiException
     */
    // SỬA ĐỔI: Thêm startIndex và limit
    public QueryResultBaseItemDto getQueryResultFullBaseItemDto(String parentID, ItemsServiceApi itemsServiceApi, Integer startIndex, Integer limit) {
        QueryResultBaseItemDto result = null;
        try {
            result = itemsServiceApi.getItems(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, startIndex, limit, true, null, "Descending", parentID, null, null, "Movie", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "ProductionYear,PremiereDate,SortName", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        } catch (ApiException e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

    public QueryResultBaseItemDto searchBaseItemDto(String keywords, ItemsServiceApi itemsServiceApi, Integer startIndex, Integer limit, String sortOrder, String sortBy) {

        QueryResultBaseItemDto result = null;
        try {
            itemsServiceApi.getItems(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, startIndex, limit, true, keywords, sortOrder, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, sortBy, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        } catch (ApiException e) {
            System.out.println(e.getMessage());
        }
        return result;
    }


    /**
     * Lấy danh sách library theo userID
     *
     * @param userID
     * @param itemsServiceApi
     * @return BaseItemDTO
     */
    public QueryResultBaseItemDto getUsersByUseridItems(String userID, ItemsServiceApi itemsServiceApi) {
        try {
            return itemsServiceApi.getUsersByUseridItems(userID, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        } catch (ApiException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    /**
     * (*** HÀM ĐÃ SỬA ĐỔI: Thêm ApiClient ***)
     * Lấy danh sách tất cả các tag đã dùng trong thư viện của người dùng.
     *
     * @param apiClient ApiClient đã được cấu hình (từ EmbyService)
     * @return Danh sách các UserLibraryTagItem hoặc null nếu lỗi.
     */
    public List<UserLibraryTagItem> getListTagsItem(ApiClient apiClient) { // <-- Thêm ApiClient

        // Khởi tạo TagServiceApi với ApiClient được cung cấp
        TagServiceApi tagServiceApi = new TagServiceApi(apiClient);
        try {
            // Gọi API getTags (không cần truyền nhiều tham số null như vậy)
            // Hầu hết các tham số có giá trị mặc định là null trong hàm gọi
            QueryResultUserLibraryTagItem listTag = tagServiceApi.getTags(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
            if (listTag != null && listTag.getItems() != null) {
                return listTag.getItems();
            }
        } catch (ApiException e) {
            System.err.println("API Error getting tags: " + e.getMessage()); // Sửa thành System.err
        } catch (Exception e) {
            System.err.println("Unexpected Error getting tags: " + e.getMessage()); // Thêm catch tổng quát
            e.printStackTrace();
        }

        return Collections.emptyList(); // Trả về list rỗng thay vì null
    }

    public List<BaseItemDto> getListStudio(ApiClient apiClient) { // <-- Thêm ApiClient

        // Khởi tạo TagServiceApi với ApiClient được cung cấp
        StudiosServiceApi studiosServiceApi = new StudiosServiceApi(apiClient);
        try {
            // Gọi API getTags (không cần truyền nhiều tham số null như vậy)
            // Hầu hết các tham số có giá trị mặc định là null trong hàm gọi
            embyclient.model.QueryResultBaseItemDto resultBaseItemDto = studiosServiceApi.getStudios(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

            if (resultBaseItemDto != null && resultBaseItemDto.getItems() != null) {
                return resultBaseItemDto.getItems();
            }
        } catch (ApiException e) {
            System.err.println("API Error getting tags: " + e.getMessage()); // Sửa thành System.err
        } catch (Exception e) {
            System.err.println("Unexpected Error getting tags: " + e.getMessage()); // Thêm catch tổng quát
            e.printStackTrace();
        }

        return Collections.emptyList(); // Trả về list rỗng thay vì null
    }

    public List<BaseItemDto> getListPeoples(ApiClient apiClient) { // <-- Thêm ApiClient
        PersonsServiceApi personsServiceApi = new PersonsServiceApi(apiClient);
        try {
            QueryResultBaseItemDto resultBaseItemDto = personsServiceApi.getPersons(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

            if (resultBaseItemDto != null && resultBaseItemDto.getItems() != null) {
                return resultBaseItemDto.getItems();
            }
        } catch (ApiException e) {
            System.err.println("API Error getting tags: " + e.getMessage()); // Sửa thành System.err
        } catch (Exception e) {
            System.err.println("Unexpected Error getting tags: " + e.getMessage()); // Thêm catch tổng quát
            e.printStackTrace();
        }

        return Collections.emptyList(); // Trả về list rỗng thay vì null
    }

    public List<BaseItemDto> getListGenres(ApiClient apiClient) { // <-- Thêm ApiClient
        GenresServiceApi genresServiceApi = new GenresServiceApi(apiClient);
        try {

            QueryResultBaseItemDto genreResult = genresServiceApi.getGenres(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

            if (genreResult != null && genreResult.getItems() != null) {
                return genreResult.getItems();
            }
        } catch (ApiException e) {
            System.err.println("API Error getting tags: " + e.getMessage()); // Sửa thành System.err
        } catch (Exception e) {
            System.err.println("Unexpected Error getting tags: " + e.getMessage()); // Thêm catch tổng quát
            e.printStackTrace();
        }

        return Collections.emptyList(); // Trả về list rỗng thay vì null
    }
}