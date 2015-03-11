package dk.glutter.izbrannick.groupsmsforwarder.contacts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.provider.ContactsContract;

/**
 * Created by izbrannick on 11-02-2015.
 * Synchronizing Contacts with gmail
 */
public class SyncContacts {

    public static void requestSync(Context context)
    {
        AccountManager am = AccountManager.get(context);
        Account[] accounts = am.getAccounts();

        for (Account account : accounts)
        {
            int isSyncable = ContentResolver.getIsSyncable(account, ContactsContract.AUTHORITY);

            if (isSyncable > 0)
            {
                Bundle extras = new Bundle();
                extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                ContentResolver.requestSync(accounts[0], ContactsContract.AUTHORITY, extras);
            }
        }
    }
}
