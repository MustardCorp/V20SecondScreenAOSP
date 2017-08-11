package xyz.mustardcorp.secondscreen.layouts;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;

import xyz.mustardcorp.secondscreen.R;
import xyz.mustardcorp.secondscreen.activities.AddAppShortcutActivity;
import xyz.mustardcorp.secondscreen.services.SignBoardService;

import static xyz.mustardcorp.secondscreen.misc.Util.openApp;

/**
 * QuickLaunch for Android essentially
 * A set of customizable launcher icons
 */

public class AppLauncher extends BaseLayout implements View.OnClickListener, View.OnLongClickListener
{
    public static final String APP_1 = "app_1_id";
    public static final String APP_2 = "app_2_id";
    public static final String APP_3 = "app_3_id";
    public static final String APP_4 = "app_4_id";
    public static final String APP_5 = "app_5_id";
    public static final String APP_6 = "app_6_id";

    public static final String APP_ID = "app_id";

    public static final int APP_KEY = R.string.app_key;
    public static final int DEF_KEY = R.string.def_key;

    private Context mContext;
    private LinearLayout mView;
    private Display display;
    private ArrayList<View> originalView = new ArrayList<>();
    private ContentObserver stateObserver;

    private Handler mHandler;

    /**
     * Create new instance of this class, and prepare all views, taking account of current orientation
     * @param context of the caller
     */
    public AppLauncher(Context context) {
        super(context);
        mContext = context;
        mView = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.layout_apps, null, false);
        display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        mHandler = SignBoardService.mAppLauncherHandler;

        for (int i = 0; i < mView.getChildCount(); i++) {
            originalView.add(mView.getChildAt(i));
        }

        int rotation = display.getRotation();
        switch (rotation) {
            case Surface.ROTATION_270:
            case Surface.ROTATION_90:
                mView.setOrientation(LinearLayout.VERTICAL);
                break;
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                mView.setOrientation(LinearLayout.HORIZONTAL);
                break;
        }

