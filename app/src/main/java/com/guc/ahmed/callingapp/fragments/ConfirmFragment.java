package com.guc.ahmed.callingapp.fragments;


import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
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
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.dd.processbutton.iml.ActionProcessButton;
import com.gitonway.lee.niftymodaldialogeffects.lib.NiftyDialogBuilder;
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
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.guc.ahmed.callingapp.MainActivity;
import com.guc.ahmed.callingapp.R;
import com.guc.ahmed.callingapp.apiclasses.MyVolleySingleton;
import com.guc.ahmed.callingapp.classes.Trip;
import com.guc.ahmed.callingapp.fcm.MyFirebaseInstanceIDService;
import com.guc.ahmed.callingapp.gucpoints.GucPlace;
import com.guc.ahmed.callingapp.gucpoints.GucPoints;
import com.guc.ahmed.callingapp.map.CustomMarker;
import com.tapadoo.alerter.Alerter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfirmFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private View view;

    private LatLng lastLocation;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private HashMap<String,Marker> markers;
    private NiftyDialogBuilder dialogBuilder;
    private ProgressDialog progressDialog;
    private List<Polyline> polylines;
    private ActionProcessButton button;
    private AppCompatActivity activity;
    private ActionBar actionBar;
    private Trip requestTrip;

    private LinearLayout destination2;
    private LinearLayout destination3;
    private LinearLayout dot1to2;
    private LinearLayout dot2to3;
    private TextView pickupTxt;
    private TextView destination1Txt;
    private TextView destination2Txt;
    private TextView destination3Txt;
    private Gson gson;

    public ConfirmFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view =  inflater.inflate(R.layout.fragment_confirm, container, false);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

        dialogBuilder=NiftyDialogBuilder.getInstance(getContext());

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
        polylines = new ArrayList<>();

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

    private View.OnClickListener requestTripButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            button.setProgress(1);

            requestTrip.setUserID(MainActivity.mAuth.getCurrentUser().getEmail());
            requestTrip.setUserFcmToken(FirebaseInstanceId.getInstance().getToken());

            String str = gson.toJson(requestTrip);
            JSONObject trip = new JSONObject();
            try {
                trip = new JSONObject(str);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String url = getResources().getString(R.string.url_request_trip);
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.POST, url, trip, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            Log.v("Confirmed Trip", response.toString());
                            Toast.makeText(getContext(),"Trip successfully requested.",Toast.LENGTH_SHORT).show();
                            Trip createdTrip = gson.fromJson(response.toString(),Trip.class);
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    });

            // Access the RequestQueue through your singleton class.
            MyVolleySingleton.getInstance(getActivity()).addToRequestQueue(jsonObjectRequest);

        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if(mMap != null) {
            checkLocationPermission();
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }
        actionBar.setTitle("Your Trip");
//        Alerter.create(getActivity())
//                .setTitle("Trip Details")
//                .setText("Please revise your trip details showing the pickup location followed by your chosen destination(s). For any changes press the back button.")
//                .enableSwipeToDismiss()
//                .enableIconPulse(true)
//                .setBackgroundColorRes(R.color.colorAccent)
//                .setDuration(5000)
//                .show();

        CoordinatorLayout coordinatorLayout = getActivity().findViewById(R.id.confirm_fragment);
        Snackbar snackbar = Snackbar.make(coordinatorLayout, "Please revise your trip details before requesting", Snackbar.LENGTH_LONG);
        View view = snackbar.getView();
        view.setBackgroundColor(getResources().getColor(R.color.fbutton_color_turquoise));
        TextView textView = view.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        CoordinatorLayout.LayoutParams params=(CoordinatorLayout.LayoutParams)view.getLayoutParams();
        params.gravity = Gravity.TOP;
        view.setLayoutParams(params);
        snackbar.show();

        Log.v("Tracking....", "RESUMED");
    }

    @Override
    public void onPause() {
        super.onPause();
        Alerter.clearCurrent(getActivity());
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        Log.v("Tracking....", "PAUSED");
    }

    @Override
    public void onStop() {
        super.onStop();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        Log.v("Tracking....", "STOPPED");
    }

    private PolygonOptions gucBorders = new PolygonOptions()
            .add(new LatLng(29.990504, 31.438447),
                    new LatLng(29.986922, 31.438330),
                    new LatLng(29.986035, 31.437930),
                    new LatLng(29.984474, 31.437909),
                    new LatLng(29.984441, 31.445711),
                    new LatLng(29.989794, 31.445640),
                    new LatLng(29.990705, 31.444224) );

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
        List<LatLng> waypoints = new ArrayList<>();
        for (LatLng latLng : requestTrip.getDestinations()){
            waypoints.add(latLng);
        }
        GoogleDirection.withServerKey(getResources().getString(R.string.google_maps_key))
                .from(requestTrip.getPickupLocation())
                .and(waypoints)
                .to(waypoints.get(waypoints.size()-1))
                .transportMode(TransportMode.DRIVING)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {
                        if(direction.isOK()) {
                            Route route = direction.getRouteList().get(0);
                            for (Leg leg : route.getLegList()) {
                                ArrayList<LatLng> directionPositionList = leg.getDirectionPoint();
                                PolylineOptions polylineOptions = DirectionConverter.createPolyline(getContext(), directionPositionList, 3, Color.GRAY);
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
        //customMarker.setText(GucPoints.getGucPlaceByLatLng(requestTrip.getPickupLocation()).getName());
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
            //customMarker.setText(place.getName());
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
            Log.v("LocationCallback", "UPDATING LOCATION");
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

    public void setRequestTrip(Trip requestTrip) {
        this.requestTrip = requestTrip;
    }
}
