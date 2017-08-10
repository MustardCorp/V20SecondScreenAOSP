package xyz.mustardcorp.secondscreen.layouts;

import android.content.Context;
import android.view.View;

/**
 * Base layout class, containing methods required in all layouts
 */

public abstract class BaseLayout
{
    private Context mContext;

    public BaseLayout(Context context) {
        mContext = context;
    }

    /**
     * @return context given in construction
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Retrieve the main view
     * @return instance of the current layout
     */
    public abstract View getView();

    /**
     * Set proper orientation listeners and actions
     */
    public abstract void setOrientationListener();

    /**
     * Make sure all listeners, receivers and observers are unregistered when the parent is
     * (Must call BaseLayout.onDestroy() on current instance in parent's onDestroy())
     */
    public abstract void onDestroy();
}
