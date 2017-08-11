package xyz.mustardcorp.secondscreen.layouts;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Handler;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import xyz.mustardcorp.secondscreen.R;
import xyz.mustardcorp.secondscreen.misc.Util;
import xyz.mustardcorp.secondscreen.services.SignBoardService;

/**
 * Short list of recent bluetooth
 * (BROKEN)
 */

public class Recents extends BaseLayout
{
    private LinearLayout mView;
    private SortedMap<Long, UsageStats> mApps = new TreeMap<>();
    private Display display;
    private ArrayList<View> originalLayout = new ArrayList<>();
    TableLayout.LayoutParams params = new TableLayout.LayoutParams(120, 120, 1);

    private Handler mHandler;

    public Recents(Context context) {
        super(context);
        mView = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.layout_recents, null, false);
        display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        mHandler = SignBoardService.mRecentsHandler;

        params.gravity = Gravity.CENTER;

        parseRecents();
        addRecents();
        addViews();
        setOrientationListener();
    }

    @Override
    public void setOrientationListener()
    {
        OrientationEventListener listener = new OrientationEventListener(getContext())
        {
            private int oldRotation = display.getRotation();

            @Override
            public void onOrientationChanged(int i)
            {
                if (oldRotation != display.getRotation()) {
                    addViews();

                    oldRotation = display.getRotation();
                }
            }
        };
        listener.enable();
    }

    @Override
    public View getView()
    {
        return mView;
    }

    @Override
    public void onDestroy()
    {

    }

    private void parseRecents() {
        UsageStatsManager mUsageStatsManager = (UsageStatsManager)getContext().getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        // We get usage stats for the last 10 seconds
        List<UsageStats> stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000*10, time);
        // Sort the stats by the last time used
        if(stats != null) {
            for (UsageStats usageStats : stats) {
                if (!mUsageStatsManager.isAppInactive(usageStats.getPackageName())) mApps.put(usageStats.getLastTimeUsed(), usageStats);
            }
        }
    }

    private void addRecents() {
        originalLayout = new ArrayList<>();

        for (int i = 0; i < (mApps.size() > 6 ? 6 : mApps.size()); i++) {
            UsageStats stats = (UsageStats) mApps.values().toArray()[i];

            ImageView view = new ImageView(getContext());
            view.setLayoutParams(params);
            try {
                view.setImageDrawable(getContext().getPackageManager().getApplicationIcon(stats.getPackageName()));
            } catch (Exception e) {
                continue;
            }

            view.setTag(R.string.app_key, stats.getPackageName());
            clickListener(view);
            originalLayout.add(view);
        }
    }

    private void addViews() {
        boolean shouldReverse = display.getRotation() == Surface.ROTATION_90;
        mView.removeAllViews();

        if (display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270) {
            mView.setOrientation(LinearLayout.VERTICAL);

            if (shouldReverse) {
                for (int j = originalLayout.size() - 1; j > 0; j--) {
                    originalLayout.get(j).setLayoutParams(params);
                    mView.addView(originalLayout.get(j));
                }
            } else {
                for (View view : originalLayout) {
                    view.setLayoutParams(params);
                    mView.addView(view);
                }
            }
        } else {
            mView.setOrientation(LinearLayout.HORIZONTAL);
            for (View view : originalLayout) {
                view.setLayoutParams(params);
                mView.addView(view);
            }
        }
    }

    private void clickListener(View view) {
        view.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Util.openApp(getContext(), view.getTag(R.string.app_key) != null ? view.getTag(R.string.app_key).toString() : "");
            }
        });
    }
}
