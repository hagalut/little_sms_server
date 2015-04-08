package dk.glutter.izbrannick.nativesmsforwarder.contacts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;

public class ContactsHandler {

	Context context;
	public static String googleAccountName = "someemail@gmail.com";

	public ContactsHandler(Context cont) {
		this.context = cont;

        getDefaultGoogleAccountName();
	}

    private void getDefaultGoogleAccountName()
    {
        Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
        Account[] accounts = AccountManager.get(context).getAccounts();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches() &&
                    account.type.equals("com.google")) {
                googleAccountName = account.name;
                break;
            }
        }
    }

	// ------------------------------------------------------ Create Google
	// Contact ()
	public void createGoogleContact(String name, String email, String phone, String group) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        String ctctName = getContactName(phone);

        if ( ctctName == null ) {

            ops = new ArrayList<ContentProviderOperation>();
            ops.add(ContentProviderOperation
                    .newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE,
                            "com.google")
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME,
                            googleAccountName)
                            // .withValue(RawContacts.AGGREGATION_MODE,
                            // RawContacts.AGGREGATION_MODE_DEFAULT)
                    .build());

            // ---------- Add Contacts First and Last names
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(StructuredName.GIVEN_NAME, name).build());

            // ---------- Add Contacts Mobile Phone Number
            ops.add(ContentProviderOperation
                    .newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                            Phone.TYPE_MOBILE).build());

            // ---------- Add Contacts Email
        /*
		ops.add(ContentProviderOperation
				.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, 0)
				.withValue(
						ContactsContract.Data.MIMETYPE,
						ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
				.withValue(ContactsContract.CommonDataKinds.Email.DATA, email)
				.withValue(ContactsContract.CommonDataKinds.Email.TYPE,
						ContactsContract.CommonDataKinds.Email.TYPE_WORK)
				.build());
        */
            // ---------- Add Contact To Group
            addContactToGroup(ops, group);
        }else
        {
            String id = "0";
            try{
                id = getContactID(phone);
            }
            catch (Exception e)
            {
                Log.d("GetNumberID error", e.getMessage());
            }
            // Name
            try {

                long contactID = Long.parseLong(getContactID(phone));

                long ctctRawId = Long.parseLong(id);
                long grpRawId =  Long.parseLong(getGroupId(group));

                ops.add(ContentProviderOperation.newUpdate(addToGroup(ctctRawId , grpRawId )).withValue(ContactsContract.RawContacts.ACCOUNT_TYPE,
                        "com.google").build());

            } catch (Exception e) {
                Log.w("UpdateContact", e.getMessage()+"");
                for(StackTraceElement ste : e.getStackTrace()) {
                    Log.w("UpdateContact", "\t" + ste.toString());
                }
                Context ctx = context.getApplicationContext();
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(ctx, "Update failed", duration);
                toast.show();
            }

        }

		try {
			ContentProviderResult[] results = context.getContentResolver()
					.applyBatch(ContactsContract.AUTHORITY, ops);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ------------------------------------------------------ Function return
	// group id by Group Title
	private String getGroupId(String name) {
		String selection = ContactsContract.Groups.DELETED + "=? and "
				+ ContactsContract.Groups.GROUP_VISIBLE + "=?";
		String[] selectionArgs = { "0", "1" };
		Cursor cursor = context.getContentResolver().query(
				ContactsContract.Groups.CONTENT_URI, null, selection,
				selectionArgs, null);
		cursor.moveToFirst();
		int len = cursor.getCount();

		String GroupId = null;
		for (int i = 0; i < len; i++) {
			String id = cursor.getString(cursor
					.getColumnIndex(ContactsContract.Groups._ID));
			String title = cursor.getString(cursor
					.getColumnIndex(ContactsContract.Groups.TITLE));

			if (title.equals(name)) {
				GroupId = id;
				break;
			}

			cursor.moveToNext();
		}
		cursor.close();

		return GroupId;
	}

	// ------------------------------------------------------- All Google Groups
	// get all Group Names
		public ArrayList<String> getAllGroupNames() {


			String selection = ContactsContract.Groups.DELETED + "=? and "
					+ ContactsContract.Groups.ACCOUNT_TYPE + "='com.google' and "
					+ ContactsContract.Groups.GROUP_VISIBLE + "=?";


			String[] selectionArgs = { "0", "1" };
			Cursor cursor = context.getContentResolver().query(
					ContactsContract.Groups.CONTENT_URI, null, selection,
					selectionArgs, null);
			cursor.moveToFirst();
			int len = cursor.getCount();

			ArrayList<String> groupNames = new ArrayList<String>();
			for (int i = 0; i < len; i++) {
				String title = cursor.getString(cursor
						.getColumnIndex(ContactsContract.Groups.TITLE));

				/*// to print out Group Type
				title += " T: " + cursor.getString(cursor
						.getColumnIndex(ContactsContract.Groups.ACCOUNT_TYPE));
				*/

				//if (cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.ACCOUNT_TYPE)).startsWith("com.google")) {
					groupNames.add(title);
					//break; // if I want anly first group
				//}

				cursor.moveToNext();
			}
			cursor.close();

			return groupNames;
		}

	// ------------------------------------------------------ get All phone #s
	// numbers
	public ArrayList<String> getAllNumbers() {
		Uri uri = ContactsContract.Data.CONTENT_URI;

		Cursor cursor = context.getContentResolver().query(uri, null, null,
				null, null);
		ArrayList<String> tempNumbers = null;

		while (cursor.moveToNext()) {
			String contactId = cursor.getString(cursor
					.getColumnIndex(ContactsContract.Contacts._ID));
			String hasPhone = cursor
					.getString(cursor
							.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

			if (Boolean.parseBoolean(hasPhone)) {
				// You know have the number so now query it like this
				Cursor phones = context.getContentResolver().query(
						ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
						null,
						ContactsContract.CommonDataKinds.Phone.CONTACT_ID
								+ " = " + contactId, null, null);

				while (phones.moveToNext()) {
					String phoneNumber = phones
							.getString(phones
									.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
					tempNumbers.add(phoneNumber);
				}
				phones.close();
			}
		}
		return tempNumbers;
	}

	// ------------------------------------------------------ getAll phone
	// numbers from specific group #name
	public ArrayList<String> getAllNumbersFromGroupName(String navn) {
		Cursor cursor = context.getContentResolver().query(
				ContactsContract.Groups.CONTENT_URI, null, null, null, null);
		cursor.moveToFirst();
		int len = cursor.getCount();

		ArrayList<String> numbers = new ArrayList<String>();
		for (int i = 0; i < len; i++) {
			String title = cursor.getString(cursor
					.getColumnIndex(ContactsContract.Groups.TITLE));
			String id = cursor.getString(cursor
					.getColumnIndex(ContactsContract.Groups._ID));

			if (title.equals(navn)) {
				String[] cProjection = { Contacts.DISPLAY_NAME,
						GroupMembership.CONTACT_ID };

				Cursor groupCursor = context
						.getContentResolver()
						.query(Data.CONTENT_URI,
								cProjection,
								CommonDataKinds.GroupMembership.GROUP_ROW_ID
										+ "= ?"
										+ " AND "
										+ ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE
										+ "='"
										+ ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE
										+ "'",
								new String[] { String.valueOf(id) }, null);
				if (groupCursor != null && groupCursor.moveToFirst()) {
					do {

						long contactId = groupCursor.getLong(groupCursor
								.getColumnIndex(GroupMembership.CONTACT_ID));

						Cursor numberCursor = context.getContentResolver()
								.query(Phone.CONTENT_URI,
										new String[] { Phone.NUMBER },
										Phone.CONTACT_ID + "=" + contactId,
										null, null);

						if (numberCursor.moveToFirst()) {
							int numberColumnIndex = numberCursor
									.getColumnIndex(Phone.NUMBER);
							String phoneNumber = numberCursor
									.getString(numberColumnIndex);
							numbers.add(phoneNumber);
							numberCursor.close();
						}
					} while (groupCursor.moveToNext());
					groupCursor.close();
				}
				break;
			}

			cursor.moveToNext();
		}
		cursor.close();

		return numbers;
	}

	// ------------------------------------------------------ Add Contact To
	// Group
	private void addContactToGroup(ArrayList<ContentProviderOperation> ops,
			String groupName) {
		String GroupId = getGroupId(groupName);
		ops.add(ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI)
				.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
				.withValue(
						ContactsContract.Data.MIMETYPE,
						ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
				.withValue(
						ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID,
						GroupId).build());
	}

	// ------------------------------------------------------ Remove Contact
	// From Group
	public boolean deleteContactFromGroup(String phoneNr, String group)
	{
		long rawContactId = Long.valueOf(getContactID(phoneNr));
	 	long groupId = Long.valueOf(getGroupId(group));
//		long groupId = getGroupRawIdFor(rawContactId);

		ContentResolver cr = context.getContentResolver();
		String where = ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID
				+ "="
				+ groupId
				+ " AND "
				+ ContactsContract.CommonDataKinds.GroupMembership.RAW_CONTACT_ID
				+ "=?"
				+ " AND "
				+ ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE
				+ "='"
				+ ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE
				+ "'";

		for (Long id : getRawContactIdsForContact(rawContactId)) {
			try {
				cr.delete(ContactsContract.Data.CONTENT_URI, where,
						new String[] { String.valueOf(id) });
			} catch (Exception e) {
				return false;
			}
		}
		return true;
	}

	private HashSet<Long> getRawContactIdsForContact(long contactId) {
		HashSet<Long> ids = new HashSet<Long>();

		Cursor cursor = context.getContentResolver().query(
				RawContacts.CONTENT_URI, new String[] { RawContacts._ID },
				RawContacts.CONTACT_ID + "=?",
				new String[] { String.valueOf(contactId) }, null);

		if (cursor != null && cursor.moveToFirst()) {
			do {
				ids.add(cursor.getLong(0));
			} while (cursor.moveToNext());
			cursor.close();
		}

		return ids;
	}

	private String getContactID(String phoneNr) {
		ContentResolver contentResolver = context.getContentResolver();

		Uri uri = Uri.withAppendedPath(
				ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(phoneNr));

		String[] projection = new String[] { PhoneLookup.DISPLAY_NAME,
				PhoneLookup._ID };

		Cursor cursor = contentResolver
				.query(uri, projection, null, null, null);

		if (cursor != null) {
			while (cursor.moveToNext()) {
				String contactName = cursor.getString(cursor
						.getColumnIndexOrThrow(PhoneLookup.DISPLAY_NAME));
				String contactId = cursor.getString(cursor
						.getColumnIndexOrThrow(PhoneLookup._ID));
				return contactId;
			}
			cursor.close();
		}
		return null;
	}

    /* -------------------- UNUSED Methods --------------------- */

    // ------------------------------------------------------ Add person ID To
    // Group
    private Uri addToGroup(long rawPersonId, long rawGroupId) {

        ContentValues values = new ContentValues();
        values.put(
                ContactsContract.CommonDataKinds.GroupMembership.RAW_CONTACT_ID,
                rawPersonId);
        values.put(
                ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID,
                rawGroupId);
        values.put(
                ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE,
                ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE);

        return this.context.getContentResolver().insert(
                ContactsContract.Data.CONTENT_URI, values);

    }

    // -------- Return Group RAW ID for contact ID
    private long getGroupRawIdFromID(Long contactId){
        Uri uri = Data.CONTENT_URI;
        String where = String.format(
                "%s = ? AND %s = ?",
                Data.MIMETYPE,
                GroupMembership.CONTACT_ID);

        String[] whereParams = new String[] {
                GroupMembership.CONTENT_ITEM_TYPE,
                Long.toString(contactId),
        };

        String[] selectColumns = new String[]{
                GroupMembership.GROUP_ROW_ID,
        };


        Cursor groupIdCursor = context.getContentResolver().query(
                uri,
                selectColumns,
                where,
                whereParams,
                null);
        try{
            if (groupIdCursor.moveToFirst()) {
                return groupIdCursor.getLong(0);
            }
            return Long.MIN_VALUE; // Has no group ...
        }finally{
            groupIdCursor.close();
        }
    }

    private long getGroupRawIdFromName(String groupName){
        Uri uri = Data.CONTENT_URI;
        String where = String.format(
                "%s = ? AND %s = ?",
                Data.MIMETYPE,
                GroupMembership.CONTACT_ID);

        String[] whereParams = new String[] {
                GroupMembership.DISPLAY_NAME,
                groupName,
        };

        String[] selectColumns = new String[]{
                GroupMembership.GROUP_ROW_ID,
        };


        Cursor groupIdCursor = context.getContentResolver().query(
                uri,
                selectColumns,
                where,
                whereParams,
                null);
        try{
            if (groupIdCursor.moveToFirst()) {
                return groupIdCursor.getLong(0);
            }
            return Long.MIN_VALUE; // Has no group ...
        }finally{
            groupIdCursor.close();
        }
    }

	public String getContactName(String phoneNr) {
		ContentResolver contentResolver = context.getContentResolver();

		Uri uri = Uri.withAppendedPath(
				ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(phoneNr));

		String[] projection = new String[] { PhoneLookup.DISPLAY_NAME,
				PhoneLookup._ID };

		Cursor cursor = contentResolver
				.query(uri, projection, null, null, null);

		if (cursor != null) {
			while (cursor.moveToNext()) {
				String contactName = cursor.getString(cursor
						.getColumnIndexOrThrow(PhoneLookup.DISPLAY_NAME));
				return contactName;
			}
			cursor.close();
		}
		return null;
	}

    // ------------------------------------------------------ Create Google
    // Group ()
    public void createGoogleGroup(String groupName) {
        Log.i("Creating group", groupName);
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        ops = new ArrayList<ContentProviderOperation>();

        ops.add(ContentProviderOperation
                .newInsert(ContactsContract.Groups.CONTENT_URI)
                .withValue(ContactsContract.Groups.TITLE, groupName)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE,
                        "com.google")
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME,
                        googleAccountName).build());
        try {

            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY,
                    ops);
            Log.i("Creating group", groupName + " is completed");

        } catch (Exception e) {
            Log.e("Error Creating Group", e.toString());
        }
    }

}
