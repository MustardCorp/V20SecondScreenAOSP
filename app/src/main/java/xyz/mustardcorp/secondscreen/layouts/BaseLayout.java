package xyz.mustardcorp.secondscreen.layouts;

import android.content.Context;
import android.view.View;

public abstract class BaseLayout
{
    private Context mContext;

    public BaseLayout(Context context) {
        mContext = context;
    }

    public Context getContext() {
        return mContext;
    }

    public abstract View getView();
    public abstract void setOrientationListener();
    public abstract void onDestroy();
}
