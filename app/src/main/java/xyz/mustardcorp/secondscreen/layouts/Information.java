package xyz.mustardcorp.secondscreen.layouts;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import xyz.mustardcorp.secondscreen.R;
import xyz.mustardcorp.secondscreen.activities.RequestPermissionsActivity;
import xyz.mustardcorp.secondscreen.misc.Util;
import xyz.mustardcorp.secondscreen.misc.Values;
import xyz.mustardcorp.secondscreen.services.SignBoardService;

import static android.content.Context.BATTERY_SERVICE;
import static android.content.Context.CONNECTIVITY_SERVICE;

/**
 * Basic information layout
 * Shows time, battery status, signal status and notifications
 */

public class Information extends BaseLayout
{
    private LinearLayout mView;
    private LinearLayout mNotifsView;
    private BroadcastReceiver actionChange;
    private Display display;
    private ArrayList<View> originalLayout = new ArrayList<>();
    private BroadcastReceiver localReceiver;
    private ContentObserver mObserver;
    private final PowerManager.WakeLock wakeLock;
    private final PowerManager manager;

    /**
     * Create new instance, set up orientation and views, and set listeners
     * @param context caller's context
     */
    public Information(Context context) {
        super(context);

        mView = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.layout_info, null, false);
        mNotifsView = mView.findViewById(R.id.notification_layout);
        display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        manager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "aod_service");

        for (int i = 0; i < mView.getChildCount(); i++) {
            originalLayout.add(mView.getChildAt(i));
        }

        if (getContext().checkCallingOrSelfPermission(Manifest.permission.ACCESS_NOTIFICATION_POLICY) != PackageManager.PERMISSION_GRANTED) {
            getContext().startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
        }

        setProperOrientation();
        setOrientationListener();
        registerObserversAndReceivers();
        batteryValues();
        wifiLevels();

        TextClock textClock = mView.findViewById(R.id.current_time);
        textClock.setTextColor(Settings.Global.getInt(getContext().getContentResolver(), "clock_color", Color.WHITE));

        setContentObserver();

        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(Values.ACTION_INFORMATION_ADDED));
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
        try {
            getContext().unregisterReceiver(actionChange);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(localReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        getContext().getContentResolver().unregisterContentObserver(mObserver);
    }

    /**
     * Register a {@link ContentObserver} and listen for relevant changes
     */
    private void setContentObserver() {
        mObserver = new ContentObserver(new Handler())
        {
            @Override
            public void onChange(boolean selfChange, Uri uri)
            {
                Log.e("Something changed", uri.toString());

                Uri notifs = Settings.Global.getUriFor("notification_icon_color");
                Uri wifi = Settings.Global.getUriFor("wifi_signal_color");
                Uri mobile = Settings.Global.getUriFor("cell_signal_color");
                Uri battery = Settings.Global.getUriFor("battery_color");
                Uri clock = Settings.Global.getUriFor("clock_color");

                if (uri.equals(notifs)) {
                    LinearLayout layout = mView.findViewById(R.id.notification_layout);
                    for (int i = 0; i < layout.getChildCount(); i++) {
                        ImageView view = (ImageView) layout.getChildAt(i);
                        view.setColorFilter(Settings.Global.getInt(getContext().getContentResolver(), "notification_icon_color", Color.WHITE), PorterDuff.Mode.SRC_IN);
                    }
                }

                if (uri.equals(wifi)) {
                    ImageView wifiView = mView.findViewById(R.id.wifi_level);
                    wifiView.setColorFilter(Settings.Global.getInt(getContext().getContentResolver(), "wifi_signal_color", Color.WHITE), PorterDuff.Mode.SRC_IN);
                }

                if (uri.equals(mobile)) {
                    ImageView mobileView = mView.findViewById(R.id.signal_level);
                    mobileView.setColorFilter(Settings.Global.getInt(getContext().getContentResolver(), "cell_signal_color", Color.WHITE), PorterDuff.Mode.SRC_IN);
                }

                if (uri.equals(battery)) {
                    TextView batteryView = mView.findViewById(R.id.battery_percent);
                    batteryView.setTextColor(Settings.Global.getInt(getContext().getContentResolver(), "battery_color", Color.WHITE));
                    batteryView.setCompoundDrawableTintList(ColorStateList.valueOf(Settings.Global.getInt(getContext().getContentResolver(), "battery_color", Color.WHITE)));
                }

                if (uri.equals(clock)) {
                    TextClock textClock = mView.findViewById(R.id.current_time);
                    textClock.setTextColor(Settings.Global.getInt(getContext().getContentResolver(), "clock_color", Color.WHITE));
                }

                super.onChange(selfChange, uri);
            }
        };

        getContext().getContentResolver().registerContentObserver(Settings.Global.CONTENT_URI, true, mObserver);
    }

    /**
     * Order and rotate the child views properly for the current orientation
     */
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

    /**
     * {@link AppLauncher#reverseViews()}
     */
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

    /**
     * Register {@link BroadcastReceiver}s
     */
    private void registerObserversAndReceivers() {
        IntentFilter changeFilter = new IntentFilter();
        changeFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        changeFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        changeFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        changeFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        changeFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        changeFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        changeFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        changeFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        changeFilter.addAction(Intent.ACTION_SCREEN_OFF);
        changeFilter.addAction(Intent.ACTION_SCREEN_ON);

        actionChange = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                String action = intent.getAction();

                if (action.equals(Intent.ACTION_BATTERY_CHANGED) || action.equals(Intent.ACTION_POWER_CONNECTED) || action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
                    batteryValues();
                }
                if (action.equals(WifiManager.RSSI_CHANGED_ACTION) || action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                    wifiLevels();
                }

                if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION) || action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                    wifiLevels();
                    cellLevels();
                }

                if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
                    cellLevels();
                }

