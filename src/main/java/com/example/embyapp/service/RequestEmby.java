package com.example.embyapp.service;

import com.example.emby.EmbyClient.ApiException;
import com.example.emby.EmbyClient.Java.ItemsServiceApi;
import com.example.emby.modelEmby.QueryResultBaseItemDto;

public class RequestEmby {


    /**
     * Lấy danh sách các Item con theo parentID
     * @param parentID
     * @param itemsServiceApi
     * @return
     * @throws ApiException
     */
    public QueryResultBaseItemDto getQueryResultBaseItemDto(String parentID, ItemsServiceApi itemsServiceApi){
        QueryResultBaseItemDto result = null;
        try {
            result = itemsServiceApi.getItems(
                    null,   // artistType
                    null,   // maxOfficialRating
                    null,   // hasThemeSong
                    null,   // hasThemeVideo
                    null,   // hasSubtitles
                    null,   // hasSpecialFeature
                    null,   // hasTrailer
                    null,   // adjacentTo
                    null,   // minIndexNumber
                    null,   // minStartDate
                    null,   // maxStartDate
                    null,   // minEndDate
                    null,   // maxEndDate
                    null,   // minPlayers
                    null,   // maxPlayers
                    null,   // parentIndexNumber
                    null,   // hasParentalRating
                    null,   // isHD
                    null,   // isUnaired
                    null,   // minCommunityRating
                    null,   // minCriticRating
                    null,   // airedDuringSeason
                    null,   // minPremiereDate
                    null,   // minDateLastSaved
                    null,   // minDateLastSavedForUser
                    null,   // maxPremiereDate
                    null,   // hasOverview
                    null,   // hasImdbId
                    null,   // hasTmdbId
                    null,   // hasTvdbId
                    null,   // excludeItemIds
                    null,   // startIndex
                    null,   // limit
                    null,   // recursive
                    null,   // searchTerm
                    null,   // sortOrder
                    parentID,   // parentId
                    null,   // fields
                    null,   // excludeItemTypes
                    null,   // includeItemTypes
                    null,   // anyProviderIdEquals
                    null,   // filters
                    null,   // isFavorite
                    null,   // isMovie
                    null,   // isSeries
                    null,   // isFolder
                    null,   // isNews
                    null,   // isKids
                    null,   // isSports
                    null,   // isNew
                    null,   // isPremiere
                    null,   // isNewOrPremiere
                    null,   // isRepeat
                    null,   // projectToMedia
                    null,   // mediaTypes
                    null,   // imageTypes
                    null,   // sortBy
                    null,   // isPlayed
                    null,   // genres
                    null,   // officialRatings
                    null,   // tags
                    null,   // excludeTags
                    null,   // years
                    null,   // enableImages
                    null,   // enableUserData
                    null,   // imageTypeLimit
                    null,   // enableImageTypes
                    null,   // person
                    null,   // personIds
                    null,   // personTypes
                    null,   // studios
                    null,   // studioIds
                    null,   // artists
                    null,   // artistIds
                    null,   // albums
                    null,   // ids
                    null,   // videoTypes
                    null,   // containers
                    null,   // audioCodecs
                    null,   // audioLayouts
                    null,   // videoCodecs
                    null,   // extendedVideoTypes
                    null,   // subtitleCodecs
                    null,   // path
                    null,   // userId
                    null,   // minOfficialRating
                    null,   // isLocked
                    null,   // isPlaceHolder
                    null,   // hasOfficialRating
                    null,   // groupItemsIntoCollections
                    null,   // is3D
                    null,   // seriesStatus
                    null,   // nameStartsWithOrGreater
                    null,   // artistStartsWithOrGreater
                    null,   // albumArtistStartsWithOrGreater
                    null,   // nameStartsWith
                    null
            );
        } catch (ApiException e) {
            System.out.println(e.getMessage());
        }
        return result;
    }


    /**
     * Lấy danh sách library theo userID
     * @param userID
     * @param itemsServiceApi
     * @return BaseItemDTO
     */
    public QueryResultBaseItemDto getUsersByUseridItems(String userID, ItemsServiceApi itemsServiceApi) {
        try {
            return itemsServiceApi.getUsersByUseridItems(
                    userID, // userId
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
                    null, // seriesStatus
                    // seriesStatus
                    null,null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null
            );
        } catch (ApiException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }
}
