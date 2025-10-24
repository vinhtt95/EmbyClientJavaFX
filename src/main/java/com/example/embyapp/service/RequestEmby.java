package com.example.embyapp.service;

import com.example.emby.EmbyClient.ApiException;
import com.example.emby.EmbyClient.Java.ItemsServiceApi;
import com.example.emby.modelEmby.QueryResultBaseItemDto;

public class RequestEmby {

    public QueryResultBaseItemDto getQueryResultBaseItemDto(String parentID, ItemsServiceApi itemsServiceApi) throws ApiException {
        QueryResultBaseItemDto result = itemsServiceApi.getItems(
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
        return result;
    }
}
