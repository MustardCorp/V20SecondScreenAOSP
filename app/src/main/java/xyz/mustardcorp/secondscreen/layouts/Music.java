package xyz.mustardcorp.secondscreen.layouts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.media.MediaControlIntent;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL;

import xyz.mustardcorp.secondscreen.R;
import xyz.mustardcorp.secondscreen.misc.Util;
import xyz.mustardcorp.secondscreen.services.SignBoardService;

/**
 * Simple flash controller
 * (BROKEN)
 */

public class Music extends BaseLayout
{
    private Context mContext;
    private LinearLayout mView;
    private  Display display;
    private  ArrayList<View> originalView;
    private  AudioManager audioManager;
    private ContentObserver stateObserver;
    private BroadcastReceiver playingMusicReceiver;

    private Handler mHandler;
    private final MediaSessionManager sessionManager;
    private List<MediaController> controllers;

    public Music(Context context) {
        super(context);
        mContext = context;
        mView = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.layout_music, null, false);
        display = ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        sessionManager = (MediaSessionManager) getContext().getSystemService(Context.MEDIA_SESSION_SERVICE);

        mHandler = new Handler(Looper.getMainLooper());

        originalView = new ArrayList<>();
        for (int i = 0; i < mView.getChildCount(); i++) {
            originalView.add(mView.getChildAt(i));
        }

        int rotation = display.getRotation();
        switch (rotation) {
            case Surface.ROTATION_270:
            case Surface.ROTATION_90:
                mView.setOrientation(LinearLayout.VERTICAL);
                break;
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                mView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                mView.setOrientation(LinearLayout.HORIZONTAL);
                break;
        }

        reverseViews(rotation);
        registerMediaCallbacks();
        setColorsAndStates();
        listenForColorChangeOrMusicChange();

        setOrientationListener();
    }

    public View getView() {
        return mView;
    }

    public void onDestroy() {
        mContext.getContentResolver().unregisterContentObserver(stateObserver);
        mContext.unregisterReceiver(playingMusicReceiver);
    }

    private void listenForColorChangeOrMusicChange() {
        stateObserver = new ContentObserver(null)
        {
            @Override
            public void onChange(boolean selfChange, Uri uri)
            {
                Uri backUri = Settings.Global.getUriFor("skip_prev_color");
                Uri ppUri = Settings.Global.getUriFor("play_pause_color");
                Uri forUri = Settings.Global.getUriFor("skip_forward_color");
                Uri infoUri = Settings.Global.getUriFor("song_info_color");

                if (uri.equals(backUri) || uri.equals(ppUri) || uri.equals(forUri) || uri.equals(infoUri)) {
                    mHandler.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            setColorsAndStates();
                        }
                    });
                }
            }
        };

        mContext.getContentResolver().registerContentObserver(Settings.Global.CONTENT_URI, true, stateObserver);

        playingMusicReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, final Intent intent)
            {
                mHandler.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        setColorsAndStates();
                    }
                });
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(MediaControlIntent.ACTION_PLAY);
        filter.addAction(MediaControlIntent.ACTION_PAUSE);
        filter.addAction(MediaControlIntent.ACTION_STOP);

