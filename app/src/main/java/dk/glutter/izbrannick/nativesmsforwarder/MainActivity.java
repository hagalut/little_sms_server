package dk.glutter.izbrannick.nativesmsforwarder;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {

    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 2;
    public static String currentCountryCode;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentCountryCode = GetCountryZipCode();

        setContentView(R.layout.activity_info);


        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS);


                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BROADCAST_SMS},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);

    }

    public String GetCountryZipCode(){
        String CountryID="";
        String CountryZipCode="0";

        try {
            TelephonyManager manager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            //getNetworkCountryIso
            CountryID= manager.getSimCountryIso().toUpperCase();
            String[] rl=this.getResources().getStringArray(R.array.CountryCodes);
            for(int i=0;i<rl.length;i++){
                String[] g=rl[i].split(",");
                if(g[1].trim().equals(CountryID.trim())){
                    CountryZipCode=g[0];
                    break;
                }
            }
        }catch (Exception e)
        {
            Log.e("GetCountryZipCode", e.getMessage());
        }

        return CountryZipCode;
    }

    private void showLocalInfo(final Context context) {

        Button btn = (Button) findViewById(R.id.buttonNxt);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Settings", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(context, SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
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
            if (!isNetworkAvailable())
            {
                Toast.makeText(this, "Warning could not detect internet connection!!!", Toast.LENGTH_LONG).show();
            }
            showLocalInfo(getApplicationContext());
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
