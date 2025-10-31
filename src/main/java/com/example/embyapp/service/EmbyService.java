package com.example.embyapp.service;

import embyclient.ApiClient;
import embyclient.ApiException;
// *** THÊM CÁC IMPORT BỊ THIẾU ***
import embyclient.Configuration;
import embyclient.api.*;
import embyclient.model.*;
// *** KẾT THÚC THÊM IMPORT ***
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;


public class EmbyService {

    private static EmbyService instance;
    private final ApiClient apiClient;
    private AuthenticationAuthenticationResult currentAuthResult; // <- Dùng class đã import
    private String clientAuthHeader;
    private String currentAccessToken;


    // Preferences node for storing session data
    private static final String PREF_NODE_PATH = "/com/example/embyapp";
    private static final String KEY_SERVER_URL = "serverUrl";
    private static final String KEY_ACCESS_TOKEN = "accessToken";
    private static final String KEY_CLIENT_AUTH_HEADER = "clientAuthHeader";
    private static final String KEY_USER_ID = "userId";
    private final Preferences prefs;


    // Cached API service instances
    private UserServiceApi userServiceApi;
    private ItemsServiceApi itemsServiceApi;
    private SystemServiceApi systemServiceApi;
    private ItemUpdateServiceApi itemUpdateServiceApi;
    private ImageServiceApi imageServiceApi;


    private final BooleanProperty loggedIn = new SimpleBooleanProperty(false);


    // --- Interceptor (TEMPORARY CHANGE: ONLY SEND TOKEN) ---
    private class AuthHeaderInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request.Builder builder = originalRequest.newBuilder();

            // 2. Add X-Emby-Token header IF we have an access token stored in EmbyService
            String localAccessToken = EmbyService.this.currentAccessToken; // Use local copy from outer class
            if (localAccessToken != null && !localAccessToken.isEmpty()) {
                builder.removeHeader("X-Emby-Token"); // Remove existing (safer)
                builder.header("X-Emby-Token", localAccessToken); // Add our own
                // System.out.println("Interceptor: Adding X-Emby-Token"); // Debug (Optional)
            } else {
                // System.out.println("Interceptor: NOT Adding X-Emby-Token (token is null/empty)"); // Debug (Optional)
            }


