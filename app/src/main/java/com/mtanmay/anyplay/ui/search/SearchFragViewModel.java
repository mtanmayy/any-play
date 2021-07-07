package com.mtanmay.anyplay.ui.search;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mtanmay.anyplay.song.Song;

import io.reactivex.disposables.CompositeDisposable;

public class SearchFragViewModel extends AndroidViewModel {

    private static final String TAG = "SearchFragViewModel";

    //variables
    private MutableLiveData<String> enteredUrl, songId, imgUrl, artistName, likeCount, dislikeCount, cname, cartist, downloadProgress, downloadEta, ytName;
    private MutableLiveData<Boolean> downloading, resultsLoaded;
    private MutableLiveData<Song> searchedSong;
    private MutableLiveData<CompositeDisposable> compositeDisposable;

    private MutableLiveData<Integer> progress;
    private MutableLiveData<String> progressText;
    private MutableLiveData<Long> etaInSeconds;
    private MutableLiveData<Boolean> downloadInProgress;


    public SearchFragViewModel(@NonNull Application application) {
        super(application);

        initMutableLiveData();
        setDefaultValues();

    }

    private void initMutableLiveData() {

        enteredUrl = new MutableLiveData<>();
        songId = new MutableLiveData<>();
        imgUrl = new MutableLiveData<>();
        searchedSong = new MutableLiveData<>();
        compositeDisposable = new MutableLiveData<>();
        ytName = new MutableLiveData<>();
        artistName = new MutableLiveData<>();
        likeCount = new MutableLiveData<>();
        dislikeCount = new MutableLiveData<>();
        cname = new MutableLiveData<>();
        cartist = new MutableLiveData<>();
        downloadProgress = new MutableLiveData<>();
        downloadEta = new MutableLiveData<>();
        resultsLoaded = new MutableLiveData<>();
        downloading = new MutableLiveData<>();

        progress = new MutableLiveData<>();
        progressText = new MutableLiveData<>();
        etaInSeconds = new MutableLiveData<>();
        downloadInProgress = new MutableLiveData<>();

    }

    private void setDefaultValues() {

        enteredUrl.setValue("");
        compositeDisposable.setValue(new CompositeDisposable());
        ytName.setValue("");
        artistName.setValue("");
        likeCount.setValue("NA");
        dislikeCount.setValue("NA");
        cname.setValue("");
        cartist.setValue("");
        downloadProgress.setValue("");
        downloadEta.setValue("");
        imgUrl.setValue("NA");
        songId.setValue(null);
        downloading.setValue(false);
        resultsLoaded.setValue(false);

        progress.setValue(0);
        etaInSeconds.setValue(0L);
        downloadInProgress.setValue(false);
        progressText.setValue("");

    }

    //----------------------------setters---------------------------------

    public void setDownloading(boolean downloading) {
        this.downloading.setValue(downloading);
    }

    public void setResultsLoaded(boolean newResult) {
        this.resultsLoaded.setValue(newResult);
    }

    public void setEnteredUrl(String enteredUrl) {
        this.enteredUrl.setValue(enteredUrl);
    }

    public void setSongId(String songId) {
        this.songId.setValue(songId);
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl.setValue(imgUrl);
    }

    public void setSearchedSong(Song searchedSong) {
        this.searchedSong.setValue(searchedSong);
    }

    public void setYtName(String ytName) {
        this.ytName.setValue(ytName);
    }

    public void setArtistName(String artistName) {
        this.artistName.setValue(artistName);
    }

    public void setLikeCount(String likeCount) {
        this.likeCount.setValue(likeCount);
    }

    public void setDislikeCount(String dislikeCount) {
        this.dislikeCount.setValue(dislikeCount);
    }

    public void setCname(String cname) {
        this.cname.setValue(cname);
    }

    public void setCartist(String cartist) {
        this.cartist.setValue(cartist);
    }

    public void setDownloadEta(String downloadEta) {
        this.downloadEta.setValue(downloadEta);
    }

    public void setProgress(Integer progress) {
        this.progress.postValue(progress);
    }

    public void setEtaInSeconds(Long etaInSeconds) {
        this.etaInSeconds.postValue(etaInSeconds);
    }


//------------------------------getters----------------------------------------

    public LiveData<Boolean> getResultsLoaded() {
        return this.resultsLoaded;
    }

    public LiveData<String> getEnteredUrl() {
        return enteredUrl;
    }

    public LiveData<String> getSongId() {
        return songId;
    }

    public LiveData<String> getImgUrl() {
        return imgUrl;
    }

    public LiveData<Song> getSearchedSong() {
        return searchedSong;
    }

    public LiveData<String> getYtName() {
        return ytName;
    }

    public LiveData<String> getArtistName() {
        return artistName;
    }

    public LiveData<String> getLikeCount() {
        return likeCount;
    }

    public LiveData<String> getDislikeCount() {
        return dislikeCount;
    }

    public LiveData<String> getCname() {
        return cname;
    }

    public LiveData<String> getCartist() {
        return cartist;
    }

    public LiveData<String> getDownloadEta() {
        return downloadEta;
    }

    public LiveData<Boolean> isDownloading() {
        return downloading;
    }

    public LiveData<Integer> getProgress() {
        return progress;
    }

    public LiveData<Long> getEtaInSeconds() {
        return etaInSeconds;
    }

}
