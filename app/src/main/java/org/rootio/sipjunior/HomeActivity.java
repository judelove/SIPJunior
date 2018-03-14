package org.rootio.sipjunior;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Calendar;

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
        this.hnd = new SIPHandler(this);
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

    private void answer(View v) {
this.hnd.answer();
    }

    private void loadConfiguration()
    {

    }

    private void startChronometry()
    {
        Chronometer cr = ((Chronometer)findViewById(R.id.chronometer2));
        cr.setVisibility(View.VISIBLE);
        final long base = Calendar.getInstance().getTimeInMillis();
        cr.setBase(base);
        cr.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {

                long time = Calendar.getInstance().getTimeInMillis() - base;
                int h   = (int)(time /3600000);
                int m = (int)(time - h*3600000)/60000;
                int s= (int)(time - h*3600000- m*60000)/1000 ;
                chronometer.setText(String.format("%02d:%02d:%02d",h, m, s));
            }
        });
        cr.start();
    }

    private void stopChronometry() {
        Chronometer cr = ((Chronometer)findViewById(R.id.chronometer2));
        cr.setVisibility(View.INVISIBLE);
        cr.stop();
    }

    @Override
    public void updateCallState(CallState callState)
    {

        Button btn = ((Button)findViewById(R.id.call_btn));
        switch(callState)
        {
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
                break;
            case INCALL:
                this.startChronometry();
                btn.setText("End");
                btn.setBackgroundColor(Color.parseColor("#ff996600"));
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        HomeActivity.this.hangup(v);
                    }
                });
                break;
            case RINGING:
                btn.setText("Answer Call");
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        HomeActivity.this.answer(v);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        this.getMenuInflater().inflate(R.menu.navigation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.config:
                Intent intent = new Intent(this, Config.class);
                this.startActivity(intent);
                break;
        }
        return true;
    }
}
