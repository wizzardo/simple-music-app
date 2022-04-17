package com.example.be.db.dto

class AlbumDto(
    var date: String = "",
    var name: String = "",
    var songs: List<Song> = emptyList(),
) {

    class Song(
        var track: Int = 0,
        var title: String = "",
        var comment: String = "",
        var duration: Int = 0,
        var stream: String = "",
        var path: String = ""
    ) {
        override fun toString(): String {
            return "Song(track=$track, title='$title', comment='$comment', duration=$duration, stream='$stream', path='$path')"
        }
    }

    override fun toString(): String {
        return "AlbumDto(date='$date', name='$name', songs=$songs)"
    }
}
