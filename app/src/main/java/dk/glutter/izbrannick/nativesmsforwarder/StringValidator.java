package dk.glutter.izbrannick.nativesmsforwarder;

/**
 * Created by izbrannick on 23-02-2015.
 */
public class StringValidator {

    // checks if message contains requested char ':'
    public static boolean isMessageValid(String message)
    {
        if (!message.isEmpty()) {
            String[] splitedMessage = message.split(" ");
            if (splitedMessage.length > 1) {
                String firstAndSecondWord = splitedMessage[0] + splitedMessage[1];
                if (firstAndSecondWord.contains(":")) {
                    return true;
                }
            }
        }
        return false;
    }

    // checks if message contains requested signup fraze
    public static boolean isSignup(String message)
    {
        if (!message.isEmpty()) {
            String[] splitedMessage = message.split(" ");
            if (splitedMessage.length > 1) {
                if (splitedMessage[0].equalsIgnoreCase("tilmeld") || splitedMessage[0].equalsIgnoreCase("signup") ) {
                    return true;
                }
            }
        }
        return false;
    }

    // checks if message contains requested resign fraze
    public static boolean isResign(String message)
    {
        if (!message.isEmpty()) {
            String[] splitedMessage = message.split(" ");
            if (splitedMessage.length > 1) {
                if (splitedMessage[0].equalsIgnoreCase("afmeld") || splitedMessage[0].equalsIgnoreCase("resign")) {
                    return true;
                }
            }
        }
        return false;
    }
}



