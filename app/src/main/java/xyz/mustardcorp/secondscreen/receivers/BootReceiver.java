package xyz.mustardcorp.secondscreen.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import xyz.mustardcorp.secondscreen.services.SignBoardService;

/**
 * Start service on boot
 */

public class BootReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();

        if (action != null && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.e("Received", action);
            context.startService(new Intent(context, SignBoardService.class));
        }
    }
}
