package dk.glutter.izbrannick.nativesmsforwarder;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import java.util.ArrayList;

import dk.glutter.izbrannick.nativesmsforwarder.contacts.ContactsHandler;
import dk.glutter.izbrannick.nativesmsforwarder.contacts.SyncContacts;

public class SmsHandler
{
	private static Context context;
    private boolean feedback;
    private String accountName;

    private ContactsHandler myContacs;
	private ArrayList<String> allGroupNames;
	private ArrayList<String> currentGroupNumbers = new ArrayList<>();
	public String currentPhoneNr;
	public String besked;
	public String currentName;
	public String beskedLowCase;
	public String currentGroup;
    public boolean isGroupMsg;
    public String feedbackMessage;
    public String signupMessage;
    public String resignMessage;
    public boolean ignore_foreign_number;
    public boolean sender_information;
    public boolean group_members_only;

    SmsHandler(Context context)
    {
        this.context = context;
    }

    SmsHandler(Context context, String nr, String msg, boolean feedback, boolean ignore_foreign_numbers, boolean group_sender_information, boolean group_members_only)
	{
        accountName = ContactsHandler.googleAccountName;
		this.context = context;
		myContacs = new ContactsHandler(context);
		currentPhoneNr = nr;
		besked = msg;
		beskedLowCase = msg.toLowerCase();
        this.feedback = feedback;
        this.feedbackMessage = PreferenceManager.getDefaultSharedPreferences(context).getString("feedback_text", "");
        this.signupMessage = PreferenceManager.getDefaultSharedPreferences(context).getString("signup_text", "");
        this.resignMessage = PreferenceManager.getDefaultSharedPreferences(context).getString("resign_text", "");
        this.ignore_foreign_number = ignore_foreign_numbers;
        this.sender_information = group_sender_information;
        this.group_members_only = group_members_only;

        currentGroupNumbers = null;
        isGroupMsg = false;

        allGroupNames = null;
        allGroupNames = myContacs.getAllGroupNames();

        if (!isValidMessage()){
            new LongOperation().execute(currentPhoneNr, feedbackMessage);
        }
	}

