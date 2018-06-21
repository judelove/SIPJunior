package org.rootio.sipjunior;

import android.content.ContentValues;

/**
 * Created by Jude Mukundane on 3/9/2018.
 */

public interface SipEventsNotifiable {
    void updateCallState(CallState callState, ContentValues values);

    void updateRegistrationState(RegistrationState registrationState, ContentValues values);
}
