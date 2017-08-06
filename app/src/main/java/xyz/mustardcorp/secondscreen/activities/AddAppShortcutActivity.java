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

public class AddAppShortcutActivity extends AppCompatActivity
{
    private String whichApp = null;

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

        LoadApps.newInstance(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public String getWhichApp() {
        return whichApp;
    }

    private static class LoadApps extends AsyncTask<Void, Void, Void> {
        private WeakReference<AddAppShortcutActivity> mContext;
        private CustomRecyclerAdapter mAdapter;

        public static LoadApps newInstance(AddAppShortcutActivity context) {
            LoadApps apps = new LoadApps();
            apps.mContext = new WeakReference<>(context);
            return apps;
        }

        @Override
        protected Void doInBackground(Void... voids)
        {
            mAdapter = CustomRecyclerAdapter.newInstance(mContext.get());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            mContext.get().findViewById(R.id.apps_loading).setVisibility(View.GONE);
            RecyclerView recyclerView = mContext.get().findViewById(R.id.app_list_rv);
            recyclerView.setVisibility(View.VISIBLE);
            LinearLayoutManager layoutManager = new LinearLayoutManager(mContext.get());
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(mAdapter);
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                    layoutManager.getOrientation());
            recyclerView.addItemDecoration(dividerItemDecoration);
        }
    }

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
                        if (mActivity.get().getWhichApp() != null) {
                            Settings.Global.putString(mActivity.get().getContentResolver(), mActivity.get().getWhichApp(), getAppId());
                            mActivity.get().finish();
                        }
                    }
                });

                appName = mView.findViewById(R.id.app_name);
                appIcon = mView.findViewById(R.id.app_icon);
            }

            public void setAppName(String name) {
                appName.setText(name);
            }

            public void setAppIcon(Drawable icon) {
                appIcon.setImageDrawable(icon);
            }

            public void setAppId(String id) {
                appId = id;
            }

            public String getAppName() {
                return appName.getText().toString();
            }

            public Drawable getAppIcon() {
                return appIcon.getDrawable();
            }

            public String getAppId() {
                return appId;
            }
        }
    }

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
