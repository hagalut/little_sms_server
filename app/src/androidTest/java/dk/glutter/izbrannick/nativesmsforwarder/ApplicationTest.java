package dk.glutter.izbrannick.nativesmsforwarder;

import android.app.Application;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.test.ApplicationTestCase;
import android.util.Log;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {

    private SmsHandler smsHandler;
    private MainActivity mainActivity;
    private String recieverNumber = "64978754";
    private String recievedMessage = "I'm from another country";
    String CountryID="";
    String CountryZipCode="0";
    String CurrentCountryCode="";

    public ApplicationTest() {
        super(Application.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mainActivity = new MainActivity();
        CurrentCountryCode = GetCountryZipCode();
        smsHandler = new SmsHandler(getContext(), recieverNumber, recievedMessage, "0", false, true, true, true, false);
        TelephonyManager manager = (TelephonyManager) getSystemContext().getSystemService(Context.TELEPHONY_SERVICE);
        //getNetworkCountryIso
        CountryID= manager.getSimCountryIso().toUpperCase();

        testForwarder_countryCode();

    }

    public void testForwarder_countryCode()
    {
        assertEquals(recieverNumber, smsHandler.currentPhoneNr);

        String[] rl= getSystemContext().getResources().getStringArray(R.array.CountryCodes);
        for(int i=0; i<rl.length; i++){
            String[] g=rl[i].split(",");

                CountryZipCode=g[0];
                if (!CountryZipCode.equals(CurrentCountryCode))
                    assertEquals(true, StringValidator.isForeignNumber(CountryZipCode + recieverNumber));

        }
    }

    public void testPreconditions() {
        assertNotNull("smsHandler is null", smsHandler);
        assertNotNull("mainActivity is null", mainActivity);
    }

    public String GetCountryZipCode(){
        String CountryID="";
        String CountryZipCode="0";

        try {
            TelephonyManager manager = (TelephonyManager) getSystemContext().getSystemService(Context.TELEPHONY_SERVICE);
            //getNetworkCountryIso
            CountryID= manager.getSimCountryIso().toUpperCase();
            String[] rl=getSystemContext().getResources().getStringArray(R.array.CountryCodes);
            for(int i=0;i<rl.length;i++){
                String[] g=rl[i].split(",");
                if(g[1].trim().equals(CountryID.trim())){
                    CountryZipCode=g[0];
                    break;
                }
            }
        }catch (Exception e)
        {
            Log.e("GetCountryZipCode", e.getMessage());
        }

        return CountryZipCode;
    }
}