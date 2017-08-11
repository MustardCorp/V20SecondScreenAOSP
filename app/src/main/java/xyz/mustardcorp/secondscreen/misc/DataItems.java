package xyz.mustardcorp.secondscreen.misc;

import android.content.Context;

import java.util.ArrayList;

import xyz.mustardcorp.secondscreen.OptionsActivity;
import xyz.mustardcorp.secondscreen.R;
import xyz.mustardcorp.secondscreen.custom.AbstractDataProvider;

import static xyz.mustardcorp.secondscreen.misc.Values.APPS_ID;
import static xyz.mustardcorp.secondscreen.misc.Values.APPS_KEY;
import static xyz.mustardcorp.secondscreen.misc.Values.CONTACTS_ID;
import static xyz.mustardcorp.secondscreen.misc.Values.CONTACTS_KEY;
import static xyz.mustardcorp.secondscreen.misc.Values.INFO_ID;
import static xyz.mustardcorp.secondscreen.misc.Values.INFO_KEY;
import static xyz.mustardcorp.secondscreen.misc.Values.MUSIC_ID;
import static xyz.mustardcorp.secondscreen.misc.Values.MUSIC_KEY;
import static xyz.mustardcorp.secondscreen.misc.Values.RECENTS_ID;
import static xyz.mustardcorp.secondscreen.misc.Values.RECENTS_KEY;
import static xyz.mustardcorp.secondscreen.misc.Values.TOGGLES_ID;
import static xyz.mustardcorp.secondscreen.misc.Values.TOGGLES_KEY;

/**
 * Contains list of current pages and their values
 * (used for {@link OptionsActivity.PageOrderPreferenceFragment}
 */

public class DataItems
{
    private Context mContext;

    public DataItems(Context context) {
        mContext = context;
    }

    /**
     * Get all views in order
     * @return list of current pages, in proper order (disabled at the end)
     */
    public ArrayList<AbstractDataProvider.Data> getAll() {
        ArrayList<String> saved = Util.parseSavedViews(mContext, Values.defaultLoad);
        ArrayList<AbstractDataProvider.Data> def = new ArrayList<AbstractDataProvider.Data>() {{
            add(info);
            add(toggles);
            add(apps);
//            add(flash);
//            add(airplane);
            add(contacts);
        }};
        if (saved.equals(Values.defaultLoad)) {
            return def;
        } else {
            ArrayList<AbstractDataProvider.Data> data = new ArrayList<>();

            outerloop:
            for (String key : saved) {
                for (AbstractDataProvider.Data dat : def) {
                    if (key.equals(dat.getKey())) {
                        data.add(dat);
                        continue outerloop;
                    }
                }
            }

            for (AbstractDataProvider.Data dat : def) {
                if (!data.contains(dat)) data.add(dat);
            }

            return data;
        }
    }

    public AbstractDataProvider.Data info = new AbstractDataProvider.Data()
    {
        @Override
        public long getId()
        {
            return INFO_ID;
        }

        @Override
        public int getViewType()
        {
            return 0;
        }

        @Override
        public String getText()
        {
            return mContext.getResources().getString(R.string.information);
        }

        @Override
        public boolean isEnabled()
        {
            return Util.isEnabled(mContext, INFO_KEY);
        }

        @Override
        public String getKey()
        {
            return INFO_KEY;
        }
    };

    public AbstractDataProvider.Data toggles = new AbstractDataProvider.Data()
    {
        @Override
        public long getId()
        {
            return TOGGLES_ID;
        }

        @Override
        public int getViewType()
        {
            return 0;
        }

        @Override
        public String getText()
        {
            return mContext.getResources().getString(R.string.toggles);
        }

        @Override
        public boolean isEnabled()
        {
            return Util.isEnabled(mContext, TOGGLES_KEY);
        }

        @Override
        public String getKey()
        {
            return TOGGLES_KEY;
        }
    };

    public AbstractDataProvider.Data music = new AbstractDataProvider.Data()
    {
        @Override
        public long getId()
        {
            return MUSIC_ID;
        }

        @Override
        public int getViewType()
        {
            return 0;
        }

        @Override
        public String getText()
        {
            return mContext.getResources().getString(R.string.music);
        }

        @Override
        public boolean isEnabled()
        {
            return Util.isEnabled(mContext, MUSIC_KEY);
        }

        @Override
        public String getKey()
        {
            return MUSIC_KEY;
        }
    };

    public AbstractDataProvider.Data apps = new AbstractDataProvider.Data()
    {
        @Override
        public long getId()
        {
            return APPS_ID;
        }

        @Override
        public int getViewType()
        {
            return 0;
        }

        @Override
        public String getText()
        {
            return mContext.getResources().getString(R.string.launcher);
        }

        @Override
        public boolean isEnabled()
        {
            return Util.isEnabled(mContext, APPS_KEY);
        }

        @Override
        public String getKey()
        {
            return APPS_KEY;
        }
    };

    public AbstractDataProvider.Data recents = new AbstractDataProvider.Data()
    {

        @Override
        public long getId()
        {
            return RECENTS_ID;
        }

        @Override
        public int getViewType()
        {
            return 0;
        }

        @Override
        public String getText()
        {
            return mContext.getResources().getString(R.string.recents);
        }

        @Override
        public boolean isEnabled()
        {
            return Util.isEnabled(mContext, RECENTS_KEY);
        }

        @Override
        public String getKey()
        {
            return RECENTS_KEY;
        }
    };

    public AbstractDataProvider.Data contacts = new AbstractDataProvider.Data()
    {

        @Override
        public long getId()
        {
            return CONTACTS_ID;
        }

        @Override
        public int getViewType()
        {
            return 0;
        }

        @Override
        public String getText()
        {
            return mContext.getResources().getString(R.string.contacts);
        }

        @Override
        public boolean isEnabled()
        {
            return Util.isEnabled(mContext, CONTACTS_KEY);
        }

        @Override
        public String getKey()
        {
            return CONTACTS_KEY;
        }
    };
}
