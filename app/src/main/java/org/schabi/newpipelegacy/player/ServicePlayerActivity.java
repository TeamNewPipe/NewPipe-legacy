package org.schabi.newpipelegacy.player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;

import org.schabi.newpipelegacy.R;
import org.schabi.newpipelegacy.databinding.ActivityPlayerQueueControlBinding;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipelegacy.fragments.OnScrollBelowItemsListener;
import org.schabi.newpipelegacy.local.dialog.PlaylistAppendDialog;
import org.schabi.newpipelegacy.local.dialog.PlaylistCreationDialog;
import org.schabi.newpipelegacy.player.event.PlayerEventListener;
import org.schabi.newpipelegacy.player.helper.PlaybackParameterDialog;
import org.schabi.newpipelegacy.player.playqueue.PlayQueue;
import org.schabi.newpipelegacy.player.playqueue.PlayQueueAdapter;
import org.schabi.newpipelegacy.player.playqueue.PlayQueueItem;
import org.schabi.newpipelegacy.player.playqueue.PlayQueueItemBuilder;
import org.schabi.newpipelegacy.player.playqueue.PlayQueueItemHolder;
import org.schabi.newpipelegacy.player.playqueue.PlayQueueItemTouchCallback;
import org.schabi.newpipelegacy.util.Localization;
import org.schabi.newpipelegacy.util.NavigationHelper;
import org.schabi.newpipelegacy.util.PermissionHelper;
import org.schabi.newpipelegacy.util.ThemeHelper;

import java.util.Collections;
import java.util.List;

import static org.schabi.newpipelegacy.player.helper.PlayerHelper.formatSpeed;
import static org.schabi.newpipelegacy.util.Localization.assureCorrectAppLanguage;

