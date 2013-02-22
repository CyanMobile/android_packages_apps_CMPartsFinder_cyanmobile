package com.cyanmobile.finder;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class SmsService extends Service {
	public static final String NAME = "com.cyanmobile.finder.FINDER_SERVICE";

	SmsListener mSmsListener = new SmsListener();

	@Override
	public IBinder onBind(Intent intent) {
	     // TODO Auto-generated method stub
	     return null;
	}

	@Override
	public void onCreate() {
	      Log.d("lzj", "service created");
	      mSmsListener.setContext(this);
	      IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
	      intentFilter.setPriority(10001);
	      registerReceiver(mSmsListener, intentFilter);
	      super.onCreate();
	}

	@Override
	public void onDestroy() {
	      Log.d("lzj", "service destroyed");
	      unregisterReceiver(mSmsListener);
	      super.onDestroy();
	}
	
}
