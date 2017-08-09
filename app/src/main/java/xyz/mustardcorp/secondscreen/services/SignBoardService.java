package xyz.mustardcorp.secondscreen.services;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.PermissionChecker;
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
import android.widget.Toast;

import com.mcs.viewpager.OrientationViewPager;

import java.util.ArrayList;
import java.util.HashMap;

import xyz.mustardcorp.secondscreen.R;
import xyz.mustardcorp.secondscreen.activities.RequestPermissionsActivity;
import xyz.mustardcorp.secondscreen.custom.CustomViewPager;
import xyz.mustardcorp.secondscreen.layouts.AppLauncher;
import xyz.mustardcorp.secondscreen.layouts.BaseLayout;
import xyz.mustardcorp.secondscreen.layouts.Contacts;
import xyz.mustardcorp.secondscreen.layouts.Information;
import xyz.mustardcorp.secondscreen.layouts.Music;
import xyz.mustardcorp.secondscreen.layouts.Recents;
import xyz.mustardcorp.secondscreen.layouts.Toggles;
import xyz.mustardcorp.secondscreen.misc.Util;
import xyz.mustardcorp.secondscreen.misc.Values;

import static xyz.mustardcorp.secondscreen.misc.Values.APPS_KEY;
import static xyz.mustardcorp.secondscreen.misc.Values.CONTACTS_KEY;
import static xyz.mustardcorp.secondscreen.misc.Values.INFO_KEY;
import static xyz.mustardcorp.secondscreen.misc.Values.MUSIC_KEY;
import static xyz.mustardcorp.secondscreen.misc.Values.RECENTS_KEY;
import static xyz.mustardcorp.secondscreen.misc.Values.TOGGLES_KEY;
import static xyz.mustardcorp.secondscreen.misc.Values.defaultLoad;

public class SignBoardService extends Service
{
    private static final String TAG = "SignBoardService";

    private WindowManager windowManager;
    private CustomViewPager screenLayout;

    private BaseLayout mToggles;
    private BaseLayout mMusic;
    private BaseLayout mLauncher;
    private BaseLayout mInfo;
    private BaseLayout mRecents;
    private BaseLayout mContacts;

    private BroadcastReceiver mReceiver;

    private Display display;

    private boolean isStock = false; //should be false unless debugging on stock V20 ROM

    private ContentObserver observer;
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    private Handler mHandler;

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

        mHandler = new Handler(Looper.myLooper());
        