    private boolean isValidMessage()
	{
        StringValidator.signup = context.getString(R.string.signup);
        StringValidator.resign = context.getString(R.string.resign);

        // --------- - Handle Foreign Number ?? - ---------
        if (ignore_foreign_number && StringValidator.isForeignNumber(currentPhoneNr))
            return false;

		// --------- - Signup - ---------
		if (StringValidator.isSignup(beskedLowCase)) {
            currentGroup = StringValidator.words.get(1);
            currentName = "No Name";
            currentGroupNumbers = myContacs.getAllNumbersFromGroupName(currentGroup);

            //fabric io log
            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName("Signup")
                    .putContentType("Group: " + currentGroup)
                    .putContentId(accountName));

            boolean userExists = isAlreadyInGroup(currentPhoneNr, currentGroup, currentGroupNumbers);

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
                            + feedbackMessage);
                }
                return true;
            } else {
                if (feedback) {
                    Log.d("DENY Respond", currentName);
                    new LongOperation().execute(currentPhoneNr, context.getString(R.string.already_signed)
                            + currentGroup + ". "
                            + feedbackMessage);
                }
                return false;
            }
        }
		// --------- - Resign - ---------
		if (StringValidator.isResign(beskedLowCase)){
            currentGroup = StringValidator.words.get(1);
            currentGroupNumbers = myContacs.getAllNumbersFromGroupName(currentGroup);
            boolean userExists = isAlreadyInGroup(currentPhoneNr, currentGroup, currentGroupNumbers);

            //fabric io log
            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName("Resign")
                    .putContentType("Group: " + currentGroup)
                    .putContentId(accountName));

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
            currentGroup = StringValidator.words.get(0);
            currentGroupNumbers = StringValidator.groupNumbers;
            boolean userExists = isAlreadyInGroup(currentPhoneNr, currentGroup, currentGroupNumbers);

            //fabric io log
            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName("Group Message")
                    .putContentType("Group: " + currentGroup)
                    .putContentId(accountName));

            if (group_members_only)
            {
                if (userExists)
                {
                    if ( currentGroupNumbers.size() > 0) {
                        isGroupMsg = true;
                        if (sender_information) {
                            new LongOperation().execute(currentPhoneNr, besked + " " + context.getString(R.string.sent_from) + currentPhoneNr);
                        } else {
                            new LongOperation().execute(currentPhoneNr, besked);
                        }
                        return true;
                    }
                }
                return false;
            }
            if (!group_members_only)
            {
                if (currentGroupNumbers.size() > 0) {
                    isGroupMsg = true;
                    if (sender_information) {
                        new LongOperation().execute(currentPhoneNr, besked + " " + context.getString(R.string.sent_from) + currentPhoneNr);
                    } else {
                        new LongOperation().execute(currentPhoneNr, besked);
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

            //fabric io log
            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName("Create Group")
                    .putContentType("Group: " + currentGroup)
                    .putContentId(accountName));

            return true;
        }
        else{
            return false;
        }
	}

    private boolean isAlreadyInGroup(String phoneNr, String currentGroup, ArrayList<String> currentGroupNumbers) {
        boolean exists = false;
        String tempNr;

        for (int i = 0; i < currentGroupNumbers.size(); i++)
        {
            tempNr = currentGroupNumbers.get(i).replace(" ", "").replace("+"+MainActivity.currentCountryCode, "");
            phoneNr = phoneNr.replace(" ", "").replace("+"+MainActivity.currentCountryCode, "");

            if (tempNr.equalsIgnoreCase(phoneNr))
                exists = true;
        }
        return exists;
    }

    private class LongOperation extends AsyncTask<String, Void, String> {
        SmsManager smsManager = null;
        ArrayList<String> iFragmentList = null;

        @Override
        protected String doInBackground(String... params)
        {
            Log.d("LongOperation", "doInBackground");

            iFragmentList = smsManager.divideMessage (params[1]);

            try {
                if (isGroupMsg) {
                    for (int i = 0; i < currentGroupNumbers.size(); i++) {
                        smsManager.sendMultipartTextMessage(currentGroupNumbers.get(i), null, iFragmentList, null, null);
                    }
                }
                else
                {
                    if(feedback)
                        smsManager.sendMultipartTextMessage(params[0], null, iFragmentList, null, null);
                }
            } catch (Exception e) {
                Thread.interrupted();
                Log.d("Exception", e.getMessage());
            }
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("LongOperation", "onPostExecute");

            SyncContacts.requestSync(context);
        }

        @Override
        protected void onPreExecute() {
            Log.d("LongOperation", "onPreExecute");


            smsManager = SmsManager.getDefault();
            if (isGroupMsg) {
                if (feedback) {
                    iFragmentList = null;
                    iFragmentList = smsManager.divideMessage( context.getString(R.string.message_was_sent_to, currentGroupNumbers.size()) + context.getString(R.string.message_is_being_sent) + currentGroup + context.getString(R.string.message_is_being_sent_2));
                    smsManager.sendMultipartTextMessage(currentPhoneNr, null, iFragmentList, null, null);
                }
            }
        }

        int m = 0;
        @Override
        protected void onProgressUpdate(Void... values) {
            m += 1;
            Log.d("LongOperation", "onProgressUpdate");
        }

    }
	
	// ------------------------------------------------------ Afmeld bruger
	private void removeUser(String phoneNr, String besked){

        String failedMsg = phoneNr+": " + "SMS: " + besked;
        try {
            myContacs.deleteContactFromGroup( phoneNr, currentGroup);
            if (feedback)
                new LongOperation().execute(phoneNr, resignMessage + currentGroup);
        }catch (Exception e)
        {
            Log.d(failedMsg, e.getMessage());
        }

	}

    private void logUser(String userIdentifyer, String userGroup) {
        // TODO: Use the current user's information
        // You can call any combination of these three methods
        Crashlytics.setUserIdentifier(userIdentifyer);
        Crashlytics.setUserEmail(accountName);
        Crashlytics.setUserName(userGroup);
    }


}