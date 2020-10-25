package org.schabi.newpipelegacy.player.event;

import org.schabi.newpipelegacy.player.MainPlayer;
import org.schabi.newpipelegacy.player.VideoPlayerImpl;

public interface PlayerServiceExtendedEventListener extends PlayerServiceEventListener {
    void onServiceConnected(VideoPlayerImpl player,
                            MainPlayer playerService,
                            boolean playAfterConnect);
    void onServiceDisconnected();
}
