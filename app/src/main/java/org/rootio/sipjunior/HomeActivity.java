package org.rootio.sipjunior;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class HomeActivity extends AppCompatActivity implements SipEventsNotifiable {

    private ComponentName registrationService;
    private SIPHandler handler;


    public void toggleRegistration(View v) {
        if (((Switch) v).isChecked()) {

            this.handler.register();
        } else {
            this.stopService(new Intent(this, SIPHandler.class));
             this.handler.deregister(false);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }


    @Override
    protected void onStart() {
        super.onStart();
        this.bindService(new Intent(this, SIPHandler.class), new ServiceConnectionAgent(), Context.BIND_AUTO_CREATE);
        this.registrationService = this.startService(new Intent(this, SIPHandler.class));
    }

    public void call(View v) {
        TextView phoneNumberTv = (TextView) this.findViewById(R.id.phoneNumber_tv);
        String phoneNumber = phoneNumberTv.getText().toString();
        this.handler.call(phoneNumber);

    }

    public void hangup(View v) {
        this.handler.hangup();
    }

    private void answer(View v) {
        this.handler.answer();
    }

    private void startChronometry() {
        Chronometer cr = ((Chronometer) findViewById(R.id.chronometer2));
        cr.setVisibility(View.VISIBLE);
        final long base = Calendar.getInstance().getTimeInMillis();
        cr.setBase(base);
        cr.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {

                long time = Calendar.getInstance().getTimeInMillis() - base;
                int h = (int) (time / 3600000);
                int m = (int) (time - h * 3600000) / 60000;
                int s = (int) (time - h * 3600000 - m * 60000) / 1000;
                chronometer.setText(String.format("%02d:%02d:%02d", h, m, s));
            }
        });
        cr.start();
    }

    private void stopChronometry() {
        Chronometer cr = ((Chronometer) findViewById(R.id.chronometer2));
        cr.setVisibility(View.INVISIBLE);
        cr.stop();
    }

    @Override
    public void updateCallState(CallState callState, ContentValues values) {

        Button btn = ((Button) findViewById(R.id.call_btn));
        switch (callState) {
            case IDLE:
                this.stopChronometry();
                btn.setText("Call");
                btn.setBackgroundColor(Color.parseColor("#ff669900"));
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        HomeActivity.this.call(v);
                    }
                });
                if(values != null && values.containsKey("otherParty")) //not being sent au moment
                {
                    Toast.makeText(this,"Call with " + values.getAsString("otherParty") +" terminated", Toast.LENGTH_LONG).show();
                }
                break;
            case INCALL:
                this.startChronometry();
                btn.setText("End");
                btn.setBackgroundColor(Color.parseColor("#ff990000"));
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        HomeActivity.this.hangup(v);
                    }
                });
                if(values != null && values.containsKey("otherParty")) //ideally check for direction and report if outgoing or incoming
                {
                    Toast.makeText(this,"In call with " + values.getAsString("otherParty") , Toast.LENGTH_LONG).show();
                }
                break;
            case RINGING:
                btn.setText("Answer Call");
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        HomeActivity.this.answer(v);
                    }
                });
                if(values != null && values.containsKey("otherParty"))
                {
                    Toast.makeText(this,"Incoming call from " + values.getAsString("otherParty"), Toast.LENGTH_LONG).show();
                }
                break;
            case CALLING:
                btn.setText("Cancel");
                btn.setBackgroundColor(Color.parseColor("#ff996600"));
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        HomeActivity.this.hangup(v);
                    }
                });
                if(values != null && values.containsKey("otherParty"))
                {
                    Toast.makeText(this,"Calling " + values.getAsString("otherParty") , Toast.LENGTH_LONG).show();
                }
                break;
        }

    }


   @Override
    public void updateRegistrationState(RegistrationState registrationState, ContentValues values) {
        Switch sw = ((Switch) findViewById(R.id.register_sw));
        TextView acc = ((TextView)findViewById(R.id.account_tv));
        switch (registrationState) {
            case REGISTERING:
                sw.setEnabled(false);
                sw.setText("Registering...");
                break;
            case REGISTERED:
                sw.setText("Registered");
                sw.setEnabled(true);
                sw.setChecked(true);
                acc.setVisibility(View.VISIBLE);
                if(values != null && values.containsKey("username")) //Requested notifications will not come with values
                {
                    acc.setText(values.getAsString("username") + "@" + values.getAsString("domain"));
                    Toast.makeText(this,"Registered " + values.getAsString("username") +"@"+ values.getAsString("domain"), Toast.LENGTH_LONG).show();
                }
                break;
            case UNREGISTERED:
                sw.setText("Not Registered");
                sw.setEnabled(true);
                sw.setChecked(false);
                acc.setVisibility(View.INVISIBLE);
                if(values != null && values.containsKey("localProfileUri"))
                {
                    Toast.makeText(this,"Unregistered " + values.getAsString("localProfileUri"), Toast.LENGTH_LONG).show();
                }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.navigation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.config:
                Intent intent = new Intent(this, Config.class);
                this.startActivity(intent);
                break;
        }
        return true;
    }

    public class ServiceConnectionAgent implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SIPHandler.BindingAgent bindingAgent = (SIPHandler.BindingAgent) service;
            HomeActivity.this.handler = (SIPHandler) bindingAgent.getService();
            HomeActivity.this.handler.setNotifiable(HomeActivity.this);

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }


    }

}
