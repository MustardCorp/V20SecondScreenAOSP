package xyz.mustardcorp.secondscreen.misc;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

public class Util
{
    /**
     * Get the current height of the statusbar
     * @param context caller's context
     * @return height in pixels
     */
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * Get the current height of the navbar
     * @param context caller's context
     * @return height in pixels
     */
    public static int getNavBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * Convert pixels to DP
     * @param context caller's context
     * @param px pixels to convert
     * @return corresponding DP value
     */
    public static float pxToDp(Context context, int px) {
        Resources r = context.getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, r.getDisplayMetrics());
    }

    /**
     * Get saved views from Settings.Global
     * @param context caller's context
     * @param def default return
     * @return either the custom list of pages or the def list
     */
    public static ArrayList<String> parseSavedViews(Context context, ArrayList<String> def) {
        String load = Settings.Global.getString(context.getContentResolver(), "saved_views");

        if (load == null || load.isEmpty()) return def;
        else {
            return new ArrayList<>(Arrays.asList(load.split("[,]")));
        }
    }

    /**
     * Remove a page from the saved_views if it exists
     * @param context caller's context
     * @param key of page (EG: {@link Values#TOGGLES_KEY}
     */
    public static void removeView(Context context, String key) {
        ArrayList<String> saved = parseSavedViews(context, Values.defaultLoad);
        if (saved.contains(key)) saved.remove(key);
        saveViews(context, saved);
    }

    /**
     * Add a page to the saved_views if needed
     * @param context caller's context
     * @param key of page (EG: {@link Values#TOGGLES_KEY}
     * @param index where to add the page
     */
    public static void addViewIfNeeded(Context context, String key, int index) {
        ArrayList<String> saved = parseSavedViews(context, Values.defaultLoad);
        if (!saved.contains(key)) saved.add(index, key);
        saveViews(context, saved);
    }

    /**
     * Save list of pages to Settings.Global
     * @param context caller's context
     * @param viewsList list of pages to save
     */
    public static void saveViews(Context context, ArrayList<String> viewsList) {
        StringBuilder builder = new StringBuilder(viewsList.get(0));

        for (int i = 1; i < viewsList.size(); i++) {
            String view = viewsList.get(i);
            builder.append(",").append(view);
        }

        Settings.Global.putString(context.getContentResolver(), "saved_views", builder.toString());
    }

    /**
     * Check if page is enabled
     * @param context caller's context
     * @param key of page (EG: {@link Values#TOGGLES_KEY}
     * @return whether or not page is enabled
     */
    public static boolean isEnabled(Context context, String key) {
        String load = Settings.Global.getString(context.getContentResolver(), "saved_views");

        return isEmptyNull(load) || load.contains(key);
    }

    /**
     * Whether input String is empty/null
     * @param load input
     * @return is empty or null
     */
    public static boolean isEmptyNull(String load) {
        return load == null || load.isEmpty();
    }

    /**
     * Open app by specified package name/ID
     * @param context caller's context
     * @param packageName desired app's package name
     * @return success
     */
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


    /**
     * Get contact image by ID
     * @param context caller's context
     * @param contactId ID of contact
     * @return InputStream containing contact's avatar
     */
    public static InputStream openDisplayPhoto(Context context, long contactId) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);

        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] {ContactsContract.Contacts.Photo.PHOTO}, null, null, null);

        Cursor name = context.getContentResolver().query(contactUri,
                new String[] {ContactsContract.Contacts.DISPLAY_NAME}, null, null, null);

        if (cursor == null) {
            String string = "?";

            if (name.moveToFirst()) {
                byte[] data = name.getBlob(0);
                if (data != null) {
                    ByteArrayInputStream in = new ByteArrayInputStream(data);

                    int n = in.available();
                    byte[] bytes = new byte[n];
                    in.read(bytes, 0, n);
                    string = new String(bytes, StandardCharsets.UTF_8); // Or any encoding.
                }
            }

            return returnDefaultContact(context, string, contactId);
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

        String string = "?";

        if (name.moveToFirst()) {
            byte[] data = name.getBlob(0);
            if (data != null) {
                ByteArrayInputStream in = new ByteArrayInputStream(data);

                int n = in.available();
                byte[] bytes = new byte[n];
                in.read(bytes, 0, n);
                string = new String(bytes, StandardCharsets.UTF_8); // Or any encoding.
            }
        }

        return returnDefaultContact(context, string, contactId);
    }

    /**
     * Create or load a generic colored image based on the contact's name
     * @param context caller's context
     * @param name contact's name
     * @param id contact's ID
     * @return InputStream containing generic contact avatar
     */
    private static InputStream returnDefaultContact(Context context, String name, long id) {
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(80);

        int color = PreferenceManager.getDefaultSharedPreferences(context).getInt("contact_by_id_" + id, 0);

        if (color == 0) {
            int colorValue1 = (int)((56 + Math.random() * 200));
            int colorValue2 = (int)((56 + Math.random() * 200));
            int colorValue3 = (int)((56 + Math.random() * 200));

            color = Color.rgb(colorValue1, colorValue2, colorValue3);

            PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("contact_by_id_" + id, color).apply();
        }

        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(color);

        Bitmap bitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);

        int xPos = (canvas.getWidth() / 2);
        int yPos = (int) ((canvas.getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)) ;

        canvas.drawText(name.substring(0, 1), xPos, yPos, textPaint);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] imageInByte = stream.toByteArray();

        return new ByteArrayInputStream(imageInByte);
    }

    /**
     * Retrieve and compile a map of contacts, sorted by name
     * @param context caller's context
     * @return sorted map of contacts
     */
    public static TreeMap<String, Contact> compileContactsList(Context context) {
        TreeMap<String, Contact> contacts = new TreeMap<>();

        Cursor phones = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null,null, "display_name");

        while (phones.moveToNext())
        {
            String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            String id = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));

            Contact contact = new Contact();
            contact.name = name;
            contact.number = phoneNumber;
            contact.id = Long.decode(id);

            InputStream inputStream = openDisplayPhoto(context, contact.id);
            contact.avatar = Drawable.createFromStream(inputStream, "avatar");

            contacts.put(contact.name, contact);
        }
        phones.close();

        return contacts;
    }
}