        boolean shouldBeSticky = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Values.SHOULD_FORCE_START, true);

        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (PermissionChecker.checkCallingOrSelfPermission(SignBoardService.this, Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE) != PackageManager.PERMISSION_GRANTED) {
                    startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                }

                if (PermissionChecker.checkCallingOrSelfPermission(SignBoardService.this, Manifest.permission.PACKAGE_USAGE_STATS) != PackageManager.PERMISSION_GRANTED) {
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                }

                if (PermissionChecker.checkCallingOrSelfPermission(SignBoardService.this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    Intent perm = new Intent(SignBoardService.this, RequestPermissionsActivity.class);
                    perm.putExtra("permission", Manifest.permission.READ_CONTACTS);
                    startActivity(perm);
                }

                if (PermissionChecker.checkCallingOrSelfPermission(SignBoardService.this, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    Intent perm = new Intent(SignBoardService.this, RequestPermissionsActivity.class);
                    perm.putExtra("permission", Manifest.permission.WRITE_CONTACTS);
                    startActivity(perm);
                }

                windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

                display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

                CustomPagerAdapter adapter = new CustomPagerAdapter(SignBoardService.this, display.getRotation() == Surface.ROTATION_90, -1);

                screenLayout = (CustomViewPager) LayoutInflater.from(SignBoardService.this).inflate(R.layout.layout_main, null, false);
                screenLayout.setAdapter(adapter);
                if (display.getRotation() == Surface.ROTATION_90) screenLayout.setCurrentItem(screenLayout.getChildCount() - 1);
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
                        setNormalOrientation(Surface.ROTATION_0);
                        break;
                    case Surface.ROTATION_90:
                        setHorizontalRightOrientation(Surface.ROTATION_0);
                        break;
                    case Surface.ROTATION_180:
                        setUpsideDownOrientation(Surface.ROTATION_0);
                        break;
                    case Surface.ROTATION_270:
                        setHorizontalLeftOrientation(Surface.ROTATION_0);
                        break;
                }

                setupOrientationListener();
                setContentObserver();
                registerReceiver();
                setSharedPrefsListener();
            }
        });

        return shouldBeSticky ? Service.START_STICKY : Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                sendBroadcast(new Intent(Values.KILL_BC_ACTION));

                try {
                    windowManager.removeView(screenLayout);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (mToggles != null) mToggles.onDestroy();
                if (mMusic != null) mMusic.onDestroy();
                if (mLauncher != null) mLauncher.onDestroy();
                if (mInfo != null) mInfo.onDestroy();
                if (mRecents != null) mRecents.onDestroy();
                if (mContacts != null) mContacts.onDestroy();

                getContentResolver().unregisterContentObserver(observer);
                try {
                    unregisterReceiver(mReceiver);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                PreferenceManager.getDefaultSharedPreferences(SignBoardService.this).unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
            }
        });
    }

    private void registerReceiver() {
        mReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                        windowManager.removeView(screenLayout);
                    }

                    if (action.equals(Intent.ACTION_SCREEN_ON)) {
                        windowManager.addView(screenLayout, screenLayout.getLayoutParams());
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter() {{
            addAction(Intent.ACTION_SCREEN_ON);
            addAction(Intent.ACTION_SCREEN_OFF);
        }};

        registerReceiver(mReceiver, filter);
    }

    private void setSharedPrefsListener() {
        sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener()
        {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s)
            {
                if (s.equals("should_force_start")) {
                    onDestroy();
                }
            }
        };

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    private void setupOrientationListener() {
        OrientationEventListener listener = new OrientationEventListener(this)
        {
            private int orientation = Surface.ROTATION_0;

            @Override
            public void onOrientationChanged(int i)
            {
                if (display.getRotation() != orientation)
                {
                    switch (display.getRotation())
                    {
                        case Surface.ROTATION_0:
                            setNormalOrientation(orientation);
                            break;
                        case Surface.ROTATION_270:
                            setHorizontalLeftOrientation(orientation);
                            break;
                        case Surface.ROTATION_90:
                            setHorizontalRightOrientation(orientation);
                            break;
                        case Surface.ROTATION_180:
                            setUpsideDownOrientation(orientation);
                            break;
                    }
                    orientation = display.getRotation();
                }
            }
        };
        listener.enable();
    }

    private void setNormalOrientation(int previousOrientation) {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) screenLayout.getLayoutParams();

        params.height = 160;
        params.width = 1040;
        params.gravity = Gravity.TOP | Gravity.RIGHT;
        params.x = 0;
        params.y = -Util.getStatusBarHeight(this)-(isStock ? 0 : 160);

        int position;

        if (previousOrientation == Surface.ROTATION_90) {
            if (screenLayout.getCurrentItem() == 0) position = screenLayout.getChildCount() + 1; //I have no idea why this is needed, but it is...
            else position = screenLayout.getChildCount() - screenLayout.getCurrentItem();
        }
        else position = screenLayout.getCurrentItem();

        screenLayout.setAdapter(new CustomPagerAdapter(this, false, position));
        screenLayout.setCurrentItem(position);
        screenLayout.setOrientation(CustomViewPager.ORIENTATION_HORIZONTAL);
        windowManager.updateViewLayout(screenLayout, params);
    }

    private void setHorizontalLeftOrientation(int previousOrientation) {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) screenLayout.getLayoutParams();

        params.height = 1040;
        params.width = 160;
        params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        params.x = -Util.getNavBarHeight(this)-(isStock ? 0 : 160);
        params.y = 0;

        int position;
        if (previousOrientation == Surface.ROTATION_90) {
            if (screenLayout.getCurrentItem() == 0) position = screenLayout.getChildCount() + 1; //I have no idea why this is needed, but it is...
            else position = screenLayout.getChildCount() - screenLayout.getCurrentItem();
        }
        else position = screenLayout.getCurrentItem();

        screenLayout.setAdapter(new CustomPagerAdapter(this, false, position));
        screenLayout.setCurrentItem(position);
        screenLayout.setOrientation(CustomViewPager.ORIENTATION_VERTICAL);
        windowManager.updateViewLayout(screenLayout, params);
    }

    private void setHorizontalRightOrientation(int previousOrientation) {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) screenLayout.getLayoutParams();

        params.height = 1040;
        params.width = 160;
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = -(isStock ? 0 : 160);
        params.y = -Util.getStatusBarHeight(this);

        int position;
        if (previousOrientation != Surface.ROTATION_90) {
            if (screenLayout.getCurrentItem() == 0) position = screenLayout.getChildCount() + 1; //I have no idea why this is needed, but it is...
            else position = screenLayout.getChildCount() - screenLayout.getCurrentItem();
        }
        else position = screenLayout.getCurrentItem();

        screenLayout.setAdapter(new CustomPagerAdapter(this, true, position));
        screenLayout.setCurrentItem(position);
        screenLayout.setOrientation(CustomViewPager.ORIENTATION_VERTICAL);
        windowManager.updateViewLayout(screenLayout, params);
    }

    private void setUpsideDownOrientation(int previousOrientation) {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) screenLayout.getLayoutParams();

        params.height = 160;
        params.width = 1040;
        params.gravity = Gravity.BOTTOM | Gravity.LEFT;
        params.x = 0;
        params.y = -Util.getNavBarHeight(this)-(isStock ? 0 : 160);

        int position;
        if (previousOrientation == Surface.ROTATION_90) {
            if (screenLayout.getCurrentItem() == 0) position = screenLayout.getChildCount() + 1; //I have no idea why this is needed, but it is...
            else position = screenLayout.getChildCount() - screenLayout.getCurrentItem();
        }
        else position = screenLayout.getCurrentItem();

        screenLayout.setAdapter(new CustomPagerAdapter(this, false, position));
        screenLayout.setCurrentItem(position);
        screenLayout.setOrientation(CustomViewPager.ORIENTATION_HORIZONTAL);
        windowManager.updateViewLayout(screenLayout, params);
    }

    private void setContentObserver() {
        observer = new ContentObserver(new Handler())
        {
            @Override
            public void onChange(boolean selfChange, Uri uri)
            {
                Uri ssColor = Settings.Global.getUriFor("ss_color");
                Uri savedViews = Settings.Global.getUriFor("saved_views");

                if (uri.equals(ssColor)) {
                    screenLayout.setBackgroundColor(Settings.Global.getInt(getContentResolver(), "ss_color", Color.BLACK));
                }

                if (uri.equals(savedViews)) {
                    CustomPagerAdapter oldAdapter = (CustomPagerAdapter) screenLayout.getAdapter();
                    screenLayout.setAdapter(new CustomPagerAdapter(SignBoardService.this, oldAdapter.getShouldReverse(), oldAdapter.getPosition()));
                }
            }
        };

        getContentResolver().registerContentObserver(Settings.Global.CONTENT_URI, true, observer);
    }

    public class CustomPagerAdapter extends com.mcs.viewpager.PagerAdapter
    {
        private Context mContext;
        private boolean shouldReverse;
        private int position;
        private HashMap<String, Object> mAvailablePages = new HashMap<>();

        private ArrayList<View> currentViews = new ArrayList<>();


        OrientationViewPager.LayoutParams layoutParams = new OrientationViewPager.LayoutParams();
        private final ArrayList<String> load;

        public CustomPagerAdapter(Context context, boolean shouldReverse, int position) {
            mContext = context;
            this.shouldReverse = shouldReverse;
            this.position = position;

            load = Util.parseSavedViews(mContext, defaultLoad);

            if (load.contains(TOGGLES_KEY)) {
                mToggles = new Toggles(mContext);
                mAvailablePages.put(TOGGLES_KEY, mToggles);
            }
            if (load.contains(MUSIC_KEY)) {
                mMusic = new Music(mContext);
                mAvailablePages.put(MUSIC_KEY, mMusic);
            }
            if (load.contains(APPS_KEY)) {
                mLauncher = new AppLauncher(mContext);
                mAvailablePages.put(APPS_KEY, mLauncher);
            }
            if (load.contains(INFO_KEY)) {
                mInfo = new Information(mContext);
                mAvailablePages.put(INFO_KEY, mInfo);
            }
            if (load.contains(RECENTS_KEY)) {
                mRecents = new Recents(context);
                mAvailablePages.put(RECENTS_KEY, mRecents);
            }
            if (load.contains(CONTACTS_KEY)) {
                mContacts = new Contacts(context);
                mAvailablePages.put(CONTACTS_KEY, mContacts);
            }

            if (shouldReverse) {
                for (int i = load.size() - 1; i >= 0; i--) {
                    String s = load.get(i);
                    ((BaseLayout) mAvailablePages.get(s)).getView().setLayoutParams(layoutParams);
                    currentViews.add(((BaseLayout) mAvailablePages.get(s)).getView());
                    notifyDataSetChanged();
                }
            } else {
                for (String s : load)
                {
                    ((BaseLayout) mAvailablePages.get(s)).getView().setLayoutParams(layoutParams);
                    currentViews.add(((BaseLayout) mAvailablePages.get(s)).getView());
                }
            }

            notifyDataSetChanged();
            if (position > -1) screenLayout.setCurrentItem(position);
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

        public boolean getShouldReverse() {
            return shouldReverse;
        }

        public int getPosition() {
            return position;
        }
    }
}
