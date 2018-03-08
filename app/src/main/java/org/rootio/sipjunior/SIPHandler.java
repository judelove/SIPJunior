package org.rootio.sipjunior;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.text.ParseException;

import static android.content.ContentValues.TAG;

/**
 * Created by Jude Mukundane on 06/03/2018.
 */

public class SIPHandler {

    private SipManager sipManager;
    private SipProfile SIPProfile;
    private final String userName, password, domain;
    private final int port;
    private Context parent;

    SIPHandler(Context parent, String userName, String password, String domain, int port)
    {
        this.userName = userName;
        this.password = password;
        this.domain = domain;
        this.port = port;
        this.parent = parent;
        this.sipManager = SipManager.newInstance(this.parent);
        this.prepareSipProfile();

    }

    private void prepareSipProfile()
    {
        SipProfile.Builder builder = null;
        try {
            builder = new SipProfile.Builder(this.userName, this.domain);

        builder.setPassword(this.password);
        this.SIPProfile = builder.build();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    
    SipManager register()
    {
        try {
            Intent intent = new Intent();
            intent.setAction("org.rootio.sipjunior.INCOMING_CALL");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.parent, 0, intent, Intent.FILL_IN_DATA);
            this.sipManager.open(this.SIPProfile, pendingIntent, null);
            Listener lst = new Listener();
            this.sipManager.register(this.SIPProfile, 30, lst);
            //this.sipManager.setRegistrationListener(this.SIPProfile.getUriString(), lst);
return this.sipManager;
        } catch (SipException e) {
            e.printStackTrace();
            return null;
        }
    }

    class Listener implements SipRegistrationListener
    {

        @Override
        public void onRegistering(String localProfileUri) {
            showToast("reging");
            Log.i(TAG, "onRegistering: reging");
        }

        @Override
        public void onRegistrationDone(String localProfileUri, long expiryTime) {
            showToast("reged");
            Log.i(TAG, "onRegistrationDone: reged with expiry time" + expiryTime);
        }

        @Override
        public void onRegistrationFailed(String localProfileUri, int errorCode, String errorMessage) {
            showToast("failed");
            Log.i(TAG, "onRegistrationFailed: " +errorMessage);
        }

        private void showToast(final String message)
        {
            Runnable msg = new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    Toast.makeText(SIPHandler.this.parent, message, Toast.LENGTH_LONG).show();
                }
            };
            Thread thr = new Thread(msg);
            thr.start();
        }
    }
}
