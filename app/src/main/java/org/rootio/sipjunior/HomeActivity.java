package org.rootio.sipjunior;

import android.content.IntentFilter;
import android.graphics.Color;
import android.net.sip.SipManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class HomeActivity extends AppCompatActivity implements SipEventsNotifiable {

    private SIPHandler hnd;


    public void toggleRegistration(View v) {
        if(((Switch)v).isChecked())
        {
            this.hnd.register();
        }
        else
        {
            this.hnd.deregister();
        }

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        this.hnd = new SIPHandler(this, "1011", "1234", "10.2.0.233", 5060);
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    public void call(View v)
    {
        TextView phoneNumberTv = (TextView)this.findViewById(R.id.phoneNumber_tv);
        String phoneNumber = phoneNumberTv.getText().toString();
        this.hnd.call(phoneNumber);

    }

    public void hangup(View v)
    {
        this.hnd.hangup();
    }

    private void loadConfiguration()
    {

    }

    @Override
    public void updateCallState(CallState callState)
    {
        Chronometer cr = ((Chronometer)findViewById(R.id.chronometer2));
        Button btn = ((Button)findViewById(R.id.call_btn));
        switch(callState)
        {
            case IDLE:
                cr.stop();
                cr.setVisibility(View.INVISIBLE);
                btn.setText("Call");
                btn.setBackgroundColor(Color.parseColor("#ff669900"));
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        HomeActivity.this.call(v);
                    }
                });
                break;
            case INCALL:
                cr.stop();
                cr.setVisibility(View.VISIBLE);
                btn.setText("End");
                btn.setBackgroundColor(Color.parseColor("#ff996600"));
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        HomeActivity.this.hangup(v);
                    }
                });
                break;
        }

    }

    @Override
    public void updateRegistrationState(RegistrationState registrationState)
    {
        Switch sw = ((Switch)findViewById(R.id.register_sw));
        switch(registrationState)
        {
            case REGISTERING:
                sw.setEnabled(false);
                sw.setText("Registering...");
                break;
            case REGISTERED:
                sw.setText("Registered");
                sw.setEnabled(true);
                sw.setChecked(true);
                break;
            case UNREGISTERED:
                sw.setText("Not Registered");
                sw.setEnabled(true);
                sw.setChecked(false);
        }

    }
}
