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
            result = itemsServiceApi.getItems(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "Ascending", parentID, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "ProductionYear,PremiereDate,SortName", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,null);

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
     * @param sortOrder
     * @param sortBy
     * @return
     * @throws ApiException
     */
    public QueryResultBaseItemDto getQueryResultFullBaseItemDto(String parentID, ItemsServiceApi itemsServiceApi, Integer startIndex, Integer limit, String sortOrder, String sortBy) {
        QueryResultBaseItemDto result = null;
        try {
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
     * Lấy danh sách tất cả các tag đã dùng trong thư viện của người dùng.
     *
     * @param apiClient ApiClient đã được cấu hình (từ EmbyService)
     * @return Danh sách các UserLibraryTagItem hoặc list rỗng nếu lỗi.
     */
    public List<UserLibraryTagItem> getListTagsItem(ApiClient apiClient) {
        TagServiceApi tagServiceApi = new TagServiceApi(apiClient);
        try {
            QueryResultUserLibraryTagItem listTag = tagServiceApi.getTags(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
            if (listTag != null && listTag.getItems() != null) {
                return listTag.getItems();
            }
        } catch (ApiException e) {
            System.err.println("API Error getting tags: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected Error getting tags: " + e.getMessage());
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    public List<BaseItemDto> getListStudio(ApiClient apiClient) {
        StudiosServiceApi studiosServiceApi = new StudiosServiceApi(apiClient);
        try {
            embyclient.model.QueryResultBaseItemDto resultBaseItemDto = studiosServiceApi.getStudios(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

            if (resultBaseItemDto != null && resultBaseItemDto.getItems() != null) {
                return resultBaseItemDto.getItems();
            }
        } catch (ApiException e) {
            System.err.println("API Error getting tags: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected Error getting tags: " + e.getMessage());
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    public List<BaseItemDto> getListPeoples(ApiClient apiClient) {
        PersonsServiceApi personsServiceApi = new PersonsServiceApi(apiClient);
        try {
            QueryResultBaseItemDto resultBaseItemDto = personsServiceApi.getPersons(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

            if (resultBaseItemDto != null && resultBaseItemDto.getItems() != null) {
                return resultBaseItemDto.getItems();
            }
        } catch (ApiException e) {
            System.err.println("API Error getting tags: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected Error getting tags: " + e.getMessage());
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    public List<BaseItemDto> getListGenres(ApiClient apiClient) {
        GenresServiceApi genresServiceApi = new GenresServiceApi(apiClient);
        try {

            QueryResultBaseItemDto genreResult = genresServiceApi.getGenres(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

            if (genreResult != null && genreResult.getItems() != null) {
                return genreResult.getItems();
            }
        } catch (ApiException e) {
            System.err.println("API Error getting tags: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected Error getting tags: " + e.getMessage());
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    public OffsetDateTime getDateRelease(String code) {
        String apiUrl = "http://localhost:8081/movies/movie/date/?movieCode=" + code;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String dataValue = jsonResponse.optString("data", null);
                    if (dataValue != null && !dataValue.equals("null")) {
                        return OffsetDateTime.parse(dataValue);
                    } else {
                        return null;
                    }
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    /**
     * Sao chép Tags từ item nguồn sang tất cả item con của một thư mục cha.
     *
     * @param itemService Instance ItemService đã được xác thực
     * @param itemCopyID  ID của item nguồn (để lấy tags)
     * @param parentID    ID của item cha (để tìm item đích)
     * @return Số lượng item con đã được cập nhật
     * @throws ApiException
     */
    public int copyTags(ItemService itemService, String itemCopyID, String parentID) throws ApiException {
        BaseItemDto itemCopy = itemService.getInforItem(itemCopyID);

        if (itemCopy == null) {
            System.out.println("Not found item copy");
            return 0;
        }

        List<NameLongIdPair> listTagsItemCopy = itemCopy.getTagItems();
        System.out.println("List Tags of Item copy (" + listTagsItemCopy.size() + "):");
        for (NameLongIdPair eachtags : listTagsItemCopy) {
            System.out.println("ID: " + eachtags.getId() + " Name: " + eachtags.getName());
        }

        List<BaseItemDto> listItemPaste = itemService.getListItemByParentID(parentID, null, null, true);
        if (listItemPaste == null || listItemPaste.isEmpty()) {
            System.out.println("Not found item paste");
            return 0;
        }

        int updateCount = 0;
        BaseItemDto itemPaste;
        for (BaseItemDto eachItemPaste : listItemPaste) {
            System.out.println("Updating Tags for: ID: " + eachItemPaste.getId() + " Name: " + eachItemPaste.getName());
            itemPaste = itemService.getInforItem(eachItemPaste.getId());
            if (itemPaste == null) continue;

            itemPaste.setTagItems(listTagsItemCopy);

            if (itemService.updateInforItem(itemPaste.getId(), itemPaste)) {
                System.out.println("Update success " + eachItemPaste.getName());
                updateCount++;
            }
        }
        return updateCount;
    }

    /**
     * Sao chép Studios từ item nguồn sang tất cả item con của một thư mục cha.
     * @param itemService Instance ItemService đã được xác thực
     * @param itemCopyID ID của item nguồn
     * @param parentID ID của item cha
     * @return Số lượng item con đã được cập nhật
     * @throws ApiException
     */
    public int copyStudio(ItemService itemService, String itemCopyID, String parentID) throws ApiException {
        BaseItemDto itemCopy = itemService.getInforItem(itemCopyID);

        if (itemCopy == null) {
            System.out.println("Not found item copy");
            return 0;
        }

        List<NameLongIdPair> listStudoItemCopy = itemCopy.getStudios();
        System.out.println("List Studio of Item copy (" + listStudoItemCopy.size() + "):");
        for (NameLongIdPair eachStudio : listStudoItemCopy) {
            System.out.println("ID: " + eachStudio.getId() + " Name: " + eachStudio.getName());
        }

        List<BaseItemDto> listItemPaste = itemService.getListItemByParentID(parentID, null, null, true);
        if (listItemPaste == null || listItemPaste.isEmpty()) {
            System.out.println("Not found item paste");
            return 0;
        }

        int updateCount = 0;
        BaseItemDto itemPaste;
        for (BaseItemDto eachItemPaste : listItemPaste) {
            System.out.println("Updating Studio for: ID: " + eachItemPaste.getId() + " Name: " + eachItemPaste.getName());
            itemPaste = itemService.getInforItem(eachItemPaste.getId());
            if (itemPaste == null) continue;

            itemPaste.setStudios(listStudoItemCopy);

            if (itemService.updateInforItem(itemPaste.getId(), itemPaste)) {
                System.out.println("Update success " + eachItemPaste.getName());
                updateCount++;
            }
        }
        return updateCount;
    }

    /**
     * Sao chép People từ item nguồn sang tất cả item con của một thư mục cha.
     * @param itemService Instance ItemService đã được xác thực
     * @param itemCopyID ID của item nguồn
     * @param parentID ID của item cha
     * @return Số lượng item con đã được cập nhật
     * @throws ApiException
     */
    public int copyPeople(ItemService itemService, String itemCopyID, String parentID) throws ApiException {
        BaseItemDto itemCopy = itemService.getInforItem(itemCopyID);

        if (itemCopy == null) {
            System.out.println("Not found item copy");
            return 0;
        }

        List<BaseItemPerson> listPeopleItemCopy = itemCopy.getPeople();
        System.out.println("List People of Item copy (" + listPeopleItemCopy.size() + "):");
        for (BaseItemPerson eachPeople : listPeopleItemCopy) {
            System.out.println("ID: " + eachPeople.getId() + " Name: " + eachPeople.getName());
        }

        List<BaseItemDto> listItemPaste = itemService.getListItemByParentID(parentID, null, null, true);
        if (listItemPaste == null || listItemPaste.isEmpty()) {
            System.out.println("Not found item paste");
            return 0;
        }

        int updateCount = 0;
        BaseItemDto itemPaste;
        for (BaseItemDto eachItemPaste : listItemPaste) {
            System.out.println("Updating People for: ID: " + eachItemPaste.getId() + " Name: " + eachItemPaste.getName());
            itemPaste = itemService.getInforItem(eachItemPaste.getId());
            if (itemPaste == null) continue;

            itemPaste.setPeople(listPeopleItemCopy);

            if (itemService.updateInforItem(itemPaste.getId(), itemPaste)) {
                System.out.println("Update success " + eachItemPaste.getName());
                updateCount++;
            }
        }
        return updateCount;
    }

    /**
     * Sao chép Genres (GenreItems) từ item nguồn sang tất cả item con của một thư mục cha.
     * @param itemService Instance ItemService đã được xác thực
     * @param itemCopyID ID của item nguồn
     * @param parentID ID của item cha
     * @return Số lượng item con đã được cập nhật
     * @throws ApiException
     */
    public int copyGenres(ItemService itemService, String itemCopyID, String parentID) throws ApiException {
        BaseItemDto itemCopy = itemService.getInforItem(itemCopyID);

        if (itemCopy == null) {
            System.out.println("Not found item copy");
            return 0;
        }

        List<NameLongIdPair> listGenresItemCopy = itemCopy.getGenreItems();
        System.out.println("List Genres of Item copy (" + (listGenresItemCopy != null ? listGenresItemCopy.size() : 0) + "):");
        if (listGenresItemCopy != null) {
            for (NameLongIdPair eachGenres : listGenresItemCopy) {
                System.out.println("ID: " + eachGenres.getId() + " Name: " + eachGenres.getName());
            }
        }

        List<BaseItemDto> listItemPaste = itemService.getListItemByParentID(parentID, null, null, true);
        if (listItemPaste == null || listItemPaste.isEmpty()) {
            System.out.println("Not found item paste");
            return 0;
        }

        int updateCount = 0;
        BaseItemDto itemPaste;
        for (BaseItemDto eachItemPaste : listItemPaste) {
            System.out.println("Updating Genres for: ID: " + eachItemPaste.getId() + " Name: " + eachItemPaste.getName());
            itemPaste = itemService.getInforItem(eachItemPaste.getId());
            if (itemPaste == null) continue;

            itemPaste.setGenreItems(listGenresItemCopy);

            if (itemService.updateInforItem(itemPaste.getId(), itemPaste)) {
                System.out.println("Update success " + eachItemPaste.getName());
                updateCount++;
            }
        }
        return updateCount;
    }

}