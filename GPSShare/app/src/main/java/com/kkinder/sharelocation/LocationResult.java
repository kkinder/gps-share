package com.kkinder.sharelocation;

import com.google.android.gms.maps.model.LatLng;

public class LocationResult {
    public final String address;
    public final LatLng position;

    LocationResult(String address, LatLng position) {
        this.address = address;
        this.position = position;
    }
}
