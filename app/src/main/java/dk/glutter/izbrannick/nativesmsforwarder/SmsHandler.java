package dk.glutter.izbrannick.nativesmsforwarder;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;

import dk.glutter.izbrannick.nativesmsforwarder.contacts.ContactsHandler;
import dk.glutter.izbrannick.nativesmsforwarder.contacts.SyncContacts;

public class SmsHandler
{
	
	private static Context context;
    private boolean deleteMessages;
    private boolean feedback;

    private ContactsHandler myContacs;
	private ArrayList<String> allGroupNames = null;
	private ArrayList<String> currentGroupNumbers = new ArrayList<>();
	public String currentPhoneNr;
	public String besked;
	public String currentName;
	public String beskedLowCase;
	public String currentGroup;
    public String currSmsId;
    public boolean isGroupMsg;
    public String feedbackMessage;
    public String signupMessage;
    public String resignMessage;
    public boolean ignore_foreign_number;
    public boolean sender_information;
    public boolean group_members_only;
    //public boolean feedback_after_group_message;
    public int number_of_group_members;

    SmsHandler(Context context)
    {
        this.context = context;
    }

    SmsHandler(Context context, String nr, String msg, String currSmsId, boolean deleteMessages, boolean feedback, boolean ignore_foreign_numbers, boolean group_sender_information, boolean group_members_only)
	{
		this.context = context;
        this.currSmsId = currSmsId;
		myContacs = new ContactsHandler(context);
		currentPhoneNr = nr;
		besked = msg;
		beskedLowCase = msg.toLowerCase();
        this.deleteMessages = deleteMessages;
        this.feedback = feedback;
        this.feedbackMessage = PreferenceManager.getDefaultSharedPreferences(context).getString("feedback_text", "");
        this.signupMessage = PreferenceManager.getDefaultSharedPreferences(context).getString("signup_text", "");
        this.resignMessage = PreferenceManager.getDefaultSharedPreferences(context).getString("resign_text", "");
        this.ignore_foreign_number = ignore_foreign_numbers;
        this.sender_information = group_sender_information;
        this.group_members_only = group_members_only;
        //this.feedback_after_group_message = feedback_after_group_message;

        currentGroupNumbers = null;
        isGroupMsg = false;
        this.number_of_group_members = 0;

        allGroupNames = myContacs.getAllGroupNames();

        if (!isValidMessage()){
                new LongOperation().execute(currentPhoneNr, feedbackMessage, currSmsId);
        }
        // force Sync with google contacts
        SyncContacts.requestSync(context);
	}

    private boolean isValidMessage()
	{
        StringValidator.signup = context.getString(R.string.signup);
        StringValidator.resign = context.getString(R.string.resign);
        boolean userExists = isAlreadyInGroup(currentPhoneNr, currentGroup);

        // --------- - Handle Foreign Number ?? - ---------
        if (ignore_foreign_number && StringValidator.isForeignNumber(currentPhoneNr))
            return false;

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
            if (!userExists) {
                Log.d("Creating contact", currentName + "-in-" + currentGroup);
                myContacs.createGoogleContact(currentName, currentPhoneNr, currentGroup);

                Log.d("Signup sending", currentName);
                if (feedback) {
                    new LongOperation().execute(currentPhoneNr, signupMessage
                            + currentGroup + ". "
                            + feedbackMessage, currSmsId);
                }
                return true;
            } else {
                if (feedback) {
                    Log.d("DENY Respond", currentName);
                    new LongOperation().execute(currentPhoneNr, context.getString(R.string.already_signed)
                            + currentGroup + ". "
                            + feedbackMessage, currSmsId);
                }
                return false;
            }
        }
		// --------- - Resign - ---------
		if (StringValidator.isResign(beskedLowCase)){
            if (userExists)
            {
                currentGroup = StringValidator.words.get(1);
                // no name needed
                // currentName = StringValidator.words.get(2);
                removeUser(currentPhoneNr, currentGroup);
                return true;
            }
        }
		// --------- - GROUP Message - ---------
		if (StringValidator.isGroupMessage(beskedLowCase, context)){
            if (group_members_only)
            {
                if (userExists)
                {
                    currentGroup = StringValidator.words.get(0);
                    currentGroupNumbers = StringValidator.groupNumbers;
                    if (currentGroupNumbers.size() > 0) {
                        isGroupMsg = true;
                        if (sender_information) {
                            new LongOperation().execute(currentPhoneNr, besked + " " + context.getString(R.string.sent_from) + currentPhoneNr, currSmsId);
                        } else {
                            new LongOperation().execute(currentPhoneNr, besked, currSmsId);
                        }
                        return true;
                    }
                }
                return false;
            }
            if (!group_members_only)
            {
                currentGroup = StringValidator.words.get(0);
                currentGroupNumbers = StringValidator.groupNumbers;
                if (currentGroupNumbers.size() > 0) {
                    isGroupMsg = true;
                    if (sender_information) {
                        new LongOperation().execute(currentPhoneNr, besked + " " + context.getString(R.string.sent_from) + currentPhoneNr, currSmsId);
                    } else {
                        new LongOperation().execute(currentPhoneNr, besked, currSmsId);
                    }
                    return true;
                }

                return false;
            }
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

    private boolean isAlreadyInGroup(String phoneNr, String currentGroup) {
        ArrayList<String> allNumbers = myContacs.getAllNumbersFromGroupName(currentGroup);
        int size = allNumbers.size();

        boolean exists = false;
        String tempNr;

        for (int i = 0; i < size; i++)
        {
            tempNr = allNumbers.get(i).replace(" ", "").replace("+"+MainActivity.currentCountryCode, "");
            phoneNr = phoneNr.replace(" ", "").replace("+"+MainActivity.currentCountryCode, "");

            if (tempNr.equalsIgnoreCase(phoneNr))
                exists = true;
        }
        return exists;
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
                        number_of_group_members ++;
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
                    if(feedback)
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
            /*
            if (feedback_after_group_message) {
                if (isGroupMsg) {
                    context.getString(number_of_group_members, R.string.message_was_sent_to);
                }
            }
            */
            // trigger Sync with google contacts
            SyncContacts.requestSync(context);
        }

        @Override
        protected void onPreExecute() {
            if (isGroupMsg) {
                if (feedback) {
                    iFragmentList = smsManager.divideMessage( context.getString(R.string.message_was_sent_to, number_of_group_members) + context.getString(R.string.message_is_being_sent) + currentGroup + context.getString(R.string.message_is_being_sent_2));
                    smsManager.sendMultipartTextMessage(currentPhoneNr, null, iFragmentList, null, null);
                }
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
            if (feedback)
                new LongOperation().execute(phoneNr, resignMessage + currentGroup, currSmsId);
        }catch (Exception e)
        {
            Log.d(failedMsg, e.getMessage());
        }

	}

}