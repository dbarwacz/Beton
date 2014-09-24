package com.cointica.beton;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements GooglePlayServicesClient
        .ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private static LocationClient sLocationClient;
    private Button mButton;
    private List<ParseObject> mSeenBetons = new ArrayList<ParseObject>();

    public static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
    // Update frequency in milliseconds
    public static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    public static final int FASTEST_INTERVAL_IN_SECONDS = 1;
    // A fast frequency ceiling in milliseconds
    public static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND *
            FASTEST_INTERVAL_IN_SECONDS;

    // Global constants
    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private LocationRequest mLocationRequest;
    private static int sUpdateCounter = 0;
    private static boolean sFirstLocation;

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;

        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        sLocationClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        sLocationClient.disconnect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpMapIfNeeded();
        sLocationClient = new LocationClient(this, this, this);
        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        mButton = (Button) findViewById(R.id.add_beton_button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dialog = new AddBetonDialogFragment();
                dialog.show(getFragmentManager(), "BetonDialogFragment");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();

        //        stopService(new Intent(this, BetonUpdateService.class));
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.

        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        if (getIntent() != null) {
            Double longitude = getIntent().getDoubleExtra(ParseBetonFactory.KEY_LNG, 0.0d),
                    latitude = getIntent().getDoubleExtra(ParseBetonFactory.KEY_LAT, 0.0d);
            if (longitude != 0.0d && latitude != 0.0d) {
                LatLng position = new LatLng(latitude, longitude);
                MarkerOptions marker = new MarkerOptions().position(position);
                mMap.addMarker(new MarkerOptions().position(new LatLng(latitude,
                        longitude)).title("Beton you wanted to see"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 13));


            }
        }
        mMap.setMyLocationEnabled(true);
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                DialogFragment dialog = new AddBetonDialogFragment(latLng);
                dialog.show(getFragmentManager(), "Another beton fragment ragment");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Decide what to do based on the original request code
        switch (requestCode) {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
            /*
             * If the result code is Activity.RESULT_OK, try
             * to connect again
             */
                switch (resultCode) {
                    case Activity.RESULT_OK:
                    /*
                     * Try the request again
                     */
                        break;
                }
        }
    }

    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.
                isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates", "Google Play services is available.");
            // Continue
            return true;
            // Google Play services was not available for some reason.
            // resultCode holds the error code.
        } else {
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(getFragmentManager(), "Location Updates");
            }
        }
        return true;
    }

    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
//        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        sLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        // Display the connection status
//        Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            //            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        ParseGeoPoint point = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        ParseQuery<ParseObject> betonsAround = ParseQuery.getQuery(Beton.beton.toString());
        betonsAround.whereWithinKilometers(ParseBetonFactory.KEY_LOCATION, point, 30);
        betonsAround.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e == null) {
                    boolean flag = false;
                    for (ParseObject object : parseObjects) {
                        for (ParseObject ob : mSeenBetons) {
                            if (ob.getObjectId().equals(object.getObjectId())) flag = true;
                        }
                        if (!flag) {
                            ParseGeoPoint location = object.getParseGeoPoint(ParseBetonFactory
                                    .KEY_LOCATION);
                            mMap.addMarker(new MarkerOptions().position(new LatLng(location
                                    .getLatitude(), location.getLongitude())).title(object.get
                                    (ParseBetonFactory.KEY_USER) + "").snippet(object.get
                                    (ParseBetonFactory.KEY_TITLE) + ""));
                            mSeenBetons.add(object);
                        }
                        flag = false;
                    }
                }
            }
        });
        if (!sFirstLocation) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(sLocationClient
                    .getLastLocation().getLatitude(), sLocationClient.getLastLocation()
                    .getLongitude()), 15));
            sFirstLocation = true;
        }
        //        List<ParseQuery<ParseObject>> queries = new ArrayList<ParseQuery<ParseObject>>();
        //        queries.add(betonsAround);
        //        queries.add(fewWins);
        //
        //        ParseQuery<ParseObject> mainQuery = ParseQuery.or(queries);
        //        mainQuery.findInBackground(new FindCallback<ParseObject>() {
        //            public void done(List<ParseObject> results, ParseException e) {
        //                // results has the list of players that win a lot or haven't won much.
        //            }
        //        });
        // Report to the UI that the location was updated
        //        String msg = "Updated Location: " +
        //                Double.toString(location.getLatitude()) + "," +
        //                Double.toString(location.getLongitude());
        //

        //        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        //        if (sUpdateCounter < 5) {
        //            ParseObject object = ParseBetonFactory.start().location(location).title
        // ("title").user
        //                    ("dombar").build();
        //
        //            object.saveInBackground();
        //        }
    }

    public static class AddBetonDialogFragment extends DialogFragment {

        private LatLng loc;

        public AddBetonDialogFragment() {

        }

        AddBetonDialogFragment(LatLng loc) {
            this.loc = loc;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            View view = View.inflate(getActivity(), R.layout.content_view, null);
            final EditText name = (EditText) view.findViewById(R.id.dialog_name);
            final EditText desc = (EditText) view.findViewById(R.id.dialog_title);
            builder.setView(view);
            String title;
            if (loc == null) {
                loc = new LatLng(sLocationClient.getLastLocation().getLatitude(),
                        sLocationClient.getLastLocation().getLongitude());
                title = getResources().getString(R.string.add_beton);
            } else title = getResources().getString(R.string.add_at_this_position);
            builder.setMessage(title).setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String username;
                            if (name.getText().toString().length() < 1) username = "default";
                            else username = name.getText().toString();
                            if (loc == null)
                                loc = new LatLng(sLocationClient.getLastLocation().getLatitude(),
                                        sLocationClient.getLastLocation().getLongitude());
                            ParseObject object = ParseBetonFactory.start().location(loc).title
                                    (desc.getText().toString()).user(username).build();
                            object.saveInBackground();
                        }
                    }).setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dismiss();
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }


}
