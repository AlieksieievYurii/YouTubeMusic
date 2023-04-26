package com.youtubemusic.core.data

enum class OrderEnum {
    RELEVANCE, UPLOAD_DATE, BY_TITLE, VIEW_COUNT, RATING
}

enum class DurationEnum {
    ANY, SHORT, MEDIUM, LONG
}

enum class UploadDateEnum {
    ANYTIME, LAST_HOUR, TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR
}

data class SearchFilterData(
    val orderBy: OrderEnum,
    val duration: DurationEnum,
    val uploadDate: UploadDateEnum,
    val featureSyndicated: Boolean,
    val featureEpisode: Boolean,
    val featureMovie: Boolean
) {
    companion object {
        val DEFAULT = SearchFilterData(OrderEnum.RELEVANCE, DurationEnum.ANY, UploadDateEnum.ANYTIME,
            featureSyndicated = false,
            featureEpisode = false,
            featureMovie = false
        )
    }
}