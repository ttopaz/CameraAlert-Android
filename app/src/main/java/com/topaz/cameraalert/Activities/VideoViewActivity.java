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
//        if (!LibsChecker.checkVitamioLibs(this))
  //          return;

        Bundle bundle = getIntent().getExtras();

        // Get the layout from video_main.xml
        setContentView(R.layout.video_view);
        // Find your VideoView in your video_main.xml layout
        videoview = (VideoView) findViewById(R.id.video);
        // Execute StreamVideo AsyncTask

        String VideoURL = bundle.getString("url");

        pb = (ProgressBar) findViewById(R.id.probar);

        downloadRateView = (TextView) findViewById(R.id.download_rate);
        loadRateView = (TextView) findViewById(R.id.load_rate);

        //videoview.setBufferSize(0);
        videoview.setHardwareDecoder(true);
        // Create a progressbar
/*        progress = new ProgressDialog(this);
        progress.setMessage("Buffering...");
        progress.setCancelable(true);
        // Show progressbar
        progress.show();
*/
        try {
            // Start the MediaController
            MediaController mediacontroller = new MediaController(this);
//            mediacontroller.setAnchorView(videoview);
            // Get the URL from String VideoURL
            Uri video = Uri.parse(VideoURL);
//            videoview.setBufferSize(2048);
  //          videoview.setVideoQuality(MediaPlayer.VIDEOQUALITY_MEDIUM);
            videoview.setMediaController(mediacontroller);
            HashMap<String, String> options = new HashMap<String, String>();
            options.put("rtsp_transport", "tcp");
            options.put("rtsp_flags", "prefer_tcp");// udp

            videoview.setVideoURI(video, options);

        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
//            if (progress.isShowing())
  //              progress.dismiss();
         //   pDialog.dismiss();
        }

        videoview.setOnInfoListener(this);
        videoview.setOnBufferingUpdateListener(this);
        videoview.requestFocus();

        videoview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
               // mediaPlayer.setBufferSize(0);
                mediaPlayer.setPlaybackSpeed(1.0f);
//                if (progress.isShowing())
  //                  progress.dismiss();
            }
        });
/*        videoview.setOnPreparedListener(new OnPreparedListener() {
            // Close the progress bar and play the video
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
                pDialog.dismiss();
                videoview.start();
            }
        });*/

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
