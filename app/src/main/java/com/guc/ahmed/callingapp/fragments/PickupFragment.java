package com.guc.ahmed.callingapp.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
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
import android.widget.TextView;
import android.widget.Toast;

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
import com.guc.ahmed.callingapp.R;
import com.guc.ahmed.callingapp.classes.Trip;
import com.guc.ahmed.callingapp.gucpoints.GucPlace;
import com.guc.ahmed.callingapp.gucpoints.GucPoints;
import com.guc.ahmed.callingapp.map.CustomMarker;
import com.tapadoo.alerter.Alert;
import com.tapadoo.alerter.Alerter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PickupFragment extends Fragment
        implements OnMapReadyCallback {

    private GoogleMap mMap;
    private View view;

    private LatLng lastLocation;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private HashMap<String,Marker> markers;
    private NiftyDialogBuilder dialogBuilder;
    private List<Polyline> polylines;
    private Marker selectedMarker;
    private PickupFragment.OnPickupLocationListener onPickupLocationListener;

    private TextView pickupLocationText;
    private GucPlace pickupLocation;
    private ActionProcessButton button;

    private AppCompatActivity activity;
    private ActionBar actionBar;
    private Trip requestTrip;
    private Alert alert;

    public void setRequestTrip(Trip requestTrip) {
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

        Log.v("PICKUP", "onCreate");

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

        dialogBuilder=NiftyDialogBuilder.getInstance(getContext());

        button = (ActionProcessButton) view.findViewById(R.id.pickup_btn);
        button.setMode(ActionProcessButton.Mode.ENDLESS);
        button.setOnClickListener(confirmPickupOnClickListener);
        Typeface roboBlack = Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Regular.ttf");
        button.setTypeface(roboBlack);

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
        polylines = new ArrayList<>();

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
//        alert = Alerter.create(getActivity())
//                .setTitle("Choose Pickup Location")
//                .setText("Click on a pin on the map to choose your pickup location.")
//                .enableSwipeToDismiss()
//                .enableIconPulse(true)
//                .setIcon(R.drawable.custom_marker_start)
//                .setBackgroundColorRes(R.color.colorAccent)
//                .setDuration(5000)
//                .show();

        CoordinatorLayout coordinatorLayout = getActivity().findViewById(R.id.pickup_fragment);
        Snackbar snackbar = Snackbar.make(coordinatorLayout, "Click on a pin to choose your pickup location", Snackbar.LENGTH_LONG);
        View view = snackbar.getView();
        view.setBackgroundColor(getResources().getColor(R.color.fbutton_color_turquoise));
        TextView textView = view.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        CoordinatorLayout.LayoutParams params=(CoordinatorLayout.LayoutParams)view.getLayoutParams();
        params.gravity = Gravity.TOP;
        view.setLayoutParams(params);
        snackbar.show();

        Log.v("PICKUP", "onREsume");
    }

    @Override
    public void onPause() {
        super.onPause();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        Alerter.clearCurrent(getActivity());
        Log.v("PICKUP", "onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        Log.v("PICKUP", "onStop");
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
        Log.v("PICKUP", "onMapCreated");
        mMap = googleMap;

        mMap.clear();

        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setMinZoomPreference(16.0f);
        mMap.setBuildingsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        boolean success = googleMap.setMapStyle(new MapStyleOptions(getResources()
                .getString(R.string.style_json)));

        if (!success) {
            Log.e("PickupFragment", "Style parsing failed.");
        }

        // Constrain the camera target to the Adelaide bounds.
        mMap.setLatLngBoundsForCameraTarget(GucPoints.GUC);

        addMarkersToMap();

//        Polygon polygon = mMap.addPolygon(
//                gucBorders
//                        .strokeColor(Color.BLUE).strokeWidth(5)
//                        .fillColor(Color.BLUE)
//                        .strokeJointType(JointType.BEVEL)
//        );
//        polygon.setFillColor(Color.argb(
//                20, Color.red(Color.BLUE), Color.green(Color.BLUE),
//                Color.blue(Color.BLUE)));

        locationRequest = new LocationRequest();
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //to be removed
        LatLng latLng = new LatLng(29.987243, 31.441902);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));


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

    private GoogleMap.OnMarkerClickListener onMarkerClickListener = new GoogleMap.OnMarkerClickListener() {

        @Override
        public boolean onMarkerClick(Marker marker) {

            CustomMarker customMarker = new CustomMarker(getContext());
            if(selectedMarker != null){
                customMarker.setImage(R.drawable.custom_marker_pin);
                customMarker.setText(selectedMarker.getTitle());
                selectedMarker.setIcon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView()));
            }
            selectedMarker = marker;

            customMarker.setImage(R.drawable.custom_marker_start);
            customMarker.setText(marker.getTitle());
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView()));

            GucPlace gucPlace = GucPoints.getGucPlaceByName(marker.getTitle());
            //onPickupLocationListener.onPickupMarkerSelected(gucPlace);
            updatePickupLocation(gucPlace);

            return true;
        }
    };

    private View.OnClickListener confirmPickupOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(pickupLocation == null){
                CoordinatorLayout coordinatorLayout = getActivity().findViewById(R.id.pickup_fragment);
                Snackbar snackbar = Snackbar.make(coordinatorLayout, "Please select a pickup location", Snackbar.LENGTH_SHORT);
                View view = snackbar.getView();
                view.setBackgroundColor(getResources().getColor(R.color.red_error));
                TextView textView = view.findViewById(android.support.design.R.id.snackbar_text);
                textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                CoordinatorLayout.LayoutParams params=(CoordinatorLayout.LayoutParams)view.getLayoutParams();
                params.gravity = Gravity.TOP;
                view.setLayoutParams(params);
                snackbar.show();
            }else{
                if(!isGpsAvailable(getActivity().getApplicationContext())){
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
                } else if(lastLocation == null) {
                    Alerter.clearCurrent(getActivity());
                    alert = Alerter.create(getActivity())
                            .setTitle("Can not get Location updates !")
                            .setText("Please check your location settings.")
                            .enableIconPulse(true)
                            .setBackgroundColorRes(R.color.red_error)
                            .setDuration(5000)
                            .show();
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
                } else {
                    Alerter.clearCurrent(getActivity());
                    onPickupLocationListener.onPickupConfirmed(pickupLocation);
                }
            }
        }
    };

    private boolean isGpsAvailable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void addMarkersToMap(){
        CustomMarker customMarker = new CustomMarker(getContext());
        customMarker.setImage(R.drawable.custom_marker_pin);

        customMarker.setText(GucPoints.D4_U_AREA.getName());
        markers.put( GucPoints.D4_U_AREA.getName(),
                mMap.addMarker(new MarkerOptions().position(GucPoints.D4_U_AREA.getLatLng())
                        .title(GucPoints.D4_U_AREA.getName()).icon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView())))
        );

        customMarker.setText(GucPoints.C3_U_AREA.getName());
        markers.put( GucPoints.C3_U_AREA.getName(),
                mMap.addMarker(new MarkerOptions().position(GucPoints.C3_U_AREA.getLatLng())
                        .title(GucPoints.C3_U_AREA.getName()).icon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView())))
        );

        customMarker.setText(GucPoints.C6_U_AREA.getName());
        markers.put( GucPoints.C6_U_AREA.getName(),
                mMap.addMarker(new MarkerOptions().position(GucPoints.C6_U_AREA.getLatLng())
                        .title(GucPoints.C6_U_AREA.getName()).icon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView())))
        );

        customMarker.setText(GucPoints.B3_U_AREA.getName());
        markers.put( GucPoints.B3_U_AREA.getName(),
                mMap.addMarker(new MarkerOptions().position(GucPoints.B3_U_AREA.getLatLng())
                        .title(GucPoints.B3_U_AREA.getName()).icon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView())))
        );

        customMarker.setText(GucPoints.GUC_GYM.getName());
        markers.put( GucPoints.GUC_GYM.getName(),
                mMap.addMarker(new MarkerOptions().position(GucPoints.GUC_GYM.getLatLng())
                        .title(GucPoints.GUC_GYM.getName()).icon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView())))
        );

        customMarker.setText(GucPoints.GATE_1.getName());
        markers.put( GucPoints.GATE_1.getName(),
                mMap.addMarker(new MarkerOptions().position(GucPoints.GATE_1.getLatLng())
                        .title(GucPoints.GATE_1.getName()).icon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView())))
        );

        customMarker.setText(GucPoints.GATE_3.getName());
        markers.put( GucPoints.GATE_3.getName(),
                mMap.addMarker(new MarkerOptions().position(GucPoints.GATE_3.getLatLng())
                        .title(GucPoints.GATE_3.getName()).icon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView())))
        );

        customMarker.setText(GucPoints.GATE_4.getName());
        markers.put( GucPoints.GATE_4.getName(),
                mMap.addMarker(new MarkerOptions().position(GucPoints.GATE_4.getLatLng())
                        .title(GucPoints.GATE_4.getName()).icon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView())))
        );
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
