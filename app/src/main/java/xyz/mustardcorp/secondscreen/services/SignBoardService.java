package xyz.mustardcorp.secondscreen.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import xyz.mustardcorp.secondscreen.R;
import xyz.mustardcorp.secondscreen.custom.CustomViewPager;
import xyz.mustardcorp.secondscreen.layouts.AppLauncher;
import xyz.mustardcorp.secondscreen.layouts.BaseLayout;
import xyz.mustardcorp.secondscreen.layouts.Music;
import xyz.mustardcorp.secondscreen.layouts.Toggles;
import xyz.mustardcorp.secondscreen.misc.Util;
import xyz.mustardcorp.secondscreen.misc.Values;

public class SignBoardService extends Service
{
    private static final String TAG = "SignBoardService";

    private WindowManager windowManager;
    private CustomViewPager screenLayout;

    private BaseLayout mToggles;
    private BaseLayout mMusic;
    private BaseLayout mLauncher;

    private Display display;

    private boolean isStock = false; //should be false unless debugging on stock V20 ROM

    private HashMap<String, Object> mAvailablePages = new HashMap<>();
    private ContentObserver observer;

    public SignBoardService()
    {
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Toast.makeText(this, "SignBoard Started!", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "SignBoard Service Started!");

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        mToggles = new Toggles(this);
        mMusic = new Music(this);
        mLauncher = new AppLauncher(this);

        mAvailablePages.put("toggles", mToggles);
        mAvailablePages.put("music", mMusic);
        mAvailablePages.put("launcher", mLauncher);

        display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        CustomPagerAdapter adapter = new CustomPagerAdapter(this);

        screenLayout = (CustomViewPager) LayoutInflater.from(this).inflate(R.layout.layout_main, null, false);
        screenLayout.setAdapter(adapter);
        screenLayout.setBackgroundColor(Settings.Global.getInt(getContentResolver(), "ss_color", Color.BLACK));

        // Setup layout parameter
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                0,
                0,
                WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                        WindowManager.LayoutParams.FLAG_LAYOUT_ATTACHED_IN_DECOR |
                        WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT);
        // At it to window manager for display, it will be printed over any thing
        windowManager.addView(screenLayout, params);

        switch (display.getRotation()) {
            case Surface.ROTATION_0:
                setNormalOrientation();
                break;
            case Surface.ROTATION_90:
                setHorizontalRightOrientation();
                break;
            case Surface.ROTATION_180:
                setUpsideDownOrientation();
                break;
            case Surface.ROTATION_270:
                setHorizontalLeftOrientation();
                break;
        }

        setupOrientationListener();
        setContentObserver();

        boolean shouldBeSticky = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Values.SHOULD_FORCE_START, true);

        return shouldBeSticky ? Service.START_STICKY : Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        sendBroadcast(new Intent(Values.KILL_BC_ACTION));

        // Make sure to remove it when you are done, else it will stick there until you reboot
        // Do keep track of same reference of view you added, don't mess with that
        windowManager.removeView(screenLayout);
        mToggles.onDestroy();
        mMusic.onDestroy();
        mLauncher.onDestroy();

        getContentResolver().unregisterContentObserver(observer);
    }

    private void setupOrientationListener() {
        OrientationEventListener listener = new OrientationEventListener(this)
        {
            @Override
            public void onOrientationChanged(int i)
            {
                switch (display.getRotation()) {
                    case Surface.ROTATION_0:
                        setNormalOrientation();
                        break;
                    case Surface.ROTATION_270:
                        setHorizontalLeftOrientation();
                        break;
                    case Surface.ROTATION_90:
                        setHorizontalRightOrientation();
                        break;
                    case Surface.ROTATION_180:
                        setUpsideDownOrientation();
                        break;
                }
            }
        };
        listener.enable();
    }

    private void setNormalOrientation() {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) screenLayout.getLayoutParams();

        params.height = 160;
        params.width = 1040;
        params.gravity = Gravity.TOP | Gravity.RIGHT;
        params.x = 0;
        params.y = -Util.getStatusBarHeight(this)-(isStock ? 0 : 160);

        windowManager.updateViewLayout(screenLayout, params);
    }

    private void setHorizontalLeftOrientation() {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) screenLayout.getLayoutParams();

        params.height = 1040;
        params.width = 160;
        params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        params.x = -Util.getNavBarHeight(this)-(isStock ? 0 : 160);
        params.y = 0;

        windowManager.updateViewLayout(screenLayout, params);
    }

    private void setHorizontalRightOrientation() {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) screenLayout.getLayoutParams();

        params.height = 1040;
        params.width = 160;
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = -(isStock ? 0 : 160);
        params.y = -Util.getStatusBarHeight(this);

        windowManager.updateViewLayout(screenLayout, params);
    }

    private void setUpsideDownOrientation() {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) screenLayout.getLayoutParams();

        params.height = 160;
        params.width = 1040;
        params.gravity = Gravity.BOTTOM | Gravity.LEFT;
        params.x = 0;
        params.y = -Util.getNavBarHeight(this)-(isStock ? 0 : 160);

        windowManager.updateViewLayout(screenLayout, params);
    }

    private void setContentObserver() {
        observer = new ContentObserver(new Handler())
        {
            @Override
            public void onChange(boolean selfChange)
            {
                screenLayout.setBackgroundColor(Settings.Global.getInt(getContentResolver(), "ss_color", Color.BLACK));
            }
        };

        getContentResolver().registerContentObserver(Settings.Global.CONTENT_URI, true, observer);
    }

    public class CustomPagerAdapter extends PagerAdapter
    {
        private Context mContext;

        private ArrayList<View> currentViews = new ArrayList<>();
        private ArrayList<String> defaultLoad = new ArrayList<String>() {{
            add("toggles");
            add("music");
            add("launcher");
        }};

        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        public CustomPagerAdapter(Context context) {
            mContext = context;

            ArrayList<String> load = defaultLoad;
            for (String s : load) {
                ((BaseLayout) mAvailablePages.get(s)).getView().setLayoutParams(layoutParams);
                currentViews.add(((BaseLayout) mAvailablePages.get(s)).getView());
                notifyDataSetChanged();
            }
        }

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            View view = currentViews.get(position);
            collection.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }

        @Override
        public int getCount() {
            return currentViews.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "";
        }
    }
}
