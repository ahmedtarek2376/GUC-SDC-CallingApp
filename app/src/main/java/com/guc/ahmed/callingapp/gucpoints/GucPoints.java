package com.guc.ahmed.callingapp.gucpoints;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.guc.ahmed.callingapp.gucpoints.GucPlace;

/**
 * Created by ahmed on 27-Mar-18.
 */

public class GucPoints {

    private GucPoints(){}

    public static final GucPlace C3_U_AREA = new GucPlace("C3",new LatLng(29.986905, 31.439592));
    public static final GucPlace C6_U_AREA = new GucPlace("C6",new LatLng(29.986169, 31.438699));
    public static final GucPlace D4_U_AREA = new GucPlace("D4",new LatLng(29.986926, 31.440630));
    public static final GucPlace B3_U_AREA = new GucPlace("B3",new LatLng(29.985092, 31.439574));

    public static final GucPlace GUC_GYM = new GucPlace("Gym",new LatLng(29.985884, 31.438944));

    public static final GucPlace GATE_1 = new GucPlace("Gate 1",new LatLng(29.984504, 31.440095));
    public static final GucPlace GATE_3 = new GucPlace("Gate 3",new LatLng(29.987276, 31.438320));
    public static final GucPlace GATE_4 = new GucPlace("Gate 4",new LatLng(29.988201, 31.438331));

    // Create a LatLngBounds that includes the GUC boundaries
    public static final LatLngBounds GUC = new LatLngBounds(
            new LatLng(29.984414, 31.437786), new LatLng(29.990841, 31.445829) );

    public static GucPlace getGucPlaceByName(String name){
        switch (name){
            case "C3": return C3_U_AREA;
            case "C6": return C6_U_AREA;
            case "D4": return D4_U_AREA;
            case "B3": return B3_U_AREA;
            case "Gym": return GUC_GYM;
            case "Gate 1": return GATE_1;
            case "Gate 3": return GATE_3;
            case "Gate 4": return GATE_4;
            default: return null;
        }
    }

    public static GucPlace getGucPlaceByLatLng(LatLng latLng){
        if(latLng.equals(C3_U_AREA.getLatLng())){
            return C3_U_AREA;
        } else if(latLng.equals(C6_U_AREA.getLatLng())){
            return C6_U_AREA;
        }else if(latLng.equals(D4_U_AREA.getLatLng())){
            return D4_U_AREA;
        }else if(latLng.equals(B3_U_AREA.getLatLng())){
            return B3_U_AREA;
        }else if(latLng.equals(GATE_1.getLatLng())){
            return GATE_1;
        }else if(latLng.equals(GATE_3.getLatLng())){
            return GATE_3;
        }else if(latLng.equals(GATE_4.getLatLng())){
            return GATE_4;
        }else if(latLng.equals(GUC_GYM.getLatLng())){
            return GUC_GYM;
        }
        return GUC_GYM;
    }
}
