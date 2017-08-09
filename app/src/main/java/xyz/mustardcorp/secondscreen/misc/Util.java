package xyz.mustardcorp.secondscreen.misc;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.TypedValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import xyz.mustardcorp.secondscreen.R;

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

    public static void removeView(Context context, String key) {
        ArrayList<String> saved = parseSavedViews(context, Values.defaultLoad);
        if (saved.contains(key)) saved.remove(key);
        saveViews(context, saved);
    }

    public static void addViewIfNeeded(Context context, String key, int index) {
        ArrayList<String> saved = parseSavedViews(context, Values.defaultLoad);
        if (!saved.contains(key)) saved.add(index, key);
        saveViews(context, saved);
    }

    public static void saveViews(Context context, ArrayList<String> viewsList) {
        StringBuilder builder = new StringBuilder(viewsList.get(0));

        for (int i = 1; i < viewsList.size(); i++) {
            String view = viewsList.get(i);
            builder.append(",").append(view);
        }

        Settings.Global.putString(context.getContentResolver(), "saved_views", builder.toString());
    }

    public static boolean isEnabled(Context context, String key) {
        String load = Settings.Global.getString(context.getContentResolver(), "saved_views");

        return isEmptyNull(context, load) || load.contains(key);
    }

    public static boolean isEmptyNull(Context context, String load) {
        return load == null || load.isEmpty();
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

    public static InputStream openDisplayPhoto(Context context, long contactId) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] {ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
        if (cursor == null) {
            return returnDefaultContact(context);
        }
        try {
            if (cursor.moveToFirst()) {
                byte[] data = cursor.getBlob(0);
                if (data != null) {
                    return new ByteArrayInputStream(data);
                }
            }
        } finally {
            cursor.close();
        }
        return returnDefaultContact(context);
    }

    private static InputStream returnDefaultContact(Context context) {
        Drawable d = context.getResources().getDrawable(R.drawable.ic_help_white_24dp, null);
        Bitmap bitmap = Bitmap.createBitmap(d.getIntrinsicWidth(),
                d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        d.draw(canvas);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] imageInByte = stream.toByteArray();
        return new ByteArrayInputStream(imageInByte);
    }
}
