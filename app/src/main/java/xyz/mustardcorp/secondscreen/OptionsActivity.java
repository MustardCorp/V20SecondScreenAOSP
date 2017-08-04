package xyz.mustardcorp.secondscreen;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.view.MenuItem;

import com.jaredrummler.android.colorpicker.ColorPreference;

import java.util.List;

import xyz.mustardcorp.secondscreen.misc.Values;
import xyz.mustardcorp.secondscreen.services.SignBoardService;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class OptionsActivity extends AppCompatPreferenceActivity
{

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context)
    {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar()
    {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane()
    {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target)
    {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName)
    {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || OtherPreferenceFragment.class.getName().equals(fragmentName)
                || ColorPreferenceFragment.class.getName().equals(fragmentName);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class OtherPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_other);
            setHasOptionsMenu(true);

            findPreference(Values.SHOULD_FORCE_START).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o)
                {
                    boolean force = Boolean.valueOf(o.toString());
                    if (force) {
                        getContext().startService(new Intent(getContext(), SignBoardService.class));
                    }
                    return true;
                }
            });
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), OptionsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class ColorPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_colors);
            setHasOptionsMenu(true);

            for (int i = 0; i < getPreferenceScreen().getRootAdapter().getCount(); i++) {
                Object o = getPreferenceScreen().getRootAdapter().getItem(i);

                if (o instanceof ColorPreference) {
                    ColorPreference preference = (ColorPreference) o;

                    int colorVal = Settings.Global.getInt(getContext().getContentResolver(), preference.getKey(), Color.WHITE);
                    preference.saveValue(colorVal);

                    preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
                    {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o)
                        {
                            Settings.Global.putInt(getContext().getContentResolver(), preference.getKey(), Integer.valueOf(o.toString()));
                            return true;
                        }
                    });
                }
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), OptionsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
