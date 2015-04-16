package dk.glutter.izbrannick.nativesmsforwarder;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;

import dk.glutter.izbrannick.nativesmsforwarder.contacts.ContactsHandler;
import dk.glutter.izbrannick.nativesmsforwarder.contacts.SyncContacts;

public class SmsHandler
{
	
	private static Context context;
    private boolean deleteMessages;
    private boolean respondMessages;

    private ContactsHandler myContacs;
	private ArrayList<String> allGroupNames = null;
	private ArrayList<String> currentGroupNumbers = new ArrayList<>();
	private String phoneNr;
	private String besked;
	private String currentName;
	private String beskedLowCase;
	private String currentGroup;
    private String currSmsId;
    private boolean isGroupMsg;

    SmsHandler(Context context)
    {
        this.context = context;
    }

    SmsHandler(Context context, String nr, String msg, String currSmsId, boolean deleteMessages, boolean respondMessages)
	{
		this.context = context;
        this.currSmsId = currSmsId;
		myContacs = new ContactsHandler(context);
		phoneNr = nr;
		besked = msg;
		beskedLowCase = msg.toLowerCase();
        this.deleteMessages = deleteMessages;
        this.respondMessages = respondMessages;

        currentGroupNumbers = null;
        isGroupMsg = false;

        allGroupNames = myContacs.getAllGroupNames();

        if (!isValidMessage()){
                new LongOperation().execute(phoneNr, context.getString(R.string.help_msg), currSmsId);
        }
        // force Sync with google contacts
        SyncContacts.requestSync(context);
	}

    private boolean isValidMessage()
	{
        StringValidator.signup = context.getString(R.string.signup);
        StringValidator.resign = context.getString(R.string.resign);

		// --------- - Signup - ---------
		if (StringValidator.isSignup(beskedLowCase)) {
            currentGroup = StringValidator.words.get(1);
            currentName = "No Name";

            for (int i = 0; i <= StringValidator.words.size(); i++)
            {
                if (i == 3)
                {
                    currentName = StringValidator.words.get(i-1);
                }
                if (i == 4)
                {
                    currentName = StringValidator.words.get(2) + " " + StringValidator.words.get(i-1);
                    break;
                }
            }


            ArrayList<String> allNumbers = myContacs.getAllNumbersFromGroupName(currentGroup);
            int size = allNumbers.size();

            boolean exists = false;
            String tempNr;

            for (int i = 0; i < size; i++)
            {
                tempNr = allNumbers.get(i).replace(" ", "").replace("+45", "");
                phoneNr = phoneNr.replace(" ", "").replace("+45", "");

                if (tempNr.equalsIgnoreCase(phoneNr))
                    exists = true;
            }


            if (!exists) {
                Log.d("Creating contact", currentName + "-in-" + currentGroup);
                myContacs.createGoogleContact(currentName, phoneNr, currentGroup);

                Log.d("Signup sending", currentName);
                if (respondMessages) {
                    new LongOperation().execute(phoneNr, context.getString(R.string.signup_sucress)
                            + currentGroup + ". "
                            + context.getString(R.string.help_msg), currSmsId);
                }
                return true;
            } else {
                if (respondMessages) {
                    Log.d("DENY Respond", currentName);
                    new LongOperation().execute(phoneNr, context.getString(R.string.already_signed)
                            + currentGroup + ". "
                            + context.getString(R.string.help_msg), currSmsId);
                }
                return false;
            }
        }
		// --------- - Resign - ---------
		if (StringValidator.isResign(beskedLowCase)){
            currentGroup = StringValidator.words.get(1);
            // no name needed
            // currentName = StringValidator.words.get(2);
            removeUser(phoneNr, currentGroup);
            return true;
        }
		// --------- - GROUP Message - ---------
		if (StringValidator.isGroupMessage(beskedLowCase, context)){
            currentGroup = StringValidator.words.get(0);
			currentGroupNumbers = StringValidator.groupNumbers;
            if (currentGroupNumbers.size() > 0)
            {
                isGroupMsg = true;
                new LongOperation().execute(phoneNr, besked, currSmsId);
                return true;
            }
            return false;
        }
        // --------- - Create GROUP - ---------
        if (StringValidator.isCreateGroup(beskedLowCase, context)){
            currentGroup = StringValidator.words.get(2);
            ContactsHandler ch = new ContactsHandler(context);
            ch.createGoogleGroup(currentGroup);
            return true;
        }
        else{
            return false;
        }
	}

