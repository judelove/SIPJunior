package org.rootio.sipjunior;

import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipProfile;
import android.util.Log;

import static android.content.ContentValues.TAG;

/**
 * Created by Jude Mukundane on 07/03/2018.
 */

class IncomingCallHandler extends SipAudioCall.Listener {

    @Override
    public void onCallEstablished(SipAudioCall call)
    {
        call.startAudio();
    }

    @Override
    public void onCallEnded(SipAudioCall call)
    {

    }

    @Override
    public void onRinging (SipAudioCall call, SipProfile Caller)
    {
        try {
            call.answerCall(30);
            Log.i(TAG, "answered call...");
        } catch (SipException e) {
            e.printStackTrace();
        }
    }



}
