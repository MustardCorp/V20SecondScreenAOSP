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
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.h6ah4i.android.widget.advrecyclerview.animator.DraggableItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;
import com.jaredrummler.android.colorpicker.ColorPreference;

import java.util.ArrayList;
import java.util.List;

import xyz.mustardcorp.secondscreen.custom.AbstractDataProvider;
import xyz.mustardcorp.secondscreen.custom.CustomDragAndDropAdapter;
import xyz.mustardcorp.secondscreen.misc.DataItems;
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
                || ColorPreferenceFragment.class.getName().equals(fragmentName)
                || OrderPreferenceFragment.class.getName().equals(fragmentName);
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class OrderPreferenceFragment extends PreferenceFragment
    {
        private RecyclerView mRecyclerView;
        private RecyclerView.LayoutManager mLayoutManager;
        private RecyclerViewDragDropManager mRecyclerViewDragDropManager;
        private CustomDragAndDropAdapter mAdapter;
        private RecyclerView.Adapter mWrappedAdapter;

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
        {
            return inflater.inflate(R.layout.layout_page_order, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
        {
            super.onViewCreated(view, savedInstanceState);

            mRecyclerView = (RecyclerView) getView();
            mLayoutManager = new LinearLayoutManager(getContext());

            // drag & drop manager
            mRecyclerViewDragDropManager = new RecyclerViewDragDropManager();
//        mRecyclerViewDragDropManager.setDraggingItemShadowDrawable(
//                (NinePatchDrawable) ContextCompat.getDrawable(getContext(), R.drawable.material_shadow_z3));
            // Start dragging after long press
            mRecyclerViewDragDropManager.setInitiateOnLongPress(true);
            mRecyclerViewDragDropManager.setInitiateOnMove(false);

            //adapter
            mAdapter = new CustomDragAndDropAdapter(getDataProvider(), getContext());

            mWrappedAdapter = mRecyclerViewDragDropManager.createWrappedAdapter(mAdapter);      // wrap for dragging

            final GeneralItemAnimator animator = new DraggableItemAnimator();

            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerView.setAdapter(mWrappedAdapter);  // requires *wrapped* adapter
            mRecyclerView.setItemAnimator(animator);

            mRecyclerView.addItemDecoration(new SimpleListDividerDecorator(ContextCompat.getDrawable(getContext(), R.drawable.horizontal_divider), true));

            mRecyclerViewDragDropManager.attachRecyclerView(mRecyclerView);
        }

        @Override
        public void onPause()
        {
            mRecyclerViewDragDropManager.cancelDrag();
            super.onPause();
        }

        @Override
        public void onDestroyView()
        {
            if (mRecyclerViewDragDropManager != null)
            {
                mRecyclerViewDragDropManager.release();
                mRecyclerViewDragDropManager = null;
            }

            if (mRecyclerView != null)
            {
                mRecyclerView.setItemAnimator(null);
                mRecyclerView.setAdapter(null);
                mRecyclerView = null;
            }

            if (mWrappedAdapter != null)
            {
                WrapperAdapterUtils.releaseAll(mWrappedAdapter);
                mWrappedAdapter = null;
            }
            mAdapter = null;
            mLayoutManager = null;

            super.onDestroyView();
        }

        public AbstractDataProvider getDataProvider()
        {

            DataItems dataItems = new DataItems(getContext());
            final ArrayList<AbstractDataProvider.Data> items = dataItems.getAll();

            AbstractDataProvider provider = new AbstractDataProvider()
            {
                private Data mRemovedItem = null;
                private int mRemovedPosition = -1;

                @Override
                public int getCount()
                {
                    return items.size();
                }

                @Override
                public Data getItem(int index)
                {
                    return items.get(index);
                }

                @Override
                public void removeItem(int position)
                {
                    mRemovedItem = items.remove(position);
                }

                @Override
                public void moveItem(int fromPosition, int toPosition)
                {
                    Data item = items.remove(fromPosition);
                    items.add(toPosition, item);
                }

                @Override
                public void swapItem(int fromPosition, int toPosition)
                {
                    Data from = items.get(fromPosition);
                    Data to = items.get(toPosition);

                    items.set(fromPosition, to);
                    items.set(toPosition, from);
                }

                @Override
                public int undoLastRemoval()
                {
                    if (mRemovedItem != null && mRemovedPosition != -1) {
                        items.add(mRemovedPosition, mRemovedItem);
                        return mRemovedPosition;
                    }

                    return -1;
                }

                @Override
                public int indexOf(Data item)
                {
                    return items.indexOf(item);
                }
            };

            return provider;
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
