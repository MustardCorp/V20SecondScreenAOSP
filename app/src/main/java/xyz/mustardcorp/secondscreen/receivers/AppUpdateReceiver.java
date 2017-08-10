package xyz.mustardcorp.secondscreen.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import xyz.mustardcorp.secondscreen.services.SignBoardService;

/**
 * Make sure the service is restarted when the app gets reinstalled or updated
 */

public class AppUpdateReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();

        if (action != null && action.equals(Intent.ACTION_MY_PACKAGE_REPLACED)) {
            Log.e("Received", action);
            context.startService(new Intent(context, SignBoardService.class));
        }
    }
}
