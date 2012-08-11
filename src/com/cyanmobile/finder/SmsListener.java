package com.cyanmobile.finder;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SmsListener extends BroadcastReceiver{

	Context mContext;
	public void setContext(Context context) {
		mContext = context;		
	}

	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (mContext == null) {
			return;
		}
		
		// get user setting of PASSWORD from preference
		String defaultPassword = "cyanmobile";
                long dateTaken = System.currentTimeMillis();
		SharedPreferences preferences = mContext.getSharedPreferences(FinderActivity.PREFERENCE_NAME, Context.MODE_WORLD_READABLE);
		String password = preferences.getString(FinderActivity.PREFERENCE_KEY_NAME, Util.md5(defaultPassword));

		StringBuilder body = new StringBuilder();// sms content
		StringBuilder number = new StringBuilder();// sms sender number
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			Object[] _pdus = (Object[]) bundle.get("pdus");
			SmsMessage[] message = new SmsMessage[_pdus.length];
			for (int i = 0; i < _pdus.length; i++) {
				message[i] = SmsMessage.createFromPdu((byte[]) _pdus[i]);
			}
			for (SmsMessage currentMessage : message) {
				body.append(currentMessage.getDisplayMessageBody());
				number.append(currentMessage.getDisplayOriginatingAddress());
			}
			String smsBody = body.toString();
			String smsNumber = number.toString();
			Log.d("lzj", "message : "+smsBody);
			Log.d("lzj", "number : "+smsNumber);
			
			if (smsNumber.contains("+62")) {
				smsNumber = smsNumber.substring(3);
			}
			// filter message content
			if (Util.md5(smsBody).equals(password)) {
				// get location
				LocationManager locationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
				Criteria criteria = new Criteria();
				criteria.setAccuracy(Criteria.ACCURACY_FINE);
				criteria.setAltitudeRequired(false);
				criteria.setBearingRequired(false);
				criteria.setCostAllowed(true);
				criteria.setPowerRequirement(Criteria.POWER_LOW);
				String provider = locationManager.getBestProvider(criteria, true);
				
				Location location = locationManager.getLastKnownLocation(provider);
			    
				String locationString = "your android's gps or network is not on, location can't be obtained";
				if (location != null) {
					locationString = "Latitude: "+location.getLatitude()+", Longitude: "+location.getLongitude()+", Time: "+createName(dateTaken)+", ::Powered by CyanMobile::";
				}
				
				// sending location
				PendingIntent pi = PendingIntent.getActivity(mContext, 0,
			            new Intent(mContext, mContext.getClass()), 0);                
			        SmsManager sms = SmsManager.getDefault();
			        sms.sendTextMessage(smsNumber, null, locationString, pi, null);  
				// stop broadcast of this message
				abortBroadcast();
			}
		}
		
	}

    private String createName(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyyMMdd_HHmmss");

        return dateFormat.format(date);
    }

}
