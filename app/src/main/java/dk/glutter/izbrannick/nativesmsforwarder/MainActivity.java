package dk.glutter.izbrannick.nativesmsforwarder;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import dk.glutter.izbrannick.nativesmsforwarder.util.WebAppInterface;


public class MainActivity extends ActionBarActivity {

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_web_info);
    }

    private void showWebInfo() {
        WebView webView = (WebView) findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webView.loadUrl("http://www.glutter.dk/sms-forwarder-app.html");

        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
    }

    private void showLocalInfo() {
        setContentView(R.layout.activity_info);
        Button btn = (Button) findViewById(R.id.buttonNxt);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebAppInterface webAppInterface = new WebAppInterface(getApplicationContext());
                webAppInterface.moveOn("Starting SMS Forwarder");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Run web view if SDK version is newer than android 2.3.3(6)
        // Web view is extremely slow on 2.3.3
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2)) {
            if (isNetworkAvailable())
                showWebInfo();
            else {
                Toast.makeText(this, "Warning could not detect internet connection!!!", Toast.LENGTH_LONG).show();
                showLocalInfo();
            }
        } else
            showLocalInfo();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
