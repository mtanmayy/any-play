package com.mtanmay.anyplay.ui.search;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.mtanmay.anyplay.R;
import com.mtanmay.anyplay.database.MDatabase;
import com.mtanmay.anyplay.song.Song;
import com.mtanmay.anyplay.song.SongViewModel;
import com.mtanmay.anyplay.ui.library.LibraryFragment;
import com.mtanmay.anyplay.utils.EncryptDecryptUtils;
import com.mtanmay.anyplay.utils.FileUtils;
import com.mtanmay.anyplay.utils.KeyUtils;
import com.mtanmay.anyplay.utils.NetworkUtils;
import com.mtanmay.anyplay.utils.ToastUtils;
import com.yausername.youtubedl_android.DownloadProgressCallback;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLResponse;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.bumptech.glide.Glide.with;
import static com.mtanmay.anyplay.Constants.DIR_AUDIO;
import static com.mtanmay.anyplay.Constants.DIR_CACHE;
import static com.mtanmay.anyplay.Constants.DIR_IMAGES;

public class SearchFragment extends Fragment {

    private static final String TAG = "SearchFragment";

    //view models
    private SongViewModel songViewModel;
    private SearchFragViewModel searchViewModel;

    // request response
    YoutubeDLResponse response = null;

    //views
    private EditText searchBar, etCName, etArtist;
    private Button btnDownload;
    private TextView ytName, artist, likeCount, dislikeCount, downloadProgressTv, downloadEta, startingDownload;
    private ImageView img, searchResultsLoadingImg;
    private ProgressBar downloadProgressBar;

    //layouts
    private CardView root_layout;
    private LinearLayout search_cname_grp, search_artist_grp;

    File downloadedAudioFile;
    private boolean audioFileSaved = false;
    private Song searchedSong;
    private byte[] secretKey;

    private WeakReference<Context> mContext;

