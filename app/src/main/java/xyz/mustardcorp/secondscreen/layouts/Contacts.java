package xyz.mustardcorp.secondscreen.layouts;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
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
import xyz.mustardcorp.secondscreen.activities.AddContactActivity;
import xyz.mustardcorp.secondscreen.services.SignBoardService;

import static xyz.mustardcorp.secondscreen.misc.Util.openDisplayPhoto;

/**
 * Set of customizable contacts
 * Refer to {@link AppLauncher} for comments (similar class)
 */

public class Contacts extends BaseLayout implements View.OnClickListener, View.OnLongClickListener
{
    public static final String CONTACT_ID = "contact_id";

    public static final int DEF_KEY = R.string.def_key;
    public static final int CONTACT_KEY = R.string.contact_key;

    public static final String CONTACT_1 = "contact_1";
    public static final String CONTACT_2 = "contact_2";
    public static final String CONTACT_3 = "contact_3";
    public static final String CONTACT_4 = "contact_4";
    public static final String CONTACT_5 = "contact_5";
    public static final String CONTACT_6 = "contact_6";
    public static final String CONTACT_7 = "contact_7";
    public static final String CONTACT_8 = "contact_8";

    ArrayList<View> originalView = new ArrayList<>();

    private LinearLayout mView;
    private final Display display;
    private ContentObserver stateObserver;

    private Handler mHandler;

    public Contacts(Context context) {
        super(context);
        mView = (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.layout_contacts, null, false);
        display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        mHandler = SignBoardService.mContactsHandler;

        for (int i = 0; i < mView.getChildCount(); i++) {
            originalView.add(mView.getChildAt(i));
        }

        addIcons();
        listenForContactChange();
        setOrientationListener();
    }

    @Override
    public View getView()
    {
        return mView;
    }

    @Override
    public void onDestroy()
    {
        getContext().getContentResolver().unregisterContentObserver(stateObserver);
    }

    public void setOrientationListener() {
        OrientationEventListener listener = new OrientationEventListener(getContext())
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

    private void listenForContactChange() {
        stateObserver = new ContentObserver(null)
        {
            @Override
            public void onChange(boolean selfChange, final Uri uri)
            {
                Uri contact1 = Settings.Global.getUriFor(CONTACT_1);
                Uri contact2 = Settings.Global.getUriFor(CONTACT_2);
                Uri contact3 = Settings.Global.getUriFor(CONTACT_3);
                Uri contact4 = Settings.Global.getUriFor(CONTACT_4);
                Uri contact5 = Settings.Global.getUriFor(CONTACT_5);
                Uri contact6 = Settings.Global.getUriFor(CONTACT_6);
                Uri contact7 = Settings.Global.getUriFor(CONTACT_7);
                Uri contact8 = Settings.Global.getUriFor(CONTACT_8);

                if (uri.equals(contact1) ||
                        uri.equals(contact2) ||
                        uri.equals(contact3) ||
                        uri.equals(contact4) ||
                        uri.equals(contact5) ||
                        uri.equals(contact6) ||
                        uri.equals(contact7) ||
                        uri.equals(contact8)) {
                    addIcons();
                }
            }
        };

        getContext().getContentResolver().registerContentObserver(Settings.Global.CONTENT_URI, true, stateObserver);
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

    private void addIcons() {
        String a1 = Settings.Global.getString(getContext().getContentResolver(), CONTACT_1);
        String a2 = Settings.Global.getString(getContext().getContentResolver(), CONTACT_2);
        String a3 = Settings.Global.getString(getContext().getContentResolver(), CONTACT_3);
        String a4 = Settings.Global.getString(getContext().getContentResolver(), CONTACT_4);
        String a5 = Settings.Global.getString(getContext().getContentResolver(), CONTACT_5);
        String a6 = Settings.Global.getString(getContext().getContentResolver(), CONTACT_6);
        String a7 = Settings.Global.getString(getContext().getContentResolver(), CONTACT_7);
        String a8 = Settings.Global.getString(getContext().getContentResolver(), CONTACT_8);

        ImageView contact1 = mView.findViewById(R.id.contact_1);
        contact1.setTag(DEF_KEY, CONTACT_1);
        ImageView contact2 = mView.findViewById(R.id.contact_2);
        contact2.setTag(DEF_KEY, CONTACT_2);
        ImageView contact3 = mView.findViewById(R.id.contact_3);
        contact3.setTag(DEF_KEY, CONTACT_3);
        ImageView contact4 = mView.findViewById(R.id.contact_4);
        contact4.setTag(DEF_KEY, CONTACT_4);
        ImageView contact5 = mView.findViewById(R.id.contact_5);
        contact5.setTag(DEF_KEY, CONTACT_5);
        ImageView contact6 = mView.findViewById(R.id.contact_6);
        contact6.setTag(DEF_KEY, CONTACT_6);
        ImageView contact7 = mView.findViewById(R.id.contact_7);
        contact7.setTag(DEF_KEY, CONTACT_7);
        ImageView contact8 = mView.findViewById(R.id.contact_8);
        contact8.setTag(DEF_KEY, CONTACT_8);

        if (a1 != null && !a1.isEmpty()) {
            try {
                contact1.setImageDrawable(Drawable.createFromStream(openDisplayPhoto(getContext(), Long.decode(a1)), "contact_1"));
                contact1.setTag(CONTACT_KEY, a1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            contact1.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_account_circle_white_24dp, null));
            contact1.setTag(CONTACT_KEY, null);
        }

        if (a2 != null && !a2.isEmpty()) {
            try {
                contact2.setImageDrawable(Drawable.createFromStream(openDisplayPhoto(getContext(), Long.decode(a2)), "contact_2"));
                contact2.setTag(CONTACT_KEY, a2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            contact2.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_account_circle_white_24dp, null));
            contact2.setTag(CONTACT_KEY, null);
        }

        if (a3 != null && !a3.isEmpty()) {
            try {
                contact3.setImageDrawable(Drawable.createFromStream(openDisplayPhoto(getContext(), Long.decode(a3)), "contact_3"));
                contact3.setTag(CONTACT_KEY, a3);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            contact3.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_account_circle_white_24dp, null));
            contact3.setTag(CONTACT_KEY, null);
        }

        if (a4 != null && !a4.isEmpty()) {
            try {
                contact4.setImageDrawable(Drawable.createFromStream(openDisplayPhoto(getContext(), Long.decode(a4)), "contact_4"));
                contact4.setTag(CONTACT_KEY, a4);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            contact4.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_account_circle_white_24dp, null));
            contact4.setTag(CONTACT_KEY, null);
        }

        if (a5 != null && !a5.isEmpty()) {
            try {
                contact5.setImageDrawable(Drawable.createFromStream(openDisplayPhoto(getContext(), Long.decode(a5)), "contact_5"));
                contact5.setTag(CONTACT_KEY, a5);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            contact5.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_account_circle_white_24dp, null));
            contact5.setTag(CONTACT_KEY, null);
        }

