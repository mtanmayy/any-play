package com.mtanmay.anyplay.ui.library;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.mtanmay.anyplay.MusicPlayer;
import com.mtanmay.anyplay.R;
import com.mtanmay.anyplay.adapters.SongAdapter;
import com.mtanmay.anyplay.database.MDatabase;
import com.mtanmay.anyplay.main.MainActivity;
import com.mtanmay.anyplay.services.MusicPlayerService;
import com.mtanmay.anyplay.song.Song;
import com.mtanmay.anyplay.song.SongViewModel;
import com.mtanmay.anyplay.utils.FileUtils;
import com.mtanmay.anyplay.utils.NotificationUtils;


import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

import static com.mtanmay.anyplay.Constants.DIR_AUDIO;
import static com.mtanmay.anyplay.Constants.DIR_IMAGES;

public class LibraryFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    // view models
    private SongViewModel songViewModel;
    private LibraryFragViewModel playMusicViewModel;

    private boolean delete = true;
    private boolean showBottomBar = true;

    private WeakReference<Context> mContext;
    public static WeakReference<SongAdapter> adapter;

    private EditText filter;
    private TextView noSongsTv;
    private ImageView noSongsDownloadedImg;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = new WeakReference<>(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // inflate the layout
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        noSongsTv = view.findViewById(R.id.no_songs_downloaded_tv);
        noSongsDownloadedImg = view.findViewById(R.id.no_song_downloaded_ic);

        RecyclerView songsList = view.findViewById(R.id.songs_recycler_view);
        songsList.setLayoutManager(new LinearLayoutManager(getContext()));
        songsList.setHasFixedSize(true);

        adapter = new WeakReference<>(new SongAdapter(getContext()));
        songsList.setAdapter(adapter.get());

        // getting the view model
        songViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getActivity().getApplication()))
                .get(SongViewModel.class);

        playMusicViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getActivity().getApplication()))
                .get(LibraryFragViewModel.class);

        // observing the songs list
        songViewModel.getAllSongs().observe(getViewLifecycleOwner(), new Observer<List<Song>>() {
            @Override
            public void onChanged(List<Song> songs) {
                adapter.get().submitList(songs);

                if(songs.size() <= 0) {
                    songsList.setVisibility(View.INVISIBLE);
                    filter.setVisibility(View.INVISIBLE);
                    noSongsTv.setVisibility(View.VISIBLE);
                    noSongsDownloadedImg.setVisibility(View.VISIBLE);
                }
                else {
                    songsList.setVisibility(View.VISIBLE);
                    filter.setVisibility(View.VISIBLE);
                    noSongsTv.setVisibility(View.INVISIBLE);
                    noSongsDownloadedImg.setVisibility(View.INVISIBLE);
                }

                Log.d(TAG, "onChanged: NEW LIST SUBMITTED | " + songs.size());
            }
        });

        filter = view.findViewById(R.id.filter);
        filter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                List<Song> list;
                String query = "%" + s + "%";

                if(s == null || s.length() == 0 || s.equals("")) {
                    list = Single.fromCallable(() -> MDatabase.getInstance(mContext.get()).songDao().getSongs())
                            .subscribeOn(Schedulers.io())
                            .blockingGet();;
                }
                else {
                    list = Single.fromCallable(() -> MDatabase.getInstance(mContext.get()).songDao().searchSongs(query))
                            .subscribeOn(Schedulers.io())
                            .blockingGet();
                }

                adapter.get().submitList(list);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // song search filter
        filter.setOnKeyListener((v, keyCode, event) -> {
            if(event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                filter.setText("");
                InputMethodManager manager = (InputMethodManager) mContext.get().getSystemService(Context.INPUT_METHOD_SERVICE);
                if(manager != null)
                    manager.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            return false;
        });

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                int songPos = viewHolder.getAdapterPosition();

                if(MusicPlayerService.songIdLData != null) {
                    if(MusicPlayerService.songIdLData.getValue().equals(adapter.get().getSongAt(songPos).getSongId())) {
                        showBottomBar = false;
                        MusicPlayer.getInstance(mContext.get()).reset();
                        NotificationUtils.cancelAll(mContext.get());
                    }
                }

                Song deletedSong = adapter.get().getSongAt(songPos);
                songViewModel.deleteSong(deletedSong);
                MainActivity.weakBottomBar.get().setVisibility(View.GONE);

                Snackbar.make(songsList, "Song deleted", Snackbar.LENGTH_SHORT)
                        .setAction("UNDO", v -> {
                            delete = false;
                            Log.d(TAG, "onSwiped: set delete to:" + delete);
                            if(songViewModel == null) {
                                new ViewModelProvider(LibraryFragment.this,
                                        ViewModelProvider.AndroidViewModelFactory.getInstance(getActivity().getApplication()))
                                        .get(SongViewModel.class);
                            }
                            songViewModel.insertSong(deletedSong);
                        })
                        .addCallback(new Snackbar.Callback() {

                            @Override
                            public void onDismissed(Snackbar transientBottomBar, int event) {

                                if(delete) {
                                    File songToDelete = FileUtils.getFile(mContext.get(), DIR_AUDIO, deletedSong.getSongId());
                                    File imgToDelete = FileUtils.getFile(mContext.get(), DIR_IMAGES, deletedSong.getSongId());

                                    if (songToDelete != null && songToDelete.exists()) {
                                        songToDelete.delete();
                                    }

                                    if(imgToDelete != null && imgToDelete.exists())
                                        imgToDelete.delete();

                                }

                                delete = true;
                                if(showBottomBar && MusicPlayerService.isPlaying != null)
                                    if(MusicPlayerService.isPlaying.getValue())
                                        MainActivity.weakBottomBar.get().setVisibility(View.VISIBLE);
                            }
                        }).show();

            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addSwipeLeftActionIcon(R.drawable.ic_delete)
                        .addSwipeLeftBackgroundColor(ContextCompat.getColor(getContext(), R.color.red))
                        .create()
                        .decorate();

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }).attachToRecyclerView(songsList);

        // executed when a song is clicked on from the list
        adapter.get().setOnItemClickListener(song -> {
            playMusicViewModel.playSong(song);
        });

    }

}