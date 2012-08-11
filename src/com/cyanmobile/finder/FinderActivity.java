package com.cyanmobile.finder;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class FinderActivity extends Activity {
	public static final String PREFERENCE_NAME = "password_of_find_my_android"; 
	public static final String PREFERENCE_KEY_NAME = "password";

	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        if ("0".equals(SystemProperties.get("ro.squadzone.build", "0"))) {
            Toast toast = Toast.makeText(FinderActivity.this, "GOTCHA!!!", Toast.LENGTH_LONG);
            toast.show();
            finish();
        } else {
            Toast toast = Toast.makeText(FinderActivity.this, "Default password: cyanmobile", Toast.LENGTH_LONG);
            toast.show();
        }

        setContentView(R.layout.main);        

        // password setup
        Button setPasswordButton = (Button)findViewById(R.id.set_password_btn);
        setPasswordButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				String defaultPassword = "cyanmobile";
				SharedPreferences preferences = getSharedPreferences(PREFERENCE_NAME,MODE_WORLD_READABLE);
				String passwordMD5 = preferences.getString(PREFERENCE_KEY_NAME, Util.md5(defaultPassword));

				EditText oldpasswordEditText = (EditText)findViewById(R.id.old_password_et);
				EditText newpasswordEditText = (EditText)findViewById(R.id.new_password_et);
				if (!passwordMD5.equals(Util.md5(oldpasswordEditText.getText().toString()))) {
					Toast.makeText(FinderActivity.this, R.string.old_password_wrong, Toast.LENGTH_SHORT).show();
					return;
				}
				
				if (newpasswordEditText.getText() == null ||newpasswordEditText.getText().toString().equals("")) {
					Toast.makeText(FinderActivity.this, R.string.empty_password, Toast.LENGTH_SHORT).show();
					return;
				}
				
				// update password
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(PREFERENCE_KEY_NAME, Util.md5(newpasswordEditText.getText().toString())).commit();
				Toast.makeText(FinderActivity.this, R.string.set_password_success, Toast.LENGTH_SHORT).show();
			}
		});
        
    }
	
}
