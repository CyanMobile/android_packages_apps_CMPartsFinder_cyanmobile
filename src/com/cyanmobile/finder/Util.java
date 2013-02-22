package com.cyanmobile.finder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;

public class Util {

      // if the device does not support md5, original string will be returned
      public static final String md5(final String s) {
         try {
              // Create MD5 Hash
              MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
              digest.update(s.getBytes());
              byte messageDigest[] = digest.digest();

              // Create Hex String
              StringBuffer hexString = new StringBuffer();
              for (int i = 0; i < messageDigest.length; i++) {
                   String h = Integer.toHexString(0xFF & messageDigest[i]);
                   while (h.length() < 2)
                          h = "0" + h;
                   hexString.append(h);
               }
               return hexString.toString();
          } catch (NoSuchAlgorithmException e) {
	        e.printStackTrace();
          }
          return s;
      }

      public static boolean isMyServiceRunning(Context context) {
           ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
           for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if ("com.cyanmobile.finder.SmsService".equals(service.service.getClassName())) {
                    return true;
                }
           }
           return false;
      }
}
