package dk.glutter.izbrannick.nativesmsforwarder;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
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
	private boolean isTilmelding = false;
	private boolean isAfmelding = false;

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
        this.respondMessages = deleteMessages;

        allGroupNames = myContacs.getAllGroupNames();

        if (isValidMessage())
        {
            boolean groupFound = false;

            for (int i = 0; i < allGroupNames.size(); i++) {
                    if (allGroupNames.get(i).equalsIgnoreCase(currentGroup)) {
                        currentGroup = allGroupNames.get(i);
                        groupFound = true;
                        break;
                    }
            }
            if (groupFound)
                treatSmsLikeAKing();
            else
            {
                if (respondMessages)
                sendSmsThenDelete(phoneNr, context.getString(R.string.no_group), currSmsId, deleteMessages);
            }

        }
        else {
            if (respondMessages)
            sendSmsThenDelete(phoneNr, context.getString(R.string.help_msg), currSmsId, deleteMessages);
        }
	}

    private boolean isValidMessage()
	{
        StringValidator.signup = context.getString(R.string.signup);
        StringValidator.resign = context.getString(R.string.resign);

		// --------- - Signup - ---------
		if (StringValidator.isSignup(beskedLowCase)) {
            currentGroup = StringValidator.words.get(1);

            for (int i = 0; i <= StringValidator.words.size(); i++)
            {
                if (i == 2)
                {
                    currentName = StringValidator.words.get(2);
                }
                if (i == 3)
                {
                    currentName = StringValidator.words.get(2) + " " + StringValidator.words.get(3);
                    break;
                }else
                {
                    currentName = "No Name";
                }
            }
            isTilmelding = true;
            return true;
        }
		// --------- - Resign - ---------
		if (StringValidator.isResign(beskedLowCase)){
            currentGroup = StringValidator.words.get(1);
            // no name needed
            // currentName = StringValidator.words.get(2);
            isAfmelding = true;
            return true;
        }
		// --------- - GROUP Message - ---------
		if (StringValidator.isGroupMessage(beskedLowCase, context)){
            currentGroup = StringValidator.words.get(0);
			currentGroupNumbers = StringValidator.groupNumbers;
            return true;
        }
        else{
            return false;
        }
	}
	
	private void treatSmsLikeAKing()
	{
		if (currentGroup != null) {
			
			if (isTilmelding)
			{
                if ( !(myContacs.getAllNumbersFromGroupName(currentGroup).contains(phoneNr)) )
                {
                    Log.d("Creating contact", currentName +"-in-"+  currentGroup);
                    myContacs.createGoogleContact(currentName, "", phoneNr, currentGroup);

                    Log.d("Signup sending", currentName);
                    if (respondMessages) {
                        sendSmsThenDelete(phoneNr, context.getString(R.string.signup_sucress)
                                + currentGroup + ". "
                                + context.getString(R.string.help_msg), currSmsId, deleteMessages);
                    }

                    // force Sync phone contacts with gmail contacts
                    SyncContacts.requestSync(context);
                }else
                {
                    if (respondMessages) {
                        Log.d("DENY Respond", currentName);
                        sendSmsThenDelete(phoneNr, context.getString(R.string.already_signed)
                                + currentGroup + ". "
                                + context.getString(R.string.help_msg), currSmsId, deleteMessages);
                    }
                }

                return;
			}
			if (isAfmelding)
			{
                removeUser(phoneNr, currentGroup);

                // force Sync with google contacts
                SyncContacts.requestSync(context);

                return;
			}
			else
			{
				for (int i = 0; i < currentGroupNumbers.size(); i++)
				{
					sendSmsThenDelete(currentGroupNumbers.get(i), besked, currSmsId, deleteMessages);
					Log.d("IMUSMS sending to", currentGroupNumbers.get(i));
				}
                return;
			}
		}else {
            if (respondMessages)
            sendSmsThenDelete(phoneNr, context.getString(R.string.no_group), currSmsId, deleteMessages);
        }
	}

	public static boolean sendSmsThenDelete(final String aDestination, final String aMessageText, final String currSmsId, final boolean deleteMessages)
	{
        final SmsManager smsManager = SmsManager.getDefault();
        final ArrayList<String> iFragmentList = smsManager.divideMessage (aMessageText);

        try {
            Log.d("Sendign SMS to...", aDestination +" messge: "+  aMessageText);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    smsManager.sendMultipartTextMessage(aDestination, null, iFragmentList, null, null);

                    if (deleteMessages) {
                        // try ---------  DELETE SMS
                        try {
                            // TODO: if failed to delete sms add sms ID to ignore list
                            delete_thread(currSmsId);
                        } catch (Exception e) {
                            Log.d("Error deleting SMS ", aDestination + " messge: " + aMessageText);
                        }
                    }

                }
            }, 3300);
        }
        catch (Exception e)
        {
            Log.d("Error sending SMS to...", aDestination +" messge: "+  aMessageText);
            return false;
        }
		return true;
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
            sendSmsThenDelete(phoneNr,context.getString(R.string.resign_sucress) + currentGroup, currSmsId, deleteMessages);
        }catch (Exception e)
        {
            Log.d(failedMsg, e.getMessage());
            if (respondMessages)
            sendSmsThenDelete(context.getString(R.string.ADMIN_NR), failedMsg, currSmsId, deleteMessages);
        }

	}

}