            Request newRequest = builder.build();
            return chain.proceed(newRequest);
        }
    }

    // (*** THÊM HÀM GETTER NÀY ***)

    /**
     * Trả về một instance mới của interceptor xác thực.
     * Dùng để tạo các OkHttpClient tùy chỉnh (ví dụ: upload file).
     */
    public Interceptor getAuthHeaderInterceptor() {
        return new AuthHeaderInterceptor();
    }


    // Private constructor
    private EmbyService() {
        apiClient = new ApiClient();
        OkHttpClient defaultClient = apiClient.getHttpClient();
        // (*** SỬA LẠI DÒNG NÀY ĐỂ DÙNG GETTER ***)
        defaultClient.interceptors().add(getAuthHeaderInterceptor());
        prefs = Preferences.userRoot().node(PREF_NODE_PATH);
        // System.out.println("Using Preferences node: " + prefs.absolutePath());
    }


    // Get singleton instance
    public static synchronized EmbyService getInstance() {
        if (instance == null) {
            instance = new EmbyService();
        }
        return instance;
    }

    // Other getters
    public ApiClient getApiClient() {
        return apiClient;
    }

    public AuthenticationAuthenticationResult getCurrentAuthResult() {
        return currentAuthResult;
    } // <- Dùng class đã import

    public String getCurrentUserId() {
        if (currentAuthResult != null && currentAuthResult.getUser() != null) {
            return currentAuthResult.getUser().getId();
        }
        return null;
    }

    public ReadOnlyBooleanProperty loggedInProperty() {
        return loggedIn;
    }

    public boolean isLoggedIn() {
        return loggedIn.get();
    }


    // --- Session Management ---
    public void setCurrentAuthResult(AuthenticationAuthenticationResult authResult, String serverUrl) { // <- Dùng class đã import
        this.currentAuthResult = authResult;
        String userIdToSave = null;

        if (authResult != null && authResult.getAccessToken() != null && serverUrl != null && !serverUrl.isEmpty()) {
            this.clientAuthHeader = generateClientAuthHeader();
            this.currentAccessToken = authResult.getAccessToken();
            apiClient.setBasePath(serverUrl);
            loggedIn.set(true);

            if (authResult.getUser() != null && authResult.getUser().getId() != null) {
                userIdToSave = authResult.getUser().getId();
            } else {
                System.err.println("Warning: UserDto or UserId is null in AuthenticationResult during login.");
            }

            saveSession(serverUrl, this.currentAccessToken, this.clientAuthHeader, userIdToSave);

            clearCachedApis();
        } else {
            this.currentAccessToken = null;
            loggedIn.set(false);
            this.clientAuthHeader = null;
            clearCachedApis();
        }
    }

    // Helper to generate auth header
    private String generateClientAuthHeader() {
        String appName = "EmbyClientJavaFX";
        String appVersion = "1.0.0";
        String deviceName = "MyMacDesktop";
        String deviceId = prefs.get("deviceId", UUID.randomUUID().toString());
        prefs.put("deviceId", deviceId);

        return String.format("Emby Client=\"%s\", Device=\"%s\", DeviceId=\"%s\", Version=\"%s\"",
                appName, deviceName, deviceId, appVersion);
    }

    // Helper to save session
    private void saveSession(String serverUrl, String accessToken, String clientHeader, String userId) {
        if (serverUrl != null && !serverUrl.isEmpty() && accessToken != null && !accessToken.isEmpty() && clientHeader != null && !clientHeader.isEmpty() && userId != null && !userId.isEmpty()) {
            try {
                // System.out.println("Saving session: URL=" + serverUrl + ", Token=" + accessToken.substring(0, Math.min(accessToken.length(), 10)) + "..., Header=" + clientHeader + ", UserID=" + userId);
                prefs.put(KEY_SERVER_URL, serverUrl);
                prefs.put(KEY_ACCESS_TOKEN, accessToken);
                prefs.put(KEY_CLIENT_AUTH_HEADER, clientHeader);
                prefs.put(KEY_USER_ID, userId);
                prefs.flush();
                // System.out.println("Session saved successfully.");
            } catch (Exception e) {
                System.err.println("Error saving session preferences: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Attempted to save session with null or empty URL/Token/Header/UserID.");
        }
    }

    // Helper to load session
    private String[] loadSession() {
        String serverUrl = prefs.get(KEY_SERVER_URL, null);
        String accessToken = prefs.get(KEY_ACCESS_TOKEN, null);
        String clientHeader = prefs.get(KEY_CLIENT_AUTH_HEADER, null);
        String userId = prefs.get(KEY_USER_ID, null);

        if (serverUrl != null && !serverUrl.isEmpty() && accessToken != null && !accessToken.isEmpty() && clientHeader != null && !clientHeader.isEmpty() && userId != null && !userId.isEmpty()) {
            // System.out.println("Loaded session: URL=" + serverUrl + ", Token=" + accessToken.substring(0, Math.min(accessToken.length(), 10)) + "..., Header=" + clientHeader + ", UserID=" + userId);
            return new String[]{serverUrl, accessToken, clientHeader, userId};
        } else {
            // System.out.println("No saved session found or essential info missing (URL/Token/Header/UserID).");
            return null;
        }
    }

    // Helper to clear session
    public void clearSession() {
        try {
            // System.out.println("Clearing session (keeping server URL)...");
            // KHÔNG XÓA SERVER URL: prefs.remove(KEY_SERVER_URL);
            prefs.remove(KEY_ACCESS_TOKEN);
            prefs.remove(KEY_CLIENT_AUTH_HEADER);
            prefs.remove(KEY_USER_ID);
            prefs.flush();
            // System.out.println("Session tokens cleared.");
        } catch (Exception e) {
            System.err.println("Error clearing session preferences: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to attempt restoring session
    public boolean tryRestoreSession() {
        String[] sessionData = loadSession();
        if (sessionData != null) {
            String serverUrl = sessionData[0];
            String accessToken = sessionData[1];
            String loadedClientHeader = sessionData[2];
            String loadedUserId = sessionData[3];

            apiClient.setBasePath(serverUrl);
            this.currentAccessToken = accessToken;
            this.clientAuthHeader = loadedClientHeader;

            try {
                // System.out.println("Attempting to restore session (Step 1: System Info)...");
                SystemInfo systemInfo = getSystemServiceApi().getSystemInfo(); // <- Dùng class đã import

                if (systemInfo != null) {
                    // System.out.println("System Info check OK. (Step 2: Get User Info using ID: " + loadedUserId + ")...");
                    UserDto currentUser = getUserServiceApi().getUsersById(loadedUserId); // <- Dùng class đã import

                    if (currentUser != null) {
                        // System.out.println("User Info check OK. Session restored successfully for User: " + currentUser.getName());
                        AuthenticationAuthenticationResult restoredAuth = new AuthenticationAuthenticationResult(); // <- Dùng class đã import
                        restoredAuth.setAccessToken(accessToken);
                        restoredAuth.setUser(currentUser);
                        this.currentAuthResult = restoredAuth;
                        loggedIn.set(true);
                        clearCachedApis();
                        return true;
                    } else {
                        System.err.println("Session restore failed: Could not retrieve User Info...");
                        clearSession();
                    }
                } else {
                    System.err.println("Session restore failed: Could not retrieve System Info...");
                    clearSession();
                }

            } catch (ApiException e) {
                System.err.println("API Exception during session restore (Code: " + e.getCode() + "): " + e.getMessage());
                if (e.getCode() != 0) {
                    clearSession();
                }
            } catch (Exception e) {
                System.err.println("Non-API Exception during session restore: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (!loggedIn.get()) {
                    this.currentAccessToken = null;
                    this.currentAuthResult = null;
                    this.clientAuthHeader = null;
                    // System.out.println("Cleared client state after failed restore attempt.");
                }
            }
        }
        // System.out.println("Session restore failed or no session found.");
        loggedIn.set(false);
        this.currentAccessToken = null;
        this.currentAuthResult = null;
        this.clientAuthHeader = null;
        return false;
    }

    // Logout method
    public void logout() {
        // System.out.println("Logging out...");
        setCurrentAuthResult(null, null);
        clearSession();
    }

    // Helper to clear all cached API instances
    private void clearCachedApis() {
        this.userServiceApi = null;
        this.itemsServiceApi = null;
        this.systemServiceApi = null;
        this.itemUpdateServiceApi = null;
        this.imageServiceApi = null;
    }


    // --- Getters for specific API services (cached) ---

    public synchronized UserServiceApi getUserServiceApi() {
        if (userServiceApi == null) {
            // System.out.println("Creating UserServiceApi");
            userServiceApi = new UserServiceApi(apiClient);
        }
        return userServiceApi;
    }

    public synchronized ItemsServiceApi getItemsServiceApi() {
        if (!isLoggedIn()) {
            System.err.println("Attempted to get ItemsServiceApi while not logged in.");
            return null;
        }
        if (itemsServiceApi == null) {
            // System.out.println("Creating ItemsServiceApi");
            itemsServiceApi = new ItemsServiceApi(apiClient);
        }
        return itemsServiceApi;
    }

    public synchronized SystemServiceApi getSystemServiceApi() {
        // SystemServiceApi can be used before login (for restore check)
        if (systemServiceApi == null) {
            // System.out.println("Creating SystemServiceApi");
            systemServiceApi = new SystemServiceApi(apiClient);
        }
        return systemServiceApi;
    }

    public synchronized ItemUpdateServiceApi getItemUpdateServiceApi() {
        if (!isLoggedIn()) {
            System.err.println("Attempted to get ItemUpdateServiceApi while not logged in.");
            return null;
        }
        if (itemUpdateServiceApi == null) {
            // System.out.println("Creating ItemUpdateServiceApi");
            itemUpdateServiceApi = new ItemUpdateServiceApi(apiClient);
        }
        return itemUpdateServiceApi;
    }

    public synchronized ImageServiceApi getImageServiceApi() {
        if (!isLoggedIn()) {
            System.err.println("Attempted to get ImageServiceApi while not logged in.");
            return null;
        }
        if (imageServiceApi == null) {
            // System.out.println("Creating ImageServiceApi");
            imageServiceApi = new ImageServiceApi(apiClient);
        }
        return imageServiceApi;
    }

    // Add this inside EmbyService.java
    public String getCurrentAccessToken() {
        return this.currentAccessToken;
    }

    public List<BaseItemDto> getListItemByTagId(String tagsName, Integer startIndex, Integer limit, boolean recursive) {
        // ... (Không thay đổi gì ở hàm này) ...
        ItemsServiceApi itemsServiceApi = getItemsServiceApi();
        TagServiceApi tagServiceApi = new TagServiceApi(apiClient);

        try {
            QueryResultBaseItemDto listItems = itemsServiceApi.getItems(
                    null,    //artistType
                    null,    //maxOfficialRating
                    null,    //hasThemeSong
                    null,    //hasThemeVideo
                    null,    //hasSubtitles
                    null,    //hasSpecialFeature
                    null,    //hasTrailer
                    null,    //isSpecialSeason
                    null,    //adjacentTo
                    null,    //startItemId
                    null,    //minIndexNumber
                    null,    //minStartDate
                    null,    //maxStartDate
                    null,    //minEndDate
                    null,    //maxEndDate
                    null,    //minPlayers
                    null,    //maxPlayers
                    null,    //parentIndexNumber
                    null,    //hasParentalRating
                    null,    //isHD
                    null,    //isUnaired
                    null,    //minCommunityRating
                    null,    //minCriticRating
                    null,    //airedDuringSeason
                    null,    //minPremiereDate
                    null,    //minDateLastSaved
                    null,    //minDateLastSavedForUser
                    null,    //maxPremiereDate
                    null,    //hasOverview
                    null,    //hasImdbId
                    null,    //hasTmdbId
                    null,    //hasTvdbId
                    null,    //excludeItemIds
                    startIndex,    //startIndex
                    limit,    //limit
                    recursive,    //recursive
                    null,    //searchTerm
                    "Ascending",    //sortOrder
                    null,    //parentId
                    null,    //fields
                    null,    //excludeItemTypes
                    "Movie,Series,Video,Game",    //includeItemTypes
                    null,    //anyProviderIdEquals
                    null,    //filters
                    null,    //isFavorite
                    null,    //isMovie
                    null,    //isSeries
                    null,    //isFolder
                    null,    //isNews
                    null,    //isKids
                    null,    //isSports
                    null,    //isNew
                    null,    //isPremiere
                    null,    //isNewOrPremiere
                    null,    //isRepeat
                    null,    //projectToMedia
                    null,    //mediaTypes
                    null,    //imageTypes
                    null,    //sortBy
                    null,    //isPlayed
                    null,    //genres
                    null,    //officialRatings
                    tagsName,    //tags
                    null,    //excludeTags
                    null,    //years
                    null,    //enableImages
                    null,    //enableUserData
                    null,    //imageTypeLimit
                    null,    //enableImageTypes
                    null,    //person
                    null,    //personIds
                    null,    //personTypes
                    null,    //studios
                    null,    //studioIds
                    null,    //artists
                    null,    //artistIds
                    null,    //albums
                    null,    //ids
                    null,    //videoTypes
                    null,    //containers
                    null,    //audioCodecs
                    null,    //audioLayouts
                    null,    //videoCodecs
                    null,    //extendedVideoTypes
                    null,    //subtitleCodecs
                    null,    //path
                    null,    //userId
                    null,    //minOfficialRating
                    null,    //isLocked
                    null,    //isPlaceHolder
                    null,    //hasOfficialRating
                    null,    //groupItemsIntoCollections
                    null,    //is3D
                    null,    //seriesStatus
                    null,    //nameStartsWithOrGreater
                    null,    //artistStartsWithOrGreater
                    null,    //albumArtistStartsWithOrGreater
                    null,    //nameStartsWith
                    null    //nameLessThan
            );
            if (listItems.getItems().isEmpty()) {
                System.out.println("Empty Item Tags");
            }

            if (!listItems.getItems().isEmpty()) {

                return listItems.getItems();
            }

        } catch (ApiException e) {
            System.out.println("Error fetching tags: " + e.getMessage());
        }

        return  null;
    }

    public List<BaseItemDto> getListItemByStudioId(String studioId, Integer startIndex, Integer limit, boolean recursive) {

        StudiosServiceApi studiosServiceApi = new StudiosServiceApi(apiClient);
        // ... (Không thay đổi gì ở hàm này) ...
        ItemsServiceApi itemsServiceApi = new ItemsServiceApi(apiClient);

        try {
            QueryResultBaseItemDto listItems = itemsServiceApi.getItems(
                    null,    //artistType
                    null,    //maxOfficialRating
                    null,    //hasThemeSong
                    null,    //hasThemeVideo
                    null,    //hasSubtitles
                    null,    //hasSpecialFeature
                    null,    //hasTrailer
                    null,    //isSpecialSeason
                    null,    //adjacentTo
                    null,    //startItemId
                    null,    //minIndexNumber
                    null,    //minStartDate
                    null,    //maxStartDate
                    null,    //minEndDate
                    null,    //maxEndDate
                    null,    //minPlayers
                    null,    //maxPlayers
                    null,    //parentIndexNumber
                    null,    //hasParentalRating
                    null,    //isHD
                    null,    //isUnaired
                    null,    //minCommunityRating
                    null,    //minCriticRating
                    null,    //airedDuringSeason
                    null,    //minPremiereDate
                    null,    //minDateLastSaved
                    null,    //minDateLastSavedForUser
                    null,    //maxPremiereDate
                    null,    //hasOverview
                    null,    //hasImdbId
                    null,    //hasTmdbId
                    null,    //hasTvdbId
                    null,    //excludeItemIds
                    startIndex,    //startIndex
                    limit,    //limit
                    recursive,    //recursive
                    null,    //searchTerm
                    "Ascending",    //sortOrder
                    null,    //parentId
                    null,    //fields
                    null,    //excludeItemTypes
                    "Movie,Series,Video,Game",    //includeItemTypes
                    null,    //anyProviderIdEquals
                    null,    //filters
                    null,    //isFavorite
                    null,    //isMovie
                    null,    //isSeries
                    null,    //isFolder
                    null,    //isNews
                    null,    //isKids
                    null,    //isSports
                    null,    //isNew
                    null,    //isPremiere
                    null,    //isNewOrPremiere
                    null,    //isRepeat
                    null,    //projectToMedia
                    null,    //mediaTypes
                    null,    //imageTypes
                    null,    //sortBy
                    null,    //isPlayed
                    null,    //genres
                    null,    //officialRatings
                    null,    //tags
                    null,    //excludeTags
                    null,    //years
                    null,    //enableImages
                    null,    //enableUserData
                    null,    //imageTypeLimit
                    null,    //enableImageTypes
                    null,    //person
                    null,    //personIds
                    null,    //personTypes
                    null,    //studios
                    studioId,    //studioIds
                    null,    //artists
                    null,    //artistIds
                    null,    //albums
                    null,    //ids
                    null,    //videoTypes
                    null,    //containers
                    null,    //audioCodecs
                    null,    //audioLayouts
                    null,    //videoCodecs
                    null,    //extendedVideoTypes
                    null,    //subtitleCodecs
                    null,    //path
                    null,    //userId
                    null,    //minOfficialRating
                    null,    //isLocked
                    null,    //isPlaceHolder
                    null,    //hasOfficialRating
                    null,    //groupItemsIntoCollections
                    null,    //is3D
                    null,    //seriesStatus
                    null,    //nameStartsWithOrGreater
                    null,    //artistStartsWithOrGreater
                    null,    //albumArtistStartsWithOrGreater
                    null,    //nameStartsWith
                    null    //nameLessThan
            );

            if (listItems.getItems().isEmpty()) {
                System.out.println("Empty Studios");
            }

            if (!listItems.getItems().isEmpty()) {

                return listItems.getItems();
            }

        } catch (ApiException e) {
            System.out.println("Error fetching studios: " + e.getMessage());
        }

        return null;
    }

    public  List<BaseItemDto> getListPeopleByID(String peopleID, Integer startIndex, Integer limit, boolean recursive) {

        PersonsServiceApi personsServiceApi = new PersonsServiceApi(apiClient);
        // ... (Không thay đổi gì ở hàm này) ...
        ItemsServiceApi itemsServiceApi = new ItemsServiceApi(apiClient);

        try{
            QueryResultBaseItemDto listPeople = itemsServiceApi.getItems(
                    null,    //artistType
                    null,    //maxOfficialRating
                    null,    //hasThemeSong
                    null,    //hasThemeVideo
                    null,    //hasSubtitles
                    null,    //hasSpecialFeature
                    null,    //hasTrailer
                    null,    //isSpecialSeason
                    null,    //adjacentTo
                    null,    //startItemId
                    null,    //minIndexNumber
                    null,    //minStartDate
                    null,    //maxStartDate
                    null,    //minEndDate
                    null,    //maxEndDate
                    null,    //minPlayers
                    null,    //maxPlayers
                    null,    //parentIndexNumber
                    null,    //hasParentalRating
                    null,    //isHD
                    null,    //isUnaired
                    null,    //minCommunityRating
                    null,    //minCriticRating
                    null,    //airedDuringSeason
                    null,    //minPremiereDate
                    null,    //minDateLastSaved
                    null,    //minDateLastSavedForUser
                    null,    //maxPremiereDate
                    null,    //hasOverview
                    null,    //hasImdbId
                    null,    //hasTmdbId
                    null,    //hasTvdbId
                    null,    //excludeItemIds
                    startIndex,    //startIndex
                    limit,    //limit
                    recursive,    //recursive
                    null,    //searchTerm
                    "Ascending",    //sortOrder
                    null,    //parentId
                    null,    //fields
                    null,    //excludeItemTypes
                    "Movie,Series,Video,Game",    //includeItemTypes
                    null,    //anyProviderIdEquals
                    null,    //filters
                    null,    //isFavorite
                    null,    //isMovie
                    null,    //isSeries
                    null,    //isFolder
                    null,    //isNews
                    null,    //isKids
                    null,    //isSports
                    null,    //isNew
                    null,    //isPremiere
                    null,    //isNewOrPremiere
                    null,    //isRepeat
                    null,    //projectToMedia
                    null,    //mediaTypes
                    null,    //imageTypes
                    null,    //sortBy
                    null,    //isPlayed
                    null,    //genres
                    null,    //officialRatings
                    null,    //tags
                    null,    //excludeTags
                    null,    //years
                    null,    //enableImages
                    null,    //enableUserData
                    null,    //imageTypeLimit
                    null,    //enableImageTypes
                    null,    //person
                    peopleID,    //personIds
                    null,    //personTypes
                    null,    //studios
                    null,    //studioIds
                    null,    //artists
                    null,    //artistIds
                    null,    //albums
                    null,    //ids
                    null,    //videoTypes
                    null,    //containers
                    null,    //audioCodecs
                    null,    //audioLayouts
                    null,    //videoCodecs
                    null,    //extendedVideoTypes
                    null,    //subtitleCodecs
                    null,    //path
                    null,    //userId
                    null,    //minOfficialRating
                    null,    //isLocked
                    null,    //isPlaceHolder
                    null,    //hasOfficialRating
                    null,    //groupItemsIntoCollections
                    null,    //is3D
                    null,    //seriesStatus
                    null,    //nameStartsWithOrGreater
                    null,    //artistStartsWithOrGreater
                    null,    //albumArtistStartsWithOrGreater
                    null,    //nameStartsWith
                    null    //nameLessThan
            );

            if (listPeople.getItems().isEmpty()) {
                System.out.println("Empty People");
            }

            if (!listPeople.getItems().isEmpty()) {

                return listPeople.getItems();
            }
        } catch (ApiException e) {
            System.out.println("Error fetching people: " + e.getMessage());
        }

        return null;
    }

    public List<BaseItemDto> getListItemByGenreId(String nameGenres, Integer startIndex, Integer limit, boolean recursive) {

        GenresServiceApi genresServiceApi = new GenresServiceApi(apiClient);
        // ... (Không thay đổi gì ở hàm này) ...
        ItemsServiceApi itemsServiceApi = new ItemsServiceApi(apiClient);

        try {
            QueryResultBaseItemDto listItems = itemsServiceApi.getItems(
                    null,    //artistType
                    null,    //maxOfficialRating
                    null,    //hasThemeSong
                    null,    //hasThemeVideo
                    null,    //hasSubtitles
                    null,    //hasSpecialFeature
                    null,    //hasTrailer
                    null,    //isSpecialSeason
                    null,    //adjacentTo
                    null,    //startItemId
                    null,    //minIndexNumber
                    null,    //minStartDate
                    null,    //maxStartDate
                    null,    //minEndDate
                    null,    //maxEndDate
                    null,    //minPlayers
                    null,    //maxPlayers
                    null,    //parentIndexNumber
                    null,    //hasParentalRating
                    null,    //isHD
                    null,    //isUnaired
                    null,    //minCommunityRating
                    null,    //minCriticRating
                    null,    //airedDuringSeason
                    null,    //minPremiereDate
                    null,    //minDateLastSaved
                    null,    //minDateLastSavedForUser
                    null,    //maxPremiereDate
                    null,    //hasOverview
                    null,    //hasImdbId
                    null,    //hasTmdbId
                    null,    //hasTvdbId
                    null,    //excludeItemIds
                    startIndex,    //startIndex
                    limit,    //limit
                    recursive,    //recursive
                    null,    //searchTerm
                    null,    //sortOrder
                    null,    //parentId
                    null,    //fields
                    null,    //excludeItemTypes
                    "Movie, Series, Video, Game, MusicAlbum",    //includeItemTypes
                    null,    //anyProviderIdEquals
                    null,    //filters
                    null,    //isFavorite
                    null,    //isMovie
                    null,    //isSeries
                    null,    //isFolder
                    null,    //isNews
                    null,    //isKids
                    null,    //isSports
                    null,    //isNew
                    null,    //isPremiere
                    null,    //isNewOrPremiere
                    null,    //isRepeat
                    null,    //projectToMedia
                    null,    //mediaTypes
                    null,    //imageTypes
                    null,    //sortBy
                    null,    //isPlayed
                    nameGenres,    //genres
                    null,    //officialRatings
                    null,    //tags
                    null,    //excludeTags
                    null,    //years
                    null,    //enableImages
                    null,    //enableUserData
                    null,    //imageTypeLimit
                    null,    //enableImageTypes
                    null,    //person
                    null,    //personIds
                    null,    //personTypes
                    null,    //studios
                    null,    //studioIds
                    null,    //artists
                    null,    //artistIds
                    null,    //albums
                    null,    //ids
                    null,    //videoTypes
                    null,    //containers
                    null,    //audioCodecs
                    null,    //audioLayouts
                    null,    //videoCodecs
                    null,    //extendedVideoTypes
                    null,    //subtitleCodecs
                    null,    //path
                    null,    //userId
                    null,    //minOfficialRating
                    null,    //isLocked
                    null,    //isPlaceHolder
                    null,    //hasOfficialRating
                    null,    //groupItemsIntoCollections
                    null,    //is3D
                    null,    //seriesStatus
                    null,    //nameStartsWithOrGreater
                    null,    //artistStartsWithOrGreater
                    null,    //albumArtistStartsWithOrGreater
                    null,    //nameStartsWith
                    null    //nameLessThan
            );
            if (listItems.getItems().isEmpty()) {
                System.out.println("Empty Genres");
            }

            if (!listItems.getItems().isEmpty()) {

                return listItems.getItems();
            }
        } catch (ApiException e) {
            System.out.println("Error fetching Genres: " + e.getMessage());
        }
        return null;
    }

}