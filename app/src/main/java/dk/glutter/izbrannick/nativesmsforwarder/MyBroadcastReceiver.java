package dk.glutter.izbrannick.nativesmsforwarder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.widget.Toast;

import dk.glutter.izbrannick.nativesmsforwarder.otherapps.ThirdPartyApp;

/**
 * Created by luther on 02/04/15.
 */
public class MyBroadcastReceiver extends android.content.BroadcastReceiver {

    static String beskedOld = "";
    String currMsg = "";
    String currNr = "";
    Context context;
    SharedPreferences preferences;


    @Override
    public void onReceive(Context context, Intent intent) {
        //BACKUP SMS - sync with SMS Backup PLus
        ThirdPartyApp la = new ThirdPartyApp();
        la.startAppAction(context, "com.zegoggles.smssync.BACKUP");


        if (intent.getAction().equals(
                "android.provider.Telephony.SMS_DELIVERED")) {
            Toast.makeText(context.getApplicationContext(), "SMS_DELIVERED",
                    Toast.LENGTH_LONG).show();

        }
        if (intent.getAction().equals(
                "android.provider.Telephony.SMS_RECEIVED")) {

            SmsMessage[] msg = null;
            this.context = context;
            preferences = context.getApplicationContext().getSharedPreferences("LittleSmsBroadcaster", context.MODE_PRIVATE);

            Bundle bundle = intent.getExtras();
            Object[] pdus = (Object[]) bundle.get("pdus");
            msg = new SmsMessage[pdus.length];
            for (int i = 0; i < pdus.length; i++) {
                msg[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                currNr = msg[i].getOriginatingAddress();
            }

            SmsMessage sms = msg[0];
            try {
                if (msg.length == 1 || sms.isReplace()) {
                    currMsg = sms.getDisplayMessageBody();
                } else {
                    StringBuilder bodyText = new StringBuilder();
                    for (int i = 0; i < msg.length; i++) {
                        bodyText.append(msg[i].getMessageBody());
                    }
                    currMsg = bodyText.toString();
                }
            } catch (Exception e) {
            }

                if (!currMsg.equals(beskedOld)) {


                    boolean forwarding = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enable_forwarding_checkbox", false);
                    boolean feedback = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enable_feedback_checkbox", false);
                    boolean ignore_foreign_numbers = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("ignore_foreign_numbers", false);
                    boolean add_group_sender_information = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("add_group_sender_information", false);

                    if (forwarding) {
                        new SmsHandler(context, currNr, currMsg, "0", false, feedback, ignore_foreign_numbers, add_group_sender_information );
                    }
                }

            beskedOld = currMsg;
        }
    }

}