package dk.glutter.izbrannick.nativesmsforwarder;

import dk.glutter.izbrannick.nativesmsforwarder.otherapps.ThirdPartyApp;
import dk.glutter.izbrannick.nativesmsforwarder.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class ForwarderActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    private Context context = null;
    private TextView tv;
    private ToggleButton forward_toggle_btn;
    private ToggleButton delete_sms_toggle_btn;
    private ToggleButton respond_usr_toggle_btn;
    private SharedPreferences.Editor editor;
    private SharedPreferences preferences;

    private String text = "";
    private String currSmsId = "";
    private String currMsg = "";
    private String currNr = "";
    private int messageCount = 0;
    private Handler handler;
    private SmsHandler smsHandler;
    private boolean forwarding;
    private boolean deleteMessages;
    private boolean respondMessages;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_forwarder);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        this.context = getApplicationContext();

        preferences = context.getApplicationContext().getSharedPreferences("LittleSmsBroadcaster", MODE_PRIVATE);
        forwarding = preferences.getBoolean("forwarding", false);
        deleteMessages = preferences.getBoolean("deleteMessages", false);
        respondMessages = preferences.getBoolean("respondMessages", false);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);
        tv = (TextView) findViewById(R.id.fullscreen_content);

        forward_toggle_btn = ((ToggleButton) findViewById(R.id.forwarder_toggl));
        delete_sms_toggle_btn = ((ToggleButton) findViewById(R.id.toggleButton_delete_smss));
        respond_usr_toggle_btn = ((ToggleButton) findViewById(R.id.toggleButton_respond_users));

        forward_toggle_btn.setChecked(forwarding);
        delete_sms_toggle_btn.setChecked(deleteMessages);
        respond_usr_toggle_btn.setChecked(respondMessages);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        findViewById(R.id.forwarder_toggl).setOnClickListener(toggleBtnTouchListener);
        findViewById(R.id.toggleButton_delete_smss).setOnClickListener(toggleDeleteBtnTouchListener);
        findViewById(R.id.toggleButton_respond_users).setOnClickListener(toggleRespondBtnTouchListener);

        /* Greate a group
        ContactsHandler contactsHandler = new ContactsHandler(context);
        contactsHandler.createGoogleGroup("GROUP1:");
        */
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    View.OnClickListener toggleBtnTouchListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            forwarding = forward_toggle_btn.isChecked();

            editor = getSharedPreferences("LittleSmsBroadcaster", MODE_PRIVATE).edit();
            editor.putBoolean("forwarding", forwarding); // value to store
            editor.commit();

            if (forwarding) {
                tv.setText(getString(R.string.forwarding_on));
                //new LongOperation().execute("");
            }else
            {
                forward_toggle_btn.setChecked(false);
                tv.setText(getString(R.string.forwarding_off));
            }
        }
    };

    View.OnClickListener toggleDeleteBtnTouchListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            deleteMessages = delete_sms_toggle_btn.isChecked();

            editor = getSharedPreferences("LittleSmsBroadcaster", MODE_PRIVATE).edit();
            editor.putBoolean("deleteMessages", deleteMessages); // value to store
            editor.commit();
        }
    };

    View.OnClickListener toggleRespondBtnTouchListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            respondMessages = respond_usr_toggle_btn.isChecked();

            editor = getSharedPreferences("LittleSmsBroadcaster", MODE_PRIVATE).edit();
            editor.putBoolean("respondMessages", respondMessages); // value to store
            editor.commit();
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private class LongOperation extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                run();
            } catch (Exception e) {
                Thread.interrupted();
                Log.d("AAAAAA", e.getMessage());
            }
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            //TextView txt = (TextView) findViewById(R.id.output);
            //txt.setText("Executed"); // txt.setText(result);
            // might want to change "executed" for the returned string passed
            // into onPostExecute() but that is upto you
            Toast.makeText(context, " onPostExecute ", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(context, " onPreExecute ", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            Toast.makeText(context, " onProgressUpdate ", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * API greater than 10 handling
     * @return
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private List<Sms> getAllSms() {

        int currentApiVersion = Build.VERSION.SDK_INT;
        List<Sms> lstSms = new ArrayList<Sms>();

        if (currentApiVersion > 10) {

            Sms objSms;
            Uri message = Uri.parse("content://sms/");

            CursorLoader cl = new CursorLoader(context);
            cl.setUri(message);
            //cl.setSelection("content://sms/");
            Cursor c = cl.loadInBackground();

            int totalSMS = c.getCount();
            if (c.moveToFirst()) {
                for (int i = 0; i < totalSMS; i++) {

                    objSms = new Sms();
                    objSms.setId(c.getString(c.getColumnIndexOrThrow("_id")));
                    objSms.setAddress(c.getString(c
                            .getColumnIndexOrThrow("address")));
                    objSms.setMsg(c.getString(c.getColumnIndexOrThrow("body")));
                    objSms.setReadState(c.getString(c.getColumnIndex("read")));
                    objSms.setTime(c.getString(c.getColumnIndexOrThrow("date")));
                    if (c.getString(c.getColumnIndexOrThrow("type")).contains("1")) {
                        objSms.setFolderName("inbox");
                    } else {
                        objSms.setFolderName("sent");
                    }

                    if (objSms.getReadState().equals("0")){
                        lstSms.add(objSms);
                    }
                    c.moveToNext();
                }
            }
            c.close();

            return lstSms;
        } else {
            lstSms = getAllSmsAPI10();
        }
        return lstSms;
    }

    /**
     * API 10 handling
     * @return
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    public List<Sms> getAllSmsAPI10() {
        List<Sms> lstSms = new ArrayList<Sms>();
        Sms objSms = new Sms();
        Uri message = Uri.parse("content://sms/");
        ContentResolver cr = this.getContentResolver();

        Cursor c = cr.query(message, null, null, null, null);

        this.startManagingCursor(c);
        int totalSMS = c.getCount();

        if (c.moveToFirst()) {
            for (int i = 0; i < totalSMS; i++) {

                objSms = new Sms();
                objSms.setId(c.getString(c.getColumnIndexOrThrow("_id")));
                objSms.setAddress(c.getString(c
                        .getColumnIndexOrThrow("address")));
                objSms.setMsg(c.getString(c.getColumnIndexOrThrow("body")));
                objSms.setReadState(c.getString(c.getColumnIndex("read")));
                objSms.setTime(c.getString(c.getColumnIndexOrThrow("date")));
                if (c.getString(c.getColumnIndexOrThrow("type")).contains("1")) {
                    objSms.setFolderName("inbox");
                } else {
                    objSms.setFolderName("sent");
                }

                lstSms.add(objSms);
                c.moveToNext();
            }
        }
        c.close();

        return lstSms;
    }

    /**
     * Fetches a list of unread messages from the system database
     *
     * @param context
     *            app context
     * @return ArrayList of SmsMmsMessage
     */

    // Content URIs for SMS app, these may change in future SDK
    public static final Uri MMS_SMS_CONTENT_URI = Uri.parse("content://mms-sms/");
    private static final String UNREAD_CONDITION = "read=1";
    public static final Uri SMS_CONTENT_URI = Uri.parse("content://sms/");
    public static final Uri SMS_INBOX_CONTENT_URI = Uri.withAppendedPath(SMS_CONTENT_URI, "inbox");
    public static final int READ_THREAD = 1;
    public static final Uri MMS_CONTENT_URI = Uri.parse("content://mms/");
    public static final Uri MMS_INBOX_CONTENT_URI = Uri.withAppendedPath(MMS_CONTENT_URI, "inbox");

    public static ArrayList<Sms> getUnreadMessages(Context context) {
        ArrayList<Sms> messages = null;

        final String[] projection =
                new String[] { "_id", "thread_id", "address", "date", "body" };
        String selection = UNREAD_CONDITION;
        String[] selectionArgs = null;
        final String sortOrder = "date ASC";

        // Create cursor
        Cursor cursor = context.getContentResolver().query(
                SMS_INBOX_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder);

        long messageId;
        long threadId;
        String address;
        long timestamp;
        String body;
        Sms message;

        if (cursor != null) {
            try {
                int count = cursor.getCount();
                if (count > 0) {
                    messages = new ArrayList<Sms>(count);
                    while (cursor.moveToNext()) {
                        messageId = cursor.getLong(0);
                        threadId = cursor.getLong(1);
                        address = cursor.getString(2);
                        timestamp = cursor.getLong(3);
                        body = cursor.getString(4);

                        message = new Sms();
                        message.setId( String.valueOf(messageId) );
                        message.setThreadId(String.valueOf(threadId));
                        message.setAddress(address);
                        message.setMsg(body);
                        message.setReadState(cursor.getString(cursor.getColumnIndex("read")));
                        message.setTime( String.valueOf(timestamp) );

                        messages.add(message);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return messages;
    }

    /**
     * Marks a specific message as read
     */
    synchronized public void setMessageRead(
            long messageId, int messageType) {

        if (messageId > 0) {
            ContentValues values = new ContentValues(1);
            values.put("read", READ_THREAD);

            Uri messageUri;

            if (Sms.MESSAGE_TYPE_MMS == messageType) {
                // Used to use URI of MMS_CONTENT_URI and it wasn't working, not sure why
                // this is diff to SMS
                messageUri = Uri.withAppendedPath(MMS_INBOX_CONTENT_URI, String.valueOf(messageId));
            } else if (Sms.MESSAGE_TYPE_SMS == messageType) {
                messageUri = Uri.withAppendedPath(SMS_CONTENT_URI, String.valueOf(messageId));
            } else {
                return;
            }

            // Log.v("messageUri for marking message read: " + messageUri.toString());

            ContentResolver cr = context.getContentResolver();
            int result;
            try {
                result = cr.update(messageUri, values, null, null);
            } catch (Exception e) {
                result = 0;
            }
            if (BuildConfig.DEBUG);
        }
    }

    /**
     * Marks a specific message as unread
     */
    synchronized public void setMessageUnRead(
            long messageId, int messageType) {

        if (messageId > 0) {
            ContentValues values = new ContentValues(1);
            values.put("read", 0);

            Uri messageUri;

            if (Sms.MESSAGE_TYPE_MMS == messageType) {
                // Used to use URI of MMS_CONTENT_URI and it wasn't working, not sure why
                // this is diff to SMS
                messageUri = Uri.withAppendedPath(MMS_INBOX_CONTENT_URI, String.valueOf(messageId));
            } else if (Sms.MESSAGE_TYPE_SMS == messageType) {
                messageUri = Uri.withAppendedPath(SMS_CONTENT_URI, String.valueOf(messageId));
            } else {
                return;
            }

            // Log.v("messageUri for marking message read: " + messageUri.toString());

            ContentResolver cr = context.getContentResolver();
            int result;
            try {
                result = cr.update(messageUri, values, null, null);
            } catch (Exception e) {
                result = 0;
            }
            if (BuildConfig.DEBUG);
        }
    }

    private void run()
    {
        Runnable runnable = null;

        if(runnable != null)
            handler.removeCallbacks(runnable);

        if(handler == null)
        {
            handler = new Handler();
            runnable = new Runnable()
            {
                public void run()
                {
                    List<Sms> messagess = getAllSms();
                    messageCount = messagess.size();
                    currSmsId = null;

                    if (forwarding)
                    {
                        if (messageCount > 0)
                        {
                            //TODO: Get all unreaded messages

                            currMsg = messagess.get(0).getMsg();
                            currSmsId = messagess.get(0).getId();
                            currNr = messagess.get(0).getAddress();

                            text = "Message from " + "  " + currNr + ": " + currMsg;

                            //BACKUP SMS - sync with SMS Backup PLus
                            ThirdPartyApp la = new ThirdPartyApp();
                            la.startAppAction(context, "com.zegoggles.smssync.BACKUP");

                            if (forwarding)
                            {
                                // Handle SMS
                                text = getString(R.string.sendingMsg) + currNr + " : " + currMsg;
                                smsHandler = new SmsHandler(context, currNr, currMsg, currSmsId, deleteMessages, respondMessages);
                                setMessageRead(Long.valueOf(currSmsId), Sms.MESSAGE_TYPE_SMS);
                            }
                        }

                        if (currSmsId == null)
                            text = getString(R.string.currentSMSs);

                        tv.setText(text);
                    }

                    handler.postDelayed(this, 60000); // now is every 1 minutes
                }
            };

            handler.postDelayed(runnable , 3300); // Every 120000 ms (2 minutes)
        }

    }

    /**
     * Checks if network is available
     * @return
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

 }
