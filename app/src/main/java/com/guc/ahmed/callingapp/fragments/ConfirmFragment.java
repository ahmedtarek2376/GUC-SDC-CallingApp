package com.guc.ahmed.callingapp.fragments;


import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.dd.processbutton.iml.ActionProcessButton;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guc.ahmed.callingapp.MainActivity;
import com.guc.ahmed.callingapp.R;
import com.guc.ahmed.callingapp.MyVolleySingleton;
import com.guc.ahmed.callingapp.gucpoints.GucPlace;
import com.guc.ahmed.callingapp.gucpoints.GucPoints;
import com.guc.ahmed.callingapp.map.CustomMarker;
import com.guc.ahmed.callingapp.objects.Profile;
import com.guc.ahmed.callingapp.objects.RequestTrip;
import com.guc.ahmed.callingapp.objects.Trip;
import com.tapadoo.alerter.Alert;
import com.tapadoo.alerter.Alerter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConfirmFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private View view;

    private LatLng lastLocation;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private HashMap<String,Marker> markers;
    private ActionProcessButton button;
    private AppCompatActivity activity;
    private ActionBar actionBar;
    private RequestTrip requestTrip;

    private LinearLayout destination2;
    private LinearLayout destination3;
    private LinearLayout dot1to2;
    private LinearLayout dot2to3;
    private TextView pickupTxt;
    private TextView destination1Txt;
    private TextView destination2Txt;
    private TextView destination3Txt;
    private Gson gson;
    private Alert alert;

    private static final String TAG = "ConfirmFragment";

    public ConfirmFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view =  inflater.inflate(R.layout.fragment_confirm, container, false);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

        button = (ActionProcessButton) view.findViewById(R.id.request_trip_btn);
        button.setMode(ActionProcessButton.Mode.ENDLESS);
        button.setOnClickListener(requestTripButtonListener);
        button.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Regular.ttf"));

        activity = (AppCompatActivity) getActivity();
        actionBar = activity.getSupportActionBar();

        destination2 = view.findViewById(R.id.summary_destination_2);
        destination3 = view.findViewById(R.id.summary_destination_3);
        dot1to2 = view.findViewById(R.id.dot_1_2);
        dot2to3 = view.findViewById(R.id.dot_2_3);
        pickupTxt = view.findViewById(R.id.pickup_txt);
        destination1Txt = view.findViewById(R.id.destination_1_txt);
        destination2Txt = view.findViewById(R.id.destination_2_txt);
        destination3Txt = view.findViewById(R.id.destination_3_txt);

        drawTripPathGraph();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        markers = new HashMap<>();

        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();

        return view;
    }

    private void drawTripPathGraph() {
        pickupTxt.setText(GucPoints.getGucPlaceByLatLng(requestTrip.getPickupLocation()).getName());
        destination1Txt.setText(GucPoints.getGucPlaceByLatLng(requestTrip.getDestinations().get(0)).getName());
        if(requestTrip.getDestinations().size() == 2){
            destination2Txt.setText(GucPoints.getGucPlaceByLatLng(requestTrip.getDestinations().get(1)).getName());
            destination2.setVisibility(View.VISIBLE);
            dot1to2.setVisibility(View.VISIBLE);
        }else if(requestTrip.getDestinations().size() == 3){
            destination2Txt.setText(GucPoints.getGucPlaceByLatLng(requestTrip.getDestinations().get(1)).getName());
            destination3Txt.setText(GucPoints.getGucPlaceByLatLng(requestTrip.getDestinations().get(2)).getName());
            destination2.setVisibility(View.VISIBLE);
            dot1to2.setVisibility(View.VISIBLE);
            destination3.setVisibility(View.VISIBLE);
            dot2to3.setVisibility(View.VISIBLE);
        }
    }

    private Trip createdTrip;
    private View.OnClickListener requestTripButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            boolean allowed = checkAllPermissions();
            if(allowed){
                accountVerifiedAndFree();
            }
        }
    };

    private boolean checkAllPermissions() {
        button.setProgress(1);
        if(!isNetworkStatusAvialable(getContext())) {
            Alerter.clearCurrent(getActivity());
            alert = Alerter.create(getActivity())
                    .setTitle("No Internet Connection !!")
                    .setText("Please enable internet connection to proceed. Click to dismiss when internet connection is available.")
                    .enableIconPulse(true)
                    .disableOutsideTouch()
                    .setBackgroundColorRes(R.color.red_error)
                    .enableInfiniteDuration(true)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(isNetworkStatusAvialable(getContext())){
                                alert.hide();
                            }
                        }
                    })
                    .show();
            button.setProgress(0);
            return false;
        } else if(!isGpsAvailable(getActivity().getApplicationContext())){
            Alerter.clearCurrent(getActivity());
            alert = Alerter.create(getActivity())
                    .setTitle("Location is turned off !")
                    .setText("Click here to enable your Location from Settings. Location is used to check whether you are inside the GUC campus.")
                    .enableIconPulse(true)
                    .setBackgroundColorRes(R.color.red_error)
                    .setDuration(5000)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .show();
            button.setProgress(0);

            return false;
        } else if(lastLocation == null) {
            Alerter.clearCurrent(getActivity());
            alert = Alerter.create(getActivity())
                    .setTitle("Can not get Location updates !")
                    .setText("Please check your location settings.")
                    .enableIconPulse(true)
                    .setBackgroundColorRes(R.color.red_error)
                    .setDuration(5000)
                    .show();
            button.setProgress(0);

            return false;
        }else if(GucPoints.GUC.contains(lastLocation)) {
            ////////////////////This has to be changed to NOT////////////////////////////////
            Alerter.clearCurrent(getActivity());
            alert = Alerter.create(getActivity())
                    .setTitle("You are not inside GUC campus !")
                    .setText("You can order a car only inside the GUC campus.")
                    .enableIconPulse(true)
                    .setBackgroundColorRes(R.color.red_error)
                    .setDuration(5000)
                    .show();
            button.setProgress(0);

            return false;
        } else {
            Alerter.clearCurrent(getActivity());
        }
        return true;
    }

    private void accountVerifiedAndFree() {
        button.setProgress(1);

        String verifyUrl = getResources().getString(R.string.url_get_profile)+ MainActivity.mAuth.getCurrentUser().getEmail();
        JsonObjectRequest verifiedRequest = new JsonObjectRequest
                (Request.Method.GET, verifyUrl, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        Profile profile = gson.fromJson(response.toString(), Profile.class);

                        if(profile.isVerified()){
                            if(getContext()==null){
                                return;
                            }
                            checkOngoingTrip();
                        }else {
                            notifyAccountNotVerified();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getContext(),"Error, please try again.", Toast.LENGTH_LONG).show();
                    }
                });

        verifiedRequest.setTag(TAG);
        MyVolleySingleton.getInstance(getContext()).addToRequestQueue(verifiedRequest);
    }

    private void checkOngoingTrip() {
        String freeUrl = getResources().getString(R.string.url_check_ongoing_trip)+ MainActivity.mAuth.getCurrentUser().getEmail();
        JsonObjectRequest freeRequest = new JsonObjectRequest
                (Request.Method.GET, freeUrl, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        boolean free = false;
                        try {
                            free = response.getBoolean("FREE");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if(free){
//                            Toast.makeText(getContext(),"No ongoing trips.", Toast.LENGTH_SHORT).show();
                            submitTripRequest();
                        }else {
                            String tripID = "";
                            try {
                                tripID = response.getString("TRIP_ID");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            notifyAlreadyOnTrip(tripID);
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if(getContext()==null){
                            return;
                        }
                        Toast.makeText(getContext(),"Error, please try again.", Toast.LENGTH_LONG).show();
                    }
                });

        freeRequest.setTag(TAG);
        MyVolleySingleton.getInstance(getContext()).addToRequestQueue(freeRequest);

    }

    private void submitTripRequest() {

        requestTrip.setUserID(MainActivity.mAuth.getCurrentUser().getEmail());
        requestTrip.setUserFcmToken(FirebaseInstanceId.getInstance().getToken());

        String str = gson.toJson(Trip.toTrip(requestTrip));
        JSONObject trip = new JSONObject();
        try {
            trip = new JSONObject(str);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = getResources().getString(R.string.url_request_trip);
        JsonObjectRequest tripRequest = new JsonObjectRequest
                (Request.Method.POST, url, trip, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        createdTrip = gson.fromJson(response.toString(),Trip.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("EVENT","CREATE");
                        bundle.putString("CAR_ID",createdTrip.getCarID());
                        bundle.putString("TRIP_ID",createdTrip.getId());
                        if(getContext()==null){
                            return;
                        }
                        Toast.makeText(getContext(),"RequestTrip successfully requested.",Toast.LENGTH_SHORT).show();
                        ((MainActivity)getActivity()).showOnTripFragment(bundle);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if(getContext()==null){
                            return;
                        }
                        Toast.makeText(getContext(),"Error, please try again.", Toast.LENGTH_LONG).show();
                    }
                });

        tripRequest.setTag(TAG);
        MyVolleySingleton.getInstance(getContext()).addToRequestQueue(tripRequest);

    }

    private void notifyAlreadyOnTrip(final String tripID) {
        if(getContext()==null){
            return;
        }
//        Alerter.clearCurrent(getActivity());
//        alert = Alerter.create(getActivity())
//                .setTitle("You already have an ongoing trip !")
//                .setText("Click here to track your current trip. You can not order multiple trips at the same time.")
//                .enableIconPulse(true)
//                .setBackgroundColorRes(R.color.red_error)
//                .setDuration(10000)
//                .setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        Bundle bundle = new Bundle();
//                        bundle.putString("TRIP_ID", tripID);
//                        ((MainActivity)getActivity()).showOnTripFragment(bundle);
//                    }
//                })
//                .show();

        Snackbar snackbar = Snackbar.make(getActivity().getCurrentFocus(),"You already have an ongoing trip",Snackbar.LENGTH_LONG);
        snackbar.setAction("View", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString("TRIP_ID", tripID);
                ((MainActivity)getActivity()).showOnTripFragment(bundle);
            }
        });
        snackbar.show();

        button.setProgress(0);
    }

    private void notifyAccountNotVerified() {
        if(getContext()==null){
            return;
        }
        Snackbar snackbar = Snackbar.make(getActivity().getCurrentFocus(),"Please verify your account",Snackbar.LENGTH_LONG);
        snackbar.setAction("Verify", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)getActivity()).showVerifyFragment();
            }
        });
        snackbar.show();
        button.setProgress(0);
    }

    public static boolean isNetworkStatusAvialable (Context context) {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        return isConnected;
    }

    private boolean isGpsAvailable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mMap != null) {
            checkLocationPermission();
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }
        actionBar.setTitle("Request Car");

        CoordinatorLayout coordinatorLayout = getActivity().findViewById(R.id.confirm_fragment);
        Snackbar snackbar = Snackbar.make(coordinatorLayout, "Please revise your trip details before requesting", Snackbar.LENGTH_LONG);
        View view = snackbar.getView();
        view.setBackgroundColor(getResources().getColor(R.color.snackbar_black));
        TextView textView = view.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        CoordinatorLayout.LayoutParams params=(CoordinatorLayout.LayoutParams)view.getLayoutParams();
        params.gravity = Gravity.TOP;
        view.setLayoutParams(params);
        snackbar.show();
        }

    @Override
    public void onPause() {
        super.onPause();
        Alerter.clearCurrent(getActivity());
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onStop() {
        super.onStop();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        if (MyVolleySingleton.getInstance(getActivity()).getRequestQueue() != null) {
            MyVolleySingleton.getInstance(getActivity()).getRequestQueue().cancelAll(TAG);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.clear();

        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setMinZoomPreference(16.0f);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        mMap.setLatLngBoundsForCameraTarget(GucPoints.GUC);

        boolean success = googleMap.setMapStyle(new MapStyleOptions(getResources()
                .getString(R.string.style_json)));

        if (!success) {
            Log.e("PickupFragment", "Style parsing failed.");
        }

        addMarkersToMap();

        locationRequest = new LocationRequest();
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //to be removed
        LatLng latLng = new LatLng(29.987243, 31.441902);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));

        drawTripRoute();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                checkLocationPermission();
            }else{
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }
        }else {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }
    }

    private void drawTripRoute() {
//        List<LatLng> waypoints = new ArrayList<>();
//        for (LatLng latLng : requestTrip.getDestinations()){
//            waypoints.add(latLng);
//        }
        GoogleDirection.withServerKey(getResources().getString(R.string.google_maps_key))
                .from(requestTrip.getPickupLocation())
//                .and(waypoints)
                .to(requestTrip.getDestinations().get(0))
                .transportMode(TransportMode.DRIVING)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {
                        if(getContext()==null){
                            return;
                        }
                        if(direction.isOK()) {
                            Route route = direction.getRouteList().get(0);
                            for (Leg leg : route.getLegList()) {
                                ArrayList<LatLng> directionPositionList = leg.getDirectionPoint();
                                PolylineOptions polylineOptions = DirectionConverter.createPolyline(getActivity(), directionPositionList, 3, Color.GRAY);
                                mMap.addPolyline(polylineOptions);
                            }
                        } else {
                            // Do something
                        }
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        // Do something
                    }
                });

    }

    public void addMarkersToMap(){
        CustomMarker customMarker = new CustomMarker(getContext());

        //pickup marker
        customMarker.setImage(R.drawable.custom_marker_start);
        customMarker.setText("Pickup");
        markers.put( GucPoints.getGucPlaceByLatLng(requestTrip.getPickupLocation()).getName(),
                mMap.addMarker(new MarkerOptions().position(GucPoints.getGucPlaceByLatLng(requestTrip.getPickupLocation()).getLatLng())
                        .title(GucPoints.getGucPlaceByLatLng(requestTrip.getPickupLocation()).getName()).icon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView())))
        );

        //destination markers
        ArrayList<GucPlace> destinationArray = new ArrayList<>();
        for (LatLng latLng : requestTrip.getDestinations()){
            destinationArray.add( GucPoints.getGucPlaceByLatLng(latLng) );
        }
        customMarker.setImage(R.drawable.custom_marker_end);
        for(GucPlace place : destinationArray){
            customMarker.setText(place.getName());
            markers.put( place.getName(),
                    mMap.addMarker(new MarkerOptions().position(place.getLatLng())
                            .title(place.getName()).icon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView())))
            );
        }

    }

    LocationCallback locationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            for (Location location : locationResult.getLocations()){
                lastLocation = new LatLng(location.getLatitude(), location.getLongitude());
            }
        }
    };

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)){
                new AlertDialog.Builder(getContext())
                        .setTitle("Permission Missing")
                        .setMessage("Please give the missing permissions for the app to function")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create().show();
            }else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
        else{
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1: if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ){
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                    mMap.setMyLocationEnabled(true);
                }

            } else {
                Toast.makeText(getContext(), "Please provide location permission", Toast.LENGTH_LONG).show();
            }
                break;
        }
    }

    public void setRequestTrip(RequestTrip requestTrip) {
        this.requestTrip = requestTrip;
    }
}
