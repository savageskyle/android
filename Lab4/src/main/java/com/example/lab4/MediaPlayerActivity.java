package com.example.lab4;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

public class MediaPlayerActivity extends AppCompatActivity {
    private static final String TAG = "MediaPlayerActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private VideoView videoView;
    private FrameLayout videoContainer;
    private ImageView imgAudioPlaceholder;
    private TextView tvFileName;
    private Button btnSelectAudio, btnSelectVideo;
    private Button btnPlay, btnPause, btnStop;
    private SeekBar seekBar;
    private LinearLayout mediaControls;

    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable runnable;

    private boolean isAudioFile = true;
    private Uri selectedFileUri = null;
    private float videoAspectRatio = 16f/9f; // Default aspect ratio

    private MediaPlayerViewModel viewModel;

    private ActivityResultLauncher<String> selectAudioLauncher;
    private ActivityResultLauncher<String> selectVideoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);

        viewModel = new ViewModelProvider(this).get(MediaPlayerViewModel.class);

        initializeViews();
        setupButtonListeners();
        setupMediaControlListeners();
        setupFilePickers();

        // Check if we were launched with data (from DownloadActivity)
        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            selectedFileUri = intent.getData();
            isAudioFile = intent.getBooleanExtra("isAudio", true);

            Log.d(TAG, "Received URI: " + selectedFileUri);
            Log.d(TAG, "isAudio: " + isAudioFile);

            viewModel.setSelectedFile(selectedFileUri, isAudioFile);

            if (isAudioFile) {
                prepareAudioFile(selectedFileUri);
            } else {
                prepareVideoFile(selectedFileUri);
            }
        } else if (savedInstanceState != null) {
            restoreState();
        }

        checkAndRequestPermissions();
    }

    private void initializeViews() {
        videoView = findViewById(R.id.videoView);
        videoContainer = findViewById(R.id.videoContainer);
        imgAudioPlaceholder = findViewById(R.id.imgAudioPlaceholder);
        tvFileName = findViewById(R.id.tvFileName);
        btnSelectAudio = findViewById(R.id.btnSelectAudio);
        btnSelectVideo = findViewById(R.id.btnSelectVideo);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        seekBar = findViewById(R.id.seekBar);
        mediaControls = findViewById(R.id.mediaControls);

        handler = new Handler();
    }

    private void setupButtonListeners() {
        btnSelectAudio.setOnClickListener(v -> {
            isAudioFile = true;
            selectAudioLauncher.launch("audio/*");
        });

        btnSelectVideo.setOnClickListener(v -> {
            isAudioFile = false;
            selectVideoLauncher.launch("video/*");
        });
    }

    private void setupMediaControlListeners() {
        btnPlay.setOnClickListener(v -> playMedia());
        btnPause.setOnClickListener(v -> pauseMedia());
        btnStop.setOnClickListener(v -> stopMedia());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (isAudioFile && mediaPlayer != null) {
                        mediaPlayer.seekTo(progress);
                    } else if (!isAudioFile) {
                        videoView.seekTo(progress);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                pauseMedia();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                playMedia();
            }
        });
    }

    private void setupFilePickers() {
        selectAudioLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedFileUri = uri;
                        viewModel.setSelectedFile(uri, true);
                        prepareAudioFile(uri);
                    }
                });

        selectVideoLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedFileUri = uri;
                        viewModel.setSelectedFile(uri, false);
                        prepareVideoFile(uri);
                    }
                });
    }

    private void prepareAudioFile(Uri uri) {
        isAudioFile = true;
        updateFileName(uri);

        releaseMediaPlayer();

        imgAudioPlaceholder.setVisibility(View.VISIBLE);
        videoContainer.setVisibility(View.GONE);

        mediaPlayer = new MediaPlayer();
        try {
            Log.d(TAG, "Preparing audio file: " + uri);
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.setOnPreparedListener(mp -> {
                seekBar.setMax(mp.getDuration());
                enableControls(true);
                Log.d(TAG, "Audio prepared successfully, duration: " + mp.getDuration() + "ms");
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                btnPlay.setEnabled(true);
                btnPause.setEnabled(false);
                seekBar.setProgress(0);
                Log.d(TAG, "Audio playback completed");
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
                Toast.makeText(MediaPlayerActivity.this,
                        "Error playing audio: " + what,
                        Toast.LENGTH_SHORT).show();
                return false;
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "Error setting audio source: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            enableControls(false);
        }
    }

    private void prepareVideoFile(Uri uri) {
        isAudioFile = false;
        updateFileName(uri);

        releaseMediaPlayer();

        imgAudioPlaceholder.setVisibility(View.GONE);
        videoContainer.setVisibility(View.VISIBLE);

        // Extract video aspect ratio before playing
        extractVideoAspectRatio(uri);

        try {
            Log.d(TAG, "Preparing video file: " + uri);
            videoView.setVideoURI(uri);
            videoView.setOnPreparedListener(mp -> {
                seekBar.setMax(mp.getDuration());
                enableControls(true);

                // Set video size based on device orientation
                adjustVideoSize();

                Log.d(TAG, "Video prepared successfully, duration: " + mp.getDuration() + "ms");
            });

            videoView.setOnCompletionListener(mp -> {
                btnPlay.setEnabled(true);
                btnPause.setEnabled(false);
                seekBar.setProgress(0);
                Log.d(TAG, "Video playback completed");
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "VideoView error: what=" + what + ", extra=" + extra);
                Toast.makeText(MediaPlayerActivity.this,
                        "Error playing video: " + what,
                        Toast.LENGTH_SHORT).show();
                return false;
            });

            videoView.requestFocus();
        } catch (Exception e) {
            Log.e(TAG, "Error setting video source: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            enableControls(false);
        }
    }

    private void extractVideoAspectRatio(Uri uri) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(this, uri);

            String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);

            if (width != null && height != null) {
                int videoWidth = Integer.parseInt(width);
                int videoHeight = Integer.parseInt(height);

                if (videoHeight > 0) {
                    videoAspectRatio = (float) videoWidth / videoHeight;
                    Log.d(TAG, "Video aspect ratio: " + videoAspectRatio + " (" + videoWidth + "x" + videoHeight + ")");
                }
            }

            retriever.release();
        } catch (Exception e) {
            Log.e(TAG, "Error extracting video aspect ratio: " + e.getMessage(), e);
            // Fall back to default 16:9 aspect ratio
            videoAspectRatio = 16f/9f;
        }
    }

    private void adjustVideoSize() {
        int orientation = getResources().getConfiguration().orientation;

        // Get screen dimensions
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        // Calculate available height for video (exclude action bar, controls, etc.)
        int availableHeight = screenHeight - mediaControls.getHeight() -
                tvFileName.getHeight() - btnSelectAudio.getHeight() -
                seekBar.getHeight() - 100; // Extra padding

        ViewGroup.LayoutParams containerParams = videoContainer.getLayoutParams();

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Portrait mode
            if (videoAspectRatio >= 1.0) {
                // Landscape video (like 16:9) in portrait mode
                // Set width to match screen, height based on aspect ratio
                containerParams.width = screenWidth;
                containerParams.height = (int) (screenWidth / videoAspectRatio);

                // Make sure it doesn't exceed available height
                if (containerParams.height > availableHeight) {
                    containerParams.height = availableHeight;
                    containerParams.width = (int) (availableHeight * videoAspectRatio);
                }
            } else {
                // Portrait video (like 9:16) in portrait mode
                // Set height to fill available space, width based on aspect ratio
                containerParams.height = availableHeight;
                containerParams.width = (int) (availableHeight * videoAspectRatio);
            }
        } else {
            // Landscape mode
            if (videoAspectRatio >= 1.0) {
                // Landscape video (like 16:9) in landscape mode
                // Fill the screen width and adjust height by aspect ratio
                containerParams.width = screenWidth;
                containerParams.height = (int) (screenWidth / videoAspectRatio);
            } else {
                // Portrait video (like 9:16) in landscape mode
                // Set height to fill available space, width based on aspect ratio
                containerParams.height = availableHeight;
                containerParams.width = (int) (availableHeight * videoAspectRatio);

                // Center it if it's smaller than screen width
                if (containerParams.width < screenWidth) {
                    ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) videoContainer.getLayoutParams();
                    marginParams.leftMargin = (screenWidth - containerParams.width) / 2;
                    marginParams.rightMargin = (screenWidth - containerParams.width) / 2;
                }
            }
        }

        videoContainer.setLayoutParams(containerParams);

        // Set video size to fill its container
        ViewGroup.LayoutParams videoParams = videoView.getLayoutParams();
        videoParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        videoParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        videoView.setLayoutParams(videoParams);
    }

    private void updateFileName(Uri uri) {
        String fileName = FileUtils.getFileName(this, uri);
        tvFileName.setText(getString(R.string.file_selected, fileName));
    }

    private void playMedia() {
        if (selectedFileUri == null) {
            Toast.makeText(this, R.string.please_select_file, Toast.LENGTH_SHORT).show();
            return;
        }

        if (isAudioFile && mediaPlayer != null) {
            mediaPlayer.start();
            updateSeekBar();
            Log.d(TAG, "Started audio playback");
        } else if (!isAudioFile) {
            videoView.start();
            updateSeekBar();
            Log.d(TAG, "Started video playback");
        }

        btnPlay.setEnabled(false);
        btnPause.setEnabled(true);
        btnStop.setEnabled(true);
    }

    private void pauseMedia() {
        if (isAudioFile && mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            Log.d(TAG, "Paused audio playback");
        } else if (!isAudioFile && videoView.isPlaying()) {
            videoView.pause();
            Log.d(TAG, "Paused video playback");
        }

        btnPlay.setEnabled(true);
        btnPause.setEnabled(false);
    }

    private void stopMedia() {
        if (isAudioFile && mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            try {
                Log.d(TAG, "Resetting audio playback");
                mediaPlayer.setDataSource(this, selectedFileUri);
                mediaPlayer.prepareAsync();
            } catch (Exception e) {
                Log.e(TAG, "Error resetting audio: " + e.getMessage(), e);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else if (!isAudioFile) {
            videoView.stopPlayback();
            videoView.setVideoURI(selectedFileUri);
            videoView.requestFocus();
            Log.d(TAG, "Resetting video playback");
        }

        seekBar.setProgress(0);
        btnPlay.setEnabled(true);
        btnPause.setEnabled(false);
        btnStop.setEnabled(false);
    }

    private void updateSeekBar() {
        if (isAudioFile && mediaPlayer != null) {
            seekBar.setProgress(mediaPlayer.getCurrentPosition());
        } else if (!isAudioFile) {
            seekBar.setProgress(videoView.getCurrentPosition());
        }

        if ((isAudioFile && mediaPlayer != null && mediaPlayer.isPlaying()) ||
                (!isAudioFile && videoView.isPlaying())) {
            runnable = this::updateSeekBar;
            handler.postDelayed(runnable, 100);
        }
    }

    private void enableControls(boolean enable) {
        btnPlay.setEnabled(enable);
        btnPause.setEnabled(false);
        btnStop.setEnabled(false);
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO},
                        PERMISSION_REQUEST_CODE);
            }
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(this, "Storage permissions are required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Adjust video size when orientation changes
        if (!isAudioFile && selectedFileUri != null) {
            adjustVideoSize();
        }
    }

    private void restoreState() {
        selectedFileUri = viewModel.getSelectedFileUri();
        isAudioFile = viewModel.isAudioFile();

        Log.d(TAG, "Restoring state: uri=" + selectedFileUri + ", isAudio=" + isAudioFile);

        if (selectedFileUri != null) {
            if (isAudioFile) {
                prepareAudioFile(selectedFileUri);
            } else {
                prepareVideoFile(selectedFileUri);
            }
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            Log.d(TAG, "Released MediaPlayer");
        }

        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseMedia();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
    }
}