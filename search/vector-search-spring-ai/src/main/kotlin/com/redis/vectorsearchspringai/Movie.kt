package com.redis.vectorsearchspringai

data class Movie(
    var title: String? = null,
    var year: Int = 0,
    var cast: List<String> = emptyList(),
    var genres: List<String> = emptyList(),
    var href: String? = null,
    var extract: String? = null,
    var thumbnail: String? = null,
    var thumbnailWidth: Int = 0,
    var thumbnailHeight: Int = 0
)