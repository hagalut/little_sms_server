package dk.glutter.izbrannick.nativesmsforwarder.contacts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
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

        try {
            googleAccountName = getDefaultGoogleAccountName();
        }catch(Exception e){
            Log.e("GetAccount ERR", e.getMessage() );
        }
	}

    private String getDefaultGoogleAccountName()
    {
        Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
        Account[] accounts = AccountManager.get(context).getAccounts();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches() &&
                    account.type.equals("com.google")) {
                return account.name;
            }
        }
        return "someemail@gmail.com";
    }

	// ------------------------------------------------------ Create Google
	// Contact ()
	public void createGoogleContact(String name, String email, String phone, String group) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        String ctctName = null;

        try
        {
            ctctName = getContactName(phone);
        }
        catch(Exception e)
        {
            Log.d("getContactName", e.getMessage());
        }

        Log.d("Create GGL CTCT", "------------");
        if ( ctctName == null ) {
            Log.d("Create New CTCT", "------------");

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
                String str = "phone:" + phone + " id:" + id;
                Log.d("GetContact id", str);
            }
            catch (Exception e)
            {
                Log.d("GetNumberID error", e.getMessage());
            }

            Log.d("Adding USR-->GRP 1", "----------------");
            try {
                String grpID = getGroupId(group);

                long ctctRawId = Long.parseLong(id);
                long grpRowId =  Long.parseLong(grpID);

                Log.d("Adding USR-->GRP", ""+grpID+" "+ctctRawId+" "+grpRowId);

                addToGroup(ctctRawId , grpRowId );

            } catch (Exception e) {
                Log.w("UpdateContact", e.getMessage());
                for(StackTraceElement ste : e.getStackTrace()) {
                    Log.w("UpdateContact", "\t" + ste.toString());
                }

                Log.d("Adding USR-->GRP 2", "------------------");
                try {
                    ContentValues values = new ContentValues();
                    Uri rawContactUri = context.getContentResolver().insert(RawContacts.CONTENT_URI, values);
                    values.put(Data.RAW_CONTACT_ID, getContactID(phone));
                    values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
                    values.put(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, getGroupId(group));
                    //values.put(StructuredName.DISPLAY_NAME, "Test Testovic"); //the contact name
                    context.getContentResolver().insert(android.provider.ContactsContract.Data.CONTENT_URI, values);
                    //context.getContentResolver().update(TableName, values , android.provider.ContactsContract.Data.CONTENT_URI);
                    Toast.makeText(context.getApplicationContext(), "NEW Contact Added", Toast.LENGTH_SHORT).show();
                }catch (Exception e1)
                {
                    Log.w("Error addToGroup", e1.getMessage());
                    Toast.makeText(context.getApplicationContext(), "Error Adding Contact to group", Toast.LENGTH_SHORT).show();
                }
            }

        }
	}

	// ------------------------------------------------------ Function return
	// group id by Group Title
	private String getGroupId(String name) {
        String id = "0";
        String[] projection = new String[]{ContactsContract.Groups._ID,ContactsContract.Groups.TITLE};
        Cursor cursor = context.getContentResolver().query(ContactsContract.Groups.CONTENT_URI,
                projection, null, null, null);
        ArrayList<String> groupTitle = new ArrayList<String>();
        while(cursor.moveToNext()){
            String currName = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.TITLE));
            if (currName.equalsIgnoreCase(name)) {
                id = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups._ID));
                break;
            }
        }
        cursor.close();

        return id;
	}

	// ------------------------------------------------------- All Google Groups
	// get all Group Names
		public ArrayList<String> getAllGroupNames() {
            ArrayList<String> groupList = new ArrayList<>();
            String[] projection = new String[]{ContactsContract.Groups._ID,ContactsContract.Groups.TITLE};
            Cursor cursor = context.getContentResolver().query(ContactsContract.Groups.CONTENT_URI,
                    projection, null, null, null);
            ArrayList<String> groupTitle = new ArrayList<String>();
            while(cursor.moveToNext()){
                //id = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups._ID));
                groupList.add(cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.TITLE)));
			}
			cursor.close();

			return groupList;
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

			if (title.equalsIgnoreCase(navn)) {
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
		}

        cursor.close();
		return ids;
	}

	private String getContactID_OLD(String phoneNr) {
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

		}
        cursor.close();
		return null;
	}

    public String getContactID(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        //String name = "?";
        String contactId = "?";

        ContentResolver contentResolver = context.getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                //name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return contactId;
    }

    // ------------------------------------------------------ Add person ID To
    // Group
    private Uri addToGroup(long rawPersonId, long groupRowId) {

        Log.d("addToGroup", "trying to add person to group");
        ContentValues values = new ContentValues();
        values.put(ContactsContract.CommonDataKinds.GroupMembership.RAW_CONTACT_ID,rawPersonId);
        values.put(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, groupRowId);
        values.put(ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE,ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE);

        return this.context.getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);

    }

    // -------- Return Group ROW ID for contact ID
    private long getGroupRowIdFromID(Long contactId){
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

    private long getGroupRowIdFromGroupName(String groupName){
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

		}
        cursor.close();
		return null;
	}

    // ------------------------------------------------------ Create Google
    // Group ()
    public void createGoogleGroup(String groupName) {
        Log.i("Creating group", groupName);
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

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