    private class LongOperation extends AsyncTask<String, Void, String> {
        final SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> iFragmentList;

        @Override
        protected String doInBackground(String... params)
        {
            iFragmentList = smsManager.divideMessage (params[1]);

            try {
                if (isGroupMsg) {
                    for (int i = 0; i < currentGroupNumbers.size(); i++) {
                        smsManager.sendMultipartTextMessage(currentGroupNumbers.get(i), null, iFragmentList, null, null);
                        Log.i("Sending msg to:", currentGroupNumbers.get(i));
                    }
                    if (deleteMessages) {
                        // try ---------  DELETE SMS
                        try {
                            Log.i("Try delete sms id:", params[2]);
                            delete_thread(params[2]);
                        } catch (Exception e) {
                            Log.d("Error deleting SMS ", params[0]+ " messge: " + params[1]);
                        }
                    }
                }
                else
                {
                    smsManager.sendMultipartTextMessage(params[0], null, iFragmentList, null, null);
                    Log.i("Send response to:", params[0]);
                    if (deleteMessages) {
                        // try ---------  DELETE SMS
                        try {
                            Log.i("Try delete sms id:", params[2]);
                            delete_thread(params[2]);
                        } catch (Exception e) {
                            Log.d("Error deleting SMS ", params[0]+ " messge: " + params[1]);
                        }
                    }
                }
            } catch (Exception e) {
                Thread.interrupted();
                Log.d("AAAAAA", e.getMessage());
            }
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            if (isGroupMsg) {
                iFragmentList = smsManager.divideMessage("Beskeden blev sendt til:" + currentGroup + ". Tak for det.");
                smsManager.sendMultipartTextMessage(phoneNr, null, iFragmentList, null, null);
            }
            // force Sync with google contacts
            SyncContacts.requestSync(context);
        }

        @Override
        protected void onPreExecute() {
            if (isGroupMsg) {
                iFragmentList = smsManager.divideMessage("Din besked bliver sendt til:" + currentGroup + ". Du vil modtage en bekræftelses besked når den er færdig med at sende.");
                smsManager.sendMultipartTextMessage(phoneNr, null, iFragmentList, null, null);
            }
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            //Toast.makeText(context, " onProgressUpdate ", Toast.LENGTH_SHORT).show();
        }

    }

    public static void delete_thread( String _id)
    {
        Cursor c = context.getContentResolver().query(
                Uri.parse("content://sms/"),new String[] {
                        "_id", "thread_id", "address", "person", "date","body" }, null, null, null);

        try {
            Log.d("Deleting SMS with ", " ID: "+  _id);
            while (c.moveToNext())
            {
                int id = c.getInt(0);
                String address = c.getString(2);
                if (id == Integer.parseInt(_id))
                {
                    context.getContentResolver().delete(
                            Uri.parse("content://sms/" + id), null, null);
                }

            }
        } catch (Exception e) {
            Log.d("Error deleting SMS ", " ID: "+  _id);
        }
    }
	
	// ------------------------------------------------------ Afmeld bruger
	private void removeUser(String phoneNr, String besked){

        String failedMsg = phoneNr+": " + "SMS: " + besked;
        try {
            myContacs.deleteContactFromGroup( phoneNr, currentGroup);
            if (respondMessages)
                new LongOperation().execute(phoneNr, context.getString(R.string.resign_sucress) + currentGroup, currSmsId);
        }catch (Exception e)
        {
            Log.d(failedMsg, e.getMessage());
            if (respondMessages)
                new LongOperation().execute(context.getString(R.string.ADMIN_NR), failedMsg, currSmsId);
        }

	}

}