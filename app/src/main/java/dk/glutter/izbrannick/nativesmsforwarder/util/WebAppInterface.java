package dk.glutter.izbrannick.nativesmsforwarder.util;

import android.content.Context;
import android.content.Intent;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import dk.glutter.izbrannick.nativesmsforwarder.ForwarderActivity;

/**
 * Created by izbrannick on 09-03-2015.
 */
public class WebAppInterface {
    Context mContext;

    /** Instantiate the interface and set the context */
    public WebAppInterface(Context c) {
        mContext = c;
    }

    /** Show a toast from the web page */
    @JavascriptInterface
    public void moveOn(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(mContext, ForwarderActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }
}