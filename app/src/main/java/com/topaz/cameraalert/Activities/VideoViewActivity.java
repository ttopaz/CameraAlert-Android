package com.topaz.cameraalert.Activities;

import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
//import android.media.MediaPlayer;
//import android.widget.MediaController;
//import android.widget.VideoView;
//import io.vov.vitamio.LibsChecker;
import com.topaz.cameraalert.R;

import java.util.HashMap;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.Vitamio;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

public class VideoViewActivity extends Activity implements MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener
{

    // Declare variables
    VideoView videoview;
    private ProgressBar pb;
    private TextView downloadRateView, loadRateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Vitamio.isInitialized(this);

        Bundle bundle = getIntent().getExtras();

        // Get the layout from video_main.xml
        setContentView(R.layout.video_view);
        // Find your VideoView in your video_main.xml layout
        videoview = (VideoView) findViewById(R.id.video);
        // Execute StreamVideo AsyncTask

        String videoURL = bundle.getString("url");

        pb = (ProgressBar) findViewById(R.id.probar);

        downloadRateView = (TextView) findViewById(R.id.download_rate);
        loadRateView = (TextView) findViewById(R.id.load_rate);

        videoview.setHardwareDecoder(true);

        try {
            // Start the MediaController
            MediaController mediacontroller = new MediaController(this);

            // Get the URL from String VideoURL
            Uri video = Uri.parse(videoURL);
            videoview.setMediaController(mediacontroller);
            HashMap<String, String> options = new HashMap<String, String>();
            options.put("rtsp_transport", "tcp");
            options.put("rtsp_flags", "prefer_tcp");// udp

            try
            {
                videoview.setVideoURI(video, options);
            }
            catch (Exception e1)
            {
                videoview.setHardwareDecoder(false);
                videoview.setVideoURI(video, options);
            }

        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }

        videoview.setOnInfoListener(this);
        videoview.setOnBufferingUpdateListener(this);
        videoview.requestFocus();

        videoview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                if (mediaPlayer != null)
                    mediaPlayer.setPlaybackSpeed(1.0f);
            }
        });
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                if (videoview.isPlaying()) {
                    videoview.pause();
                    pb.setVisibility(View.VISIBLE);
                    downloadRateView.setText("");
                    loadRateView.setText("");
                    downloadRateView.setVisibility(View.VISIBLE);
                    loadRateView.setVisibility(View.VISIBLE);
                }
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                videoview.start();
                pb.setVisibility(View.GONE);
                downloadRateView.setVisibility(View.GONE);
                loadRateView.setVisibility(View.GONE);
                break;
            case MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:
                downloadRateView.setText("" + extra + "kb/s" + "  ");
                break;
        }
        return true;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        loadRateView.setText(percent + "%");
    }
}
