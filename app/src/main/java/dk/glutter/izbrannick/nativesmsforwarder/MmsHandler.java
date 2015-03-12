package dk.glutter.izbrannick.nativesmsforwarder;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;

/**
 * Created by izbrannick on 23-02-2015.
 */


public class MmsHandler {
    private Context context;

    MmsHandler(Context context)
    {
        this.context = context;
    }

    private String  getMmsId() {
        String partId = "0";
        ContentResolver contentResolver = context.getContentResolver();
        final String[] projection = new String[]{"*"};
        Uri uri = Uri.parse("content://mms-sms/conversations/");
        Cursor query = contentResolver.query(uri, projection, null, null, null);

        if (query.moveToFirst()) {
            do {
                partId = query.getString(query.getColumnIndex("_id"));
            } while (query.moveToNext());
        }


        return partId;
    }

    public String getMMSTextContent() {
        String body = "";
        String selectionPart = "mid=" + getMmsId();
        Uri uri = Uri.parse("content://mms/part");

        Cursor cursor = context.getContentResolver().query(uri, null,
                selectionPart, null, null);

        if (cursor.moveToFirst()) {
            do {
                String partId = cursor.getString(cursor.getColumnIndex("_id"));
                String type = cursor.getString(cursor.getColumnIndex("ct"));
                if ("text/plain".equals(type)) {
                    String data = cursor.getString(cursor.getColumnIndex("_data"));

                    if (data != null) {
                        // implementation of this method below
                        body = getMmsText(partId);
                    } else {
                        body = cursor.getString(cursor.getColumnIndex("text"));
                    }
                }
            } while (cursor.moveToNext());
        }
        return body;
    }

    private String getMmsText(String id) {
        Uri partURI = Uri.parse("content://mms/part/" + id);
        InputStream is = null;
        StringBuilder sb = new StringBuilder();
        try {
            is = context.getContentResolver().openInputStream(partURI);
            if (is != null) {
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String temp = reader.readLine();
                while (temp != null) {
                    sb.append(temp);
                    temp = reader.readLine();
                }
            }
        } catch (IOException e) {}
        finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }
        return sb.toString();
    }

    private String getAddressNumber(int id) {
        String selectionAdd = new String("msg_id=" + id);
        String uriStr = MessageFormat.format("content://mms/{0}/addr", id);
        Uri uriAddress = Uri.parse(uriStr);
        Cursor cAdd = context.getContentResolver().query(uriAddress, null,
                selectionAdd, null, null);
        String name = null;
        if (cAdd.moveToFirst()) {
            do {
                String number = cAdd.getString(cAdd.getColumnIndex("address"));
                if (number != null) {
                    try {
                        Long.parseLong(number.replace("-", ""));
                        name = number;
                    } catch (NumberFormatException nfe) {
                        if (name == null) {
                            name = number;
                        }
                    }
                }
            } while (cAdd.moveToNext());
        }
        if (cAdd != null) {
            cAdd.close();
        }
        return name;
    }
}