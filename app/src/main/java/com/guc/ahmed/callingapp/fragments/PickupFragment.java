package com.guc.ahmed.callingapp.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
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
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guc.ahmed.callingapp.MainActivity;
import com.guc.ahmed.callingapp.R;
import com.guc.ahmed.callingapp.MyVolleySingleton;
import com.guc.ahmed.callingapp.objects.RequestTrip;
import com.guc.ahmed.callingapp.gucpoints.GucPlace;
import com.guc.ahmed.callingapp.gucpoints.GucPoints;
import com.guc.ahmed.callingapp.map.CustomMarker;
import com.tapadoo.alerter.Alert;
import com.tapadoo.alerter.Alerter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class PickupFragment extends Fragment
        implements OnMapReadyCallback {

    private GoogleMap mMap;
    private View view;

    private LatLng lastLocation;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private HashMap<String,Marker> markers;
    private Marker selectedMarker;
    private PickupFragment.OnPickupLocationListener onPickupLocationListener;

    private TextView pickupLocationText;
    private GucPlace pickupLocation;
    private ActionProcessButton button;

    private AppCompatActivity activity;
    private ActionBar actionBar;
    private RequestTrip requestTrip;
    private Alert alert;
    private Handler carhandler;
    private Runnable updateCars;
    private Gson gson;
    private ArrayList<GucPlace> gucPlaces;

    private static final String TAG = "PickupFragment";
    private Handler markersHandler;
    private Runnable getMarkers;

    public void setRequestTrip(RequestTrip requestTrip) {
        this.requestTrip = requestTrip;
    }

    public interface OnPickupLocationListener {
        void onPickupConfirmed(GucPlace gucPlace);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_pickup, container, false);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());


        button = (ActionProcessButton) view.findViewById(R.id.pickup_btn);
        button.setMode(ActionProcessButton.Mode.ENDLESS);
        button.setOnClickListener(confirmPickupOnClickListener);
        button.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Regular.ttf"));

        pickupLocationText = view.findViewById(R.id.pickup_txt);

        activity = (AppCompatActivity) getActivity();
        actionBar = activity.getSupportActionBar();

        try {
            onPickupLocationListener = (PickupFragment.OnPickupLocationListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement OnHeadlineSelectedListener");
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        markers = new HashMap<>();

        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();

        gucPlaces = MainActivity.gucPlaces;

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mMap != null) {
            checkLocationPermission();
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }
        actionBar.setTitle("Choose Pickup Location");

        CoordinatorLayout coordinatorLayout = getActivity().findViewById(R.id.pickup_fragment);
        Snackbar snackbar = Snackbar.make(coordinatorLayout, "Click on a pin to choose your pickup location", Snackbar.LENGTH_INDEFINITE);
        View view = snackbar.getView();
        view.setBackgroundColor(getResources().getColor(R.color.snackbar_black));
        TextView textView = view.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        CoordinatorLayout.LayoutParams params=(CoordinatorLayout.LayoutParams)view.getLayoutParams();
        params.gravity = Gravity.TOP;
        view.setLayoutParams(params);
        snackbar.show();

        resumeCarsUpdates();
        }

    @Override
    public void onPause() {
        super.onPause();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        Alerter.clearCurrent(getActivity());
        stopCarsUpdates();
    }

    @Override
    public void onStop() {
        super.onStop();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        if (MyVolleySingleton.getInstance(getActivity()).getRequestQueue() != null) {
            MyVolleySingleton.getInstance(getActivity()).getRequestQueue().cancelAll(TAG);
        }
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

        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setMinZoomPreference(16.0f);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        LatLng latLng = new LatLng(29.986654, 31.440191);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17));

        mMap.setPadding(0,150,0,0);

        boolean success = googleMap.setMapStyle(new MapStyleOptions(getResources()
                .getString(R.string.style_json)));

        if (!success) {
            Log.e("PickupFragment", "Style parsing failed.");
        }

        // Constrain the camera target to the GUC bounds.
        mMap.setLatLngBoundsForCameraTarget(GucPoints.GUC);

        addMarkersToMap();

        addCarsToMap();

        locationRequest = new LocationRequest();
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mMap.setOnMarkerClickListener(onMarkerClickListener);

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

    private void addCarsToMap() {
        carhandler = new Handler();

        updateCars = new Runnable() {
            @Override
            public void run() {

                String url = getResources().getString(R.string.url_get_available_cars);
                JsonArrayRequest jsonObjectRequest = new JsonArrayRequest
                        (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {

                            @Override
                            public void onResponse(JSONArray response) {
                                if(getContext()==null){
                                    return;
                                }
                                for(int i=0;i<response.length();i++){

                                    try {
                                        JSONObject object = response.getJSONObject(i);
                                        String carID = object.getString("carID");
                                        LatLng latLng = gson.fromJson(object.getJSONObject("latLng").toString(), LatLng.class);
                                        drawCarMarker(carID,latLng);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {

                            }
                        });

                jsonObjectRequest.setTag(TAG);
                // Access the RequestQueue through your singleton class.
                MyVolleySingleton.getInstance(getActivity()).addToRequestQueue(jsonObjectRequest);

                carhandler.postDelayed(updateCars, 3000);
            }
        };

        carhandler.postDelayed(updateCars, 0);

    }

    private void drawCarMarker(String carID, LatLng latLng) {
        Marker marker = markers.get(carID);

        if(marker != null){
            LatLng position = marker.getPosition();
            if(! position.equals(latLng)){ //Car has moved
                marker.setPosition(latLng);
            }
        } else {
            CustomMarker customMarker = new CustomMarker(getActivity());
            customMarker.setImage(R.drawable.ic_directions_car_black_24dp);
            markers.put( carID,
                    mMap.addMarker(new MarkerOptions().position(latLng)
                            .title("CAR").icon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView())))
            );
        }

    }

    public void stopCarsUpdates() {
        if(carhandler!=null){
            carhandler.removeCallbacks(updateCars);
        }
        if(markersHandler!=null){
            markersHandler.removeCallbacks(getMarkers);
        }
    }

    public void resumeCarsUpdates() {
        if(carhandler!=null && updateCars!=null && mMap!=null){
            carhandler.post(updateCars);
        }
    }

    private GoogleMap.OnMarkerClickListener onMarkerClickListener = new GoogleMap.OnMarkerClickListener() {

        @Override
        public boolean onMarkerClick(Marker marker) {
            if(marker.getTitle().equalsIgnoreCase("CAR") ){
                return true;
            }

            CustomMarker customMarker = new CustomMarker(getContext());
            if(selectedMarker != null){
                customMarker.setImage(R.drawable.ic_marker_black);
                customMarker.setText(selectedMarker.getTitle());
                selectedMarker.setIcon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView()));
            }
            selectedMarker = marker;

            customMarker.setImage(R.drawable.custom_marker_start);
            customMarker.setText(marker.getTitle());
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView()));

            GucPlace gucPlace = GucPoints.getGucPlaceByName(marker.getTitle());
            updatePickupLocation(gucPlace);

            return true;
        }
    };

    private View.OnClickListener confirmPickupOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(pickupLocation == null){
                CoordinatorLayout coordinatorLayout = getActivity().findViewById(R.id.pickup_fragment);
                Snackbar snackbar = Snackbar.make(coordinatorLayout, "Please select a pickup location", Snackbar.LENGTH_LONG);
                View view = snackbar.getView();
                view.setBackgroundColor(getResources().getColor(R.color.red_error));
                TextView textView = view.findViewById(android.support.design.R.id.snackbar_text);
                textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                CoordinatorLayout.LayoutParams params=(CoordinatorLayout.LayoutParams)view.getLayoutParams();
                params.gravity = Gravity.TOP;
                view.setLayoutParams(params);
                snackbar.show();
            }else {
                    Alerter.clearCurrent(getActivity());
                    onPickupLocationListener.onPickupConfirmed(pickupLocation);
                }
            }
    };

    private boolean isGpsAvailable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void addMarkersToMap(){

        markersHandler = new Handler();

        getMarkers = new Runnable() {
            @Override
            public void run() {
                if(MainActivity.gucPlaces.size() < 1){
                    markersHandler.postDelayed(getMarkers, 500);
                }else {
                    drawPins(MainActivity.gucPlaces);
                }
            }
        };

        markersHandler.postDelayed(getMarkers, 0);


    }

    private void drawPins(ArrayList<GucPlace> places) {
        if(getContext()==null){
            return;
        }
        CustomMarker customMarker = new CustomMarker(getContext());
        customMarker.setImage(R.drawable.ic_marker_black);
        for (GucPlace place : places){
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

    public PickupFragment() {
        // Required empty public constructor
    }


    public void updatePickupLocation(GucPlace location) {
        pickupLocation = location;
        pickupLocationText.setText(location.getName());
    }
}
