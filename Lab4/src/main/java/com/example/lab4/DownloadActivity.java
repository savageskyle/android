package com.example.lab4;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;

public class DownloadActivity extends AppCompatActivity {
    private static final String TAG = "DownloadActivity";

    private EditText etDownloadUrl;
    private RadioButton rbAudio, rbVideo;
    private Button btnDownload, btnPlayDownloaded;
    private ProgressBar progressBar;
    private TextView tvDownloadStatus;

    private DownloadManager downloadManager;
    private long downloadId = -1;
    private File downloadedFile = null;
    private boolean isDownloading = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressChecker;

    private DownloadViewModel viewModel;

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Download broadcast received");
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id == downloadId) {
                Log.d(TAG, "Download ID matched: " + id);
                checkDownloadStatus();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        viewModel = new ViewModelProvider(this).get(DownloadViewModel.class);

        initializeViews();
        setupButtonListeners();

        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        // Register receiver with proper flag
        try {
            IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(downloadReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(downloadReceiver, filter);
            }
            Log.d(TAG, "Broadcast receiver registered");
        } catch (Exception e) {
            Log.e(TAG, "Error registering receiver: " + e.getMessage());
        }

        if (savedInstanceState != null) {
            restoreState();
        }
    }

    private void initializeViews() {
        etDownloadUrl = findViewById(R.id.etDownloadUrl);
        rbAudio = findViewById(R.id.rbAudio);
        rbVideo = findViewById(R.id.rbVideo);
        btnDownload = findViewById(R.id.btnDownload);
        btnPlayDownloaded = findViewById(R.id.btnPlayDownloaded);
        progressBar = findViewById(R.id.progressBar);
        tvDownloadStatus = findViewById(R.id.tvDownloadStatus);

        // Set progress bar to be visible
        progressBar.setVisibility(View.INVISIBLE);
        progressBar.setMax(100);
    }

    private void setupButtonListeners() {
        btnDownload.setOnClickListener(v -> startDownload());

        btnPlayDownloaded.setOnClickListener(v -> {
            if (downloadedFile != null && downloadedFile.exists()) {
                Log.d(TAG, "Opening file: " + downloadedFile.getAbsolutePath());

                // Use FileProvider to create content URI
                Uri fileUri = FileProvider.getUriForFile(
                        this,
                        "com.example.lab4.fileprovider",
                        downloadedFile
                );

                Intent intent = new Intent(DownloadActivity.this, MediaPlayerActivity.class);
                // Send the file URI, not the file itself
                intent.setData(fileUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // Also pass if it's audio or video as an extra
                intent.putExtra("isAudio", rbAudio.isChecked());

                Log.d(TAG, "Starting MediaPlayerActivity with URI: " + fileUri);
                startActivity(intent);
            } else {
                String message = "File not found";
                if (downloadedFile != null) {
                    message += ": " + downloadedFile.getAbsolutePath();
                }
                Log.e(TAG, message);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startDownload() {
        String url = etDownloadUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, R.string.please_enter_url, Toast.LENGTH_SHORT).show();
            return;
        }

        // Make sure URL starts with http:// or https://
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
            etDownloadUrl.setText(url);
        }

        // Create download directory in public Downloads folder for easier access
        File downloadDir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Android 10+ use app-specific directory
            downloadDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Lab4");
        } else {
            // On older versions use public Downloads directory
            downloadDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "Lab4");
        }

        if (!downloadDir.exists() && !downloadDir.mkdirs()) {
            Toast.makeText(this, R.string.failed_create_dir, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Download directory: " + downloadDir.getAbsolutePath());

        String fileName;
        String mimeType;

        if (rbAudio.isChecked()) {
            fileName = "audio_" + System.currentTimeMillis() + ".mp3";
            mimeType = "audio/mp3";
        } else {
            fileName = "video_" + System.currentTimeMillis() + ".mp4";
            mimeType = "video/mp4";
        }

        downloadedFile = new File(downloadDir, fileName);
        Log.d(TAG, "Download file path: " + downloadedFile.getAbsolutePath());

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                    .setTitle(fileName)
                    .setDescription(getString(R.string.downloading_media))
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            // Set destination based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use app-specific storage on Android 10+
                request.setDestinationUri(Uri.fromFile(downloadedFile));
            } else {
                // Use public Downloads directory on older Android versions
                request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS, "Lab4/" + fileName);
            }

            // Set mime type only if we have one
            request.setMimeType(mimeType);

            downloadId = downloadManager.enqueue(request);
            Log.d(TAG, "Download started with ID: " + downloadId);

            viewModel.setDownloadId(downloadId);
            viewModel.setDownloadedFile(downloadedFile);
            viewModel.setIsAudio(rbAudio.isChecked());

            isDownloading = true;
            btnDownload.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
            tvDownloadStatus.setText(R.string.downloading);

            // Start checking progress immediately
            startDownloadStatusCheck();

        } catch (Exception e) {
            Log.e(TAG, "Error starting download: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            isDownloading = false;
            btnDownload.setEnabled(true);
        }
    }

    private void startDownloadStatusCheck() {
        // Cancel any existing runnable
        if (progressChecker != null) {
            handler.removeCallbacks(progressChecker);
        }

        progressChecker = new Runnable() {
            @Override
            public void run() {
                if (!isDownloading) {
                    return;
                }

                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);

                try (Cursor cursor = downloadManager.query(query)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        if (statusIndex != -1) {
                            int status = cursor.getInt(statusIndex);

                            switch (status) {
                                case DownloadManager.STATUS_RUNNING:
                                    int bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                                    int bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);

                                    if (bytesDownloadedIndex != -1 && bytesTotalIndex != -1) {
                                        long bytesDownloaded = cursor.getLong(bytesDownloadedIndex);
                                        long bytesTotal = cursor.getLong(bytesTotalIndex);

                                        if (bytesTotal > 0) {
                                            int progress = (int) (bytesDownloaded * 100 / bytesTotal);
                                            progressBar.setProgress(progress);
                                            tvDownloadStatus.setText(getString(R.string.downloading_percent, progress));
                                            Log.d(TAG, "Download progress: " + progress + "%");
                                        }
                                    }
                                    break;

                                case DownloadManager.STATUS_SUCCESSFUL:
                                    isDownloading = false;
                                    progressBar.setProgress(100);
                                    tvDownloadStatus.setText(R.string.file_downloaded);

                                    // Check if file actually exists
                                    if (downloadedFile != null && downloadedFile.exists()) {
                                        Log.d(TAG, "File downloaded successfully: " + downloadedFile.getAbsolutePath() +
                                                " Size: " + downloadedFile.length() + " bytes");
                                        btnPlayDownloaded.setEnabled(true);
                                    } else {
                                        // Try to get the file URI from DownloadManager
                                        int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                                        if (uriIndex != -1) {
                                            String uriString = cursor.getString(uriIndex);
                                            if (uriString != null) {
                                                Uri fileUri = Uri.parse(uriString);
                                                String filePath = fileUri.getPath();
                                                if (filePath != null) {
                                                    downloadedFile = new File(filePath.replace("file://", ""));
                                                    Log.d(TAG, "Retrieved file path from DownloadManager: " + downloadedFile.getAbsolutePath());
                                                    if (downloadedFile.exists()) {
                                                        Log.d(TAG, "File exists at retrieved path. Size: " + downloadedFile.length() + " bytes");
                                                        btnPlayDownloaded.setEnabled(true);
                                                    } else {
                                                        Log.e(TAG, "File doesn't exist at retrieved path");
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    btnDownload.setEnabled(true);
                                    return; // Exit the runnable

                                case DownloadManager.STATUS_FAILED:
                                    isDownloading = false;
                                    tvDownloadStatus.setText(R.string.download_failed);
                                    btnDownload.setEnabled(true);

                                    // Get the reason for failure
                                    int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                                    if (reasonIndex != -1) {
                                        int reason = cursor.getInt(reasonIndex);
                                        Log.e(TAG, "Download failed with reason: " + reason);
                                    }
                                    return; // Exit the runnable

                                case DownloadManager.STATUS_PAUSED:
                                    tvDownloadStatus.setText(R.string.download_paused);

                                    // Get the reason for pausing
                                    int reasonPauseIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                                    if (reasonPauseIndex != -1) {
                                        int reason = cursor.getInt(reasonPauseIndex);
                                        Log.d(TAG, "Download paused with reason: " + reason);
                                    }
                                    break;

                                case DownloadManager.STATUS_PENDING:
                                    tvDownloadStatus.setText(R.string.download_pending);
                                    Log.d(TAG, "Download pending");
                                    break;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error checking download status: " + e.getMessage());
                }

                // Schedule the next check
                handler.postDelayed(this, 500);
            }
        };

        // Start the progress checker
        handler.post(progressChecker);
    }

    private void checkDownloadStatus() {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);

        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor != null && cursor.moveToFirst()) {
                int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                if (statusIndex != -1) {
                    int status = cursor.getInt(statusIndex);
                    Log.d(TAG, "Download status from broadcast: " + status);

                    switch (status) {
                        case DownloadManager.STATUS_SUCCESSFUL:
                            isDownloading = false;
                            progressBar.setProgress(100);
                            tvDownloadStatus.setText(R.string.file_downloaded);

                            // Check if file exists and set up Play button
                            if (downloadedFile != null && downloadedFile.exists()) {
                                Log.d(TAG, "File exists after download: " + downloadedFile.getAbsolutePath() +
                                        " Size: " + downloadedFile.length() + " bytes");
                                btnPlayDownloaded.setEnabled(true);
                            } else {
                                // Try to get file from DownloadManager
                                int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                                if (uriIndex != -1) {
                                    String uriString = cursor.getString(uriIndex);
                                    if (uriString != null) {
                                        Uri fileUri = Uri.parse(uriString);
                                        String filePath = fileUri.getPath();
                                        if (filePath != null) {
                                            downloadedFile = new File(filePath.replace("file://", ""));
                                            Log.d(TAG, "Retrieved file path from broadcast: " + downloadedFile.getAbsolutePath());
                                            if (downloadedFile.exists()) {
                                                Log.d(TAG, "File exists at retrieved path. Size: " + downloadedFile.length() + " bytes");
                                                btnPlayDownloaded.setEnabled(true);
                                            } else {
                                                Log.e(TAG, "File doesn't exist at retrieved path");
                                            }
                                        }
                                    }
                                }
                            }

                            btnDownload.setEnabled(true);

                            // Stop the progress checker
                            if (progressChecker != null) {
                                handler.removeCallbacks(progressChecker);
                            }
                            break;

                        case DownloadManager.STATUS_FAILED:
                            isDownloading = false;
                            tvDownloadStatus.setText(R.string.download_failed);
                            btnDownload.setEnabled(true);

                            // Get the reason for failure
                            int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                            if (reasonIndex != -1) {
                                int reason = cursor.getInt(reasonIndex);
                                Log.e(TAG, "Download failed with reason: " + reason);
                            }

                            // Stop the progress checker
                            if (progressChecker != null) {
                                handler.removeCallbacks(progressChecker);
                            }
                            break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking download status from broadcast: " + e.getMessage());
        }
    }

    private void restoreState() {
        downloadId = viewModel.getDownloadId();
        downloadedFile = viewModel.getDownloadedFile();
        boolean isAudio = viewModel.isAudio();

        Log.d(TAG, "Restoring state: downloadId=" + downloadId +
                ", file=" + (downloadedFile != null ? downloadedFile.getAbsolutePath() : "null") +
                ", isAudio=" + isAudio);

        if (isAudio) {
            rbAudio.setChecked(true);
        } else {
            rbVideo.setChecked(true);
        }

        if (downloadId != -1) {
            checkDownloadStatus();

            if (downloadedFile != null && downloadedFile.exists()) {
                Log.d(TAG, "Found existing file: " + downloadedFile.getAbsolutePath() +
                        " Size: " + downloadedFile.length() + " bytes");
                btnPlayDownloaded.setEnabled(true);
            } else {
                Log.d(TAG, "File not found during restore");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Make sure we don't lose the download ID
        if (downloadId != -1) {
            viewModel.setDownloadId(downloadId);
        }
        if (downloadedFile != null) {
            viewModel.setDownloadedFile(downloadedFile);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDownloading = false;

        // Remove callbacks to prevent memory leaks
        if (progressChecker != null) {
            handler.removeCallbacks(progressChecker);
        }

        try {
            unregisterReceiver(downloadReceiver);
            Log.d(TAG, "Broadcast receiver unregistered");
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver: " + e.getMessage());
        }
    }
}