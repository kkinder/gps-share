package com.kkinder.sharelocation;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;
import java.util.Locale;


public class MapsActivity extends FragmentActivity
        implements GoogleMap.OnMyLocationChangeListener,
        com.google.android.gms.maps.GoogleMap.OnCameraChangeListener {

    private static final int SETTINGS_REQUEST = 1;

    private ShareActionProvider mShareActionProvider;
    private boolean initialZoomDone = false;
    private boolean markerDragged = false;
    private float lastAccuracy = -1;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    public TextView desc;
    private LatLng lastKnownLocation;
    private String lastReadableLocation;
    private String lastAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);

        MenuItem item = menu.findItem(R.id.menu_item_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();

        ActionBar actionBar = this.getActionBar();
        if (actionBar != null) {
            actionBar.setIcon(R.drawable.icon);
            actionBar.setTitle(R.string.title_activity_maps);
        }
    }

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

    private void setUpMap() {
        //markerOptions = new MarkerOptions();
        if (mMap != null) {
            mMap.setIndoorEnabled(true);
            mMap.setMyLocationEnabled(true);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            try {
                mMap.setMapType(prefs.getInt("map_type", GoogleMap.MAP_TYPE_HYBRID));
            } catch (Exception e) {
                // Just in case bad data is there

            }

            Location location = mMap.getMyLocation();
            if (location != null) {
                zoomToMyLocation(location);
            }

            mMap.setOnMyLocationChangeListener(this);
            mMap.setOnCameraChangeListener(this);
        }
    }

    @Override
    public void onMyLocationChange(Location location) {
        float accuracy = location.getAccuracy();
        lastAccuracy = accuracy;

        if (!initialZoomDone) {
            zoomToMyLocation(location);
            lastAccuracy = accuracy;
            initialZoomDone = true;
        }
    }

    private String getReadableLocation(LatLng position) {
        String strLongitude = Location.convert(Math.abs(position.longitude), Location.FORMAT_SECONDS);
        String strLatitude = Location.convert(Math.abs(position.latitude), Location.FORMAT_SECONDS);

        String[] latParts = strLatitude.split(":");
        String[] lngParts = strLongitude.split(":");

        String formattedLocation = "";

        formattedLocation += latParts[0] + "° " + latParts[1] + "' " + latParts[2] + "\"";
        if (position.latitude > 0) {
            formattedLocation += " N  ";
        } else {
            formattedLocation += " S  ";
        }

        formattedLocation += lngParts[0] + "° " + lngParts[1] + "' " + lngParts[2] + "\"";
        if (position.longitude > 0) {
            formattedLocation += " E";
        } else {
            formattedLocation += " W";
        }

        return formattedLocation;
    }


    @Override
    public void onCameraChange(CameraPosition position) {
        lastKnownLocation = position.target;

        lastReadableLocation = getReadableLocation(position.target);
        lastAddress = null;

        desc = (TextView) this.findViewById(R.id.locationdesc);
        desc.setText(lastReadableLocation + "\n");

        updateShareIntent(lastKnownLocation);

        (new MapsActivity.GetAddressTask(this)).execute(position.target);
    }

    private void zoomToMyLocation(Location location) {
        zoomToMyLocation(location, true);
    }


    private void zoomToMyLocation(Location location, boolean animateCamera) {
        if (mMap != null) {
            mMap.clear();

            if (animateCamera) {
                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                CameraPosition cameraPosition = new CameraPosition(ll, 15, 0, 0);
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityForResult(intent, SETTINGS_REQUEST);

                return true;
            case R.id.action_hybrid:
                updateMapType(GoogleMap.MAP_TYPE_HYBRID);
                return true;
            case R.id.action_map:
                updateMapType(GoogleMap.MAP_TYPE_NORMAL);
                return true;
            case R.id.action_satellite:
                updateMapType(GoogleMap.MAP_TYPE_SATELLITE);
                return true;
            case R.id.action_terrain:
                updateMapType(GoogleMap.MAP_TYPE_TERRAIN);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void updateMapType(int mapType) {
        if (mMap != null) {
            mMap.setMapType(mapType);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("map_type", mapType);
            editor.apply();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SETTINGS_REQUEST) {
            if (lastKnownLocation != null)
                updateShareIntent(lastKnownLocation);
        }
    }

    public void updateShareIntent(LatLng position) {
        String msg = "";

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (prefs.getBoolean("pref_include_text_enabled", true)) {
            msg += prefs.getString("pref_message_text", getString(R.string.pref_message_text_default)) + "\n\n";
        }

        if (prefs.getBoolean("pref_include_gps_coordinates", true) && lastReadableLocation != null) {
            // Coordinates
            msg += lastReadableLocation + "\n\n";
        }

        if (prefs.getBoolean("pref_include_street_address", true) && lastAddress != null) {
            // Address
            msg += lastAddress + "\n\n";
        }

        if (prefs.getBoolean("pref_include_maps_link", true)) {
            // Maps link
            msg += "https://www.google.com/maps/place/" + position.latitude + "+" +
                    position.longitude + "\n\n";
        }

        if (prefs.getBoolean("pref_include_app_link", true)) {
            // App link
            msg += getString(R.string.app_link);
        }

        msg = msg.trim();

        if (msg.equals("")) {
            msg = getString(R.string.derp);
        }

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, msg);
        sendIntent.setType("text/plain");

        mShareActionProvider.setShareIntent(sendIntent);
    }

    protected class GetAddressTask extends AsyncTask<LatLng, Void, LocationResult> {

        // Store the context passed to the AsyncTask when the system instantiates it.
        Context localContext;

        // Constructor called by the system to instantiate the task
        public GetAddressTask(Context context) {

            // Required by the semantics of AsyncTask
            super();

            // Set a Context for the background task
            localContext = context;
        }

        /**
         * Get a geocoding service instance, pass latitude and longitude to it, format the returned
         * address, and return the address to the UI thread.
         */
        @Override
        protected LocationResult doInBackground(LatLng... params) {
            /*
             * Get a new geocoding service instance, set for localized addresses. This example uses
             * android.location.Geocoder, but other geocoders that conform to address standards
             * can also be used.
             */

            LatLng position = params[0];

            List<Address> addresses = null;
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            try {
                addresses = geocoder.getFromLocation(
                        position.latitude,
                        position.longitude,
                        1);
            } catch (Exception e) {
                return null;
            }

            if (addresses != null && addresses.size() > 0) {
                // Get the first address
                Address address = addresses.get(0);
                /*
                 * Format the first line of address (if available),
                 * city, and country name.
                 */
                String addressText = String.format(
                        "%s, %s, %s %s",
                        // If there's a street address, add it
                        address.getMaxAddressLineIndex() > 0 ?
                                address.getAddressLine(0) : "",
                        // Locality is usually a city
                        address.getLocality(),
                        address.getAdminArea(),
                        // The country of the address
                        address.getCountryName());
                // Return the text

                return new LocationResult(addressText, position);
            } else {
                return null;
            }
        }

        /**
         * A method that's called once doInBackground() completes. Set the text of the
         * UI element that displays the address. This method runs on the UI thread.
         */
        @Override
        protected void onPostExecute(LocationResult locationResult) {
            //LatLng position = this.mWorker.mParams[0];

            // Set the address in the UI
            //String readableLocation = getReadableLocation(position.target);

            if (locationResult != null && lastKnownLocation != null && locationResult.address != null) {
                if (locationResult.position.latitude == lastKnownLocation.latitude &&
                        locationResult.position.longitude == lastKnownLocation.longitude) {
                    lastAddress = locationResult.address;
                    desc.setText(lastReadableLocation + "\n" + lastAddress);
                    updateShareIntent(lastKnownLocation);
                }
            }
        }
    }
}
