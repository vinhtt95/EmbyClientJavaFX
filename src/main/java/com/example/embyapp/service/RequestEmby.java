package com.example.embyapp.service;

import embyclient.ApiClient;
import embyclient.ApiException;
import embyclient.api.*;
import embyclient.model.*;
import com.example.embyapp.viewmodel.detail.SuggestionItemModel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import org.json.JSONObject;

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
    public QueryResultBaseItemDto getQueryResultFullBaseItemDto(String parentID, ItemsServiceApi itemsServiceApi, Integer startIndex, Integer limit, String sortOrder, String sortBy) {
        QueryResultBaseItemDto result = null;
        try {
            // sortOrder là tham số thứ 37, sortBy là tham số thứ 58
            result = itemsServiceApi.getItems(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, startIndex, limit, true, null, sortOrder, parentID, null, null, "Movie", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, sortBy, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        } catch (ApiException e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

    public QueryResultBaseItemDto searchBaseItemDto(String keywords, ItemsServiceApi itemsServiceApi, Integer startIndex, Integer limit, String sortOrder, String sortBy) {

        QueryResultBaseItemDto result = null;
        try {
            result = itemsServiceApi.getItems(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, startIndex, limit, true, keywords, sortOrder, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, sortBy, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
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

    public OffsetDateTime getDateRelease(String code) {
        // API này là của đồng chí, tôi giữ nguyên
        String apiUrl = "http://localhost:8081/movies/movie/date/?movieCode=" + code;
        HttpURLConnection connection = null; // Khai báo bên ngoài để đóng trong finally
        try {
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(5000); // Thêm timeout
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                // Try-with-resources để tự động đóng BufferedReader
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    // Parse JSON response to extract "data"
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String dataValue = jsonResponse.optString("data", null);
                    if (dataValue != null && !dataValue.equals("null")) {
                        return OffsetDateTime.parse(dataValue);
                    } else {
                        return null; // API trả về data: null
                    }
                }
            } else {
                // System.out.println("API call failed for code: " + code + ". Response: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            // System.out.println("Error calling API for code: " + code + " - " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect(); // Đảm bảo đóng connection
            }
        }
    }


    public void copyTags(String itemCopyID, String parentID, String userID) {

        ItemService itemService = new ItemService(userID);
        // Bỏ dòng này
        // ItemService itemService = new ItemService();
        // Dùng this.itemService
        BaseItemDto itemCopy = itemService.getInforItem(itemCopyID);

        if(itemCopy == null){
            System.out.println("Not found item copy");
            return;
        }else{
            System.out.println("List Tags of Item copy:");
            List<NameLongIdPair> listTagsItemCopy = itemCopy.getTagItems();
            for (NameLongIdPair eachtags : listTagsItemCopy) {
                System.out.println("ID: " + eachtags.getId() + " Name: " + eachtags.getName());
            }
        }

        // Dùng this.itemService
        List<BaseItemDto> listItemPaste = itemService.getListItemByParentID(parentID, null, null, true);
        if (listItemPaste == null) {
            System.out.println("Not found item paste");
            return;
        }

        BaseItemDto itemPaste = null;
        for (BaseItemDto eachItemPaste : listItemPaste) {
            System.out.println("ID: " + eachItemPaste.getId()+ " Name: " + eachItemPaste.getName());
            // Dùng this.itemService
            itemPaste = itemService.getInforItem(eachItemPaste.getId());

            List<NameLongIdPair> listTagsItemPaste = itemPaste.getTagItems();

            itemPaste.getStudios().clear();

            listTagsItemPaste.addAll(itemCopy.getTagItems());

            for (NameLongIdPair eachTagsPaste : itemPaste.getStudios()) {
                System.out.println(eachTagsPaste.toString());
            }

            // Dùng this.itemService
            if(itemService.updateInforItem(itemPaste.getId(),itemPaste)) {
                System.out.println("Update success "+eachItemPaste.getName());
            }
        }
    }

    public void copyStudio(String itemCopyID, String parentID, String userID) {

        ItemService itemService = new ItemService(userID);
        // Bỏ dòng này
        // ItemService itemService = new ItemService();
        // Dùng this.itemService
        BaseItemDto itemCopy = itemService.getInforItem(itemCopyID);

        if(itemCopy == null){
            System.out.println("Not found item copy");
            return;
        }else{
            System.out.println("List Studio of Item copy:");
            List<NameLongIdPair> listStudoItemCopy = itemCopy.getStudios();
            for (NameLongIdPair eachStudio : listStudoItemCopy) {
                System.out.println("ID: " + eachStudio.getId() + " Name: " + eachStudio.getName());
            }
        }

        // Dùng itemService
        List<BaseItemDto> listItemPaste = itemService.getListItemByParentID(parentID, null, null, true);
        if (listItemPaste == null) {
            System.out.println("Not found item paste");
            return;
        }

        BaseItemDto itemPaste = null;
        for (BaseItemDto eachItemPaste : listItemPaste) {
            System.out.println("ID: " + eachItemPaste.getId()+ " Name: " + eachItemPaste.getName());
            // Dùng itemService
            itemPaste = itemService.getInforItem(eachItemPaste.getId());

            List<NameLongIdPair> listStudioItemPaste = itemPaste.getStudios();

            itemPaste.getStudios().clear();

            listStudioItemPaste.addAll(itemCopy.getStudios());

            for (NameLongIdPair eachStudioPaste : itemPaste.getStudios()) {
                System.out.println(eachStudioPaste.toString());
            }

            // Dùng itemService
            if(itemService.updateInforItem(itemPaste.getId(),itemPaste)) {
                System.out.println("Update success "+eachItemPaste.getName());
            }
        }
    }

    public void copyPeople(String itemCopyID, String parentID, String userID) {
        ItemService itemService = new ItemService(userID);
        // Bỏ dòng này
        // ItemService itemService = new ItemService();
        // Dùng itemService
        BaseItemDto itemCopy = itemService.getInforItem(itemCopyID);

        if(itemCopy == null){
            System.out.println("Not found item copy");
            return;
        }else{
            System.out.println("List People of Item copy:");
            List<NameLongIdPair> listPeopleItemCopy = itemCopy.getStudios();
            for (NameLongIdPair eachStudio : listPeopleItemCopy) {
                System.out.println("ID: " + eachStudio.getId() + " Name: " + eachStudio.getName());
            }
        }

        // Dùng itemService
        List<BaseItemDto> listItemPaste = itemService.getListItemByParentID(parentID, null, null, true);
        if (listItemPaste == null) {
            System.out.println("Not found item paste");
            return;
        }

        BaseItemDto itemPaste = null;
        for (BaseItemDto eachItemPaste : listItemPaste) {
            System.out.println("ID: " + eachItemPaste.getId()+ " Name: " + eachItemPaste.getName());
            // Dùng itemService
            itemPaste = itemService.getInforItem(eachItemPaste.getId());

            List<BaseItemPerson> listPeopleItemPaste = itemPaste.getPeople();

            itemPaste.getPeople().clear();

            listPeopleItemPaste.addAll(itemCopy.getPeople());

            for (BaseItemPerson eachPeoplePaste : itemPaste.getPeople()) {
                System.out.println(eachPeoplePaste.toString());
            }

            // Dùng itemService
            if(itemService.updateInforItem(itemPaste.getId(),itemPaste)) {
                System.out.println("Update success "+eachItemPaste.getName());
            }
        }
    }

    public void copyGenres(String itemCopyID, String parentID, String userID) {
        ItemService itemService = new ItemService(userID);
        // Bỏ dòng này
        // ItemService itemService = new ItemService();
        // Dùng itemService
        BaseItemDto itemCopy = itemService.getInforItem(itemCopyID);

        if(itemCopy == null){
            System.out.println("Not found item copy");
            return;
        }else{
            System.out.println("List Genres of Item copy:");
            List<NameLongIdPair> listGenresItemCopy = itemCopy.getGenreItems();
            for (NameLongIdPair eachGenres : listGenresItemCopy) {
                System.out.println("ID: " + eachGenres.getId() + " Name: " + eachGenres.getName());
            }
        }

        // Dùng itemService
        List<BaseItemDto> listItemPaste = itemService.getListItemByParentID(parentID, null, null, true);
        if (listItemPaste == null) {
            System.out.println("Not found item paste");
            return;
        }

        BaseItemDto itemPaste = null;
        for (BaseItemDto eachItemPaste : listItemPaste) {
            System.out.println("ID: " + eachItemPaste.getId()+ " Name: " + eachItemPaste.getName());
            // Dùng itemService
            itemPaste = itemService.getInforItem(eachItemPaste.getId());

            List<NameLongIdPair> listGenresItemPaste = itemPaste.getGenreItems();

            itemPaste.getGenreItems().clear();

            listGenresItemPaste.addAll(itemCopy.getGenreItems());

            for (NameLongIdPair eachGenresPaste : itemPaste.getGenreItems()) {
                System.out.println(eachGenresPaste.toString());
            }

            // Dùng itemService
            if(itemService.updateInforItem(itemPaste.getId(),itemPaste)) {
                System.out.println("Update success "+eachItemPaste.getName());
            }
        }
    }

}