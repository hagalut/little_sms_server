package dk.glutter.izbrannick.nativesmsforwarder.otherapps;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by izbrannick on 11-02-2015.
 * Lunching other intents
 * Example: "com.zegoggles.smssync.BACKUP"
 */
public class ThirdPartyApp {

    // Example: "com.zegoggles.smssync"
    public boolean startApp(Context context, String packageName)
    {
        try {
            Log.d("Trying to lunch ", packageName);
            Intent i = context.getPackageManager().getLaunchIntentForPackage(packageName);
            context.startActivity(i);
        } catch (Exception e) {
            Log.d("something went wrong lunching" + packageName + "  ", e.getMessage());
            return false;
        }
        return true;
    }
    // Example: "com.zegoggles.smssync.BACKUP"
    public boolean startAppAction(Context context, String actionName)
    {
        try {
            Log.d("Trying to trigger SMSBackupPlus ", actionName);

            Intent intent = new Intent();
            intent.setAction(actionName);
            context.sendBroadcast(intent);

        } catch (Exception e) {
            Log.d("something went wrong lunching" + actionName + "  ", e.getMessage());
            return false;
        }
        return true;
    }
}
