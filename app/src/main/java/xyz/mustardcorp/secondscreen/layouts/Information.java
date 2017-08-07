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
import android.content.res.ColorStateList;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.IBinder;
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
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import xyz.mustardcorp.secondscreen.R;
import xyz.mustardcorp.secondscreen.activities.RequestPermissionsActivity;
import xyz.mustardcorp.secondscreen.misc.Values;

import static android.content.Context.BATTERY_SERVICE;
import static android.content.Context.CONNECTIVITY_SERVICE;

public class Information extends BaseLayout
{
    private static LinearLayout mView;
    private static LinearLayout mNotifsView;
    private BroadcastReceiver actionChange;
    private Display display;
    private ArrayList<View> originalLayout = new ArrayList<>();
    private BroadcastReceiver localReceiver;

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
        wifiLevels();

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
        getContext().unregisterReceiver(actionChange);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(localReceiver);
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
        IntentFilter changeFilter = new IntentFilter();
        changeFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        changeFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        changeFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        changeFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        changeFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        changeFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        changeFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        changeFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);

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

    private void showAirplaneModeIfNeeded() {
        boolean show = Settings.Global.getInt(getContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
        mView.findViewById(R.id.airplane_mode).setVisibility(show ? View.VISIBLE : View.GONE);
    }

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
            wifiView.setImageTintList(ColorStateList.valueOf(Settings.Global.getInt(getContext().getContentResolver(), "wifi_signal_color", Color.WHITE)));
            wifiView.setVisibility(View.VISIBLE);
        } else {
            wifiView.setImageDrawable(null);
            wifiView.setVisibility(View.GONE);
        }
    }

    private void cellLevels() {
        TelephonyManager tm = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        ImageView cellView = mView.findViewById(R.id.signal_level);

        try {
            CellInfo info = tm.getAllCellInfo().get(0);
            int strength = 0;

            if (info instanceof CellInfoLte) strength = ((CellInfoLte) info).getCellSignalStrength().getLevel();
            if (info instanceof CellInfoWcdma) strength = ((CellInfoWcdma) info).getCellSignalStrength().getLevel();
            if (info instanceof CellInfoCdma) strength = ((CellInfoCdma) info).getCellSignalStrength().getLevel();
            if (info instanceof CellInfoGsm) strength = ((CellInfoGsm) info).getCellSignalStrength().getLevel();

            int resId = R.drawable.ic_signal_cellular_4_bar_black_24dp;
            if (tm.getSimState() == TelephonyManager.SIM_STATE_READY && Settings.Global.getInt(getContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 0) {
                switch (strength) {
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
            } else if (Settings.Global.getInt(getContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1) {
                cellView.setImageDrawable(null);
                cellView.setVisibility(View.GONE);
            } else if (tm.getSimState() != TelephonyManager.SIM_STATE_READY) {
                cellView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_signal_cellular_no_sim_black_24dp, null));
                cellView.setVisibility(View.VISIBLE);
            }

            cellView.setImageTintList(ColorStateList.valueOf(Settings.Global.getInt(getContext().getContentResolver(), "cell_signal_color", Color.WHITE)));
        } catch (SecurityException e) {
            requestCoarsePerm();
        }
    }

    private void requestCoarsePerm() {
        Intent reqPerms = new Intent(getContext(), RequestPermissionsActivity.class);
        reqPerms.putExtra("permission", Manifest.permission.ACCESS_COARSE_LOCATION);

        getContext().startActivity(reqPerms);
    }

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

                    if (imp > 1) {
                        notifNames.add(notif);
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
