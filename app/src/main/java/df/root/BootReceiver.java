package df.root;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import java.io.File;

public class BootReceiver extends BroadcastReceiver implements IReporter {
    private static final String TAG = "dfroot";

    @Override
    public void report(String msg) {
        Log.i(TAG, msg.trim());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (new File("/dev/df").exists()) {
            Log.i(TAG, "boot: already hooked, skipping");
            return;
        }
        Log.i(TAG, "boot: " + (intent != null ? intent.getAction() : "null"));
        final Context deCtx = context.createDeviceProtectedStorageContext();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wl = (pm != null)
                ? pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dfroot:boot")
                : null;
        if (wl != null) {
            wl.acquire(10 * 60 * 1000L); // 10-minute safeguard timeout
        }
        new Thread(() -> {
            try {
                int rc = ExploitRunner.run(deCtx, this);
                Log.i(TAG, "boot: exploit rc=" + rc);
            } catch (Exception e) {
                Log.e(TAG, "boot: exploit exception", e);
            } finally {
                if (wl != null && wl.isHeld()) {
                    wl.release();
                }
            }
        }, "dfroot-boot").start();
    }
}