        reverseViews();
        setOrientationListener();
        listenForAppChange();
        addIcons();
    }

    public LinearLayout getView() {
        return mView;
    }

    public void onDestroy() {
        mContext.getContentResolver().unregisterContentObserver(stateObserver);
    }

    public void setOrientationListener() {
        OrientationEventListener listener = new OrientationEventListener(mContext)
        {
            @Override
            public void onOrientationChanged(int i)
            {
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

                reverseViews();
            }
        };
        listener.enable();
    }

    /**
     * Register {@link ContentObserver} on Settings.Global and listen for relevant changes
     */
    private void listenForAppChange() {
        stateObserver = new ContentObserver(null)
        {
            @Override
            public void onChange(boolean selfChange, final Uri uri)
            {
                Uri app1 = Settings.Global.getUriFor(APP_1);
                Uri app2 = Settings.Global.getUriFor(APP_2);
                Uri app3 = Settings.Global.getUriFor(APP_3);
                Uri app4 = Settings.Global.getUriFor(APP_4);
                Uri app5 = Settings.Global.getUriFor(APP_5);
                Uri app6 = Settings.Global.getUriFor(APP_6);

                if (uri.equals(app1) || uri.equals(app2) || uri.equals(app3) || uri.equals(app4) || uri.equals(app5) || uri.equals(app6)) {
                    addIcons();
                }
            }
        };

        mContext.getContentResolver().registerContentObserver(Settings.Global.CONTENT_URI, true, stateObserver);
    }

    /**
     * When orientation is vertical
     */
    private void setNormalOrientation() {
        ViewGroup.LayoutParams params = mView.getLayoutParams();

        params.height = 160;
        params.width = 1040;

        mView.setLayoutParams(params);
        mView.setOrientation(LinearLayout.HORIZONTAL);
        mView.requestLayout();
    }

    /**
     * When orientation is horizontal
     */
    private void setHorizontalOrientation() {
        ViewGroup.LayoutParams params = mView.getLayoutParams();

        params.height = 1040;
        params.width = 160;


        mView.setLayoutParams(params);
        mView.setOrientation(LinearLayout.VERTICAL);
        mView.requestLayout();
    }

    /**
     * With the top of the device on the left, the child views need to be reversed for consistency
     */
    private void reverseViews() {
        ArrayList<View> views = originalView;

        mView.removeAllViews();

        if (display.getRotation() == Surface.ROTATION_90)
        {
            for (int x = views.size() - 1; x >= 0; x--)
            {
                mView.addView(views.get(x));
            }
        } else {
            for (int i = 0; i < views.size(); i++) {
                mView.addView(views.get(i));
            }
        }
    }

    /**
     * Add icons and listeners to each view
     */
    private void addIcons() {
        String a1 = Settings.Global.getString(mContext.getContentResolver(), APP_1);
        String a2 = Settings.Global.getString(mContext.getContentResolver(), APP_2);
        String a3 = Settings.Global.getString(mContext.getContentResolver(), APP_3);
        String a4 = Settings.Global.getString(mContext.getContentResolver(), APP_4);
        String a5 = Settings.Global.getString(mContext.getContentResolver(), APP_5);
        String a6 = Settings.Global.getString(mContext.getContentResolver(), APP_6);

        ImageView app1 = mView.findViewById(R.id.app_1_id);
        app1.setTag(DEF_KEY, APP_1);
        ImageView app2 = mView.findViewById(R.id.app_2_id);
        app2.setTag(DEF_KEY, APP_2);
        ImageView app3 = mView.findViewById(R.id.app_3_id);
        app3.setTag(DEF_KEY, APP_3);
        ImageView app4 = mView.findViewById(R.id.app_4_id);
        app4.setTag(DEF_KEY, APP_4);
        ImageView app5 = mView.findViewById(R.id.app_5_id);
        app5.setTag(DEF_KEY, APP_5);
        ImageView app6 = mView.findViewById(R.id.app_6_id);
        app6.setTag(DEF_KEY, APP_6);

        if (a1 != null && !a1.isEmpty()) {
            try {
                app1.setImageDrawable(mContext.getPackageManager().getApplicationIcon(a1));
                app1.setTag(APP_KEY, a1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            app1.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_add_circle_white_24dp, null));
            app1.setTag(APP_KEY, null);
        }

        if (a2 != null && !a2.isEmpty()) {
            try {
                app2.setImageDrawable(mContext.getPackageManager().getApplicationIcon(a2));
                app2.setTag(APP_KEY, a2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            app2.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_add_circle_white_24dp, null));
            app2.setTag(APP_KEY, null);
        }

        if (a3 != null && !a3.isEmpty()) {
            try {
                app3.setImageDrawable(mContext.getPackageManager().getApplicationIcon(a3));
                app3.setTag(APP_KEY, a3);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            app3.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_add_circle_white_24dp, null));
            app3.setTag(APP_KEY, null);
        }

        if (a4 != null && !a4.isEmpty()) {
            try {
                app4.setImageDrawable(mContext.getPackageManager().getApplicationIcon(a4));
                app4.setTag(APP_KEY, a4);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            app4.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_add_circle_white_24dp, null));
            app4.setTag(APP_KEY, null);
        }

        if (a5 != null && !a5.isEmpty()) {
            try {
                app5.setImageDrawable(mContext.getPackageManager().getApplicationIcon(a5));
                app5.setTag(APP_KEY, a5);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            app5.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_add_circle_white_24dp, null));
            app5.setTag(APP_KEY, null);
        }

        if (a6 != null && !a6.isEmpty()) {
            try {
                app6.setImageDrawable(mContext.getPackageManager().getApplicationIcon(a6));
                app6.setTag(APP_KEY, a6);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            app6.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_add_circle_white_24dp, null));
            app6.setTag(APP_KEY, null);
        }

        app1.setOnClickListener(this);
        app2.setOnClickListener(this);
        app3.setOnClickListener(this);
        app4.setOnClickListener(this);
        app5.setOnClickListener(this);
        app6.setOnClickListener(this);

        app1.setOnLongClickListener(this);
        app2.setOnLongClickListener(this);
        app3.setOnLongClickListener(this);
        app4.setOnLongClickListener(this);
        app5.setOnLongClickListener(this);
        app6.setOnLongClickListener(this);

    }

    @Override
    public void onClick(View view)
    {
        if (view.getTag(APP_KEY) != null) {
            openApp(mContext, view.getTag(APP_KEY).toString());
        } else if (view.getTag(DEF_KEY) != null) {
            Intent intent = new Intent(mContext, AddAppShortcutActivity.class);
            intent.putExtra(APP_ID, view.getTag(DEF_KEY).toString());
            mContext.startActivity(intent);
        }
    }

    @Override
    public boolean onLongClick(View view)
    {
        if (view.getTag(DEF_KEY) != null) {
            Intent intent = new Intent(mContext, AddAppShortcutActivity.class);
            intent.putExtra(APP_ID, view.getTag(DEF_KEY).toString());
            mContext.startActivity(intent);
            return true;
        } else return false;
    }
}
