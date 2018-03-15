package org.rootio.sipjunior;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class Config extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config);
       this.loadConfig();
    }

    public void saveConfiguration(View v)
    {
        SharedPreferences prefs = this.getSharedPreferences("org.rootio.sipjunior", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("org.rootio.sipjunior.domain",((EditText)this.findViewById(R.id.domain_et)).getText().toString().trim());
        editor.putString("org.rootio.sipjunior.username",((EditText)this.findViewById(R.id.username_et)).getText().toString().trim());
        editor.putString("org.rootio.sipjunior.password",((EditText)this.findViewById(R.id.password_et)).getText().toString().trim());
        editor.commit();
        Toast.makeText(this, "Settings saved. Please re-register to use new configuration", Toast.LENGTH_SHORT).show();

        //fire off notification of account change to trigger re-registration
        this.announceConfigChange();
    }

    private void announceConfigChange()
    {
        this.sendBroadcast(new Intent("org.rootio.sipjunior.CONFIG_CHANGE"));
    }

    private void loadConfig()
    {
        SharedPreferences prefs = this.getSharedPreferences("org.rootio.sipjunior", Context.MODE_PRIVATE);
        if(prefs != null) {
            ((EditText) this.findViewById(R.id.domain_et)).setText(prefs.getString("org.rootio.sipjunior.domain", ""));
            ((EditText) this.findViewById(R.id.username_et)).setText(prefs.getString("org.rootio.sipjunior.username", ""));
            ((EditText) this.findViewById(R.id.password_et)).setText(prefs.getString("org.rootio.sipjunior.password", ""));
        }
    }
}
