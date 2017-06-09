package com.loktra.backlocation;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private boolean mShiftStarted;

    private Button mBtnShift;

    private LinearLayout mLayoutDuration;

    private TextView mTxtDuration;

    private TimeTracker tt = new TimeTracker();

    //Array for storing LatLng of path
    private ArrayList<LatLng> mShiftPoints = new ArrayList<>();

    private GoogleApiClient mGoogleApiClient;

    private LocationRequest mLocationRequest;

    private long UPDATE_INTERVAL = 10 * 1000 * 6 * 2;  /* 2 mins */

    private Location mLastKnownLocation;

    private CameraPosition mCameraPosition;


    private static final String TAG = MainActivity.class.getSimpleName();
    private GoogleMap mMap;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    private static final int DEFAULT_ZOOM = 15;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        setContentView(R.layout.activity_main);

        mBtnShift = (Button) findViewById(R.id.buton_shift);

        mLayoutDuration = (LinearLayout) findViewById(R.id.layout_duration);

        mTxtDuration = (TextView) findViewById(R.id.text_duration);

        //Google api client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
        mGoogleApiClient.connect();
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        startLocationUpdates();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.d(TAG, "Play services connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    /**
     * Handles suspension of the connection to the Google Play services client.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "Play services connection suspended");
    }

    // Trigger new location updates at interval
    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL);
        // Request location updates
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

//        // Use a custom info window adapter to handle multiple lines of text in the
//        // info window contents.
//        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
//
//            @Override
//            // Return null here, so that getInfoContents() is called next.
//            public View getInfoWindow(Marker arg0) {
//                return null;
//            }
//
//            @Override
//            public View getInfoContents(Marker marker) {
//                // Inflate the layouts for the info window, title and snippet.
//                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
//                        (FrameLayout)findViewById(R.id.map), false);
//
//                TextView title = ((TextView) infoWindow.findViewById(R.id.title));
//                title.setText(marker.getTitle());
//
//                TextView snippet = ((TextView) infoWindow.findViewById(R.id.snippet));
//                snippet.setText(marker.getSnippet());
//
//                return infoWindow;
//            }
//        });

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (mLocationPermissionGranted) {
            mLastKnownLocation = LocationServices.FusedLocationApi
                    .getLastLocation(mGoogleApiClient);
        }

        // Set the map's camera position to the current location of the device.
        if (mCameraPosition != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));
        } else if (mLastKnownLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(),
                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (mLocationPermissionGranted) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            mMap.setMyLocationEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mLastKnownLocation = null;
        }
    }

    public void shiftClick(View view) {
        if (mShiftStarted) {
            //show time only if movement is detected
            if(mShiftPoints.size() >= 1) {
                mLayoutDuration.setVisibility(View.VISIBLE);
                mTxtDuration.setText(getDuration(tt.duration() / 1000));
            }
            drawPolyline();
            mShiftStarted = false;
            mBtnShift.setText("Start shift");
            mBtnShift.setBackgroundColor(Color.parseColor("#76FF03"));
            mShiftPoints.clear();
        } else {
            mLayoutDuration.setVisibility(View.GONE);
            mShiftStarted = true;
            tt.startTimer();
            mBtnShift.setText("Stop shift");
            mBtnShift.setBackgroundColor(Color.parseColor("#D50000"));
            mMap.clear();
        }
    }

    private void drawPolyline() {

        if (mShiftPoints.size() == 0) {
            Toast.makeText(this, "No movement detected", Toast.LENGTH_SHORT).show();
            return;
        }
//
        PolylineOptions plo = new PolylineOptions().width(5).color(Color.RED);

        for (LatLng latlng : mShiftPoints) {
            plo.add(latlng);
        }

        mMap.addPolyline(plo);
        mMap.addMarker(new MarkerOptions().position(mShiftPoints.get(0))
                .title("Start Position"));
        mMap.addMarker(new MarkerOptions().position(mShiftPoints.get(mShiftPoints.size() - 1))
                .title("End Position"));
    }

    @Override
    public void onLocationChanged(Location location) {

        if (mShiftStarted) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mShiftPoints.add(latLng);
        }
    }

    private String getDuration(long seconds) {

        int hours = (int) seconds / 3600;
        int mins = (int) (seconds % 3600) / 60;

        if (hours == 0) {
            return 1 + mins + " mins";
        }
        return hours + " h " + mins + " mins";
    }
}
