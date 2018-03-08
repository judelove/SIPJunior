package org.rootio.sipjunior;

import android.content.IntentFilter;
import android.net.sip.SipManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class HomeActivity extends AppCompatActivity {

    private TextView mTextMessage;
    private SipManager mng;

    public void startSIP(View v)
    {
        SIPHandler hnd = new SIPHandler(this, "1011","1234","192.168.1.143",5060);
        this.mng = hnd.register();

        IntentFilter fltr = new IntentFilter();
        fltr.addAction("org.rootio.sipjunior.INCOMING_CALL");
        CallReceiver rcv = new CallReceiver(this.mng);
        this.registerReceiver(rcv, fltr);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


    }

    @Override
    protected void onStart()
    {
        super.onStart();
        Toast.makeText(this, "toast test", Toast.LENGTH_LONG).show();
    }

}
