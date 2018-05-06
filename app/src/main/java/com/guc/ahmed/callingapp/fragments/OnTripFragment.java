package com.guc.ahmed.callingapp.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guc.ahmed.callingapp.R;
import com.guc.ahmed.callingapp.apiclasses.MyVolleySingleton;
import com.guc.ahmed.callingapp.classes.Car;
import com.guc.ahmed.callingapp.classes.Profile;
import com.guc.ahmed.callingapp.classes.Trip;
import com.guc.ahmed.callingapp.gucpoints.GucPoints;
import com.guc.ahmed.callingapp.map.CustomMarker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 */
public class OnTripFragment extends Fragment implements OnMapReadyCallback {


    private View view;
    private Gson gson;
    private Car retrievedCar;
    private TextView statusTxt;
    private Button buttonStart;
    private Button buttonCancel;
    private Button buttonEnd;
    private AppCompatActivity activity;
    private ActionBar actionBar;

    private LinearLayout destination2;
    private LinearLayout destination3;
    private LinearLayout dot1to2;
    private LinearLayout dot2to3;
    private LinearLayout tripPath;
    private TextView pickupTxt;
    private TextView destination1Txt;
    private TextView destination2Txt;
    private TextView destination3Txt;

    private Trip currentTrip;
    private GoogleMap mMap;
    private Handler carhandler;
    private Runnable updateCarLocation;
    private Marker carMarker;
    private CustomMarker customMarker;

