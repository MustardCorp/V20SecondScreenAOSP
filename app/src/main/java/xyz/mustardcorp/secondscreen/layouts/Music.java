package xyz.mustardcorp.secondscreen.layouts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Handler;
import android.provider.Settings;
import android.view.Display;
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

import xyz.mustardcorp.secondscreen.R;

public class Music extends BaseLayout
{
    private Context mContext;
    private LinearLayout mView;
    private  Display display;
    private  ArrayList<View> originalView;
    private  AudioManager audioManager;
    private ContentObserver stateObserver;
    private BroadcastReceiver playingMusicReceiver;

    public Music(Context context) {
        super(context);
        mContext = context;
        mView = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.layout_music, null, false);
        display = ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);

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
        Handler handler = new Handler();

        stateObserver = new ContentObserver(handler)
        {
            @Override
            public void onChange(boolean selfChange)
            {
                setColorsAndStates();
            }
        };

        mContext.getContentResolver().registerContentObserver(Settings.Global.CONTENT_URI, true, stateObserver);

        playingMusicReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                setColorsAndStates();
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.music.playstatechanged");
        filter.addAction("fm.last.android.metachanged");
        filter.addAction("fm.last.android.playbackpaused");
        filter.addAction("fm.last.android.playbackcomplete");

        mContext.registerReceiver(playingMusicReceiver, filter);
    }

    private void setColorsAndStates() {
        ImageView back = mView.findViewById(R.id.skip_back);
        ImageView playpause = mView.findViewById(R.id.play_pause);
        ImageView forward = mView.findViewById(R.id.skip_forward);
        TextView song = mView.findViewById(R.id.song_info);

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

        back.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {

            }
        });

        playpause.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {

            }
        });

        forward.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {

            }
        });
    }

    private boolean isMusicPlaying() {
        return audioManager.isMusicActive();
    }

    @Override
    public void setOrientationListener() {
        OrientationEventListener listener = new OrientationEventListener(mContext)
        {
            @Override
            public void onOrientationChanged(int i)
            {
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
