package com.example.embyapp.viewmodel;

import com.example.emby.EmbyClient.ApiException;
import com.example.emby.EmbyClient.Java.ItemsServiceApi; // Import ItemsServiceApi
import com.example.emby.modelEmby.AuthenticationAuthenticationResult;
import com.example.emby.modelEmby.BaseItemDto;
import com.example.emby.modelEmby.QueryResultBaseItemDto;
import com.example.emby.modelEmby.UserDto;
import com.example.embyapp.service.EmbyService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException; // Import IOException
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MainViewModel {

    private final StringProperty welcomeMessage = new SimpleStringProperty("Loading user data...");
    // ObservableList để lưu trữ tên các mục thư viện
    private final ObservableList<String> libraryItems = FXCollections.observableArrayList();
    private final EmbyService embyService;

    public MainViewModel(EmbyService embyService) {
        this.embyService = embyService;
    }

    public StringProperty welcomeMessageProperty() {
        return welcomeMessage;
    }

    // Getter cho danh sách các mục thư viện
    public ObservableList<String> getLibraryItems() {
        return libraryItems;
    }

    // Tải thông tin người dùng
    public void loadUserData() {
        AuthenticationAuthenticationResult authResult = embyService.getCurrentAuthResult();
        if (authResult != null && authResult.getUser() != null) {
            UserDto user = authResult.getUser();
            Platform.runLater(() -> welcomeMessage.set("Welcome, " + user.getName() + "!"));
        } else {
            Platform.runLater(() -> welcomeMessage.set("Welcome!")); // Fallback message
        }
    }

    // Tải dữ liệu thư viện (chạy trên luồng nền)
    public void loadLibraryData() {
        new Thread(() -> {
            try {
                // Lấy ItemsServiceApi từ EmbyService (bạn cần đảm bảo đã thêm getItemsServiceApi() vào EmbyService)
                ItemsServiceApi itemsService = embyService.getItemsServiceApi();
                AuthenticationAuthenticationResult authResult = embyService.getCurrentAuthResult();

                if (itemsService == null || authResult == null || authResult.getUser() == null) {
                    Platform.runLater(() -> libraryItems.setAll("Could not load library: Service or User not available."));
                    return;
                }

                String userId = authResult.getUser().getId(); // Lấy userId

                // Gọi API để lấy các mục gốc (ví dụ: các thư viện)
                // Tham số: userId, parentId (null để lấy gốc), các tham số khác có thể là null hoặc giá trị mặc định
                // Lưu ý: SDK có thể yêu cầu nhiều tham số hơn, kiểm tra chữ ký phương thức
                QueryResultBaseItemDto result = itemsService.getItems(
                        userId, // userId is often required
                        null, // sortBy
                        null, // sortOrder
                        null, // includeItemTypes
                        null, // recursive
                        null, // fields
                        null, // startIndex
                        null, // limit
                        null, // excludeItemTypes
                        null, // enableImages
                        null, // imageTypeLimit
                        null, // enableImageTypes
                        null, // locationTypes
                        null, // parentId (null or empty for root)
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
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null// excludeLocationTypes
                );


                // Xử lý kết quả
                if (result != null && result.getItems() != null) {
                    List<String> itemNames = result.getItems().stream()
                            .map(BaseItemDto::getName) // Lấy tên của mỗi mục
                            .collect(Collectors.toList());
                    // Cập nhật UI trên luồng chính
                    Platform.runLater(() -> libraryItems.setAll(itemNames));
                } else {
                    Platform.runLater(() -> libraryItems.setAll("No items found or result is null."));
                }

            } catch (ApiException e) {
                System.err.println("API Exception loading library data: " + e.getCode());
                System.err.println("Response body: " + e.getResponseBody());
                e.printStackTrace();
                Platform.runLater(() -> libraryItems.setAll("Error loading library: " + e.getMessage()));
            }
            catch (Exception e) {
                System.err.println("Unexpected Exception loading library data:");
                e.printStackTrace();
                Platform.runLater(() -> libraryItems.setAll("Unexpected error loading library."));
            }
        }).start();
    }
}