    public OnTripFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_on_trip, container, false);

        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();

        activity = (AppCompatActivity) getActivity();
        actionBar = activity.getSupportActionBar();

        statusTxt = view.findViewById(R.id.on_trip_status);
        buttonStart = view.findViewById(R.id.on_trip_start);
        buttonEnd = view.findViewById(R.id.on_trip_end);
        buttonCancel = view.findViewById(R.id.on_trip_cancel);

        destination2 = view.findViewById(R.id.summary_destination_2);
        destination3 = view.findViewById(R.id.summary_destination_3);
        dot1to2 = view.findViewById(R.id.dot_1_2);
        dot2to3 = view.findViewById(R.id.dot_2_3);
        pickupTxt = view.findViewById(R.id.pickup_txt);
        destination1Txt = view.findViewById(R.id.destination_1_txt);
        destination2Txt = view.findViewById(R.id.destination_2_txt);
        destination3Txt = view.findViewById(R.id.destination_3_txt);
        tripPath = view.findViewById(R.id.trip_path);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        actionBar.setTitle("Your Trip");
    }

    private void drawTripPathGraph() {
        pickupTxt.setText(GucPoints.getGucPlaceByLatLng(currentTrip.getPickupLocation()).getName());
        destination1Txt.setText(GucPoints.getGucPlaceByLatLng(currentTrip.getDestinations().get(0)).getName());
        if(currentTrip.getDestinations().size() == 2){
            destination2Txt.setText(GucPoints.getGucPlaceByLatLng(currentTrip.getDestinations().get(1)).getName());
            destination2.setVisibility(View.VISIBLE);
            dot1to2.setVisibility(View.VISIBLE);
        }else if(currentTrip.getDestinations().size() == 3){
            destination2Txt.setText(GucPoints.getGucPlaceByLatLng(currentTrip.getDestinations().get(1)).getName());
            destination3Txt.setText(GucPoints.getGucPlaceByLatLng(currentTrip.getDestinations().get(2)).getName());
            destination2.setVisibility(View.VISIBLE);
            dot1to2.setVisibility(View.VISIBLE);
            destination3.setVisibility(View.VISIBLE);
            dot2to3.setVisibility(View.VISIBLE);
        }
        tripPath.setVisibility(View.VISIBLE);
    }

    public void updateData(String tripID) {
        String url = getResources().getString(R.string.url_get_trip_details) + tripID;
        Log.v("CarDetails", url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.v("CarDetails", "It worked");
                        currentTrip = gson.fromJson(response.toString(), Trip.class);
                        drawCar();
                        updateUI();
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.v("CarDetails", "It didnt work");
                    }
                });

        MyVolleySingleton.getInstance(getActivity()).addToRequestQueue(jsonObjectRequest);
    }

    private void updateUI() {
        drawTripPathGraph();

        String status = "Unknown";
        if(currentTrip.getEndTime()!=null){
            status = "Trip Ended";
            buttonCancel.setVisibility(View.GONE);
            buttonEnd.setVisibility(View.GONE);
            buttonStart.setVisibility(View.GONE);
        }else if(currentTrip.getCancelTime()!=null){
            status = "Trip Canceled";
            buttonCancel.setVisibility(View.GONE);
            buttonEnd.setVisibility(View.GONE);
            buttonStart.setVisibility(View.GONE);
        }else if(currentTrip.getStartTime()!=null){
            status = "Trip Started";
            buttonCancel.setVisibility(View.GONE);
            buttonEnd.setVisibility(View.VISIBLE);
            buttonStart.setVisibility(View.GONE);
        }else if(currentTrip.getCarArriveTime()!=null){
            buttonCancel.setVisibility(View.VISIBLE);
            buttonEnd.setVisibility(View.GONE);
            buttonStart.setVisibility(View.VISIBLE);
            status = "Car Arrived";
        }else {
            buttonCancel.setVisibility(View.VISIBLE);
            buttonEnd.setVisibility(View.GONE);
            buttonStart.setVisibility(View.GONE);
            status = "Car on the way";
        }
        statusTxt.setText(status);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.clear();
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setAllGesturesEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setScrollGesturesEnabled(true);

        boolean success = googleMap.setMapStyle(new MapStyleOptions(getResources()
                .getString(R.string.style_json)));
        // Constrain the camera target to the Adelaide bounds.
        mMap.setLatLngBoundsForCameraTarget(GucPoints.GUC);

        //to be removed
        LatLng latLng = new LatLng(29.987243, 31.441902);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17));

        updateData(getArguments().getString("TRIP_ID"));


    }

    private void drawCar() {
        carhandler = new Handler();

        updateCarLocation = new Runnable() {
            @Override
            public void run() {

                String url = getResources().getString(R.string.url_get_car_details) + currentTrip.getCarID();
                Log.v("CarDetails", url);

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                        (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(JSONObject response) {
                                Log.v("CarDetails", "It worked");
                                retrievedCar = gson.fromJson(response.toString(), Car.class);
                                animateCar();
                            }
                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.v("CarDetails", "It didnt work");
                            }
                        });

                MyVolleySingleton.getInstance(getActivity()).addToRequestQueue(jsonObjectRequest);

                carhandler.postDelayed(updateCarLocation, 3000);
            }
        };

        carhandler.postDelayed(updateCarLocation, 0);
    }

    private void animateCar() {
        if(carMarker == null){
            customMarker = new CustomMarker(getContext());
            customMarker.setImage(R.drawable.ic_directions_car_black_24dp);
            carMarker = mMap.addMarker(new MarkerOptions().position(retrievedCar.getLatLng())
                    .title("CAR").icon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView())));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(retrievedCar.getLatLng()));
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(retrievedCar.getLatLng()));
            if(!retrievedCar.getLatLng().equals(carMarker.getPosition())){
                animateMarker(carMarker, retrievedCar.getLatLng(), false);
            }
        }
    }

    private void animateMarker(final Marker marker, final LatLng newLocation, final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.elapsedRealtime();
        final long duration = 3000;
        final LatLng startLatLng = marker.getPosition();
        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.elapsedRealtime() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);

                double lng = t * newLocation.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * newLocation.latitude + (1 - t)
                        * startLatLng.latitude;

                marker.setPosition(new LatLng(lat, lng));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lng)));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }
}
