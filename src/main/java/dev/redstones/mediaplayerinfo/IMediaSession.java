package dev.redstones.mediaplayerinfo;

public interface IMediaSession {
    MediaInfo getMedia();
    void previous();
    void playPause();
    void next();
}
