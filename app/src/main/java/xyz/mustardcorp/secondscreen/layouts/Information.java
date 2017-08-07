package xyz.mustardcorp.secondscreen.layouts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import xyz.mustardcorp.secondscreen.R;

import static android.content.Context.BATTERY_SERVICE;

public class Information extends BaseLayout
{
    private LinearLayout mView;
    private LinearLayout mNotifsView;
    private BroadcastReceiver batteryChange;
    private Display display;
    private ArrayList<View> originalLayout = new ArrayList<>();

    public Information(Context context) {
        super(context);

        mView = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.layout_info, null, false);
        mNotifsView = mView.findViewById(R.id.notification_layout);
        display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        for (int i = 0; i < mView.getChildCount(); i++) {
            originalLayout.add(mView.getChildAt(i));
        }

        setProperOrientation();
        setOrientationListener();
        registerObserversAndReceivers();
        batteryValues();
    }

    @Override
    public void setOrientationListener()
    {
        OrientationEventListener listener = new OrientationEventListener(getContext())
        {
            @Override
            public void onOrientationChanged(int i)
            {
                setProperOrientation();
            }
        };
        listener.enable();
    }

    @Override
    public View getView()
    {
        return mView;
    }

    @Override
    public void onDestroy()
    {
        getContext().unregisterReceiver(batteryChange);
    }

    private void setProperOrientation() {
        reverseViewsIfNeeded();
        switch (display.getRotation()) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                mView.setOrientation(LinearLayout.HORIZONTAL);
                mNotifsView.setOrientation(LinearLayout.HORIZONTAL);
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                mView.setOrientation(LinearLayout.VERTICAL);
                mNotifsView.setOrientation(LinearLayout.VERTICAL);
                break;
        }
    }

    private void reverseViewsIfNeeded() {
        boolean shouldReverse = display.getRotation() == Surface.ROTATION_90;

        mView.removeAllViews();

        if (shouldReverse) {
            for (int i = originalLayout.size() - 1; i >= 0; i--) {
                mView.addView(originalLayout.get(i));
            }
        } else {
            for (int i = 0; i < originalLayout.size(); i++) {
                mView.addView(originalLayout.get(i));
            }
        }
    }

    private void registerObserversAndReceivers() {
        IntentFilter batteryFilter = new IntentFilter();
        batteryFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        batteryFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        batteryFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        batteryChange = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                batteryValues();
            }
        };

        getContext().registerReceiver(batteryChange, batteryFilter);
    }

    private void batteryValues() {
        BatteryManager bm = (BatteryManager)getContext().getSystemService(BATTERY_SERVICE);
        int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        boolean isCharging = bm.isCharging();

        TextView batteryLevel = mView.findViewById(R.id.battery_percent);
        batteryLevel.setText(batLevel + "%");

        int resId;

        if (batLevel >= 95) {
            resId = isCharging ? R.drawable.ic_battery_charging_full_black_24dp : R.drawable.ic_battery_full_black_24dp;
        } else if (batLevel >= 85) {
            resId = isCharging ? R.drawable.ic_battery_charging_90_black_24dp : R.drawable.ic_battery_90_black_24dp;
        } else if (batLevel >= 70) {
            resId = isCharging ? R.drawable.ic_battery_charging_60_black_24dp : R.drawable.ic_battery_60_black_24dp;
        } else if (batLevel >= 55) {
            resId = isCharging ? R.drawable.ic_battery_charging_50_black_24dp : R.drawable.ic_battery_50_black_24dp;
        } else if (batLevel >= 40) {
            resId = isCharging ? R.drawable.ic_battery_charging_30_black_24dp : R.drawable.ic_battery_30_black_24dp;
        } else if (batLevel >= 25) {
            resId = isCharging ? R.drawable.ic_battery_charging_20_black_24dp : R.drawable.ic_battery_20_black_24dp;
        } else {
            resId = isCharging ? R.drawable.ic_battery_charging_20_black_24dp : R.drawable.ic_battery_alert_black_24dp;
        }
        Drawable icon = getContext().getResources().getDrawable(resId, null);
        batteryLevel.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);
        batteryLevel.setCompoundDrawableTintList(ColorStateList.valueOf(Settings.Global.getInt(getContext().getContentResolver(), "battery_color", Color.WHITE)));
    }
}
