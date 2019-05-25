package bd.com.asiabazar.asiabazar;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NetworkConnectionReceiver extends BroadcastReceiver {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent send = new Intent("asia_bazaar_nc");
        send.putExtra("status", true);
        context.sendBroadcast(send);
    }
}