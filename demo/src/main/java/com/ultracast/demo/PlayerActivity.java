package com.ultracast.demo;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.ultracast.demo.widget.TimeLineView;
import com.ultracast.player.UC360Player;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class PlayerActivity extends AppCompatActivity implements UC360Player.EventListener, TimeLineView.Listener {

    public static final String URL_EXTRA = "url";
    private static final String TAG = "PlayerActivity";

    private Toolbar toolbar;
    private UC360Player player;
    private ProgressBar progressBar;
    private View playerControls;
    private TimeLineView timeline;
    private ImageView playPause;

    private Handler mTimeLineHandler = new Handler();
    private Runnable mUpdateTimeLineTask = new Runnable() {
        @Override
        public void run() {
            long currentTime = player.getCurrentTime();
            timeline.setTime((int) currentTime, (int) player.getDuration());
            mTimeLineHandler.postDelayed(mUpdateTimeLineTask, 10);
        }
    };
    private boolean isVREnable;
    private boolean isGyroEnable;
    private boolean isPlaying = true;
    private boolean isUIVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        bindViews();
        handleIntent(getIntent());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTimeLineHandler.postDelayed(mUpdateTimeLineTask, 0);
        player.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        player.addOnPlayerEventListener(this);
        player.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        player.removeOnPlayerEventListener(this);
        player.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mTimeLineHandler.removeCallbacksAndMessages(null);
        player.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home: {
                NavUtils.navigateUpFromSameTask(this);
                break;
            }
            case R.id.vr_btn: {
                setRequestedOrientation(isVREnable ? ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                menuItem.setIcon(ContextCompat.getDrawable(getApplicationContext(),
                        isVREnable ? R.drawable.vr_top_btn : R.drawable.novr_top_btn));
                if (isVREnable) {
                    showUI();
                } else {
                    hideUI();
                }
                isVREnable = !isVREnable;
                player.switchVrMode();
                break;
            }
            case R.id.gyro_btn: {
                menuItem.setIcon(ContextCompat.getDrawable(getApplicationContext(),
                        isGyroEnable ? R.drawable.gyro_top_btn : R.drawable.nogyro_top_btn));
                player.switchControlMode(isGyroEnable
                        ? UC360Player.MODE_MOTION_WITH_TOUCH : UC360Player.MODE_TOUCH);
                isGyroEnable = !isGyroEnable;
                break;
            }
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        player.onOrientationChanged();
    }

    /* UC360Player.EventListener */

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case UC360Player.STATE_READY:
                cancelBusy();
                mTimeLineHandler.postDelayed(mUpdateTimeLineTask, 0);
                break;
            case UC360Player.STATE_IDLE:
                Log.i(TAG, "Player status idle");
                break;
            case UC360Player.STATE_BUFFERING:
                mTimeLineHandler.removeCallbacksAndMessages(null);
                showBusy();
                Log.i(TAG, "Player status buffering");
                break;
            case UC360Player.STATE_ENDED:
                mTimeLineHandler.removeCallbacksAndMessages(null);
                Log.i(TAG, "Player status ended");
                break;
        }
    }

    @Override
    public void onPlayerError(int errorType) {
        Log.i(TAG, "PlayerError " + errorType);
    }

    @Override
    public void onMotionControlNotSupported() {
        Log.i(TAG, "onMotionControlNotSupported()");
    }

    @Override
    public void onPlayerClick() {
        Log.i(TAG, "onPlayerClick()");
        toggleUIVisibility();
    }

    /* TimeLineView.Listener */

    @Override
    public void onScrolling(int msec) {
        player.seek(msec);
    }

    @Override
    public void onScrollingStop(int msec) {
        Log.d(TAG, "scroll " + msec);
        player.seek(msec);
    }

    /* --- */

    private void bindViews() {
        playerControls = findViewById(R.id.player_controls);
        playPause = (ImageView) findViewById(R.id.play_pause);
        timeline = (TimeLineView) findViewById(R.id.timeline);
        timeline.setListener(this);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                togglePlayPause();
            }
        });
    }

    private void cancelBusy() {
        if (progressBar != null)
            progressBar.setVisibility(GONE);
    }

    private void handleIntent(Intent intent) {
        String url = intent.getStringExtra(URL_EXTRA);
        if (url == null || url.isEmpty()) {
            finish();
            return;
        }

        initPlayer(url);
    }

    private void hideUI() {
        playerControls.setVisibility(GONE);
        toolbar.setVisibility(GONE);
        isUIVisible = false;
    }

    private void initPlayer(String url) {
        player = UC360Player.Factory.create(this);
        player.init(R.id.player, UC360Player.MODE_MOTION_WITH_TOUCH);
        player.openUrl(Uri.parse(url), true, false);
    }

    private void showBusy() {
        if (progressBar != null)
            progressBar.setVisibility(VISIBLE);
    }

    private void showUI() {
        playerControls.setVisibility(VISIBLE);
        toolbar.setVisibility(VISIBLE);
        isUIVisible = true;
    }

    private void togglePlayPause() {
        if (progressBar.getVisibility() == VISIBLE) return;
        playPause.setImageResource(isPlaying ? R.drawable.play_bottom_btn
                : R.drawable.pause_bottom_btn);
        if (isPlaying) {
            player.pause();
        } else {
            player.play();
        }
        isPlaying = !isPlaying;
    }

    private void toggleUIVisibility() {
        if (isUIVisible) {
            hideUI();
        } else {
            showUI();
        }
    }
}