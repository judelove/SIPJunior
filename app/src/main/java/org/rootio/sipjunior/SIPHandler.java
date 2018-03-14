package org.rootio.sipjunior;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.util.Log;
import android.widget.Toast;

import java.text.ParseException;

import static android.content.ContentValues.TAG;

/**
 * Created by Jude Mukundane on 06/03/2018.
 */

public class SIPHandler {

    private SipManager sipManager;
    private SipProfile sipProfile;
    private SipAudioCall sipCall;
    private CallState callState = CallState.IDLE;
    private RegistrationState registrationState = RegistrationState.UNREGISTERED;
    private String username, password, domain;
    private int port;
    private Context parent;

    SIPHandler(Context parent) {

        this.parent = parent;
        this.sipManager = SipManager.newInstance(this.parent);
        this.listenForConfigChanges(); //TODO: move this to onReg and stop listening onUnReg
    }

    private void loadConfig() {
        SharedPreferences prefs = this.parent.getSharedPreferences("org.rootio.sipjunior", Context.MODE_PRIVATE);
        if (prefs != null) {
            this.domain = prefs.getString("org.rootio.sipjunior.domain", "");
            this.username = prefs.getString("org.rootio.sipjunior.username", "");
            this.password = prefs.getString("org.rootio.sipjunior.password", "");
        }
    }

    private void listenForConfigChanges() {
        this.parent.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (SIPHandler.this.registrationState == RegistrationState.REGISTERED) {
                    SIPHandler.this.deregister();
                    SIPHandler.this.register();
                }
            }
        }, new IntentFilter("org.rootio.sipjunior.CONFIG_CHANGE"));
    }

    private void listenForIncomingCalls() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("org.rootio.sipjunior.INCOMING_CALL");
        CallReceiver receiver = new CallReceiver();
        this.parent.registerReceiver(receiver, filter);
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
                this.notifyRegistrationEvent("Missing configuration information", this.registrationState);
                return;
            }

            Intent intent = new Intent();
            intent.setAction("org.rootio.sipjunior.INCOMING_CALL");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.parent, 0, intent, Intent.FILL_IN_DATA);
            this.sipManager = SipManager.newInstance(this.parent);
            this.sipManager.open(this.sipProfile);
            Log.i(TAG, String.format("register: registering with username %s, password %s to domain %s", username, password, domain));
            this.sipManager.register(this.sipProfile, 30, new RegistrationListener());
            //this.listenForIncomingCalls();
        } catch (SipException e) {
            e.printStackTrace();
            this.notifyRegistrationEvent("A SIP error occurred. Check config and Internet connection", this.registrationState);
        }
    }

    public void deregister() {
        try {
            this.sipManager.close(this.sipProfile.getUriString());
            this.sipManager.unregister(this.sipProfile, new UnregistrationListener());
            this.sipManager = null;
        } catch (SipException e) {
            e.printStackTrace();
            this.notifyRegistrationEvent("A SIP error occurred. Check config details or Internet connection", this.registrationState);
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

    private void notifyRegistrationEvent(final String message, final RegistrationState registrationState) {
        ((Activity) SIPHandler.this.parent).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SIPHandler.this.parent, message, Toast.LENGTH_LONG).show();
                SIPHandler.this.registrationState = registrationState;
                ((SipEventsNotifiable) SIPHandler.this.parent).updateRegistrationState(SIPHandler.this.registrationState);
            }
        });
    }

    private void notifyCallEvent(final String message, final CallState callState) {
        ((Activity) SIPHandler.this.parent).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SIPHandler.this.parent, message, Toast.LENGTH_LONG).show();
                SIPHandler.this.callState = callState;
                ((SipEventsNotifiable) SIPHandler.this.parent).updateCallState(SIPHandler.this.callState);
            }
        });
    }

    private void showToast(final String message) {
        ((Activity) SIPHandler.this.parent).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SIPHandler.this.parent, message, Toast.LENGTH_LONG).show();
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
                SIPHandler.this.notifyCallEvent("Incoming call from " + SIPHandler.this.sipCall.getPeerProfile().getUserName(), CallState.RINGING);
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
                SIPHandler.this.notifyCallEvent("Error", CallState.IDLE);

                Log.i(TAG, "onError: " + message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCallEnded(SipAudioCall call) {
            try {
                SIPHandler.this.sipCall = null;
                SIPHandler.this.notifyCallEvent("Call Ended", CallState.IDLE);
                Log.i(TAG, "onCallEnded: Call Ended");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCallEstablished(SipAudioCall call) {
            SIPHandler.this.sipCall = call;
            SIPHandler.this.sipCall.startAudio();
            SIPHandler.this.notifyCallEvent("Call started", CallState.IDLE.INCALL);
        }

        @Override
        public void onRinging(SipAudioCall call, SipProfile caller) {
            Log.i(TAG, "onRinging: Call is ringing");
            try {
                SIPHandler.this.sipCall = call;
                SIPHandler.this.sipCall.answerCall(30);
                SIPHandler.this.notifyCallEvent("Ringing", CallState.RINGING);
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
            notifyRegistrationEvent("Registering", RegistrationState.REGISTERING);
        }

        @Override
        public void onRegistrationDone(String localProfileUri, final long expiryTime) {
            notifyRegistrationEvent("Registration successful", RegistrationState.REGISTERED);
        }

        @Override
        public void onRegistrationFailed(final String localProfileUri, final int errorCode, final String errorMessage) {
            notifyRegistrationEvent("Registration failed", RegistrationState.UNREGISTERED);
        }
    }

    class UnregistrationListener implements SipRegistrationListener {

        @Override
        public void onRegistering(String localProfileUri) {
            notifyRegistrationEvent("Deregistering", RegistrationState.DEREGISTERING);
        }

        @Override
        public void onRegistrationDone(String localProfileUri, final long expiryTime) {
            notifyRegistrationEvent("Deregistration successful", RegistrationState.UNREGISTERED);
        }

        @Override
        public void onRegistrationFailed(final String localProfileUri, final int errorCode, final String errorMessage) {
            notifyRegistrationEvent(String.format("Deregistration failed with code %d and msg %s", errorCode,errorMessage), RegistrationState.REGISTERED);
        }
    }


}
