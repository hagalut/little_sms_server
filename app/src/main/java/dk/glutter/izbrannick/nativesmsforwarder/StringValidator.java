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
    public static ArrayList<String> words = null;
    public static ArrayList<String> isSignup(String message)
    {
        if (!message.isEmpty()) {
            String[] splitedMessage = message.split(" ");
            if (splitedMessage.length > 1) {
                if (splitedMessage[0].equalsIgnoreCase("tilmeld") || splitedMessage[0].equalsIgnoreCase("signup") ) {
                    words = new ArrayList<String>();
                    for (int i = 0; i < splitedMessage.length; i++)
                        words.add(splitedMessage[i]);
                }
            }
        }
        return words;
    }

    // checks if message contains requested resign fraze
    public static ArrayList<String> isResign(String message)
    {
        if (!message.isEmpty()) {
            String[] splitedMessage = message.split(" ");
            if (splitedMessage.length > 1) {
                if (splitedMessage[0].equalsIgnoreCase("afmeld") || splitedMessage[0].equalsIgnoreCase("resign") ) {
                    words = new ArrayList<String>();
                    for (int i = 0; i < splitedMessage.length; i++)
                        words.add(splitedMessage[i]);
                }
            }
        }
        return words;
    }

    public static ArrayList<String> groupNumbers = null;
    public static ArrayList<String> isGroupMessage(String message, Context context)
    {
        if (!message.isEmpty()) {
            String[] splitedMessage = message.split(" ");
            if (splitedMessage.length > 1) {
                groupNumbers = new ArrayList<String>();
                words = new ArrayList<String>();
                for (int i = 0; i < splitedMessage.length; i++)
                    words.add(splitedMessage[i]);
                ContactsHandler myContacs = new ContactsHandler(context);
                try {
                    groupNumbers = myContacs.getAllNumbersFromGroupName(splitedMessage[0].toUpperCase());
                } catch (Exception e) {
                    Log.e("Error number from Group", e.getMessage());
                }
            }
        }
        return groupNumbers;
    }
}



