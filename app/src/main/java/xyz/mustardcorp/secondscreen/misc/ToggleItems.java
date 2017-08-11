package xyz.mustardcorp.secondscreen.misc;

import android.content.Context;

import java.util.ArrayList;

import xyz.mustardcorp.secondscreen.R;
import xyz.mustardcorp.secondscreen.custom.AbstractDataProvider;

public class ToggleItems
{

    private Context mContext;

    public ToggleItems(Context context) {
        mContext = context;
    }

    public ArrayList<AbstractDataProvider.Data> getAll() {
        ArrayList<String> saved = Util.parseSavedToggleOrder(mContext, Values.defaultToggleOrder);

        ArrayList<AbstractDataProvider.Data> def = new ArrayList<AbstractDataProvider.Data>() {{
            add(sound);
            add(wifi);
            add(bluetooth);
            add(flash);
            add(airplane);
        }};

        if (saved.equals(Values.defaultToggleOrder)) {
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

    public AbstractDataProvider.Data sound = new AbstractDataProvider.Data()
    {
        @Override
        public long getId()
        {
            return Values.SOUND_ID;
        }

        @Override
        public int getViewType()
        {
            return 0;
        }

        @Override
        public String getText()
        {
            return mContext.getResources().getString(R.string.sound_toggle);
        }

        @Override
        public boolean isEnabled()
        {
            return Util.isToggleShown(mContext, getKey());
        }

        @Override
        public String getKey()
        {
            return Values.SOUND_TOGGLE;
        }
    };

    public AbstractDataProvider.Data wifi = new AbstractDataProvider.Data()
    {
        @Override
        public long getId()
        {
            return Values.WIFI_ID;
        }

        @Override
        public int getViewType()
        {
            return 0;
        }

        @Override
        public String getText()
        {
            return mContext.getResources().getString(R.string.wifi_toggle);
        }

        @Override
        public boolean isEnabled()
        {
            return Util.isToggleShown(mContext, getKey());
        }

        @Override
        public String getKey()
        {
            return Values.WIFI_TOGGLE;
        }
    };

    public AbstractDataProvider.Data flash = new AbstractDataProvider.Data()
    {
        @Override
        public long getId()
        {
            return Values.FLASHLIGHT_ID;
        }

        @Override
        public int getViewType()
        {
            return 0;
        }

        @Override
        public String getText()
        {
            return mContext.getResources().getString(R.string.flash_toggle);
        }

        @Override
        public boolean isEnabled()
        {
            return Util.isToggleShown(mContext, getKey());
        }

        @Override
        public String getKey()
        {
            return Values.FLASHLIGHT_TOGGLE;
        }
    };

    public AbstractDataProvider.Data bluetooth = new AbstractDataProvider.Data()
    {
        @Override
        public long getId()
        {
            return Values.BT_ID;
        }

        @Override
        public int getViewType()
        {
            return 0;
        }

        @Override
        public String getText()
        {
            return mContext.getResources().getString(R.string.bt_toggle);
        }

        @Override
        public boolean isEnabled()
        {
            return Util.isToggleShown(mContext, getKey());
        }

        @Override
        public String getKey()
        {
            return Values.BLUETOOTH_TOGGLE;
        }
    };

    public AbstractDataProvider.Data airplane = new AbstractDataProvider.Data()
    {

        @Override
        public long getId()
        {
            return Values.AIRPLANE_ID;
        }

        @Override
        public int getViewType()
        {
            return 0;
        }

        @Override
        public String getText()
        {
            return mContext.getResources().getString(R.string.airplane_toggle);
        }

        @Override
        public boolean isEnabled()
        {
            return Util.isToggleShown(mContext, getKey());
        }

        @Override
        public String getKey()
        {
            return Values.AIRPLANE_TOGGLE;
        }
    };
}
