package xyz.mustardcorp.secondscreen.misc;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.provider.Settings;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.Arrays;

public class Util
{
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int getNavBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static float pxToDp(Context context, int px) {
        Resources r = context.getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, r.getDisplayMetrics());
    }

    public static ArrayList<String> parseSavedViews(Context context, ArrayList<String> def) {
        String load = Settings.Global.getString(context.getContentResolver(), "saved_views");

        if (load == null || load.isEmpty()) return def;
        else {
            return new ArrayList<>(Arrays.asList(load.split("[,]")));
        }
    }

    public static void saveViews(Context context, ArrayList<String> viewsList) {
        StringBuilder builder = new StringBuilder();

        for (String view : viewsList) {
            if (builder.length() < 1) builder.append(view);
            else builder.append(",").append(view);
        }

        Settings.Global.putString(context.getContentResolver(), "saved_views", builder.toString());
    }

    public static boolean openApp(Context context, String packageName) {
        PackageManager manager = context.getPackageManager();
        try {
            Intent i = manager.getLaunchIntentForPackage(packageName);
            if (i == null) {
                return false;
                //throw new PackageManager.NameNotFoundException();
            }
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            context.startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
