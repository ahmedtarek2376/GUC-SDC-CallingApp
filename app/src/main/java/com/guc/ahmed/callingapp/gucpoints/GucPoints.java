package com.guc.ahmed.callingapp.gucpoints;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.guc.ahmed.callingapp.MainActivity;
import com.guc.ahmed.callingapp.gucpoints.GucPlace;

import java.util.List;

/**
 * Created by ahmed on 27-Mar-18.
 */

public class GucPoints {

    private GucPoints(){}
    // Create a LatLngBounds that includes the GUC boundaries
    public static final LatLngBounds GUC = new LatLngBounds(
            new LatLng(29.984414, 31.437786), new LatLng(29.990841, 31.445829) );

    public static GucPlace getGucPlaceByName(String name){
        for (GucPlace place : MainActivity.gucPlaces){
            if(place.getName().equalsIgnoreCase(name)){
                return place;
            }
        }
        return null;
    }

    public static GucPlace getGucPlaceByLatLng(LatLng latLng){
        for (GucPlace place : MainActivity.gucPlaces){
            if(place.getLatLng().equals(latLng)){
                return place;
            }
        }
        return null;
    }
}
