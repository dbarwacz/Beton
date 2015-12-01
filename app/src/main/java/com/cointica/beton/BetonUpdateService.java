package com.cointica.beton;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dominik Barwacz (dombar@gmail.com) on 19 September 2014
 * as part of beton.
 */
public class BetonUpdateService extends Service implements GooglePlayServicesClient
        .ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener {

    public static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 60 * 5;
    // Update frequency in milliseconds
    public static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    public static final int FASTEST_INTERVAL_IN_SECONDS = 1;
    // A fast frequency ceiling in milliseconds
    public static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND *
            FASTEST_INTERVAL_IN_SECONDS;

    private static LocationClient sLocationClient;
    private LocationRequest mLocationRequest;
    private List<ParseObject> mSeenBetons = new ArrayList<ParseObject>();
    private static List<ParseObject> sList = new ArrayList<ParseObject>();
    private static boolean isEnabled;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("Service", "OnStartCommand");

        sLocationClient = new LocationClient(this, this, this);
        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        sLocationClient.connect();

        switchOperationReceiver = new SwitchOperationModeReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(switchOperationReceiver, FILTER);
        return (START_NOT_STICKY);
    }


    @Override
    public void onDestroy() {
        Log.i("Service", "onDestroy");
        sLocationClient.disconnect();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(switchOperationReceiver);
        switchOperationReceiver = null;

    }

    public static final String KEY_OPERATION_MODE = "operationMode";
    public static final String ACTION = BuildConfig.APPLICATION_ID + "changeOperationMode";
    private static final IntentFilter FILTER = new IntentFilter(ACTION);
    private BroadcastReceiver switchOperationReceiver;

    private class SwitchOperationModeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            isEnabled = intent.getBooleanExtra(KEY_OPERATION_MODE, false);
            if (!isEnabled) {
                stopSelf();
            }
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return (null);
    }

    public void runNotification() {
        if (sList != null && sList.size() > 0) {
            Location loc = sLocationClient.getLastLocation();

            ParseGeoPoint location, userLocation = new ParseGeoPoint(loc.getLatitude(), loc.getLongitude());

            double distance, record = Double.MAX_VALUE;
            ParseObject result = sList.get(0);

            for (ParseObject ob : sList) {
                location = ob.getParseGeoPoint(ParseBetonFactory.KEY_LOCATION);
                distance = location.distanceInKilometersTo(userLocation);
                if (distance < record) {
                    record = distance;
                    result = ob;
                }
            }

            location = result.getParseGeoPoint(ParseBetonFactory.KEY_LOCATION);

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(ParseBetonFactory.KEY_LNG, location.getLongitude());
            intent.putExtra(ParseBetonFactory.KEY_LAT, location.getLatitude());

            PendingIntent pi = TaskStackBuilder.create(this).addNextIntent(intent).getPendingIntent(1, PendingIntent.FLAG_CANCEL_CURRENT);


            NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this)
                    .setContentTitle(getResources().getString(R.string.beton_ahead))
                    .setContentText(getResources().getString(R.string.get_ready) + " " +
                            String.format("%.1f", record) + " km")
                    .setTicker(getResources().getString(R.string.beton_ahead))
                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                    .setContentIntent(pi).setSmallIcon(R.drawable.beton)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setAutoCancel(true);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            nm.notify(1, notifBuilder.build());
        }
        sList.clear();
    }

    @Override
    public void onConnected(Bundle bundle) {
        sLocationClient.requestLocationUpdates(mLocationRequest, this);

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        ParseGeoPoint point = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        ParseQuery<ParseObject> betonsAround = ParseQuery.getQuery(Beton.beton.toString());
        betonsAround.whereWithinKilometers(ParseBetonFactory.KEY_LOCATION, point, 50);
        betonsAround.findInBackground(new FindCallback<ParseObject>() {
                                          @Override
                                          public void done(List<ParseObject> parseObjects,
                                                           ParseException e) {
                                              if (e == null) {
                                                  boolean flag = false;
                                                  for (ParseObject object : parseObjects) {
                                                      for (ParseObject ob : mSeenBetons) {
                                                          if (ob.getObjectId().equals(object
                                                                  .getObjectId()))
                                                              flag = true;
                                                      }
                                                      if (!flag) {
                                                          scheduleNotification(object);
                                                          mSeenBetons.add(object);
                                                      }
                                                      flag = false;
                                                  }
                                                  runNotification();
                                              }
                                          }
                                      }

        );
    }

    private void scheduleNotification(ParseObject object) {
        sList.add(object);
    }

}
