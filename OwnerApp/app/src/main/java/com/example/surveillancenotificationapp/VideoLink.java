package com.example.surveillancenotificationapp;

public class VideoLink {

    private long ID;
    private String videoName;
    private String videoURLPath;

    public VideoLink(long ID, String videoName, String videoURLPath) {
        this.ID = ID;
        this.videoName = videoName;
        this.videoURLPath = videoURLPath;
    }

    public long getID() {
        return ID;
    }

    public String getVideoName() {
        return videoName;
    }

    public String getVideoURLPath() {
        return videoURLPath;
    }
}
