package org.rootio.sipjunior;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.gov.nist.javax.sip.message.Content;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.text.ParseException;

import static android.content.ContentValues.TAG;

/**
 * Created by Jude Mukundane on 06/03/2018.
 */

public class SIPHandler extends Service {

    private SipManager sipManager;
    private SipProfile sipProfile;
    private SipAudioCall sipCall;
    private CallState callState = CallState.IDLE;
    private RegistrationState registrationState = RegistrationState.UNREGISTERED;
    private String username, password, domain;
    private int port;
    private SipEventsNotifiable parent;
    private SharedPreferences prefs;

    @Override
    public IBinder onBind(Intent arg0) {
        BindingAgent bindingAgent = new BindingAgent(this);
        return bindingAgent;
    }

    @Override
    public void onCreate() {
        this.sipManager = SipManager.newInstance(this);
        this.prefs = this.getSharedPreferences("org.rootio.sipjunior", Context.MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //this.register();
        return Service.START_STICKY;

    }

    @Override
    public void onTaskRemoved(Intent intent) {
        this.deregister(false);
    }

    @Override
    public void onDestroy() {
        this.deregister(false);
    }


    public void setNotifiable(SipEventsNotifiable parent) //potentially bad design. perhaps announce changes over broadcasts?
    {
        this.parent = parent;
        //Ideally this happens when the home activity is reconnecting. no need to send info thrown on SIP events
        this.notifyRegistrationEvent(this.registrationState, null);
        this.notifyCallEvent(this.callState, null);
    }


    private void loadConfig() {
        if (this.prefs != null) {
            this.domain = prefs.getString("org.rootio.sipjunior.domain", "");
            this.username = prefs.getString("org.rootio.sipjunior.username", "");
            this.password = prefs.getString("org.rootio.sipjunior.password", "");
        }
    }

    private void listenForIncomingCalls() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("org.rootio.sipjunior.INCOMING_CALL");
        CallReceiver receiver = new CallReceiver();
        this.registerReceiver(receiver, filter);
    }

