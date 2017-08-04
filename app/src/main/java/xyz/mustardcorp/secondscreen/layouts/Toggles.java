package xyz.mustardcorp.secondscreen.layouts;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.ColorInt;
import android.util.ArraySet;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import xyz.mustardcorp.secondscreen.R;
import xyz.mustardcorp.secondscreen.misc.FlashlightController;

import static android.content.Context.WIFI_SERVICE;

public class Toggles
{
    public static final String SOUND_TOGGLE = "sound";
    public static final String WIFI_TOGGLE = "wifi";
    public static final String FLASHLIGHT_TOGGLE = "flashlight";
    public static final String BLUETOOTH_TOGGLE = "bluetooth";
    public static final int SIZE = 150;

    private LinearLayout.LayoutParams mParams = new LinearLayout.LayoutParams(SIZE, SIZE, 1);

    private Context mContext;
    private LinearLayout mView;
    private BroadcastReceiver mWiFiBC;
    private BroadcastReceiver mSoundBC;
    private BroadcastReceiver mBluetoothBC;

    private static final ArrayList<String> defaultOrder = new ArrayList<String>() {{
        add(SOUND_TOGGLE);
        add(WIFI_TOGGLE);
        add(FLASHLIGHT_TOGGLE);
        add(BLUETOOTH_TOGGLE);
    }};

    private ImageView bluetooth;
    private ImageView flash;
    private ImageView sound;
    private ImageView wifi;

    private ContentObserver mWiFiObserver;
    private ContentObserver mSoundObserver;
    private ContentObserver mFlashObserver;
    private ContentObserver mBluetoothObserver;

    public Toggles(Context context) {
        mContext = context;
        mView = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.layout_toggles, null, false);
        mView.setLayoutDirection(LinearLayout.LAYOUT_DIRECTION_RTL);
        mParams.gravity = Gravity.CENTER;

