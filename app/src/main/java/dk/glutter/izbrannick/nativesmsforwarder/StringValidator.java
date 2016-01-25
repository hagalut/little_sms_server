package dk.glutter.izbrannick.nativesmsforwarder;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

import dk.glutter.izbrannick.nativesmsforwarder.contacts.ContactsHandler;

/**
 * Created by izbrannick on 23-02-2015.
 */
public class StringValidator {

    // checks if message contains requested signup fraze
    // [0]Signup [1]Group Name [2]Name
    public static ArrayList<String> words;

    // array is filled if isGroupMessage() executed | else NULL
    public static ArrayList<String> groupNumbers;

    public static String signup;
    public static String resign;

    public static boolean isForeignNumber(String number)
    {
        if (number.startsWith("+") && number.startsWith( "+" + MainActivity.currentCountryCode))
        {
            return false;
        }
        if (number.startsWith("00") && number.startsWith( "00" + MainActivity.currentCountryCode))
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public static boolean isSignup(String message)
    {
        words = null;
        if (!message.isEmpty()) {
            String[] splitedMessage = message.split(" ");
            if (splitedMessage.length > 1) {
                if (splitedMessage[0].equalsIgnoreCase(signup)) {
                    words = new ArrayList<>();
                    for (int i = 0; i < splitedMessage.length; i++)
                    {
                        words.add(splitedMessage[i]);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    // checks if message contains requested resign fraze
    public static boolean isResign(String message)
    {
        words = null;
        if (!message.isEmpty()) {
            String[] splitedMessage = message.split(" ");
            if (splitedMessage.length > 1) {
                if (splitedMessage[0].equalsIgnoreCase(resign)) {
                    words = new ArrayList<String>();
                    for (int i = 0; i < splitedMessage.length; i++)
                    {
                        words.add(splitedMessage[i]);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isGroupMessage(String message, Context context)
    {
        groupNumbers = null;
        words = null;
        if (!message.isEmpty()) {
            String[] splitedMessage = message.split(" ");
            if (splitedMessage.length > 1) {
                groupNumbers = new ArrayList<>();
                words = new ArrayList<>();
                for (int i = 0; i < splitedMessage.length; i++)
                    words.add(splitedMessage[i]);
                ContactsHandler myContacs = new ContactsHandler(context);
                if (isAGroup(myContacs, words.get(0))) {
                    try {
                        myContacs.getAllGroupNames();

                        groupNumbers = myContacs.getAllNumbersFromGroupName(splitedMessage[0]);
                    } catch (Exception e) {
                        Log.e("Numbers from Group", e.getMessage());
                        return false;
                    }
                    return true;
                }
                else
                {
                    return false;
                }
            }
        }
        return false;
    }

    public static boolean isCreateGroup(String message, Context context)
    {
        groupNumbers = null;
        words = null;
        if (!message.isEmpty() && message.startsWith("create group ")) {
            String[] splitedMessage = message.split(" ");
            if (splitedMessage.length > 1) {
                groupNumbers = new ArrayList<>();
                words = new ArrayList<>();
                for (int i = 0; i < splitedMessage.length; i++)
                    words.add(splitedMessage[i]);
                ContactsHandler myContacs = new ContactsHandler(context);
                try {
                    groupNumbers = myContacs.getAllNumbersFromGroupName(splitedMessage[0].toUpperCase());
                } catch (Exception e) {
                    Log.e("Error GroupNumbers", e.getMessage());
                    return false;
                }
                return true;
            }
        }
        else {
            return false;
        }
        return false;
    }

    public static boolean isTesting(String message, Context context)
    {
        if (message.equalsIgnoreCase("test")) {
            ContactsHandler myContacs = new ContactsHandler(context);

            Log.i("GRP names:", "" + myContacs.getAllGroupNames());

            try {
                myContacs.createGoogleGroup("TestGroup");
            } catch (Exception e) {
                Log.e("Error CreateGrp", e.getMessage());
            }

            try {
                groupNumbers = myContacs.getAllNumbersFromGroupName("Test");
            } catch (Exception e) {
                Log.e("Error GrpNumbers", e.getMessage());

            }
            return true;
        }
        return false;
    }

    private static boolean isAGroup( ContactsHandler myContacs, String groupName)
    {
        ArrayList<String> allGroupNames;
        try {
            allGroupNames = myContacs.getAllGroupNames();
        }catch (Exception e)
        {
            Log.e("Error GroupNames", e.getMessage());
            return false;
        }
        for (int i = 0; i < allGroupNames.size(); i++) {
            if (allGroupNames.get(i).equalsIgnoreCase(groupName)) {
                groupName = allGroupNames.get(i);
                return true;
            }
        }
        return false;
    }
}



