package org.rootio.sipjunior;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.sip.SipAudioCall;
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
    private SipProfile sipProfile;
    private SipAudioCall sipCall;
    private CallState callState = CallState.IDLE;
    private RegistrationState registrationState = RegistrationState.UNREGISTERED;
    private final String userName, password, domain;
    private final int port;
    private Context parent;

    SIPHandler(Context parent, String userName, String password, String domain, int port) {
        this.userName = userName;
        this.password = password;
        this.domain = domain;
        this.port = port;
        this.parent = parent;
        this.sipManager = SipManager.newInstance(this.parent);
        this.prepareSipProfile();
    }

    private void listenForIncomingCalls() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("org.rootio.sipjunior.INCOMING_CALL");
        CallReceiver receiver = new CallReceiver();
        this.parent.registerReceiver(receiver, filter);
    }

    private void prepareSipProfile() {
        try {
            SipProfile.Builder builder = null;
            builder = new SipProfile.Builder(this.userName, this.domain);
            builder.setPassword(this.password);
            this.sipProfile = builder.build();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    void register() {
        try {
            Intent intent = new Intent();
            intent.setAction("org.rootio.sipjunior.INCOMING_CALL");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.parent, 0, intent, Intent.FILL_IN_DATA);
            this.sipManager.open(this.sipProfile, pendingIntent, null);
            this.sipManager.register(this.sipProfile, 30, new RegistrationListener());
            this.listenForIncomingCalls();
        } catch (SipException e) {
            e.printStackTrace();
        }
    }

    public void deregister() {
        try {
            this.sipManager.unregister(this.sipProfile, new UnregistrationListener());
            // this.sipManager.close(this.sipProfile.getUriString());
        } catch (SipException e) {
            e.printStackTrace();
        }
    }

    public void call(String phoneNumber) {
        try {
            this.sipCall = this.sipManager.makeAudioCall(this.sipProfile.getUriString(), phoneNumber.contains("@") ? phoneNumber : phoneNumber + "@" + this.sipProfile.getSipDomain(), new CallListener(), 20);
        } catch (SipException e) {
            e.printStackTrace();
        }
    }

    public void hangup() {
        try {
            this.sipCall.endCall();
        } catch (SipException e) {
            e.printStackTrace();
        }
    }

    public void answer() {
        try {
            this.sipCall.answerCall(30);
            this.sipCall.startAudio();
            Log.i(TAG, "onReceive: call answered in recv");
        } catch (SipException e) {
            e.printStackTrace();
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

    class CallReceiver extends BroadcastReceiver {

        private CallListener listener;

        @Override
        public void onReceive(Context context, Intent intent) {
            try {

                this.listener = new CallListener();
                SIPHandler.this.sipCall = SIPHandler.this.sipManager.takeAudioCall(intent, listener);
                SIPHandler.this.notifyCallEvent("Incoming call from " + SIPHandler.this.sipCall.getPeerProfile().getUserName(),CallState.RINGING);
                Log.i(TAG, "onReceive: Received incoming call event");
            } catch (Exception e) {
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
        public void onCalling(SipAudioCall call)
        {
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
            notifyRegistrationEvent("Deregistration failed", RegistrationState.UNREGISTERED);
        }
    }


}
