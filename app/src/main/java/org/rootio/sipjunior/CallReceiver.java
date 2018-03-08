package org.rootio.sipjunior;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.sip.SipAudioCall;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.util.Log;

import static android.content.ContentValues.TAG;

/**
 * Created by Jude Mukundane on 07/03/2018.
 */

public class CallReceiver extends BroadcastReceiver {

    private IncomingCallHandler hndl;
    private SipManager mng;

    CallReceiver (SipManager mng)
    {
        this.mng = mng;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                @Override
                public void onRinging(SipAudioCall call, SipProfile caller) {
                    try {
                        call.answerCall(30);
                        Log.i(TAG, "onRinging: Call answered n listener");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            SipAudioCall incomingCall = mng.takeAudioCall(intent, listener);
            incomingCall.answerCall(30);
            incomingCall.startAudio();
            Log.i(TAG, "onReceive: call answerede in recv");
            //
            //
            // incomingCall.setSpeakerMode(true);

        } catch (Exception e) {
           e.printStackTrace();
        }
    }
}
