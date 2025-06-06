package com.example.lab4;

import android.net.Uri;

import androidx.lifecycle.ViewModel;

public class MediaPlayerViewModel extends ViewModel {

    private Uri selectedFileUri;
    private boolean isAudioFile = true;

    public void setSelectedFile(Uri uri, boolean isAudio) {
        this.selectedFileUri = uri;
        this.isAudioFile = isAudio;
    }

    public Uri getSelectedFileUri() {
        return selectedFileUri;
    }

    public boolean isAudioFile() {
        return isAudioFile;
    }
}