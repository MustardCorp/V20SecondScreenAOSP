package xyz.mustardcorp.secondscreen.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import xyz.mustardcorp.secondscreen.services.SignBoardService;

public class ScreenStateReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();

        if (action != null) {
            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                context.startService(new Intent(context, SignBoardService.class));
            }

            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                context.stopService(new Intent(context, SignBoardService.class));
            }
        }
    }
}
