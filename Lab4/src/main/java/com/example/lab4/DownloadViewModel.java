package com.example.lab4;

import androidx.lifecycle.ViewModel;

import java.io.File;

public class DownloadViewModel extends ViewModel {

    private long downloadId = -1;
    private File downloadedFile = null;
    private boolean isAudio = true;

    public void setDownloadId(long downloadId) {
        this.downloadId = downloadId;
    }

    public long getDownloadId() {
        return downloadId;
    }

    public void setDownloadedFile(File file) {
        this.downloadedFile = file;
    }

    public File getDownloadedFile() {
        return downloadedFile;
    }

    public void setIsAudio(boolean isAudio) {
        this.isAudio = isAudio;
    }

    public boolean isAudio() {
        return isAudio;
    }
}