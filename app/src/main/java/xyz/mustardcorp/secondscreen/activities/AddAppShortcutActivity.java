package xyz.mustardcorp.secondscreen.activities;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import xyz.mustardcorp.secondscreen.R;
import xyz.mustardcorp.secondscreen.layouts.AppLauncher;

/**
 * Activity shown on long press of app shortcut or normal press on empty launcher
 *
 * Presents a list of available bluetooth for the user to choose, and places the chosen app in the proper Settings.Global preference
 */

public class AddAppShortcutActivity extends AppCompatActivity
{
    private String whichApp = null; //the Settings.Global key to place the chosen app ID in (EG: app_1_id)

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_app_shortcut);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            whichApp = extras.getString(AppLauncher.APP_ID);
            Log.e("APP", whichApp);
        }

        LoadApps.newInstance(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR); //the initialization takes a while, so let's just show a loading symbol until it's done
    }

    /**
     * Getter method for the slot ID
     * @return the Settings.Global key
     */
    public String getWhichApp() {
        return whichApp;
    }

    private static class LoadApps extends AsyncTask<Void, Void, Void> {
        private WeakReference<AddAppShortcutActivity> mContext; //cheaty way to avoid static context leaks
        private CustomRecyclerAdapter mAdapter;

        /**
         * Create a new instance of this task
         * @param context the parent Activity, since we can't call non-static methods from a static context
         * @return the new instance of the task
         */
        public static LoadApps newInstance(AddAppShortcutActivity context) {
            LoadApps apps = new LoadApps();
            apps.mContext = new WeakReference<>(context);
            return apps;
        }

        @Override
        protected Void doInBackground(Void... voids)
        {
            mAdapter = CustomRecyclerAdapter.newInstance(mContext.get()); //all the work happens in the RecyclerAdapter, so this is put in the background thread
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            /*
             * Now that the adapter is finished loading, hide the loading symbol, set the RecyclerView's adapter and make it visible.
             */

            mContext.get().findViewById(R.id.apps_loading).setVisibility(View.GONE);
            RecyclerView recyclerView = (RecyclerView) mContext.get().findViewById(R.id.app_list_rv);
            recyclerView.setVisibility(View.VISIBLE);
            LinearLayoutManager layoutManager = new LinearLayoutManager(mContext.get());
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(mAdapter);
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                    layoutManager.getOrientation());
            recyclerView.addItemDecoration(dividerItemDecoration);
        }
    }

    /**
     * Class for storing package sound
     */
    class PInfo {
        public String appname = "";
        public String pname = "";
        public String versionName = "";
        public int versionCode = 0;
        public Drawable icon;
    }

    public static class CustomRecyclerAdapter extends RecyclerView.Adapter<CustomRecyclerAdapter.CustomViewHolder> {

        private ArrayList<PInfo> apps;
        private WeakReference<AddAppShortcutActivity> mActivity;

        public static CustomRecyclerAdapter newInstance(AddAppShortcutActivity activity) {
            CustomRecyclerAdapter adapter = new CustomRecyclerAdapter();
            adapter.apps = new ArrayList<>(activity.getInstalledApps(false).values());
            adapter.mActivity = new WeakReference<>(activity);
            return adapter;
        }

        @Override
        public int getItemCount()
        {
            return apps.size();
        }

        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View itemView = LayoutInflater.
                    from(parent.getContext()).
                    inflate(R.layout.layout_application, parent, false);
            return new CustomViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(CustomViewHolder holder, int position)
        {
            holder.setAppName(apps.get(position).appname);
            holder.setAppIcon(apps.get(position).icon);
            holder.setAppId(apps.get(position).pname);
        }

        public class CustomViewHolder extends RecyclerView.ViewHolder {
            private View mView;
            private final TextView appName;
            private final ImageView appIcon;
            private String appId = null;

            public CustomViewHolder (View v) {
                super(v);
                mView = v;

                v.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        //if the slot ID isn't null, set the app value
                        if (mActivity.get().getWhichApp() != null) {
                            Settings.Global.putString(mActivity.get().getContentResolver(), mActivity.get().getWhichApp(), getAppId());
                            mActivity.get().finish();
                        }
                    }
                });

                appName = mView.findViewById(R.id.app_name);
                appIcon = mView.findViewById(R.id.app_icon);
            }

            /**
             * Set the app's name
             * @param name app's display name
             */
            public void setAppName(String name) {
                appName.setText(name);
            }

            /**
             * Set the app's icon
             * @param icon app's launcher icon
             */
            public void setAppIcon(Drawable icon) {
                appIcon.setImageDrawable(icon);
            }

            /**
             * Set the app's ID
             * @param id app's String ID (EG: com.android.systemui)
             */
            public void setAppId(String id) {
                appId = id;
            }

            /**
             * Retrieve app's name
             * @return app's display name
             */
            public String getAppName() {
                return appName.getText().toString();
            }

            /**
             * Retrieve app's icon
             * @return app's launcher icon
             */
            public Drawable getAppIcon() {
                return appIcon.getDrawable();
            }

            /**
             * Retrieve app's String ID
             * @return app's String ID (EG: com.android.systemui)
             */
            public String getAppId() {
                return appId;
            }
        }
    }

    /**
     * Get a list of the currently installed bluetooth, sorted by name
     * @param getSysPackages whether or not to include system packages
     * @return sorted list of applications by name
     */
    private TreeMap<String, PInfo> getInstalledApps(boolean getSysPackages) {
        TreeMap<String, PInfo> res = new TreeMap<>();
        List<PackageInfo> packs = getPackageManager().getInstalledPackages(0);
        for(int i=0;i<packs.size();i++) {
            PackageInfo p = packs.get(i);
            if ((!getSysPackages) && (p.versionName == null)) {
                continue ;
            }
            PInfo newInfo = new PInfo();
            newInfo.appname = p.applicationInfo.loadLabel(getPackageManager()).toString();
            newInfo.pname = p.packageName;
            newInfo.versionName = p.versionName;
            newInfo.versionCode = p.versionCode;
            try
            {
                newInfo.icon = getPackageManager().getApplicationIcon(newInfo.pname);
            } catch (Exception e) {
                e.printStackTrace();
                newInfo.icon = getResources().getDrawable(android.R.drawable.ic_menu_help, null);
            }
            res.put(newInfo.appname, newInfo);
        }
        return res;
    }
}
