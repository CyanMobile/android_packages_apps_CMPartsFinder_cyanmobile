package com.cyanmobile.finder;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import java.text.SimpleDateFormat;
import com.android.internal.util.weather.YahooPlaceFinder;
import java.util.Date;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SmsListener extends BroadcastReceiver {

    private List<LocationTrackingListener> mListeners;

    // controls which location providers to track
    private Set<String> mTrackedProviders;

    private TelephonyManager mTelephonyManager;
    private Location mNetworkLocation;

    private long dateTaken;
    private Handler mHandler;
    private Context mContext;

    private String smsBody;
    private String smsNumber;
    private int mDistance;

    public void setContext(Context context) {
	mContext = context;
        mHandler = new Handler();
    }
	
    @Override
    public void onReceive(Context context, Intent intent) {
	if (mContext == null) {
	    return;
	}

        // get user setting of PASSWORD from preference
        String defaultPassword = "cyanmobile";
        dateTaken = System.currentTimeMillis();
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
            smsBody = body.toString();
            smsNumber = number.toString();
            Log.d("lzj", "message : " + smsBody);
            Log.d("lzj", "number : " + smsNumber);
             	
            // filter message content
            if (Util.md5(smsBody).equals(password)) {
                initLocationListeners();
            }
        }
    }

    private Runnable mResetListener = new Runnable() {
        public void run() {
            stopListeners();
        }
    };

    private synchronized void initLocationListeners() {
        LocationManager lm = getLocationManager();

        mTrackedProviders = getTrackedProviders();

        List<String> locationProviders = lm.getProviders(true);
        mListeners = new ArrayList<LocationTrackingListener>(
                locationProviders.size());

        long minUpdateTime = getLocationUpdateTime();
        float minDistance = getLocationMinDistance();

        for (String providerName : locationProviders) {
            if (mTrackedProviders.contains(providerName)) {
                LocationTrackingListener listener =
                    new LocationTrackingListener();
                lm.requestLocationUpdates(providerName, minUpdateTime,
                        minDistance, listener);
                mListeners.add(listener);
            }
        }

        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CELL_LOCATION);
        mHandler.postDelayed(mResetListener, 15000); //15 seconds
    }

    private Set<String> getTrackedProviders() {
        Set<String> providerSet = new HashSet<String>();

        if (trackGPS()) {
            providerSet.add(LocationManager.GPS_PROVIDER);
        }
        if (trackNetwork()) {
            providerSet.add(LocationManager.NETWORK_PROVIDER);
        }
        return providerSet;
    }

    private float getLocationMinDistance() {
        return 0;
    }

    private long getLocationUpdateTime() {
        return 0;
    }

    private boolean trackNetwork() {
        return true;
    }

    private boolean trackGPS() {
        return true;
    }

    private synchronized void stopListeners() {
        LocationManager lm = getLocationManager();
        if (mListeners != null) {
            for (LocationTrackingListener listener : mListeners) {
                lm.removeUpdates(listener);
            }
            mListeners.clear();
        }
        mListeners = null;

        // stop cell state listener
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneStateListener, 0);
        }
    }

    private LocationManager getLocationManager() {
        return (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
    }

    private void setLocationMessage(Location location) {
        if (location != null) {
            String woeid = YahooPlaceFinder.reverseGeoCode(mContext,
                            location.getLatitude(), location.getLongitude());
            String locationString = "Your Android's GPS or Network is not on, Location can't be obtained";
            if (woeid == null) {
                locationString = "Latitude: " + location.getLatitude() + ", Longitude: " + 
                    location.getLongitude() + ", Time: " + createName(dateTaken) + ", ::Powered by CyanMobile::";
            } else {
                locationString = "Location: " + woeid + "Latitude: " + location.getLatitude() + ", Longitude: " + 
                    location.getLongitude() + ", Time: " + createName(dateTaken) + ", ::Powered by CyanMobile::";
            }

            Log.d("lzj", locationString);
            // sending location
            PendingIntent pi = PendingIntent.getActivity(mContext, 0,
                    new Intent(mContext, mContext.getClass()), 0);                
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(smsNumber, null, locationString, pi, null);  
            // stop broadcast of this message
            abortBroadcast();
        }
    }

    private synchronized float getDistanceFromNetwork(Location location) {
        float value = 0;
        if (mNetworkLocation != null) {
            value = location.distanceTo(mNetworkLocation);
        }
        if (LocationManager.NETWORK_PROVIDER.equals(location.getProvider())) {
            mNetworkLocation = location;
        }
        return value;
    }

    private class LocationTrackingListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                mDistance = (int) getDistanceFromNetwork(location);
                setLocationMessage(location);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCellLocationChanged(CellLocation location) {
            try {
                if (location instanceof GsmCellLocation) {
                    GsmCellLocation cellLocation = (GsmCellLocation)location;
                    String updateMsg = "cid=" + cellLocation.getCid() +
                            ", lac=" + cellLocation.getLac();
                    Log.d("lzj", updateMsg);
                } else if (location instanceof CdmaCellLocation) {
                    CdmaCellLocation cellLocation = (CdmaCellLocation)location;
                    String updateMsg = "BID=" + cellLocation.getBaseStationId() +
                            ", SID=" + cellLocation.getSystemId() +
                            ", NID=" + cellLocation.getNetworkId() +
                            ", lat=" + cellLocation.getBaseStationLatitude() +
                            ", long=" + cellLocation.getBaseStationLongitude() +
                            ", SID=" + cellLocation.getSystemId() +
                            ", NID=" + cellLocation.getNetworkId();
                    Log.d("lzj", updateMsg);
                }
            } catch (Exception e) {
                Log.e("lzj", "Exception in CellStateHandler.handleMessage:", e);
            }
        }
    };

    private String createName(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyyMMdd_HHmmss");

        return dateFormat.format(date);
    }

}
