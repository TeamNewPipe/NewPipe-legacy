package org.schabi.newpipelegacy.settings;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.schabi.newpipelegacy.NewPipeDatabase;
import org.schabi.newpipelegacy.R;
import org.schabi.newpipelegacy.database.AppDatabase;
import org.schabi.newpipelegacy.database.LocalItem;
import org.schabi.newpipelegacy.database.playlist.PlaylistLocalItem;
import org.schabi.newpipelegacy.database.playlist.PlaylistMetadataEntry;
import org.schabi.newpipelegacy.database.playlist.model.PlaylistRemoteEntity;
import org.schabi.newpipelegacy.local.playlist.LocalPlaylistManager;
import org.schabi.newpipelegacy.local.playlist.RemotePlaylistManager;
import org.schabi.newpipelegacy.report.ErrorActivity;
import org.schabi.newpipelegacy.report.ErrorInfo;
import org.schabi.newpipelegacy.report.UserAction;

import java.util.List;
import java.util.Vector;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;

public class SelectPlaylistFragment extends DialogFragment {
    /**
     * This contains the base display options for images.
     */
    private static final DisplayImageOptions DISPLAY_IMAGE_OPTIONS
            = new DisplayImageOptions.Builder().cacheInMemory(true).build();

    private final ImageLoader imageLoader = ImageLoader.getInstance();

    private OnSelectedListener onSelectedListener = null;

    private ProgressBar progressBar;
    private TextView emptyView;
    private RecyclerView recyclerView;
    private Disposable disposable = null;

    private List<PlaylistLocalItem> playlists = new Vector<>();

    public void setOnSelectedListener(final OnSelectedListener listener) {
        onSelectedListener = listener;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Fragment's Lifecycle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.select_playlist_fragment, container, false);
        progressBar = v.findViewById(R.id.progressBar);
        recyclerView = v.findViewById(R.id.items_list);
        emptyView = v.findViewById(R.id.empty_state_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        final SelectPlaylistAdapter playlistAdapter = new SelectPlaylistAdapter();
        recyclerView.setAdapter(playlistAdapter);

        loadPlaylists();
        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Load and display playlists
    //////////////////////////////////////////////////////////////////////////*/

    private void loadPlaylists() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        final AppDatabase database = NewPipeDatabase.getInstance(requireContext());
        final LocalPlaylistManager localPlaylistManager = new LocalPlaylistManager(database);
        final RemotePlaylistManager remotePlaylistManager = new RemotePlaylistManager(database);

        disposable = Flowable.combineLatest(localPlaylistManager.getPlaylists(),
                remotePlaylistManager.getPlaylists(), PlaylistLocalItem::merge)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::displayPlaylists, this::onError);
    }

    private void displayPlaylists(final List<PlaylistLocalItem> newPlaylists) {
        playlists = newPlaylists;
        progressBar.setVisibility(View.GONE);
        emptyView.setVisibility(newPlaylists.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(newPlaylists.isEmpty() ? View.GONE : View.VISIBLE);
    }

    protected void onError(final Throwable e) {
        final Activity activity = requireActivity();
        ErrorActivity.reportError(activity, e, activity.getClass(), null, ErrorInfo
                .make(UserAction.UI_ERROR, "none", "load_playlists", R.string.app_ui_crash));
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Handle actions
    //////////////////////////////////////////////////////////////////////////*/

    private void clickedItem(final int position) {
        if (onSelectedListener != null) {
            final LocalItem selectedItem = playlists.get(position);

            if (selectedItem instanceof PlaylistMetadataEntry) {
                final PlaylistMetadataEntry entry = ((PlaylistMetadataEntry) selectedItem);
                onSelectedListener.onLocalPlaylistSelected(entry.uid, entry.name);

            } else if (selectedItem instanceof PlaylistRemoteEntity) {
                final PlaylistRemoteEntity entry = ((PlaylistRemoteEntity) selectedItem);
                onSelectedListener.onRemotePlaylistSelected(
                        entry.getServiceId(), entry.getUrl(), entry.getName());
            }
        }
        dismiss();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Interfaces
    //////////////////////////////////////////////////////////////////////////*/

    public interface OnSelectedListener {
        void onLocalPlaylistSelected(long id, String name);
        void onRemotePlaylistSelected(int serviceId, String url, String name);
    }

    private class SelectPlaylistAdapter
            extends RecyclerView.Adapter<SelectPlaylistAdapter.SelectPlaylistItemHolder> {
        @NonNull
        @Override
        public SelectPlaylistItemHolder onCreateViewHolder(final ViewGroup parent,
                                                           final int viewType) {
            final View item = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_playlist_mini_item, parent, false);
            return new SelectPlaylistItemHolder(item);
        }

        @Override
        public void onBindViewHolder(@NonNull final SelectPlaylistItemHolder holder,
                                     final int position) {
            final PlaylistLocalItem selectedItem = playlists.get(position);

            if (selectedItem instanceof PlaylistMetadataEntry) {
                final PlaylistMetadataEntry entry = ((PlaylistMetadataEntry) selectedItem);

                holder.titleView.setText(entry.name);
                holder.view.setOnClickListener(view -> clickedItem(position));
                imageLoader.displayImage(entry.thumbnailUrl, holder.thumbnailView,
                        DISPLAY_IMAGE_OPTIONS);

            } else if (selectedItem instanceof PlaylistRemoteEntity) {
                final PlaylistRemoteEntity entry = ((PlaylistRemoteEntity) selectedItem);

                holder.titleView.setText(entry.getName());
                holder.view.setOnClickListener(view -> clickedItem(position));
                imageLoader.displayImage(entry.getThumbnailUrl(), holder.thumbnailView,
                        DISPLAY_IMAGE_OPTIONS);
            }
        }

        @Override
        public int getItemCount() {
            return playlists.size();
        }

        public class SelectPlaylistItemHolder extends RecyclerView.ViewHolder {
            public final View view;
            final ImageView thumbnailView;
            final TextView titleView;

            SelectPlaylistItemHolder(final View v) {
                super(v);
                this.view = v;
                thumbnailView = v.findViewById(R.id.itemThumbnailView);
                titleView = v.findViewById(R.id.itemTitleView);
            }
        }
    }
}