public abstract class ServicePlayerActivity extends AppCompatActivity
        implements PlayerEventListener, SeekBar.OnSeekBarChangeListener,
        View.OnClickListener, PlaybackParameterDialog.Callback {
    private static final int RECYCLER_ITEM_POPUP_MENU_GROUP_ID = 47;
    private static final int SMOOTH_SCROLL_MAXIMUM_DISTANCE = 80;

    protected BasePlayer player;

    private boolean serviceBound;
    private ServiceConnection serviceConnection;

    private boolean seeking;
    private boolean redraw;

    ////////////////////////////////////////////////////////////////////////////
    // Views
    ////////////////////////////////////////////////////////////////////////////

    private ActivityPlayerQueueControlBinding queueControlBinding;

    private ItemTouchHelper itemTouchHelper;

    private Menu menu;

    ////////////////////////////////////////////////////////////////////////////
    // Abstracts
    ////////////////////////////////////////////////////////////////////////////

    public abstract String getTag();

    public abstract String getSupportActionTitle();

    public abstract Intent getBindIntent();

    public abstract void startPlayerListener();

    public abstract void stopPlayerListener();

    public abstract int getPlayerOptionMenuResource();

    public abstract void setupMenu(Menu m);

    ////////////////////////////////////////////////////////////////////////////
    // Activity Lifecycle
    ////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        assureCorrectAppLanguage(this);
        super.onCreate(savedInstanceState);
        ThemeHelper.setTheme(this);

        queueControlBinding = ActivityPlayerQueueControlBinding.inflate(getLayoutInflater());
        setContentView(queueControlBinding.getRoot());

        setSupportActionBar(queueControlBinding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getSupportActionTitle());
        }

        serviceConnection = getServiceConnection();
        bind();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (redraw) {
            ActivityCompat.recreate(this);
            redraw = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu m) {
        this.menu = m;
        getMenuInflater().inflate(R.menu.menu_play_queue, m);
        getMenuInflater().inflate(getPlayerOptionMenuResource(), m);
        onMaybeMuteChanged();
        return true;
    }

    // Allow to setup visibility of menuItems
    @Override
    public boolean onPrepareOptionsMenu(final Menu m) {
        setupMenu(m);
        return super.onPrepareOptionsMenu(m);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_settings:
                NavigationHelper.openSettings(this);
                return true;
            case R.id.action_append_playlist:
                appendAllToPlaylist();
                return true;
            case R.id.action_playback_speed:
                openPlaybackParameterDialog();
                return true;
            case R.id.action_mute:
                player.onMuteUnmuteButtonClicked();
                return true;
            case R.id.action_system_audio:
                startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS));
                return true;
            case R.id.action_switch_main:
                this.player.setRecovery();
                NavigationHelper.playOnMainPlayer(this, player.getPlayQueue(), true);
                return true;
            case R.id.action_switch_popup:
                if (PermissionHelper.isPopupEnabled(this)) {
                    this.player.setRecovery();
                    NavigationHelper.playOnPopupPlayer(this, player.playQueue, true);
                } else {
                    PermissionHelper.showPopupEnablementToast(this);
                }
                return true;
            case R.id.action_switch_background:
                this.player.setRecovery();
                NavigationHelper.playOnBackgroundPlayer(this, player.playQueue, true);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbind();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Service Connection
    ////////////////////////////////////////////////////////////////////////////

    private void bind() {
        final boolean success = bindService(getBindIntent(), serviceConnection, BIND_AUTO_CREATE);
        if (!success) {
            unbindService(serviceConnection);
        }
        serviceBound = success;
    }

    private void unbind() {
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
            stopPlayerListener();

            if (player != null && player.getPlayQueueAdapter() != null) {
                player.getPlayQueueAdapter().unsetSelectedListener();
            }
            queueControlBinding.playQueue.setAdapter(null);
            if (itemTouchHelper != null) {
                itemTouchHelper.attachToRecyclerView(null);
            }

            itemTouchHelper = null;
            player = null;
        }
    }

    private ServiceConnection getServiceConnection() {
        return new ServiceConnection() {
            @Override
            public void onServiceDisconnected(final ComponentName name) {
                Log.d(getTag(), "Player service is disconnected");
            }

            @Override
            public void onServiceConnected(final ComponentName name, final IBinder service) {
                Log.d(getTag(), "Player service is connected");

                if (service instanceof PlayerServiceBinder) {
                    player = ((PlayerServiceBinder) service).getPlayerInstance();
                } else if (service instanceof MainPlayer.LocalBinder) {
                    player = ((MainPlayer.LocalBinder) service).getPlayer();
                }

                if (player == null || player.getPlayQueue() == null
                        || player.getPlayQueueAdapter() == null || player.getPlayer() == null) {
                    unbind();
                    finish();
                } else {
                    buildComponents();
                    startPlayerListener();
                }
            }
        };
    }

    ////////////////////////////////////////////////////////////////////////////
    // Component Building
    ////////////////////////////////////////////////////////////////////////////

    private void buildComponents() {
        buildQueue();
        buildMetadata();
        buildSeekBar();
        buildControls();
    }

    private void buildQueue() {
        queueControlBinding.playQueue.setLayoutManager(new LinearLayoutManager(this));
        queueControlBinding.playQueue.setAdapter(player.getPlayQueueAdapter());
        queueControlBinding.playQueue.setClickable(true);
        queueControlBinding.playQueue.setLongClickable(true);
        queueControlBinding.playQueue.clearOnScrollListeners();
        queueControlBinding.playQueue.addOnScrollListener(getQueueScrollListener());

        itemTouchHelper = new ItemTouchHelper(getItemTouchCallback());
        itemTouchHelper.attachToRecyclerView(queueControlBinding.playQueue);

        player.getPlayQueueAdapter().setSelectedListener(getOnSelectedListener());
    }

    private void buildMetadata() {
        queueControlBinding.metadata.setOnClickListener(this);
        queueControlBinding.songName.setSelected(true);
        queueControlBinding.artistName.setSelected(true);
    }

    private void buildSeekBar() {
        queueControlBinding.seekBar.setOnSeekBarChangeListener(this);
        queueControlBinding.liveSync.setOnClickListener(this);
    }

    private void buildControls() {
        queueControlBinding.controlRepeat.setOnClickListener(this);
        queueControlBinding.controlBackward.setOnClickListener(this);
        queueControlBinding.controlFastRewind.setOnClickListener(this);
        queueControlBinding.controlPlayPause.setOnClickListener(this);
        queueControlBinding.controlFastForward.setOnClickListener(this);
        queueControlBinding.controlForward.setOnClickListener(this);
        queueControlBinding.controlShuffle.setOnClickListener(this);
    }

    private void buildItemPopupMenu(final PlayQueueItem item, final View view) {
        final PopupMenu popupMenu = new PopupMenu(this, view);
        final MenuItem remove = popupMenu.getMenu().add(RECYCLER_ITEM_POPUP_MENU_GROUP_ID, 0,
                Menu.NONE, R.string.play_queue_remove);
        remove.setOnMenuItemClickListener(menuItem -> {
            if (player == null) {
                return false;
            }

            final int index = player.getPlayQueue().indexOf(item);
            if (index != -1) {
                player.getPlayQueue().remove(index);
            }
            return true;
        });

        final MenuItem detail = popupMenu.getMenu().add(RECYCLER_ITEM_POPUP_MENU_GROUP_ID, 1,
                Menu.NONE, R.string.play_queue_stream_detail);
        detail.setOnMenuItemClickListener(menuItem -> {
            // playQueue is null since we don't want any queue change
            NavigationHelper.openVideoDetail(this, item.getServiceId(), item.getUrl(),
                    item.getTitle(), null, false);
            return true;
        });

        final MenuItem append = popupMenu.getMenu().add(RECYCLER_ITEM_POPUP_MENU_GROUP_ID, 2,
                Menu.NONE, R.string.append_playlist);
        append.setOnMenuItemClickListener(menuItem -> {
            openPlaylistAppendDialog(Collections.singletonList(item));
            return true;
        });

        final MenuItem share = popupMenu.getMenu().add(RECYCLER_ITEM_POPUP_MENU_GROUP_ID, 3,
                Menu.NONE, R.string.share);
        share.setOnMenuItemClickListener(menuItem -> {
            shareUrl(item.getTitle(), item.getUrl());
            return true;
        });

        popupMenu.show();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Component Helpers
    ////////////////////////////////////////////////////////////////////////////

    private OnScrollBelowItemsListener getQueueScrollListener() {
        return new OnScrollBelowItemsListener() {
            @Override
            public void onScrolledDown(final RecyclerView recyclerView) {
                if (player != null && player.getPlayQueue() != null
                        && !player.getPlayQueue().isComplete()) {
                    player.getPlayQueue().fetch();
                } else {
                    queueControlBinding.playQueue.clearOnScrollListeners();
                }
            }
        };
    }

    private ItemTouchHelper.SimpleCallback getItemTouchCallback() {
        return new PlayQueueItemTouchCallback() {
            @Override
            public void onMove(final int sourceIndex, final int targetIndex) {
                if (player != null) {
                    player.getPlayQueue().move(sourceIndex, targetIndex);
                }
            }

            @Override
            public void onSwiped(final int index) {
                if (index != -1) {
                    player.getPlayQueue().remove(index);
                }
            }
        };
    }

    private PlayQueueItemBuilder.OnSelectedListener getOnSelectedListener() {
        return new PlayQueueItemBuilder.OnSelectedListener() {
            @Override
            public void selected(final PlayQueueItem item, final View view) {
                if (player != null) {
                    player.onSelected(item);
                }
            }

            @Override
            public void held(final PlayQueueItem item, final View view) {
                if (player == null) {
                    return;
                }

                final int index = player.getPlayQueue().indexOf(item);
                if (index != -1) {
                    buildItemPopupMenu(item, view);
                }
            }

            @Override
            public void onStartDrag(final PlayQueueItemHolder viewHolder) {
                if (itemTouchHelper != null) {
                    itemTouchHelper.startDrag(viewHolder);
                }
            }
        };
    }

    private void scrollToSelected() {
        if (player == null) {
            return;
        }

        final int currentPlayingIndex = player.getPlayQueue().getIndex();
        final int currentVisibleIndex;
        if (queueControlBinding.playQueue.getLayoutManager() instanceof LinearLayoutManager) {
            final LinearLayoutManager layout =
                    (LinearLayoutManager) queueControlBinding.playQueue.getLayoutManager();
            currentVisibleIndex = layout.findFirstVisibleItemPosition();
        } else {
            currentVisibleIndex = 0;
        }

        final int distance = Math.abs(currentPlayingIndex - currentVisibleIndex);
        if (distance < SMOOTH_SCROLL_MAXIMUM_DISTANCE) {
            queueControlBinding.playQueue.smoothScrollToPosition(currentPlayingIndex);
        } else {
            queueControlBinding.playQueue.scrollToPosition(currentPlayingIndex);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Component On-Click Listener
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public void onClick(final View view) {
        if (player == null) {
            return;
        }

        if (view.getId() == queueControlBinding.controlRepeat.getId()) {
            player.onRepeatClicked();
        } else if (view.getId() == queueControlBinding.controlBackward.getId()) {
            player.onPlayPrevious();
        } else if (view.getId() == queueControlBinding.controlFastRewind.getId()) {
            player.onFastRewind();
        } else if (view.getId() == queueControlBinding.controlPlayPause.getId()) {
            player.onPlayPause();
        } else if (view.getId() == queueControlBinding.controlFastForward.getId()) {
            player.onFastForward();
        } else if (view.getId() == queueControlBinding.controlForward.getId()) {
            player.onPlayNext();
        } else if (view.getId() == queueControlBinding.controlShuffle.getId()) {
            player.onShuffleClicked();
        } else if (view.getId() == queueControlBinding.metadata.getId()) {
            scrollToSelected();
        } else if (view.getId() == queueControlBinding.liveSync.getId()) {
            player.seekToDefault();
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Playback Parameters
    ////////////////////////////////////////////////////////////////////////////

    private void openPlaybackParameterDialog() {
        if (player == null) {
            return;
        }
        PlaybackParameterDialog.newInstance(player.getPlaybackSpeed(), player.getPlaybackPitch(),
                player.getPlaybackSkipSilence(), this).show(getSupportFragmentManager(), getTag());
    }

    @Override
    public void onPlaybackParameterChanged(final float playbackTempo, final float playbackPitch,
                                           final boolean playbackSkipSilence) {
        if (player != null) {
            player.setPlaybackParameters(playbackTempo, playbackPitch, playbackSkipSilence);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Seekbar Listener
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress,
                                  final boolean fromUser) {
        if (fromUser) {
            final String seekTime = Localization.getDurationString(progress / 1000);
            queueControlBinding.currentTime.setText(seekTime);
            queueControlBinding.seekDisplay.setText(seekTime);
        }
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
        seeking = true;
        queueControlBinding.seekDisplay.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
        if (player != null) {
            player.seekTo(seekBar.getProgress());
        }
        queueControlBinding.seekDisplay.setVisibility(View.GONE);
        seeking = false;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Playlist append
    ////////////////////////////////////////////////////////////////////////////

    private void appendAllToPlaylist() {
        if (player != null && player.getPlayQueue() != null) {
            openPlaylistAppendDialog(player.getPlayQueue().getStreams());
        }
    }

    private void openPlaylistAppendDialog(final List<PlayQueueItem> playlist) {
        final PlaylistAppendDialog d = PlaylistAppendDialog.fromPlayQueueItems(playlist);

        PlaylistAppendDialog.onPlaylistFound(getApplicationContext(),
            () -> d.show(getSupportFragmentManager(), getTag()),
            () -> PlaylistCreationDialog.newInstance(d)
                    .show(getSupportFragmentManager(), getTag()
        ));
    }

    ////////////////////////////////////////////////////////////////////////////
    // Share
    ////////////////////////////////////////////////////////////////////////////

    private void shareUrl(final String subject, final String url) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, url);
        startActivity(Intent.createChooser(intent, getString(R.string.share_dialog_title)));
    }

    ////////////////////////////////////////////////////////////////////////////
    // Binding Service Listener
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public void onQueueUpdate(final PlayQueue queue) {
    }

    @Override
    public void onPlaybackUpdate(final int state, final int repeatMode, final boolean shuffled,
                                 final PlaybackParameters parameters) {
        onStateChanged(state);
        onPlayModeChanged(repeatMode, shuffled);
        onPlaybackParameterChanged(parameters);
        onMaybePlaybackAdapterChanged();
        onMaybeMuteChanged();
    }

    @Override
    public void onProgressUpdate(final int currentProgress, final int duration,
                                 final int bufferPercent) {
        // Set buffer progress
        queueControlBinding.seekBar.setSecondaryProgress((int) (queueControlBinding.seekBar.getMax()
                * ((float) bufferPercent / 100)));

        // Set Duration
        queueControlBinding.seekBar.setMax(duration);
        queueControlBinding.endTime.setText(Localization.getDurationString(duration / 1000));

        // Set current time if not seeking
        if (!seeking) {
            queueControlBinding.seekBar.setProgress(currentProgress);
            queueControlBinding.currentTime.setText(Localization
                    .getDurationString(currentProgress / 1000));
        }

        if (player != null) {
            queueControlBinding.liveSync.setClickable(!player.isLiveEdge());
        }

        // this will make sure progressCurrentTime has the same width as progressEndTime
        final ViewGroup.LayoutParams currentTimeParams =
                queueControlBinding.currentTime.getLayoutParams();
        currentTimeParams.width = queueControlBinding.endTime.getWidth();
        queueControlBinding.currentTime.setLayoutParams(currentTimeParams);
    }

    @Override
    public void onMetadataUpdate(final StreamInfo info, final PlayQueue queue) {
        if (info != null) {
            queueControlBinding.songName.setText(info.getName());
            queueControlBinding.artistName.setText(info.getUploaderName());

            queueControlBinding.endTime.setVisibility(View.GONE);
            queueControlBinding.liveSync.setVisibility(View.GONE);
            switch (info.getStreamType()) {
                case LIVE_STREAM:
                case AUDIO_LIVE_STREAM:
                    queueControlBinding.liveSync.setVisibility(View.VISIBLE);
                    break;
                default:
                    queueControlBinding.endTime.setVisibility(View.VISIBLE);
                    break;
            }

            scrollToSelected();
        }
    }

    @Override
    public void onServiceStopped() {
        unbind();
        finish();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Binding Service Helper
    ////////////////////////////////////////////////////////////////////////////

    private void onStateChanged(final int state) {
        switch (state) {
            case BasePlayer.STATE_PAUSED:
                queueControlBinding.controlPlayPause
                        .setImageResource(R.drawable.ic_play_arrow_white_24dp);
                break;
            case BasePlayer.STATE_PLAYING:
                queueControlBinding.controlPlayPause
                        .setImageResource(R.drawable.ic_pause_white_24dp);
                break;
            case BasePlayer.STATE_COMPLETED:
                queueControlBinding.controlPlayPause
                        .setImageResource(R.drawable.ic_replay_white_24dp);
                break;
            default:
                break;
        }

        switch (state) {
            case BasePlayer.STATE_PAUSED:
            case BasePlayer.STATE_PLAYING:
            case BasePlayer.STATE_COMPLETED:
                queueControlBinding.controlPlayPause.setClickable(true);
                queueControlBinding.controlPlayPause.setVisibility(View.VISIBLE);
                queueControlBinding.controlProgressBar.setVisibility(View.GONE);
                break;
            default:
                queueControlBinding.controlPlayPause.setClickable(false);
                queueControlBinding.controlPlayPause.setVisibility(View.INVISIBLE);
                queueControlBinding.controlProgressBar.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void onPlayModeChanged(final int repeatMode, final boolean shuffled) {
        switch (repeatMode) {
            case Player.REPEAT_MODE_OFF:
                queueControlBinding.controlRepeat
                        .setImageResource(R.drawable.exo_controls_repeat_off);
                break;
            case Player.REPEAT_MODE_ONE:
                queueControlBinding.controlRepeat
                        .setImageResource(R.drawable.exo_controls_repeat_one);
                break;
            case Player.REPEAT_MODE_ALL:
                queueControlBinding.controlRepeat
                        .setImageResource(R.drawable.exo_controls_repeat_all);
                break;
        }

        final int shuffleAlpha = shuffled ? 255 : 77;
        queueControlBinding.controlShuffle.setImageAlpha(shuffleAlpha);
    }

    private void onPlaybackParameterChanged(final PlaybackParameters parameters) {
        if (parameters != null) {
            if (menu != null && player != null) {
                final MenuItem item = menu.findItem(R.id.action_playback_speed);
                item.setTitle(formatSpeed(parameters.speed));
            }
        }
    }

    private void onMaybePlaybackAdapterChanged() {
        if (player == null) {
            return;
        }
        final PlayQueueAdapter maybeNewAdapter = player.getPlayQueueAdapter();
        if (maybeNewAdapter != null
                && queueControlBinding.playQueue.getAdapter() != maybeNewAdapter) {
            queueControlBinding.playQueue.setAdapter(maybeNewAdapter);
        }
    }

    private void onMaybeMuteChanged() {
        if (menu != null && player != null) {
            final MenuItem item = menu.findItem(R.id.action_mute);

            //Change the mute-button item in ActionBar
            //1) Text change:
            item.setTitle(player.isMuted() ? R.string.unmute : R.string.mute);

            //2) Icon change accordingly to current App Theme
            // using rootView.getContext() because getApplicationContext() didn't work
            final Context context = queueControlBinding.getRoot().getContext();
            item.setIcon(ThemeHelper.resolveResourceIdFromAttr(context,
                    player.isMuted()
                            ? R.attr.ic_volume_off
                            : R.attr.ic_volume_up));
        }
    }
}
