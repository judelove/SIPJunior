package org.rootio.sipjunior;

/**
 * Created by HP Envy on 3/9/2018.
 */

public interface SipEventsNotifiable {
    void updateCallState(CallState callState);

    void updateRegistrationState(RegistrationState registrationState);
}