//                if (action.equals(Intent.ACTION_SCREEN_OFF)) {
//                    if (!wakeLock.isHeld()) wakeLock.acquire();
//                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) ((SignBoardService) getContext()).getViewPager().getLayoutParams();
//                    params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
//                    params.screenBrightness = 0;
//                    ((SignBoardService) getContext()).getViewPager().setLayoutParams(params);
//                    updateClockOnWakelock();
//                }
//
//                if (action.equals(Intent.ACTION_SCREEN_ON)) {
//                    if (wakeLock.isHeld()) wakeLock.release();
//                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) ((SignBoardService) getContext()).getViewPager().getLayoutParams();
//                    params.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
//                    params.screenBrightness = -1;
//                    ((SignBoardService) getContext()).getViewPager().setLayoutParams(params);
//                }

                showAirplaneModeIfNeeded();
            }
        };

        localReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                String action = intent.getAction() != null ? intent.getAction() : "";

                if (action.equals(Values.ACTION_NOTIFICATIONS_RECOMPILED)) {
                    ArrayList extra = intent.getExtras().getParcelableArrayList("notifications");
                    Log.e("MustardCorp Received", "Might be null");
                    if (extra != null) {
                        Log.e("MustardCorp Received", extra.toString());
                        ArrayList<Notification> notifications = new ArrayList<Notification>(extra);

                        mNotifsView.removeAllViews();

                        for (int i = 0; i < (notifications.size() > 6 ? 6 : notifications.size()); i++) { //we don't want to cause layout problems, so set the max displayed icons to 6
                            Notification notification = notifications.get(i);
                            Drawable icon = notification.getSmallIcon().loadDrawable(getContext());
                            ImageView imageView = new ImageView(getContext());

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            params.height = (int) Util.pxToDp(getContext(), 24);
                            params.width = (int) Util.pxToDp(getContext(), 24);

                            imageView.setLayoutParams(params);
                            imageView.setImageDrawable(icon);
                            imageView.setImageTintList(ColorStateList.valueOf(Settings.Global.getInt(getContext().getContentResolver(), "notification_icon_color", Color.WHITE)));

                            mNotifsView.addView(imageView);
                        }
                    }
                }
            }
        };
        IntentFilter localFilter = new IntentFilter();
        localFilter.addAction(Values.ACTION_NOTIFICATIONS_RECOMPILED);

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(localReceiver, localFilter);

        TelephonyManager manager = ((TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE));

        //Make the listener
        PhoneStateListener listener = new PhoneStateListener() {
            public void onDataConnectionStateChanged(int state, int networkType)
            {
                cellLevels();
            }
            public void onCellLocationChanged(CellLocation location) {
                cellLevels();
            }
        };

        try
        {
            //Add the listener made above into the telephonyManager
            manager.listen(listener,
                    PhoneStateListener.LISTEN_DATA_CONNECTION_STATE //connection changes 2G/3G/etc
                            | PhoneStateListener.LISTEN_CELL_LOCATION       //or tower/cell changes
            );
        } catch (SecurityException e) {
            requestCoarsePerm();
        }

        getContext().registerReceiver(actionChange, changeFilter);
    }

    /**
     * Set proper battery image and text to correspond with current battery level
     */
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
        batteryLevel.setTextColor(Settings.Global.getInt(getContext().getContentResolver(), "battery_color", Color.WHITE));
    }

    /**
     * If Airplane Mode is on, disable the WiFi and Mobile signal bars and show an airplane icon
     */
    private void showAirplaneModeIfNeeded() {
        boolean show = Settings.Global.getInt(getContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
        mView.findViewById(R.id.airplane_mode).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Set proper WiFi signal levels
     */
    private void wifiLevels() {
        WifiManager wm = (WifiManager)getContext().getSystemService(Context.WIFI_SERVICE);
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        int currentLevel = WifiManager.calculateSignalLevel(wm.getConnectionInfo().getRssi(), 5);

        ImageView wifiView = mView.findViewById(R.id.wifi_level);

        if (wifi.isConnected() && wifi.isAvailable())
        {
            int resId = R.drawable.ic_signal_wifi_0_bar_black_24dp;
            switch (currentLevel)
            {
                case 0:
                    resId = R.drawable.ic_signal_wifi_0_bar_black_24dp;
                    break;
                case 1:
                    resId = R.drawable.ic_signal_wifi_1_bar_black_24dp;
                    break;
                case 2:
                    resId = R.drawable.ic_signal_wifi_2_bar_black_24dp;
                    break;
                case 3:
                    resId = R.drawable.ic_signal_wifi_3_bar_black_24dp;
                    break;
                case 4:
                    resId = R.drawable.ic_signal_wifi_4_bar_black_24dp;
                    break;
            }
            wifiView.setImageDrawable(getContext().getResources().getDrawable(resId, null));
            wifiView.setColorFilter(Settings.Global.getInt(getContext().getContentResolver(), "wifi_signal_color", Color.WHITE));
            wifiView.setVisibility(View.VISIBLE);
        } else {
            wifiView.setImageDrawable(null);
            wifiView.setVisibility(View.GONE);
        }
    }

    /**
     * Set proper Mobile signal levels
     */
    private void cellLevels() {
        TelephonyManager tm = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        ImageView cellView = mView.findViewById(R.id.signal_level);

        try {
            if (tm.getAllCellInfo().size() > 0)
            {
                CellInfo info = tm.getAllCellInfo().get(0);
                int strength = 0;

                if (info instanceof CellInfoLte)
                    strength = ((CellInfoLte) info).getCellSignalStrength().getLevel();
                if (info instanceof CellInfoWcdma)
                    strength = ((CellInfoWcdma) info).getCellSignalStrength().getLevel();
                if (info instanceof CellInfoCdma)
                    strength = ((CellInfoCdma) info).getCellSignalStrength().getLevel();
                if (info instanceof CellInfoGsm)
                    strength = ((CellInfoGsm) info).getCellSignalStrength().getLevel();

                int resId = R.drawable.ic_signal_cellular_4_bar_black_24dp;
                if (tm.getSimState() == TelephonyManager.SIM_STATE_READY && Settings.Global.getInt(getContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 0)
                {
                    switch (strength)
                    {
                        case 0:
                            resId = info.isRegistered() ? R.drawable.ic_signal_cellular_0_bar_black_24dp : R.drawable.ic_signal_cellular_connected_no_internet_0_bar_black_24dp;
                            break;
                        case 1:
                            resId = info.isRegistered() ? R.drawable.ic_signal_cellular_1_bar_black_24dp : R.drawable.ic_signal_cellular_connected_no_internet_1_bar_black_24dp;
                            break;
                        case 2:
                            resId = info.isRegistered() ? R.drawable.ic_signal_cellular_2_bar_black_24dp : R.drawable.ic_signal_cellular_connected_no_internet_2_bar_black_24dp;
                            break;
                        case 3:
                            resId = info.isRegistered() ? R.drawable.ic_signal_cellular_3_bar_black_24dp : R.drawable.ic_signal_cellular_connected_no_internet_3_bar_black_24dp;
                            break;
                        case 4:
                            resId = info.isRegistered() ? R.drawable.ic_signal_cellular_4_bar_black_24dp : R.drawable.ic_signal_cellular_connected_no_internet_4_bar_black_24dp;
                            break;
                    }
                    cellView.setImageDrawable(getContext().getResources().getDrawable(resId, null));
                    cellView.setVisibility(View.VISIBLE);
                } else if (Settings.Global.getInt(getContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1)
                {
                    cellView.setImageDrawable(null);
                    cellView.setVisibility(View.GONE);
                } else if (tm.getSimState() != TelephonyManager.SIM_STATE_READY)
                {
                    cellView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_signal_cellular_no_sim_black_24dp, null));
                    cellView.setVisibility(View.VISIBLE);
                }
            } else {
                cellView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_signal_cellular_null_black_24dp, null));
            }
            cellView.setColorFilter(Settings.Global.getInt(getContext().getContentResolver(), "cell_signal_color", Color.WHITE));
        } catch (SecurityException e) {
            requestCoarsePerm();
        }
    }

    /**
     * If {@link Manifest.permission#ACCESS_COARSE_LOCATION} isn't granted, request it
     */
    private void requestCoarsePerm() {
        Intent reqPerms = new Intent(getContext(), RequestPermissionsActivity.class);
        reqPerms.putExtra("permission", Manifest.permission.ACCESS_COARSE_LOCATION);

        getContext().startActivity(reqPerms);
    }

    /**
     * (CURRENTLY NON-FUNCTIONAL)
     * Updates time in AOD mode
     */
    private void updateClockOnWakelock() {
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                final TextClock textClock = mView.findViewById(R.id.current_time);
                Calendar cal = Calendar.getInstance();
                while (wakeLock.isHeld()) {
                    Date currentLocalTime = cal.getTime();
                    DateFormat date = new SimpleDateFormat("h:mm a", Locale.US);
                    final String localTime = date.format(currentLocalTime);

                    new Handler(Looper.getMainLooper()).post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
//                            textClock.setText(localTime);
//                            PowerManager.WakeLock wakeLock = manager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "temp_wake");
//                            if (!textClock.getText().equals(localTime)) {
//                                wakeLock.acquire();
//                                wakeLock.release();
//
//                            }
                        }
                    });

                    try {
                        Log.e("MustardCorp WakeLock", "WakeLock Running");
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    /**
     * Listen for notification changes
     */
    public static class NotificationListener extends NotificationListenerService {
        private ArrayList<Notification> notifNames = new ArrayList<>();
        private BroadcastReceiver mReceiver;

        @Override
        public void onListenerConnected()
        {
            reAddNotifs();
            mReceiver = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    reAddNotifs();
                }
            };
            IntentFilter filter = new IntentFilter(Values.ACTION_INFORMATION_ADDED);

            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver, filter);
        }

        @Override
        public void onDestroy()
        {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
            super.onDestroy();
        }

        @Override
        public void onNotificationRankingUpdate(RankingMap rankingMap)
        {
            reAddNotifs();
            super.onNotificationRankingUpdate(rankingMap);
        }

        @Override
        public void onNotificationPosted(StatusBarNotification sbn)
        {
            reAddNotifs();
            super.onNotificationPosted(sbn);
        }

        @Override
        public void onNotificationRemoved(StatusBarNotification sbn)
        {
            reAddNotifs();
            super.onNotificationRemoved(sbn);
        }

        private void reAddNotifs() {
            StatusBarNotification[] notifs = getActiveNotifications();
            notifNames = new ArrayList<>();
            ArrayList<String> notifGroups = new ArrayList<>();

            outerloop:
            for (StatusBarNotification notification : notifs) {
                Notification notif = notification.getNotification();
                int importance = notif.priority;
                Log.e("MustardCorp", importance + "");

                try
                {
                    Class INotificationManager = Class.forName("android.app.INotificationManager");
                    Class INotificationManager$Stub = Class.forName("android.app.INotificationManager$Stub");
                    Method asInterface = INotificationManager$Stub.getMethod("asInterface", IBinder.class);
                    Class ServiceManager = Class.forName("android.os.ServiceManager");
                    Method getService = ServiceManager.getMethod("getService", String.class);
                    Method getImportance = INotificationManager.getMethod("getImportance", String.class, int.class);

                    Object notifService = getService.invoke(null, Context.NOTIFICATION_SERVICE);

                    Object manager = asInterface.invoke(null, notifService);

                    Method getUid = StatusBarNotification.class.getMethod("getUid");
                    Object uid = getUid.invoke(notification);

                    Integer imp = (Integer) getImportance.invoke(manager, notification.getPackageName(), uid);

                    Log.e("MustardCorp", Arrays.toString(getCurrentRanking().getOrderedKeys()));

                    Log.e("MustardCorp", imp + "");
                    Log.e("MustardCorp Group", notif.getGroup() + "GROUP");

                    if (notif.getGroup() != null && notifGroups.contains(notif.getGroup())) {
                        continue;
                    }

                    if (imp > 1) {
                        notifNames.add(notif);
                        notifGroups.add(notif.getGroup());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Intent intent = new Intent(Values.ACTION_NOTIFICATIONS_RECOMPILED);
            intent.putParcelableArrayListExtra("notifications", notifNames);

            Log.e("MustardCorp", "Sending BC");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }
}
