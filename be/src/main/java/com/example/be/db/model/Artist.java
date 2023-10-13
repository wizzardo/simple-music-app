package com.example.be.db.model;

import com.example.be.service.FFmpegService;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Artist {
    public Long id = null;
    public Date created = null;
    public Date updated = null;
    public String name = null;
    public List<Album> albums = Collections.emptyList();
    public String path = null;

    static public class Album {
        public String id = "";
        public String path = "";
        public String date = "";
        public String name = "";
        public List<Song> songs = Collections.emptyList();
        public String coverPath = null;
        public String coverHash = null;
        public String coverEncryptionKey = null;

        public static class Song {
            public String id = "";
            public int track = 0;
            public String title = "";
            public int duration = 0;
            public List<String> streams = Collections.emptyList();
            public String path = "";
            public FFmpegService.AudioFormat format = FFmpegService.AudioFormat.FLAC;
            public String encryptionKey = "";
        }
    }
}
