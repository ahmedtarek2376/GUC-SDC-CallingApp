package com.guc.ahmed.callingapp.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Chronometer;
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guc.ahmed.callingapp.MainActivity;
import com.guc.ahmed.callingapp.R;
import com.guc.ahmed.callingapp.MyVolleySingleton;
import com.guc.ahmed.callingapp.gucpoints.GucPlace;
import com.guc.ahmed.callingapp.objects.Car;
import com.guc.ahmed.callingapp.gucpoints.GucPoints;
import com.guc.ahmed.callingapp.map.CustomMarker;
import com.guc.ahmed.callingapp.objects.Trip;
import com.guc.ahmed.callingapp.objects.TripDestination;
import com.guc.ahmed.callingapp.objects.TripEvent;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class OnTripFragment extends Fragment implements OnMapReadyCallback, View.OnClickListener, GoogleMap.OnMyLocationButtonClickListener {


    private View view;
    private Gson gson;
    private Car retrievedCar;
    private TextView statusTxt;
    private ActionProcessButton buttonStart;
    private ActionProcessButton buttonCancel;
    private ActionProcessButton buttonEnd;
    private ActionProcessButton buttonContinue;
    private ActionProcessButton buttonDone;
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

    private TextView edit;

    private Trip currentTrip;
    private GoogleMap mMap;
    private Handler carhandler;
    private Runnable updateCarLocation;
    private Marker carMarker;
    private CustomMarker customMarker;
    private String mTripID;
    private String mGmail;
    private String mEvent;
    private Chronometer timeElapsed;
    private Long startTime;
    private HashMap<String, Marker> markers;
    private String submitTripEventUrl;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;

    private static final String TAG = "ONTRIPFRAGMENT";


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
        buttonStart.setMode(ActionProcessButton.Mode.ENDLESS);
        buttonStart.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Regular.ttf"));
        buttonStart.setOnClickListener(this);
        buttonEnd = view.findViewById(R.id.on_trip_end);
        buttonEnd.setMode(ActionProcessButton.Mode.ENDLESS);
        buttonEnd.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Regular.ttf"));
        buttonEnd.setOnClickListener(this);
        buttonCancel = view.findViewById(R.id.on_trip_cancel);
        buttonCancel.setMode(ActionProcessButton.Mode.ENDLESS);
        buttonCancel.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Regular.ttf"));
        buttonCancel.setOnClickListener(this);
        buttonContinue = view.findViewById(R.id.on_trip_continue);
        buttonContinue.setMode(ActionProcessButton.Mode.ENDLESS);
        buttonContinue.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Regular.ttf"));
        buttonContinue.setOnClickListener(this);
        buttonDone = view.findViewById(R.id.on_trip_done);
        buttonDone.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Regular.ttf"));
        buttonDone.setOnClickListener(this);


        destination2 = view.findViewById(R.id.summary_destination_2);
        destination3 = view.findViewById(R.id.summary_destination_3);
        dot1to2 = view.findViewById(R.id.dot_1_2);
        dot2to3 = view.findViewById(R.id.dot_2_3);
        pickupTxt = view.findViewById(R.id.pickup_txt);
        destination1Txt = view.findViewById(R.id.destination_1_txt);
        destination2Txt = view.findViewById(R.id.destination_2_txt);
        destination3Txt = view.findViewById(R.id.destination_3_txt);
        tripPath = view.findViewById(R.id.trip_path);

        edit = view.findViewById(R.id.on_trip_edit);

        timeElapsed = view.findViewById(R.id.time_elapsed);

        if(getArguments()!=null){
            mTripID = getArguments().getString("TRIP_ID");
            mEvent = getArguments().getString("EVENT");
        }
        mGmail = MainActivity.mAuth.getCurrentUser().getEmail();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        markers = new HashMap<>();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mMap != null) {
            checkLocationPermission();
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationButtonClickListener(this);

        }
        actionBar.setTitle("Your Ride");
        if(mMap!=null){
            updateData(null);
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        if (MyVolleySingleton.getInstance(getActivity()).getRequestQueue() != null) {
            MyVolleySingleton.getInstance(getActivity()).getRequestQueue().cancelAll(TAG);
        }
    }

    public void resumeCarsUpdates() {
        if(carhandler!=null && updateCarLocation!=null && mMap!=null){
            carhandler.post(updateCarLocation);
        }
    }

    private void drawTripPathGraph() {
        pickupTxt.setText(GucPoints.getGucPlaceByLatLng(currentTrip.getPickupLocation()).getName());
        destination1Txt.setText(GucPoints.getGucPlaceByLatLng(currentTrip.getDestinations().get(0).getLocation()).getName());
        if(currentTrip.getDestinations().size() == 2){
            destination2Txt.setText(GucPoints.getGucPlaceByLatLng(currentTrip.getDestinations().get(1).getLocation()).getName());
            destination2.setVisibility(View.VISIBLE);
            dot1to2.setVisibility(View.VISIBLE);
        }else if(currentTrip.getDestinations().size() == 3){
            destination2Txt.setText(GucPoints.getGucPlaceByLatLng(currentTrip.getDestinations().get(1).getLocation()).getName());
            destination3Txt.setText(GucPoints.getGucPlaceByLatLng(currentTrip.getDestinations().get(2).getLocation()).getName());
            destination2.setVisibility(View.VISIBLE);
            dot1to2.setVisibility(View.VISIBLE);
            destination3.setVisibility(View.VISIBLE);
            dot2to3.setVisibility(View.VISIBLE);
        }
        tripPath.setVisibility(View.VISIBLE);
    }

    public void updateData(Bundle bundle) {
        if(bundle != null){
            mTripID = bundle.getString("TRIP_ID");
            mEvent = bundle.getString("EVENT");
        }
        String url = getResources().getString(R.string.url_get_trip_details) + mTripID;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        currentTrip = gson.fromJson(response.toString(), Trip.class);
                        edit.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ((MainActivity)getActivity()).showEditDestinationFragment(currentTrip);
                            }
                        });
                        drawCar();
                        addMarkersToMap();
                        updateUI();
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });

        jsonObjectRequest.setTag(TAG);
        MyVolleySingleton.getInstance(getActivity()).addToRequestQueue(jsonObjectRequest);
    }

    public void addMarkersToMap(){

        mMap.clear();

        for(Map.Entry<String, Marker> entry : markers.entrySet()){
            Marker marker = entry.getValue();
            marker.remove();
        }

        CustomMarker customMarker = new CustomMarker(getContext());

        //pickup marker
        customMarker.setImage(R.drawable.custom_marker_start);
        customMarker.setText(GucPoints.getGucPlaceByLatLng(currentTrip.getPickupLocation()).getName());
        markers.put("PICKUP_LOCATION",
                mMap.addMarker(new MarkerOptions().position(GucPoints.getGucPlaceByLatLng(currentTrip.getPickupLocation()).getLatLng())
                        .title(GucPoints.getGucPlaceByLatLng(currentTrip.getPickupLocation()).getName()).icon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView())))
        );

        //destination markers
        ArrayList<GucPlace> destinationArray = new ArrayList<>();
        for (TripDestination tripDestination : currentTrip.getDestinations()){
            destinationArray.add( GucPoints.getGucPlaceByLatLng(tripDestination.getLocation()) );
        }
        customMarker.setImage(R.drawable.custom_marker_end);
        int i = 1;
        for(GucPlace place : destinationArray){
            customMarker.setText(place.getName());
            markers.put( "DESTINATION_" + i,
                    mMap.addMarker(new MarkerOptions().position(place.getLatLng())
                            .title(place.getName()).icon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView())))
            );
            i++;
        }

        drawTripRoute();

    }

    private void drawTripRoute() {
        LatLng fromLatLng = null;
        LatLng toLatLng = null;
        List<TripDestination> destinations = currentTrip.getDestinations();

        if(!destinations.get(0).isArrived()){
            fromLatLng = currentTrip.getPickupLocation();
            toLatLng = destinations.get(0).getLocation();
        } else if (destinations.size()>1 && !destinations.get(1).isArrived()){
            fromLatLng = destinations.get(0).getLocation();
            toLatLng = destinations.get(1).getLocation();
        } else if (destinations.size()>2 && !destinations.get(2).isArrived()){
            fromLatLng = destinations.get(1).getLocation();
            toLatLng = destinations.get(2).getLocation();
        }

        if(fromLatLng !=null && toLatLng!=null) {
            GoogleDirection.withServerKey(getResources().getString(R.string.google_maps_key))
                    .from(fromLatLng)
//                .and(waypoints)
                    .to(toLatLng)
                    .transportMode(TransportMode.DRIVING)
                    .execute(new DirectionCallback() {
                        @Override
                        public void onDirectionSuccess(Direction direction, String rawBody) {
                            if (getContext() == null) {
                                return;
                            }
                            if (direction.isOK()) {
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

    }


    private void startElapsedTime() {
        if(startTime == null){
            if(currentTrip.getStartTime()!=null){
                startTime = currentTrip.getStartTime().getTime();
                long elapsed = SystemClock.elapsedRealtime() + startTime - new Date().getTime();
                timeElapsed.setBase(elapsed);
                timeElapsed.start();
            }
        }
    }

    private void updateUI() {
        drawTripPathGraph();
        startElapsedTime();
        String status = "Unknown";
        String event = currentTrip.getEvents().get(currentTrip.getEvents().size()-1);

        if(event.equalsIgnoreCase(TripEvent.CHANGE_DESTINATION.name())){
            event = currentTrip.getEvents().get(currentTrip.getEvents().size()-2);
        }

        if(event.equalsIgnoreCase(TripEvent.END.name()) || currentTrip.getEndTime()!=null){
            timeElapsed.stop();
            status = "Ride Ended";
            edit.setVisibility(View.GONE);
            buttonCancel.setVisibility(View.GONE);
            buttonEnd.setVisibility(View.GONE);
            buttonStart.setVisibility(View.GONE);
            buttonContinue.setVisibility(View.GONE);
            buttonDone.setVisibility(View.VISIBLE);
        }else if(event.equalsIgnoreCase(TripEvent.CANCEL.name()) || currentTrip.getCancelTime()!=null ){
            timeElapsed.stop();
            status = "Ride Canceled";
            edit.setVisibility(View.GONE);
            buttonCancel.setVisibility(View.GONE);
            buttonEnd.setVisibility(View.GONE);
            buttonStart.setVisibility(View.GONE);
            buttonContinue.setVisibility(View.GONE);
            buttonDone.setVisibility(View.VISIBLE);
        }else if(event.equalsIgnoreCase(TripEvent.ARRIVE_DESTINATION.name())){
            status = "Arrived Destination";
            buttonCancel.setVisibility(View.GONE);
            buttonEnd.setVisibility(View.VISIBLE);
            buttonStart.setVisibility(View.GONE);
            buttonContinue.setVisibility(View.VISIBLE);
            buttonContinue.setProgress(0);
        }else if(event.equalsIgnoreCase(TripEvent.ARRIVE_FINAL.name())){
            edit.setVisibility(View.GONE);
            status = "Arrived Last Destination";
            buttonCancel.setVisibility(View.GONE);
            buttonEnd.setVisibility(View.VISIBLE);
            buttonEnd.setProgress(0);
            buttonStart.setVisibility(View.GONE);
            buttonContinue.setVisibility(View.GONE);
        }else if(event.equalsIgnoreCase(TripEvent.START.name()) || event.equalsIgnoreCase(TripEvent.CONTINUE.name()) || event.equalsIgnoreCase(TripEvent.CHANGE_DESTINATION.name())){
            status = "On Ride";
            buttonCancel.setVisibility(View.GONE);
            buttonEnd.setVisibility(View.VISIBLE);
            buttonStart.setVisibility(View.GONE);
            buttonContinue.setVisibility(View.GONE);
        }else if(event.equalsIgnoreCase(TripEvent.ARRIVE_PICKUP.name())){
            status = "Car Arrived";
            buttonCancel.setVisibility(View.VISIBLE);
            buttonEnd.setVisibility(View.GONE);
            buttonStart.setVisibility(View.VISIBLE);
            buttonContinue.setVisibility(View.GONE);
        }
        else if(event.equalsIgnoreCase(TripEvent.CAR_ON_WAY.name())){
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

        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setMinZoomPreference(16.0f);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        //to be removed
        LatLng latLng = new LatLng(29.986654, 31.440191);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));

        mMap.setPadding(0,300,0,300);

        boolean success = googleMap.setMapStyle(new MapStyleOptions(getResources()
                .getString(R.string.style_json)));

        if (!success) {
            Log.e("PickupFragment", "Style parsing failed.");
        }
        // Constrain the camera target to the GUC bounds.
        mMap.setLatLngBoundsForCameraTarget(GucPoints.GUC);

        locationRequest = new LocationRequest();
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        updateData(null);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                checkLocationPermission();
            }else{
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
                mMap.setOnMyLocationButtonClickListener(this);
            }
        }else {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationButtonClickListener(this);
        }
    }

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
            mMap.setOnMyLocationButtonClickListener(this);

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
                    mMap.setOnMyLocationButtonClickListener(this);

                }

            } else {
                Toast.makeText(getContext(), "Please provide location permission", Toast.LENGTH_LONG).show();
            }
                break;
        }
    }

    private LatLng lastLocation;
    private LocationCallback locationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Log.v("LocationCallback", "UPDATING LOCATION");
            for (Location location : locationResult.getLocations()){
                lastLocation = new LatLng(location.getLatitude(), location.getLongitude());
            }
        }
    };

    private void drawCar() {

        if(carhandler != null){
            resumeCarsUpdates();
        }else {

            carhandler = new Handler();

            updateCarLocation = new Runnable() {
                @Override
                public void run() {

                    String url = getResources().getString(R.string.url_get_car_details) + currentTrip.getCarID();

                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                            (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                                @Override
                                public void onResponse(JSONObject response) {
                                    retrievedCar = gson.fromJson(response.toString(), Car.class);
                                    animateCar();
                                }
                            }, new Response.ErrorListener() {

                                @Override
                                public void onErrorResponse(VolleyError error) {

                                }
                            });

                    jsonObjectRequest.setTag(TAG);
                    MyVolleySingleton.getInstance(getActivity()).addToRequestQueue(jsonObjectRequest);

                    carhandler.postDelayed(updateCarLocation, 3000);
                }
            };

            carhandler.post(updateCarLocation);
        }
    }



    private void animateCar() {
        if(carMarker == null){
            customMarker = new CustomMarker(getContext());
            customMarker.setImage(R.drawable.ic_directions_car_black_24dp);
            carMarker = mMap.addMarker(new MarkerOptions().position(retrievedCar.getLatLng())
                    .title("CAR").icon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView())));
        } else {
//            Marker newMarker = carMarker;
            carMarker.remove();
            carMarker = mMap.addMarker(new MarkerOptions().position(carMarker.getPosition())
                    .title("CAR").icon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView())));
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
//                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lng)));

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

    @Override
    public void onPause() {
        super.onPause();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        stopCarsUpdates();
        timeElapsed.stop();
    }

    public void stopCarsUpdates() {
        if(carhandler!=null){
            carhandler.removeCallbacks(updateCarLocation);
        }
    }


    @Override
    public void onClick(View v) {
        submitTripEventUrl = "";

        final ActionProcessButton button = (ActionProcessButton)v;

        switch (v.getId()) {

//            case R.id.on_trip_start:
//                submitTripEventUrl = getResources().getString(R.string.url_trip_start) + mGmail;
//                mEvent = "START";
//                AlertDialog startDialog = new AlertDialog.Builder(getContext()).create();
//                startDialog.setTitle("Confirm Start");
//                startDialog.setMessage("Car will start moving. Are you sure you want to start your ride ?");
//                startDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        button.setProgress(1);
//                        submitTripEvent();
//                    }
//                });
//                startDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                });
//                startDialog.show();
//
//                break;

            case R.id.on_trip_continue:
                submitTripEventUrl = getResources().getString(R.string.url_trip_continue) + mGmail;
                mEvent = "CONTINUE";
                AlertDialog continueDialog = new AlertDialog.Builder(getContext()).create();
                continueDialog.setTitle("Confirm Continue");
                continueDialog.setMessage("Car will start moving. Are you sure you want to continue to your next destination ?");
                continueDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        button.setProgress(1);
                        submitTripEvent();
                    }
                });
                continueDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                continueDialog.show();
                break;

            case R.id.on_trip_end:
                submitTripEventUrl = getResources().getString(R.string.url_trip_end) + mGmail;
                mEvent = "END";
                AlertDialog endDialog = new AlertDialog.Builder(getContext()).create();
                endDialog.setTitle("Confirm End");
                endDialog.setMessage("Are you sure you want to end your ride ?");
                endDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        button.setProgress(1);
                        submitTripEvent();
                    }
                });
                endDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                endDialog.show();
                break;

            case R.id.on_trip_cancel:
                submitTripEventUrl = getResources().getString(R.string.url_trip_cancel) + mGmail;
                mEvent = "CANCEL";
                AlertDialog cancelDialog = new AlertDialog.Builder(getContext()).create();
                cancelDialog.setTitle("Confirm Cancel");
                cancelDialog.setMessage("Are you sure you want to cancel your ride ?");
                cancelDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        button.setProgress(1);
                        submitTripEvent();
                    }
                });
                cancelDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                cancelDialog.show();
                break;
                
            case R.id.on_trip_done:
                PickupFragment pickupFragment = new PickupFragment();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, pickupFragment, "PICKUP_FRAGMENT").commit();
                return;
            default:
                break;
        }

    }

    private void submitTripEvent() {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, submitTripEventUrl, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        currentTrip = gson.fromJson(response.toString(), Trip.class);
                        updateUI();
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

        jsonObjectRequest.setTag(TAG);
        // Access the RequestQueue through your singleton class.
        MyVolleySingleton.getInstance(getContext()).addToRequestQueue(jsonObjectRequest);
    }

    @Override
    public boolean onMyLocationButtonClick() {
        if(mMap!=null){
            mMap.moveCamera(CameraUpdateFactory.newLatLng(retrievedCar.getLatLng()));
        }
        return true;
    }
}
