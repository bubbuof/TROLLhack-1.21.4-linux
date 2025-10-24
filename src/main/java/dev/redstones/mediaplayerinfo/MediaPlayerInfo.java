package dev.redstones.mediaplayerinfo;

import java.util.Collections;
import java.util.List;

public class MediaPlayerInfo {
    public static final MediaPlayerInfo Instance = new MediaPlayerInfo();
    
    private MediaPlayerInfo() {}
    
    public List<IMediaSession> getMediaSessions() {
        // Return empty list as stub implementation
        // In a real implementation, this would query system media sessions
        return Collections.emptyList();
    }
}