        addInSetOrder();
        registerContentObservers();
        setOrientationListener();
    }

    public LinearLayout getView() {
        return mView;
    }

    public void onDestroy() {
        if (mWiFiBC != null) mContext.unregisterReceiver(mWiFiBC);
        if (mSoundBC != null) mContext.unregisterReceiver(mSoundBC);
        if (mBluetoothBC != null) mContext.unregisterReceiver(mBluetoothBC);
        mContext.getContentResolver().unregisterContentObserver(mWiFiObserver);
        mContext.getContentResolver().unregisterContentObserver(mSoundObserver);
        mContext.getContentResolver().unregisterContentObserver(mFlashObserver);
        mContext.getContentResolver().unregisterContentObserver(mBluetoothObserver);
    }

    private void setOrientationListener() {
        OrientationEventListener listener = new OrientationEventListener(mContext)
        {
            @Override
            public void onOrientationChanged(int i)
            {
                Display display = ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

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
            }
        };
        listener.enable();
    }

    private void setNormalOrientation() {
        ViewGroup.LayoutParams params = mView.getLayoutParams();

        params.height = 160;
        params.width = 1040;

        mView.setLayoutParams(params);
        mView.setOrientation(LinearLayout.HORIZONTAL);
        mView.requestLayout();
    }

    private void setHorizontalOrientation() {
        ViewGroup.LayoutParams params = mView.getLayoutParams();

        params.height = 1040;
        params.width = 160;

        mView.setLayoutParams(params);
        mView.setOrientation(LinearLayout.VERTICAL);
        mView.requestLayout();
    }

    private void addInSetOrder() {
        ArrayList<String> set = defaultOrder;

        for (String name : set)
        {
            switch (name)
            {
                case WIFI_TOGGLE:
                    setWiFiState();
                    break;
                case SOUND_TOGGLE:
                    setSoundState();
                    break;
                case FLASHLIGHT_TOGGLE:
                    setFlashlightState();
                    break;
                case BLUETOOTH_TOGGLE:
                    setBluetoothState();
                    break;
            }
        }
    }

    private void setWiFiState() {
        final WifiManager wm = (WifiManager) mContext.getSystemService(WIFI_SERVICE);

        wifi = new ImageView(mContext);
        mView.addView(wifi, 0);
        wifi.setLayoutParams(mParams);

        setWiFiColor();
        setWiFiToggleState(wifi);

        mWiFiBC = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                setWiFiToggleState(wifi);
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");

        mContext.registerReceiver(mWiFiBC, intentFilter);

        wifi.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                wm.setWifiEnabled(!wm.isWifiEnabled());
            }
        });
    }

    private void setWiFiColor() {
        int pref = Settings.Global.getInt(mContext.getContentResolver(), "wifi_color", 0xffffff);
        @ColorInt int color = Color.rgb(Color.red(pref), Color.green(pref), Color.blue(pref));
        wifi.setImageTintList(ColorStateList.valueOf(color));
    }

    private void setWiFiToggleState(ImageView wifi) {
        final WifiManager wm = (WifiManager) mContext.getSystemService(WIFI_SERVICE);

        if (wm.isWifiEnabled()) {
            wifi.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_signal_wifi_4_bar_black_24dp, null));
        } else {
            wifi.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_signal_wifi_off_black_24dp, null));
        }
    }

    private void setSoundState() {
        final AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        sound = new ImageView(mContext);
        mView.addView(sound, 0);
        sound.setLayoutParams(mParams);

        setSoundColor();
        setSoundToggleState(sound);

        mSoundBC = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                setSoundToggleState(sound);
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);

        mContext.registerReceiver(mSoundBC, intentFilter);

        sound.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                switch (am.getRingerMode()) {
                    case AudioManager.RINGER_MODE_SILENT:
                        am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                        break;
                    case AudioManager.RINGER_MODE_VIBRATE:
                        am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                        break;
                    case AudioManager.RINGER_MODE_NORMAL:
                        am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                        break;
                }
            }
        });
    }

    private void setSoundColor() {
        int pref = Settings.Global.getInt(mContext.getContentResolver(), "sound_color", 0xffffff);
        @ColorInt int color = Color.rgb(Color.red(pref), Color.green(pref), Color.blue(pref));
        sound.setImageTintList(ColorStateList.valueOf(color));
    }

    private void setSoundToggleState(ImageView sound) {
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        switch (am.getRingerMode()) {
            case AudioManager.RINGER_MODE_SILENT:
                sound.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_volume_mute_black_24dp, null));
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                sound.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_vibration_black_24dp, null));
                break;
            case AudioManager.RINGER_MODE_NORMAL:
                sound.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_volume_up_black_24dp, null));
                break;
        }
    }

    private void setFlashlightState() {
        flash = new ImageView(mContext);
        mView.addView(flash, 0);
        flash.setLayoutParams(mParams);

        final FlashlightController controller = new FlashlightController(mContext);
        controller.addListener(new FlashlightController.FlashlightListener()
        {
            @Override
            public void onFlashlightChanged(boolean enabled)
            {
                new Handler(mContext.getMainLooper()).post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        setFlashlightToggleState(flash, controller);
                    }
                });
            }

            @Override
            public void onFlashlightError()
            {

            }

            @Override
            public void onFlashlightAvailabilityChanged(boolean available)
            {

            }
        });

        setFlashlightColor();
        setFlashlightToggleState(flash, controller);

        flash.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (controller.isEnabled()) {
                    controller.setFlashlight(false);
                } else {
                    controller.setFlashlight(true);
                }
            }
        });
    }

    private void setFlashlightColor() {
        int pref = Settings.Global.getInt(mContext.getContentResolver(), "flash_color", 0xffffff);
        @ColorInt int color = Color.rgb(Color.red(pref), Color.green(pref), Color.blue(pref));
        flash.setImageTintList(ColorStateList.valueOf(color));
    }

    private void setFlashlightToggleState(ImageView flashlight, FlashlightController controller) {
        if (controller.isEnabled()) {
            flashlight.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_flash_on_black_24dp, null));
        } else {
            flashlight.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_flash_off_black_24dp, null));
        }
    }

    private void setBluetoothState() {
        bluetooth = new ImageView(mContext);
        mView.addView(bluetooth, 0);
        bluetooth.setLayoutParams(mParams);

        final BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();

        mBluetoothBC = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                setBluetoothToggleState(bluetooth);
            }
        };

        setBluetoothColor();
        setBluetoothToggleState(bluetooth);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        mContext.registerReceiver(mBluetoothBC, filter);

        bluetooth.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (ba.isEnabled()) ba.disable();
                else ba.enable();
            }
        });
    }

    private void setBluetoothColor() {
        int pref = Settings.Global.getInt(mContext.getContentResolver(), "bt_color", 0xffffff);
        @ColorInt int color = Color.rgb(Color.red(pref), Color.green(pref), Color.blue(pref));
        bluetooth.setImageTintList(ColorStateList.valueOf(color));
    }

    private void setBluetoothToggleState(ImageView bluetooth) {
        final BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();

        if (ba.isEnabled()) {
            bluetooth.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_bluetooth_black_24dp, null));
        } else {
            bluetooth.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_bluetooth_disabled_black_24dp, null));
        }
    }

    private void registerContentObservers() {
        Handler handler = new Handler();

        mWiFiObserver = new ContentObserver(handler)
        {
            @Override
            public void onChange(boolean selfChange)
            {
                setWiFiColor();
                super.onChange(selfChange);
            }
        };

        mSoundObserver = new ContentObserver(handler)
        {
            @Override
            public void onChange(boolean selfChange)
            {
                setSoundColor();
                super.onChange(selfChange);
            }
        };

        mFlashObserver = new ContentObserver(handler)
        {
            @Override
            public void onChange(boolean selfChange)
            {
                setFlashlightColor();
                super.onChange(selfChange);
            }
        };

        mBluetoothObserver = new ContentObserver(handler)
        {
            @Override
            public void onChange(boolean selfChange)
            {
                setBluetoothColor();
                super.onChange(selfChange);
            }
        };

        mContext.getContentResolver().registerContentObserver(Settings.Global.CONTENT_URI, true, mWiFiObserver);
        mContext.getContentResolver().registerContentObserver(Settings.Global.CONTENT_URI, true, mSoundObserver);
        mContext.getContentResolver().registerContentObserver(Settings.Global.CONTENT_URI, true, mFlashObserver);
        mContext.getContentResolver().registerContentObserver(Settings.Global.CONTENT_URI, true, mBluetoothObserver);
    }
}
