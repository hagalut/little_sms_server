package dk.glutter.izbrannick.nativesmsforwarder;

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
	final static String DEVELOPR_NR = Consts.DEV_NR;
    final static String ADMIN_NR = Consts.ADMIN_NR;
	
	private static Context context;
    private final boolean deleteMessages;

    private ContactsHandler myContacs;
	private ArrayList<String> allGroupNames = null;
	private ArrayList<String> currentGroupNumbers = null;
	private String phoneNr;
	private String besked;
	private String currentName;
	private String beskedLowCase;
	private String currentGroup;
    private String currSmsId;
	private boolean isTilmelding;
	private boolean isAfmelding;
	private boolean stopNow; // if there is an String exception


    SmsHandler(Context context, String nr, String msg, String currSmsId, boolean deleteMessages)
	{
		this.context = context;
        this.currSmsId = currSmsId;
		myContacs = new ContactsHandler(context);
		phoneNr = nr;
		besked = msg;
		beskedLowCase = msg.toLowerCase();
        this.deleteMessages = deleteMessages;
		
		// TODO: IM chose when msg starts with IMU
			allGroupNames = myContacs.getAllGroupNames();
			
			// trying to extract group name and user name
			try {
				currentGroup = findGroupAndUserNameFromMsg();
			} catch (Exception e) {
				// TODO: handle exception
				Log.d("IMUSMS group ", "something went wrong: " + e.getMessage());
				Log.d("IMUSMS msg ", msg);
				sendSmsThenDelete(DEVELOPR_NR, besked, currSmsId, deleteMessages);
				stopNow = true;
			}
			
			if (stopNow != true)
			{
				currentGroupNumbers = myContacs.getAllNumbersFromGroupName(currentGroup);
                boolean groupFound = false;
				
				for (int i = 0; i < allGroupNames.size(); i++) {
					//if (beskedLowCase.startsWith(allGroupNames.get(i).toLowerCase())) {
					//Log.d("currentGroup group "+i+": ", allGroupNames.get(i));
						if (allGroupNames.get(i).equalsIgnoreCase(currentGroup)) {
							currentGroup = allGroupNames.get(i);
                            groupFound = true;
							break;
						}
				}
                if (groupFound)
                    treatSmsLikeAKing();
                else
                    sendSmsThenDelete(phoneNr, "Gruppen " + currentGroup + " findes ikke.", currSmsId, deleteMessages);
			}
			if (stopNow) {
				sendSmsThenDelete(phoneNr, "Der gik noget galt prøv igen", currSmsId, deleteMessages);
			}
	}

    private String findGroupAndUserNameFromMsg()
	{
		String groupName = "";
		int i = 0;

		// --------- - TILMELD - ---------
		if (StringValidator.isSignup(beskedLowCase)){
			i = i + 8; // to ignore string: "tilmeld " - and get group name after string
			do {
				groupName += beskedLowCase.substring(i, i+1); //Eks. IMU:  i+m+u
				i++;
			} while (!beskedLowCase.substring(6, i).contains(":"));
			isTilmelding = true;
			// Exstract string after String: tilmeld & group
			currentName = beskedLowCase.substring(8+groupName.length());

            return groupName.toUpperCase().replace(" ", "");
		}
		// --------- - AFMELD - ---------
		if (StringValidator.isResign(beskedLowCase)) {
			i = i + 6; // to ignore string: "tilmeld " - and get group name after string
			do {
				groupName += beskedLowCase.substring(i, i+1); //Eks. IMU:  i+m+u
				i++;
			} while (!beskedLowCase.substring(6, i).contains(":"));
			isAfmelding = true;
			currentName = beskedLowCase.substring(6+groupName.length());

            return groupName.toUpperCase().replace(" ", "");
		}
		// --------- - GRUPPE BESKED - ---------
		else
		if (beskedLowCase.contains(":") && !beskedLowCase.startsWith("tilmeld") && !beskedLowCase.startsWith("afmeld")) {
			do {
				groupName += beskedLowCase.substring(i, i+1); //Eks. IMU:  i+m+u
				i++;
			} while (!beskedLowCase.substring(0, i).contains(":"));
			isTilmelding = false;
			isAfmelding = false;
		}

		return groupName.toUpperCase().replace(" ", "");
	}
	
	private void treatSmsLikeAKing()
	{
		if (currentGroup != null) {
			
			if (isTilmelding)
			{
                if ( !(myContacs.getAllNumbersFromGroupName(currentGroup).contains(phoneNr)) )
                {
                    Log.d("IMUSMS creating...", currentName +"-in-"+  currentGroup);
                    myContacs.createGoogleContact(currentName, "", phoneNr, currentGroup);

                    Log.d("Signup sending", currentName);
                    sendSmsThenDelete(phoneNr, "Du er tilmeldt til "
                            + currentGroup
                            + " sms-fon. For at sende sms til alle i gruppen skriv "
                            + currentGroup + " og din besked ", currSmsId, deleteMessages);

                    // force Sync phone contacts with gmail contacts
                    SyncContacts.requestSync(context);
                }else
                {
                    Log.d("DENY Respond", currentName);
                    sendSmsThenDelete(phoneNr, "Du er allerede tilmeldt til "
                            + currentGroup
                            + " sms-fon :-) . For at sende sms til alle i gruppen skriv "
                            + currentGroup + " og din besked ", currSmsId, deleteMessages);
                }

                return;
			}
			if (isAfmelding)
			{
                removeUser(phoneNr, currentGroup);
				// TODO: send en besked to Admin for at afmelde bruger - indtil remove virker
				// TODO: remove user - make it work

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
		}else
			sendSmsThenDelete(phoneNr, "Gruppen eksisterer ikke", currSmsId, deleteMessages);
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

        String failedMsg = phoneNr+"har prøvet at afmelde sig og mislykkes. " + "besked: " + besked;
        try {
            myContacs.deleteContactFromGroup( phoneNr, currentGroup);
            sendSmsThenDelete(phoneNr, "Du er afmeldt fra " + currentGroup + " sms-fon. ", currSmsId, deleteMessages);
        }catch (Exception e)
        {
            Log.d(failedMsg, e.getMessage());
            sendSmsThenDelete(ADMIN_NR, failedMsg, currSmsId, deleteMessages);
        }

	}
}