    private void prepareSipProfile() {
        try {
            SipProfile.Builder builder = new SipProfile.Builder(username, domain);
            builder.setPassword(password);
            builder.setPort(5060);
            this.sipProfile = builder.build();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    void register() {
        try {
            this.loadConfig();
            this.prepareSipProfile();

            if (this.username == "" || this.password == "" || this.domain == "") //Some servers may take blank username or passwords. modify accordingly..
            {
                //ideally this only happen in an unreged state
                ContentValues values = new ContentValues();
                values.put("errorCode", 0);
                values.put("errorMessage", "No config information found");
                this.notifyRegistrationEvent(this.registrationState, values);
                return;
            }

            Intent intent = new Intent();
            intent.setAction("org.rootio.sipjunior.INCOMING_CALL");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, Intent.FILL_IN_DATA);
            this.sipManager = SipManager.newInstance(this);
            this.sipManager.open(this.sipProfile, pendingIntent, null);
            Log.i(TAG, String.format("register: registering with username %s, password %s to domain %s", username, password, domain));
            this.sipManager.register(this.sipProfile, 30, new RegistrationListener());
            this.listenForIncomingCalls();
        } catch (SipException e) {
            e.printStackTrace();
            //Ideally if this happens, then reg fails, the status should still be DEREGISTERED
            ContentValues values = new ContentValues();
            values.put("errorCode", 0);
            values.put("errorMessage", "SIP Error occurred. Please check config and network availability");
            this.notifyRegistrationEvent(this.registrationState, values);
        }
    }

    public void deregister(boolean isRestart) {
        try {
            this.sipManager.close(this.sipProfile.getUriString());
            this.sipManager.unregister(this.sipProfile, new UnregistrationListener());
            //this.sipManager = null;
        } catch (SipException e) {
            e.printStackTrace();
            this.notifyRegistrationEvent(this.registrationState, null); //potential conflict of handling to the receiver
        }
    }

    public void call(String phoneNumber) {
        try {
            this.sipCall = this.sipManager.makeAudioCall(this.sipProfile.getUriString(), phoneNumber.contains("@") ? phoneNumber : phoneNumber + "@" + this.sipProfile.getSipDomain(), new CallListener(), 20);
        } catch (SipException e) {
            e.printStackTrace();
            this.showToast("A SIP error occurred. Check config details or Internet connection");
        }
    }

    public void hangup() {
        try {
            this.sipCall.endCall();
        } catch (SipException e) {
            e.printStackTrace();
            this.showToast("A SIP error occurred. Check config details or Internet connection");
        }
    }

    public void answer() {
        try {
            this.sipCall.answerCall(30);
            this.sipCall.startAudio();
            Log.i(TAG, "onReceive: call answered in recv");
        } catch (SipException e) {
            e.printStackTrace();
            this.showToast("A SIP error occurred. Check config details or Internet connection");
        }
    }

    private void notifyRegistrationEvent(final RegistrationState registrationState, final ContentValues values) {
        ((Activity) SIPHandler.this.parent).runOnUiThread(new Runnable() { //updating UI of other activity
            @Override
            public void run() {
                SIPHandler.this.registrationState = registrationState;
                (SIPHandler.this.parent).updateRegistrationState(SIPHandler.this.registrationState, values);
            }
        });
    }

    private void notifyCallEvent(final CallState callState, final ContentValues values) {
        ((Activity) SIPHandler.this.parent).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SIPHandler.this.callState = callState;
                (SIPHandler.this.parent).updateCallState(SIPHandler.this.callState, values);
            }
        });
    }

    private void showToast(final String message) {
        ((Activity) SIPHandler.this.parent).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SIPHandler.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }


    class CallReceiver extends BroadcastReceiver {

        private CallListener listener;

        @Override
        public void onReceive(Context context, Intent intent) {
            try {

                this.listener = new CallListener();
                SIPHandler.this.sipCall = SIPHandler.this.sipManager.takeAudioCall(intent, listener);
                ContentValues values = new ContentValues();
                values.put("otherParty", SIPHandler.this.sipCall.getPeerProfile().getUriString());
                SIPHandler.this.notifyCallEvent(CallState.RINGING, values);
                Log.i(TAG, "onReceive: Received incoming call event");
            } catch (SipException e) {
                e.printStackTrace();

            }
        }
    }


    class CallListener extends SipAudioCall.Listener {

        @Override
        public void onError(SipAudioCall call, int errorCode, String message) {
            try {
                ContentValues values = new ContentValues();
                values.put("otherParty", call.getPeerProfile().getUriString());
                SIPHandler.this.notifyCallEvent(CallState.IDLE, values);

                Log.i(TAG, "onError: " + message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCallEnded(SipAudioCall call) {
            try {
                SIPHandler.this.sipCall = null;
                ContentValues values = new ContentValues();
                values.put("otherParty", call.getPeerProfile().getUriString());
                SIPHandler.this.notifyCallEvent(CallState.IDLE, values);
                Log.i(TAG, "onCallEnded: Call Ended");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCallEstablished(SipAudioCall call) {
            SIPHandler.this.sipCall = call;
            SIPHandler.this.sipCall.startAudio();
            ContentValues values = new ContentValues();
            values.put("otherParty", call.getPeerProfile().getUriString());
            SIPHandler.this.notifyCallEvent(CallState.IDLE.INCALL, values);
        }

        @Override
        public void onRinging(SipAudioCall call, SipProfile caller) {
            Log.i(TAG, "onRinging: Call is ringing");
            try {
                SIPHandler.this.sipCall = call;
                SIPHandler.this.sipCall.answerCall(30);
                ContentValues values = new ContentValues();
                values.put("otherParty", call.getPeerProfile().getUriString());
                SIPHandler.this.notifyCallEvent(CallState.RINGING, values);
                Log.i(TAG, "onRinging: Call is ringing");
            } catch (SipException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCalling(SipAudioCall call) {
            Log.i(TAG, "onCalling: Calling...");
        }
    }

    class RegistrationListener implements SipRegistrationListener {

        @Override
        public void onRegistering(String localProfileUri) {
            notifyRegistrationEvent(RegistrationState.REGISTERING, null);
        }

        @Override
        public void onRegistrationDone(String localProfileUri, final long expiryTime) {
            ContentValues values  = new ContentValues();
            values.put("username", SIPHandler.this.username);
            values.put("domain", SIPHandler.this.domain);
            notifyRegistrationEvent(RegistrationState.REGISTERED, values);
        }

        @Override
        public void onRegistrationFailed(final String localProfileUri, final int errorCode, final String errorMessage) {
            ContentValues values = new ContentValues();
            values.put("errorCode", errorCode);
            values.put("errorMessage", errorMessage);
            notifyRegistrationEvent(RegistrationState.UNREGISTERED, values);
        }
    }

    class UnregistrationListener implements SipRegistrationListener {


        @Override
        public void onRegistering(String localProfileUri) {
            notifyRegistrationEvent(RegistrationState.DEREGISTERING, null);
        }

        @Override
        public void onRegistrationDone(String localProfileUri, final long expiryTime) {
            ContentValues values = new ContentValues();
            values.put("localProfileUri", localProfileUri);
            notifyRegistrationEvent(RegistrationState.UNREGISTERED, values);
        }

        @Override
        public void onRegistrationFailed(final String localProfileUri, final int errorCode, final String errorMessage) {
            ContentValues values = new ContentValues();
            values.put("errorCode", errorCode);
            values.put("errorMessage", errorMessage);
            notifyRegistrationEvent(RegistrationState.REGISTERED, values);
        }
    }

    class BindingAgent extends Binder {

        private Service service;

        BindingAgent(Service service) {
            this.service = service;
        }

        /**
         * Returns the service for which this class is providing a binding
         * connection
         *
         * @return Service object for the service bound to
         */
        Service getService() {
            return this.service;
        }
    }
}