//        mContext.registerReceiver(playingMusicReceiver, filter);

        sessionManager.addOnActiveSessionsChangedListener(new MediaSessionManager.OnActiveSessionsChangedListener()
        {
            @Override
            public void onActiveSessionsChanged(@Nullable List<MediaController> list)
            {
                registerMediaCallbacks();
            }
        }, null);
    }

    private void registerMediaCallbacks() {
        controllers = sessionManager.getActiveSessions(null);

        MediaController.Callback callback = new MediaController.Callback()
        {
            @Override
            @MainThread
            public void onPlaybackStateChanged(@NonNull PlaybackState state)
            {
                mHandler.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        setColorsAndStates();
                    }
                }, 300);
            }
        };

        for (MediaController controller : controllers) {
            controller.registerCallback(callback);
        }
    }

    private void setColorsAndStates() {
        ImageView back = mView.findViewById(R.id.skip_back);
        ImageView playpause = mView.findViewById(R.id.play_pause);
        ImageView forward = mView.findViewById(R.id.skip_forward);
        TextView song = mView.findViewById(R.id.song_info);
        song.setSelected(true);

        int backColor = Settings.Global.getInt(mContext.getContentResolver(), "skip_prev_color", Color.WHITE);
        int playpauseColor = Settings.Global.getInt(mContext.getContentResolver(), "play_pause_color", Color.WHITE);
        int forwardColor = Settings.Global.getInt(mContext.getContentResolver(), "skip_forward_color", Color.WHITE);
        int songColor = Settings.Global.getInt(mContext.getContentResolver(), "song_info_color", Color.WHITE);

        back.setImageTintList(ColorStateList.valueOf(backColor));
        playpause.setImageTintList(ColorStateList.valueOf(playpauseColor));
        forward.setImageTintList(ColorStateList.valueOf(forwardColor));
        song.setTextColor(songColor);

        back.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_skip_previous_black_24dp, null));
        playpause.setImageDrawable(mContext.getResources().getDrawable(isMusicPlaying() ? R.drawable.ic_pause_black_24dp : R.drawable.ic_play_arrow_black_24dp, null));
        forward.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_skip_next_black_24dp, null));

        if (controllers != null) {
            String title = controllers.get(0).getMetadata().getString(MediaMetadata.METADATA_KEY_TITLE);
            String artist = controllers.get(0).getMetadata().getString(MediaMetadata.METADATA_KEY_ARTIST);

            final String packageName = controllers.get(0).getPackageName();

            StringBuilder info = new StringBuilder();

            if (title != null) {
                info.append(title);

                if (artist != null) {
                    info.append(" — ").append(artist);
                }
            } else {
                info.append(song.getText());
            }

            song.setText(info.toString());

            if (packageName != null) {
                mView.setOnLongClickListener(new View.OnLongClickListener()
                {
                    @Override
                    public boolean onLongClick(View view)
                    {
                        Util.openApp(getContext(), packageName);
                        return true;
                    }
                });
            }
        }

        back.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                long eventtime = SystemClock.uptimeMillis();

                KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0);
                audioManager.dispatchMediaKeyEvent(downEvent);

                KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0);
                audioManager.dispatchMediaKeyEvent(upEvent);
            }
        });

        playpause.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                long eventtime = SystemClock.uptimeMillis();

                KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
                audioManager.dispatchMediaKeyEvent(downEvent);

                KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
                audioManager.dispatchMediaKeyEvent(upEvent);
            }
        });

        forward.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                long eventtime = SystemClock.uptimeMillis();

                KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT, 0);
                audioManager.dispatchMediaKeyEvent(downEvent);

                KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT, 0);
                audioManager.dispatchMediaKeyEvent(upEvent);
            }
        });
    }

    private boolean isMusicPlaying() {
        Log.e("MustardCorp", "isMusicActive()" + audioManager.isMusicActive());
        return audioManager.isMusicActive();
    }

    public void setOrientationListener() {
        OrientationEventListener listener = new OrientationEventListener(mContext)
        {
            private int oldRotation = display.getRotation();

            @Override
            public void onOrientationChanged(int i)
            {
                if (oldRotation != display.getRotation()) {
                    switch (display.getRotation()) {
                        case Surface.ROTATION_0:
                        case Surface.ROTATION_180:
                            setNormalOrientation();
                            break;
                        case Surface.ROTATION_90:
                        case Surface.ROTATION_270:
                            setHorizontalOrientation();
                            break;
                    }

                    reverseViews(display.getRotation());

                    oldRotation = display.getRotation();
                }
            }
        };
        listener.enable();
    }

    private void setNormalOrientation() {
        ViewGroup.LayoutParams params = mView.getLayoutParams();

        params.height = 160;
        params.width = 1040;

        mView.findViewById(R.id.song_info).setRotation(0f);
        mView.setLayoutDirection(LinearLayout.LAYOUT_DIRECTION_LTR);

        mView.setLayoutParams(params);
        mView.setOrientation(LinearLayout.HORIZONTAL);
        mView.requestLayout();
    }

    private void setHorizontalOrientation() {
        ViewGroup.LayoutParams params = mView.getLayoutParams();

        params.height = 1040;
        params.width = 160;

        mView.findViewById(R.id.song_info).setRotation(90f);

        mView.setLayoutParams(params);
        mView.setOrientation(LinearLayout.VERTICAL);
        mView.requestLayout();
    }

    private void reverseViews(int rotation) {
        ArrayList<View> views = originalView;

        boolean shouldReverse = rotation == Surface.ROTATION_90;

        mView.removeAllViews();

        if (shouldReverse)
        {
            for (int x = views.size() - 1; x >= 0; x--)
            {
                mView.addView(views.get(x));
            }
        } else {
            for (int i = 0; i < views.size(); i++) {
                mView.addView(views.get(i));
            }
        }
    }
}
