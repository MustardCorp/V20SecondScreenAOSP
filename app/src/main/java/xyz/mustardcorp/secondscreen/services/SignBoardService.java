package xyz.mustardcorp.secondscreen.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import xyz.mustardcorp.secondscreen.R;
import xyz.mustardcorp.secondscreen.layouts.Toggles;
import xyz.mustardcorp.secondscreen.misc.Util;
import xyz.mustardcorp.secondscreen.misc.Values;

public class SignBoardService extends Service
{
    private static final String TAG = "SignBoardService";

    private WindowManager windowManager;
    private LinearLayout screenLayout;
    private Toggles mToggles;
    private Display display;

    private boolean isStock = false; //should be false unless debugging on stock V20 ROM

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
        display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        screenLayout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.layout_test, null, false);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mToggles.getView().setLayoutParams(layoutParams);
        screenLayout.addView(mToggles.getView());

        // Setup layout parameter
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                0,
                0,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN |
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
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
        params.y = -(isStock ? 0 : 160);

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
        params.y = 0;

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
}
