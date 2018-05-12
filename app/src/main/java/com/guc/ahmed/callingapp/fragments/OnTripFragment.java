package com.guc.ahmed.callingapp.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
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
import com.guc.ahmed.callingapp.apiclasses.MyVolleySingleton;
import com.guc.ahmed.callingapp.gucpoints.GucPlace;
import com.guc.ahmed.callingapp.objects.Car;
import com.guc.ahmed.callingapp.gucpoints.GucPoints;
import com.guc.ahmed.callingapp.map.CustomMarker;
import com.guc.ahmed.callingapp.objects.Trip;
import com.guc.ahmed.callingapp.objects.TripDestination;
import com.guc.ahmed.callingapp.objects.TripEvent;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class OnTripFragment extends Fragment implements OnMapReadyCallback, View.OnClickListener {


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

        timeElapsed = view.findViewById(R.id.time_elapsed);

        if(getArguments()!=null){
            mTripID = getArguments().getString("TRIP_ID");
            mEvent = getArguments().getString("EVENT");
        }
        mGmail = MainActivity.mAuth.getCurrentUser().getEmail();

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
        actionBar.setTitle("Your Ride");
        if(mMap!=null){
            updateData(null);
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
        Log.v("CarDetails", url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.v("CarDetails", "It worked");
                        currentTrip = gson.fromJson(response.toString(), Trip.class);
                        drawCar();
                        addMarkersToMap();
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

    public void addMarkersToMap(){

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
        List<LatLng> waypoints = new ArrayList<>();
        for (TripDestination tripDestination : currentTrip.getDestinations()){
            waypoints.add(tripDestination.getLocation());
        }
        GoogleDirection.withServerKey(getResources().getString(R.string.google_maps_key))
                .from(currentTrip.getPickupLocation())
                .and(waypoints)
                .to(waypoints.get(waypoints.size()-1))
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
        String event = currentTrip.getEvent();

        if(event.equalsIgnoreCase(TripEvent.END.name())){
            timeElapsed.stop();
            status = "Trip Ended";
            buttonCancel.setVisibility(View.GONE);
            buttonEnd.setVisibility(View.GONE);
            buttonStart.setVisibility(View.GONE);
            buttonContinue.setVisibility(View.GONE);
            buttonDone.setVisibility(View.VISIBLE);
        }else if(event.equalsIgnoreCase(TripEvent.CANCEL.name())){
            timeElapsed.stop();
            status = "Trip Canceled";
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
        }else if(event.equalsIgnoreCase(TripEvent.ARRIVE_FINAL.name())){
            status = "Arrived Last Destination";
            buttonCancel.setVisibility(View.GONE);
            buttonEnd.setVisibility(View.VISIBLE);
            buttonStart.setVisibility(View.GONE);
            buttonContinue.setVisibility(View.GONE);
        }else if(event.equalsIgnoreCase(TripEvent.START.name()) || event.equalsIgnoreCase(TripEvent.CONTINUE.name())){
            status = "On Trip";
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

        boolean success = googleMap.setMapStyle(new MapStyleOptions(getResources()
                .getString(R.string.style_json)));
        // Constrain the camera target to the GUC bounds.
        mMap.setLatLngBoundsForCameraTarget(GucPoints.GUC);

        //to be removed
        LatLng latLng = new LatLng(29.987243, 31.441902);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));

        updateData(null);
    }

    private void drawCar() {

        if(carhandler != null){
            resumeCarsUpdates();
        }else {

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
                                    Activity activity = getActivity();
                                    if (activity != null && isAdded())
                                        Log.v("CarDetails", "It didnt work");
                                }
                            });

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
//            mMap.moveCamera(CameraUpdateFactory.newLatLng(retrievedCar.getLatLng()));
        } else {
//            mMap.moveCamera(CameraUpdateFactory.newLatLng(retrievedCar.getLatLng()));
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

            case R.id.on_trip_start:
                submitTripEventUrl = getResources().getString(R.string.url_trip_start) + mGmail;
                mEvent = "START";
                AlertDialog startDialog = new AlertDialog.Builder(getContext()).create();
                startDialog.setTitle("Confirm Start");
                startDialog.setMessage("Car will start moving. Are you sure you want to start your ride ?");
                startDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        button.setProgress(1);
                        submitTripEvent();
                    }
                });
                startDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                startDialog.show();

                break;

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
                        Toast.makeText(getContext(),"Error, please try again.", Toast.LENGTH_LONG);
                    }
                });

        // Access the RequestQueue through your singleton class.
        MyVolleySingleton.getInstance(getContext()).addToRequestQueue(jsonObjectRequest);
    }

    private void updateOfflineUI() {
        Log.v("ONTRIP", "I reacher update offline");
        String status = "Unknown";
        if(mEvent.equalsIgnoreCase("START")){
            status = "Moving in Car";
            buttonCancel.setVisibility(View.VISIBLE);
            buttonEnd.setVisibility(View.GONE);
            buttonStart.setVisibility(View.GONE);
            buttonContinue.setVisibility(View.GONE);
        }else if(mEvent.equalsIgnoreCase("CONTINUE")){
            status = "Moving in Car";
            buttonCancel.setVisibility(View.VISIBLE);
            buttonEnd.setVisibility(View.GONE);
            buttonStart.setVisibility(View.GONE);
            buttonContinue.setVisibility(View.GONE);

        }else if(mEvent.equalsIgnoreCase("CANCEL")){
            status = "Trip Canceled";
            buttonCancel.setVisibility(View.GONE);
            buttonEnd.setVisibility(View.GONE);
            buttonStart.setVisibility(View.GONE);
            buttonContinue.setVisibility(View.GONE);

        }else if(mEvent.equalsIgnoreCase("END")){
            status = "Trip Ended";
            buttonCancel.setVisibility(View.GONE);
            buttonEnd.setVisibility(View.GONE);
            buttonStart.setVisibility(View.GONE);
            buttonContinue.setVisibility(View.GONE);

        }
        statusTxt.setText(status);
    }
}
