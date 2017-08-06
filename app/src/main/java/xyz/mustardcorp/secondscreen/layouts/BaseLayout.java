package xyz.mustardcorp.secondscreen.layouts;

import android.content.Context;
import android.view.View;

/**
 * Created by Zacha on 8/5/2017.
 */

public abstract class BaseLayout
{
    private Context mContext;

    public BaseLayout(Context context) {
        mContext = context;
    }

    public abstract View getView();
    public abstract void setOrientationListener();
    public abstract void onDestroy();
}
