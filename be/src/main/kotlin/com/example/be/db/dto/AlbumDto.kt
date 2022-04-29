package com.example.be.db.dto

class AlbumDto(
    var date: String = "",
    var name: String = "",
    var songs: List<Song> = emptyList(),
    var coverPath: String? = null,
    var coverHash: String? = null,
) {

    class Song(
        var track: Int = 0,
        var title: String = "",
        var comment: String = "",
        var duration: Int = 0,
        var streams: List<String> = emptyList(),
        var path: String = ""
    ) {
        override fun toString(): String {
            return "Song(track=$track, title='$title', comment='$comment', duration=$duration, streams='$streams', path='$path')"
        }
    }

    override fun toString(): String {
        return "AlbumDto(date='$date', name='$name', songs=$songs)"
    }
}
