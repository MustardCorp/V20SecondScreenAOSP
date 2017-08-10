package xyz.mustardcorp.secondscreen.misc;

import java.util.ArrayList;

public class Values
{
    public static final String KILL_BC_ACTION = "xyz.mustardcorp.secondscreen.action.SERVICE_KILLED";
    public static final String ACTION_INFORMATION_ADDED = "xyz.mustardcorp.secondscreen.action.INFO_ADDED";
    public static final String ACTION_NOTIFICATIONS_RECOMPILED = "xyz.mustardcorp.secondscreen.action.NOTIFS_REDONE";
    public static final String SHOULD_FORCE_START = "should_force_start";

    public static String INFO_KEY = "info";
    public static String TOGGLES_KEY = "toggles";
    public static String MUSIC_KEY = "music";
    public static String APPS_KEY = "launcher";
    public static String RECENTS_KEY = "recents";
    public static String CONTACTS_KEY = "contacts";

    public static int INFO_ID = 1;
    public static int TOGGLES_ID = 2;
    public static int MUSIC_ID = 3;
    public static int APPS_ID = 4;
    public static int RECENTS_ID = 5;
    public static int CONTACTS_ID = 6;

    public static int SOUND_ID = INFO_ID;
    public static int WIFI_ID = TOGGLES_ID;
    public static int FLASHLIGHT_ID = MUSIC_ID;
    public static int BT_ID = APPS_ID;
    public static int AIRPLANE_ID = RECENTS_ID;

    public static final String SOUND_TOGGLE = "sound";
    public static final String WIFI_TOGGLE = "wifi";
    public static final String FLASHLIGHT_TOGGLE = "flashlight";
    public static final String BLUETOOTH_TOGGLE = "bluetooth";
    public static final String AIRPLANE_TOGGLE = "airplane";

    public static final String SOUND_KEY = "sound_color";
    public static final String WIFI_KEY = "wifi_color";
    public static final String FLASHLIGHT_KEY = "flash_color";
    public static final String BT_KEY = "bt_color";
    public static final String AIRPLANE_KEY = "airplane_color";

    /**
     * Default order and state of pages
     */
    public static ArrayList<String> defaultLoad = new ArrayList<String>() {{
        add(INFO_KEY);
        add(TOGGLES_KEY);
        add(APPS_KEY);
//        add(MUSIC_KEY);
//        add(RECENTS_KEY);
        add(CONTACTS_KEY);
    }};

    public static final ArrayList<String> defaultToggleOrder = new ArrayList<String>() {{
        add(SOUND_TOGGLE);
        add(WIFI_TOGGLE);
        add(FLASHLIGHT_TOGGLE);
        add(BLUETOOTH_TOGGLE);
        add(AIRPLANE_TOGGLE);
    }};
}
