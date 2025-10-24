package dev.redstones.mediaplayerinfo;

public class MediaInfo {
    private final String title;
    private final String artist;
    private final byte[] artworkPng;
    private final double position;
    private final double duration;
    private final boolean playing;

    public MediaInfo(String title, String artist, byte[] artworkPng, double position, double duration, boolean playing) {
        this.title = title;
        this.artist = artist;
        this.artworkPng = artworkPng;
        this.position = position;
        this.duration = duration;
        this.playing = playing;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public byte[] getArtworkPng() {
        return artworkPng;
    }

    public double getPosition() {
        return position;
    }

    public double getDuration() {
        return duration;
    }

    public boolean getPlaying() {
        return playing;
    }
}