        if (a6 != null && !a6.isEmpty()) {
            try {
                contact6.setImageDrawable(Drawable.createFromStream(openDisplayPhoto(getContext(), Long.decode(a6)), "contact_6"));
                contact6.setTag(CONTACT_KEY, a6);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            contact6.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_account_circle_white_24dp, null));
            contact6.setTag(CONTACT_KEY, null);
        }

        if (a7 != null && !a7.isEmpty()) {
            try {
                contact7.setImageDrawable(Drawable.createFromStream(openDisplayPhoto(getContext(), Long.decode(a7)), "contact_7"));
                contact7.setTag(CONTACT_KEY, a7);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            contact7.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_account_circle_white_24dp, null));
            contact7.setTag(CONTACT_KEY, null);
        }

        if (a8 != null && !a8.isEmpty()) {
            try {
                contact8.setImageDrawable(Drawable.createFromStream(openDisplayPhoto(getContext(), Long.decode(a8)), "contact_8"));
                contact8.setTag(CONTACT_KEY, a8);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            contact8.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_account_circle_white_24dp, null));
            contact8.setTag(CONTACT_KEY, null);
        }

        contact1.setOnClickListener(this);
        contact2.setOnClickListener(this);
        contact3.setOnClickListener(this);
        contact4.setOnClickListener(this);
        contact5.setOnClickListener(this);
        contact6.setOnClickListener(this);
        contact7.setOnClickListener(this);
        contact8.setOnClickListener(this);

        contact1.setOnLongClickListener(this);
        contact2.setOnLongClickListener(this);
        contact3.setOnLongClickListener(this);
        contact4.setOnLongClickListener(this);
        contact5.setOnLongClickListener(this);
        contact6.setOnLongClickListener(this);
        contact7.setOnLongClickListener(this);
        contact8.setOnLongClickListener(this);
    }

    @Override
    public void onClick(View view)
    {
        if (view.getTag(CONTACT_KEY) != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, view.getTag(CONTACT_KEY).toString());
            intent.setData(uri);
            getContext().startActivity(intent);
        } else if (view.getTag(DEF_KEY) != null) {
            Intent intent = new Intent(getContext(), AddContactActivity.class);
            intent.putExtra(CONTACT_ID, view.getTag(DEF_KEY).toString());
            getContext().startActivity(intent);
        }
    }

    @Override
    public boolean onLongClick(View view)
    {
        if (view.getTag(DEF_KEY) != null) {
            Intent intent = new Intent(getContext(), AddContactActivity.class);
            intent.putExtra(CONTACT_ID, view.getTag(DEF_KEY).toString());
            getContext().startActivity(intent);
            return true;
        } else return false;
    }
}