    private final DownloadProgressCallback callback = new DownloadProgressCallback() {
        @Override
        public void onProgressUpdate(float progress, long etaInSeconds) {
            startingDownload.setVisibility(View.INVISIBLE);
            searchViewModel.setProgress((int) progress);
            searchViewModel.setEtaInSeconds(etaInSeconds);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = new WeakReference<>(getContext());

        // initialize view models
        initSongViewModel();
        initSearchViewModel();

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // initialize views
        initSearchViews(view);

        //observers
        initObservers();

        if(searchViewModel.isDownloading() != null && searchViewModel.isDownloading().getValue()) {
            setSearchViewsState(true);
            setDownloadViewsState(true);
            searchViewModel.setResultsLoaded(true);
//            btnDownload.setClickable(false);
        }
        else {
            setSearchViewsState(false);
            setDownloadViewsState(false);
            searchViewModel.setResultsLoaded(false);
        }

        btnDownload.setOnClickListener(download -> {

            if(!searchViewModel.getResultsLoaded().getValue()) {
                ToastUtils.create(mContext.get(), "Unable to download! Results not loaded yet");
                return;
            }

            try {
               if(NetworkUtils.isConnected(mContext.get())) {
                   executeDownloadQuery();
               }
               else {
                   AlertDialog.Builder dialog = new AlertDialog.Builder(mContext.get());
                   dialog.setTitle("No Internet Connection!")
                           .setMessage("You can play your downloaded songs.")
                           .setPositiveButton("OK", ((dialog1, which) -> {}))
                           .create();

                   dialog.show();
               }
            } catch (Exception e) {
                e.printStackTrace();
                ToastUtils.create(mContext.get(), "Unable to download file");
            }
        });


        searchBar.setOnKeyListener((v, keyCode, event) -> {
            if(event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                InputMethodManager manager = (InputMethodManager) mContext.get().getSystemService(Context.INPUT_METHOD_SERVICE);
                if(manager != null) {
                    manager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }

                searchViewModel.setEnteredUrl(searchBar.getText().toString().trim());

                if(TextUtils.isEmpty(searchViewModel.getEnteredUrl().getValue().trim())) {
                    searchBar.setError("Nothing to search");
                    return false;
                }

                if(NetworkUtils.isConnected(mContext.get())) {
                    executeSearchQuery(searchViewModel.getEnteredUrl().getValue());
                }
                else {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(mContext.get());
                    dialog.setTitle("No Internet Connection!")
                            .setMessage("You can play your downloaded songs.")
                            .setPositiveButton("OK", ((dialog1, which) -> {}))
                            .create();

                    dialog.show();
                }
            }
            return false;
        });

    }

    private void initObservers() {

        searchViewModel.getYtName().observe(getViewLifecycleOwner(), s -> ytName.setText(s));
        searchViewModel.getArtistName().observe(getViewLifecycleOwner(), s -> artist.setText(s));
        searchViewModel.getCname().observe(getViewLifecycleOwner(), s -> etCName.setText(s));
        searchViewModel.getCartist().observe(getViewLifecycleOwner(), s -> etArtist.setText(s));
        searchViewModel.getLikeCount().observe(getViewLifecycleOwner(), s -> likeCount.setText(s));
        searchViewModel.getDislikeCount().observe(getViewLifecycleOwner(), s -> dislikeCount.setText(s));
        searchViewModel.getImgUrl().observe(getViewLifecycleOwner(), s -> {

            if(searchViewModel.getImgUrl().getValue().equals("NA")) {
                with(getActivity())
                        .load(R.drawable.ic_placeholder_song)
                        .placeholder(R.drawable.loader)
                        .into(img);
            }
            else {
                with(getActivity())
                        .load(searchViewModel.getImgUrl().getValue())
                        .placeholder(R.drawable.loader)
                        .into(img);
            }
        });

        searchViewModel.getProgress().observe(getViewLifecycleOwner(), progress -> {
            downloadProgressBar.setProgress(progress);
            downloadProgressTv.setText(progress + "%");
        });

        searchViewModel.getEtaInSeconds().observe(getViewLifecycleOwner(), this::setEtaTv);
    }

    private void setEtaTv(long eta) {

        long days = eta/86400;
        long hours = eta / 3600;
        long mins = eta / 60;

        if(days <= 0)
            if(hours <= 0)
                if(mins <= 0)
                    if(eta > 0)
                        downloadEta.setText(eta + " seconds remaining");
                    else
                        downloadEta.setText("Download complete");
                else
                    downloadEta.setText(mins + " minute(s) remaining");
            else
                downloadEta.setText(hours + " hour(s) remaining");
        else
            downloadEta.setText(days + " day(s) remaining");

    }

    private void executeDownloadQuery() throws Exception {

        if(songAlreadyInList(searchViewModel.getSongId().getValue())) {
            ToastUtils.create(mContext.get(), "Downloaded");
            return;
        }

        if(searchViewModel.isDownloading().getValue()) {
//            ToastUtils.create(mContext.get(), "Please wait till the current download finishes");
            ToastUtils.create(mContext.get(), "Download in progress");
            return;
        }

        searchViewModel.setDownloading(true);
        startingDownload.setVisibility(View.VISIBLE);

        if(!TextUtils.isEmpty(etCName.getText().toString().trim()))
            searchViewModel.setCname(etCName.getText().toString().trim());
        else
            searchViewModel.setCname(ytName.getText().toString());

        if(!TextUtils.isEmpty(etArtist.getText().toString().trim()))
            searchViewModel.setCartist(etArtist.getText().toString().trim());
        else
            searchViewModel.setCartist(artist.getText().toString());

        YoutubeDLRequest request = new YoutubeDLRequest(searchViewModel.getEnteredUrl().getValue());

        downloadedAudioFile =  FileUtils.getFile(mContext.get(), DIR_AUDIO,
                searchViewModel.getSongId().getValue() + ".m4a");

        if(downloadedAudioFile == null) {
            return;
        }

        request.addOption("-o", downloadedAudioFile.getAbsolutePath());

        Disposable downloadDisposable = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, callback))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(youtubeDlResponse -> {

                    secretKey = KeyUtils.generateKey().getEncoded();

                    byte[] fileData = FileUtils.readFile(downloadedAudioFile);
                    byte[] audioData = EncryptDecryptUtils.encode(secretKey, fileData);

                    audioFileSaved = FileUtils.saveFile(mContext.get(), DIR_AUDIO, audioData, searchViewModel.getSongId().getValue());

                    Log.d(TAG, "executeDownloadQuery: AUDIO FILE SAVED: " + SearchFragment.this.audioFileSaved);

                    if(downloadedAudioFile.exists())
                        downloadedAudioFile.delete();

                    Glide.with(mContext.get())
                            .asBitmap()
                            .load(searchViewModel.getImgUrl().getValue())
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                                    File imgFile = FileUtils.getFile(mContext.get(), DIR_CACHE, "temp.png");
                                    try {
                                        OutputStream op = new FileOutputStream(imgFile);
                                        resource.compress(Bitmap.CompressFormat.PNG, 100, op);
                                        op.close();

                                        byte[] imgFileBytes = FileUtils.readFile(imgFile);
                                        byte[] imgData = EncryptDecryptUtils.encode(secretKey, imgFileBytes);
                                        boolean saved =FileUtils.saveFile(mContext.get(), DIR_IMAGES, imgData, searchViewModel.getSongId().getValue());
                                        if(!saved)
                                            ToastUtils.create(mContext.get(), "Unable to download image");

                                        if(LibraryFragment.adapter.get() != null)
                                            LibraryFragment.adapter.get().notifyDataSetChanged();

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {}
                            });

                    if(audioFileSaved) {

                        searchViewModel.setDownloading(false);
                        searchViewModel.setDownloadEta("Download complete");

                        downloadEta.setText(searchViewModel.getDownloadEta().getValue());

                        if(songViewModel == null) {
//                            Toast.makeText(mContext.get(), "ViewModel is null", Toast.LENGTH_SHORT).show();
                            initSongViewModel();
                        }

                        String strSecretKey = Base64.encodeToString(secretKey, Base64.NO_WRAP);
                        searchedSong = new Song(
                                searchViewModel.getSongId().getValue(),
                                searchViewModel.getYtName().getValue(),
                                searchViewModel.getCname().getValue(),
                                searchViewModel.getCartist().getValue(),
                                Integer.parseInt(searchViewModel.getLikeCount().getValue()),
                                Integer.parseInt(searchViewModel.getDislikeCount().getValue()),
                                searchViewModel.getImgUrl().getValue(),
                                strSecretKey,
                                System.currentTimeMillis());

                        searchViewModel.setSearchedSong(searchedSong);

                    }
                    else {
                        ToastUtils.create(mContext.get(), "Oops! Something went wrong. Unable to download");
                    }

                    if(audioFileSaved)
                        songViewModel.insertSong(searchViewModel.getSearchedSong().getValue());

                }, e -> {
                    e.printStackTrace();

                    searchViewModel.setDownloading(false);
                    ToastUtils.create(mContext.get(), "Oops! Something went wrong. Unable to download");
                });

    }

    private void initSearchViewModel() {

        searchViewModel = new ViewModelProvider(getActivity(),
                ViewModelProvider.AndroidViewModelFactory.getInstance(getActivity().getApplication()))
                .get(SearchFragViewModel.class);
    }

    private void initSongViewModel() {
        songViewModel = new ViewModelProvider(getActivity(),
                ViewModelProvider.AndroidViewModelFactory.getInstance(getActivity().getApplication()))
                .get(SongViewModel.class);
    }


    private void executeSearchQuery(String searchQuery) {

        YoutubeDLRequest request = new YoutubeDLRequest(searchQuery);
        request.addOption("--skip-download");
        request.addOption("--get-thumbnail");
        request.addOption("--get-filename");
        request.addOption("-o", "%(title)s\n%(uploader)s\n%(creator)s\n%(like_count)s\n%(dislike_count)s\n%(id)s");

        setDownloadViewsState(false);
        setSearchViewsState(false);
        searchResultsLoadingImg.setVisibility(View.VISIBLE);
        searchViewModel.setResultsLoaded(false);
        with(getActivity())
                .load(R.drawable.loader)
                .into(searchResultsLoadingImg);

        Disposable searchDisposable = Observable.fromCallable(() -> {
            response = YoutubeDL.getInstance().execute(request);
            return response;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(youtubeDLResponses -> {

                    searchViewModel.setResultsLoaded(true);
                    setSearchViewsState(true);
                    setDownloadViewsState(true);
                    searchResultsLoadingImg.setVisibility(View.GONE);

                    String[] data = response.getOut().split("\n");
                    setValues(data);

                }, e -> {
                    e.printStackTrace();
                    ToastUtils.create(mContext.get(), "Something went wrong!");
                    searchResultsLoadingImg.setVisibility(View.GONE);
                    searchViewModel.setResultsLoaded(true);
                });

    }

    private boolean songAlreadyInList(String songId) {

        List<Song> songsList;

        songsList = Single.fromCallable(() -> MDatabase.getInstance(getContext()).songDao().getSongs())
                .subscribeOn(Schedulers.io())
                .blockingGet();

        if(songsList != null) {
            Log.d(TAG, "songAlreadyInList: SONGS LIST NOT NULL");
            for(Song song : songsList) {
                if(song.getSongId().equals(songId))
                    return true;
            }
        }
        else
            Log.d(TAG, "songAlreadyInList: SONGS LIST IS NULL");

        return false;

    }

    private void setValues(@NotNull String[] data) {

        searchViewModel.setImgUrl(data[0]);
        searchViewModel.setYtName(data[1]);
        searchViewModel.setCname(data[1]);

        if (!data[3].equals("NA")) {
            searchViewModel.setArtistName(data[3]);
            searchViewModel.setCartist(data[3]);

        } else if (!data[2].equals("NA")) {
            searchViewModel.setArtistName(data[2]);
            searchViewModel.setCartist(data[2]);
    }
        else {
            searchViewModel.setArtistName("Unknown");
            searchViewModel.setCartist("Unknown");
        }

        searchViewModel.setLikeCount(data[4]);
        searchViewModel.setDislikeCount(data[5]);

        if (data[6] == null) {
            searchViewModel.setSongId("NA");
        } else
            searchViewModel.setSongId(data[6]);
    }

    //initialize views in the fragment
    private void initSearchViews(View view) {

        //views
        searchBar = view.findViewById(R.id.test_search_query);
        ytName = view.findViewById(R.id.search_name);
        artist = view.findViewById(R.id.search_artist);
        likeCount = view.findViewById(R.id.like_count);
        dislikeCount = view.findViewById(R.id.dislike_count);
        img = view.findViewById(R.id.search_img);
        btnDownload = view.findViewById(R.id.search_btn_download);
        downloadProgressBar = view.findViewById(R.id.search_progress_bar);
        downloadProgressTv = view.findViewById(R.id.search_tv_progress);
        downloadEta = view.findViewById(R.id.search_download_eta);
        etCName = view.findViewById(R.id.search_etCName);
        etArtist = view.findViewById(R.id.search_etArtist);
        searchResultsLoadingImg = view.findViewById(R.id.results_loading_placeholder);
        startingDownload = view.findViewById(R.id.starting_download_tv);
        //layouts
        root_layout = view.findViewById(R.id.search_results_root_layout);
        search_cname_grp = view.findViewById(R.id.search_cname_grp);
        search_artist_grp = view.findViewById(R.id.search_artist_grp);

    }
    
    private void setSearchViewsState(boolean enable) {

        if(enable) {
            root_layout.setVisibility(View.VISIBLE);
            search_cname_grp.setVisibility(View.VISIBLE);
            search_artist_grp.setVisibility(View.VISIBLE);
            btnDownload.setVisibility(View.VISIBLE);
            downloadProgressTv.setVisibility(View.VISIBLE);
            downloadProgressBar.setVisibility(View.VISIBLE);
            downloadEta.setVisibility(View.VISIBLE);
            etCName.setVisibility(View.VISIBLE);
            etArtist.setVisibility(View.VISIBLE);
        }
        else {
            root_layout.setVisibility(View.INVISIBLE);
            search_cname_grp.setVisibility(View.INVISIBLE);
            search_artist_grp.setVisibility(View.INVISIBLE);
            btnDownload.setVisibility(View.INVISIBLE);
            downloadProgressTv.setVisibility(View.INVISIBLE);
            downloadProgressTv.setText("");

            downloadProgressBar.setVisibility(View.INVISIBLE);
            downloadProgressBar.setProgress(0);

            downloadEta.setVisibility(View.INVISIBLE);
            downloadEta.setText("");

            etCName.setVisibility(View.INVISIBLE);
            etCName.setText("");

            etArtist.setVisibility(View.INVISIBLE);
            etArtist.setText("");
        }

    }

    private void setDownloadViewsState(boolean enable) {

        if(enable) {
            downloadProgressTv.setVisibility(View.VISIBLE);
            downloadProgressBar.setVisibility(View.VISIBLE);
            downloadEta.setVisibility(View.VISIBLE);
        }
        else {
            downloadProgressTv.setVisibility(View.INVISIBLE);
            downloadProgressBar.setVisibility(View.INVISIBLE);
            downloadEta.setVisibility(View.INVISIBLE);
        }

    }
}