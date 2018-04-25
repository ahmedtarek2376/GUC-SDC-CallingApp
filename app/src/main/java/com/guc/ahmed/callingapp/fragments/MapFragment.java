//package com.guc.ahmed.callingapp.fragments;
//
//
//import android.Manifest;
//import android.app.AlertDialog;
//import android.app.ProgressDialog;
//import android.content.DialogInterface;
//import android.content.pm.PackageManager;
//import android.graphics.Color;
//import android.location.Location;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Looper;
//import android.support.annotation.NonNull;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.app.Fragment;
//import android.support.v4.content.ContextCompat;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Toast;
//
//import com.gitonway.lee.niftymodaldialogeffects.lib.NiftyDialogBuilder;
//import com.google.android.gms.location.FusedLocationProviderClient;
//import com.google.android.gms.location.LocationCallback;
//import com.google.android.gms.location.LocationRequest;
//import com.google.android.gms.location.LocationResult;
//import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.maps.CameraUpdateFactory;
//import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.OnMapReadyCallback;
//import com.google.android.gms.maps.SupportMapFragment;
//import com.google.android.gms.maps.model.BitmapDescriptorFactory;
//import com.google.android.gms.maps.model.JointType;
//import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.LatLngBounds;
//import com.google.android.gms.maps.model.Marker;
//import com.google.android.gms.maps.model.MarkerOptions;
//import com.google.android.gms.maps.model.Polygon;
//import com.google.android.gms.maps.model.PolygonOptions;
//import com.google.android.gms.maps.model.Polyline;
//import com.google.android.gms.maps.model.PolylineOptions;
//import com.guc.ahmed.callingapp.R;
//import com.guc.ahmed.callingapp.gucpoints.GucPlace;
//import com.guc.ahmed.callingapp.gucpoints.GucPoints;
//import com.guc.ahmed.callingapp.route.AbstractRouting;
//import com.guc.ahmed.callingapp.route.Route;
//import com.guc.ahmed.callingapp.route.RouteException;
//import com.guc.ahmed.callingapp.route.Routing;
//import com.guc.ahmed.callingapp.route.RoutingListener;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//
//
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////// TO BE DELETED /////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//public class MapFragment extends Fragment
//        implements OnMapReadyCallback, RoutingListener {
//
//    private GoogleMap mMap;
//    private View view;
//
//    private Location lastLocation;
//    private LocationRequest locationRequest;
//    private FusedLocationProviderClient fusedLocationProviderClient;
//    private HashMap<String,Marker> markers;
//    private NiftyDialogBuilder dialogBuilder;
//    private ProgressDialog progressDialog;
//    private List<Polyline> polylines;
//    private Marker selectedMarker;
//
//    private PickupFragment pickupFragment;
//    private OnPickupLocationListener  onPickupLocationListener;
//
//    // Create a LatLngBounds that includes the GUC boundaries
//    private static final LatLngBounds GUC = new LatLngBounds(
//            new LatLng(29.984414, 31.437786), new LatLng(29.990841, 31.445829) );
//
//    public MapFragment() {
//        // Required empty public constructor
//    }
//
//    public interface OnPickupLocationListener {
//        void onPickupMarkerSelected(GucPlace gucPlace);
//    }
//
//
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
//        view = inflater.inflate(R.layout.fragment_map, container, false);
//
//        pickupFragment = new PickupFragment();
//        getActivity().getSupportFragmentManager().beginTransaction()
//                .add(R.id.map_fragment_container, pickupFragment, "PICKUP_FRAGMENT").commit();
//
//        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
//
//        dialogBuilder=NiftyDialogBuilder.getInstance(getContext());
//
//        try {
//            onPickupLocationListener = (OnPickupLocationListener) getActivity();
//        } catch (ClassCastException e) {
//            throw new ClassCastException(getActivity().toString()
//                    + " must implement OnHeadlineSelectedListener");
//        }
//
//        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
//        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager()
//                .findFragmentById(R.id.map);
//        mapFragment.getMapAsync(this);
//
//        markers = new HashMap<>();
//        polylines = new ArrayList<>();
//
//        return view;
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        if(mMap != null) {
//            checkLocationPermission();
//            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
//            mMap.setMyLocationEnabled(true);
//        }
//        Log.v("Tracking....", "RESUMED");
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
//        Log.v("Tracking....", "PAUSED");
//    }
//
//    @Override
//    public void onStop() {
//        super.onStop();
//        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
//        Log.v("Tracking....", "STOPPED");
//    }
//
//    @Override
//    public void onMapReady(GoogleMap googleMap) {
//        Log.v("Tracking....", "ONMAPREADYCALLED");
//        mMap = googleMap;
//
//        mMap.clear();
//
//        mMap.getUiSettings().setTiltGesturesEnabled(true);
//        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
//        mMap.setMinZoomPreference(16.0f);
//        mMap.setBuildingsEnabled(true);
//        mMap.getUiSettings().setMapToolbarEnabled(false);
//
//
//        // Constrain the camera target to the Adelaide bounds.
//        mMap.setLatLngBoundsForCameraTarget(GUC);
//
//        addMarkersToMap();
//
//        PolygonOptions gucBorders = new PolygonOptions()
//                .add(new LatLng(29.990504, 31.438447),
//                        new LatLng(29.986922, 31.438330),
//                        new LatLng(29.986035, 31.437930),
//                        new LatLng(29.984474, 31.437909),
//                        new LatLng(29.984441, 31.445711),
//                        new LatLng(29.989794, 31.445640),
//                        new LatLng(29.990705, 31.444224) );
//
//        Polygon polygon = mMap.addPolygon(
//                gucBorders
//                .strokeColor(Color.BLUE).strokeWidth(5)
//                .fillColor(Color.BLUE)
//                .strokeJointType(JointType.BEVEL)
//        );
//        polygon.setFillColor(Color.argb(
//                30, Color.red(Color.BLUE), Color.green(Color.BLUE),
//                Color.blue(Color.BLUE)));
//
//        locationRequest = new LocationRequest();
//        locationRequest.setInterval(3000);
//        locationRequest.setFastestInterval(3000);
//        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//
//        //to be removed
//        LatLng latLng = new LatLng(29.987243, 31.441902);
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
//        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));
//
//
//        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
//
//            @Override
//            public boolean onMarkerClick(Marker marker) {
////                String title = marker.getTitle();
////
////                dialogBuilder
////                        .withTitle("Pickup location")
////                        .withMessage("Set pickup location to " + title + "?")
////                        .withDividerColor("#11000000")
////                        .withDialogColor("#90A4AE")
////                        .withDuration(150)
////                        .withEffect(Effectstype.Fadein)
////                        .withButton1Text("OK")
////                        .withButton2Text("Cancel")
////                        .setButton1Click(new View.OnClickListener() {
////                            @Override
////                            public void onClick(View v) {
////                                Toast.makeText(v.getContext(), "i'm btn1", Toast.LENGTH_SHORT).show();
////                            }
////                        })
////                        .setButton2Click(new View.OnClickListener() {
////                            @Override
////                            public void onClick(View v) {
////                                Toast.makeText(v.getContext(),"i'm btn2",Toast.LENGTH_SHORT).show();
////                            }
////                        })
////                        .show();
////                return true;
//
//                if(selectedMarker != null){
//                    selectedMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
//                }
//                selectedMarker = marker;
//
//                if(pickupFragment !=null && getActivity().getSupportFragmentManager().findFragmentByTag("PICKUP_FRAGMENT").isVisible()) {
//                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
//                    GucPlace gucPlace = GucPoints.getGucPlaceByName(marker.getTitle());
//                    onPickupLocationListener.onPickupMarkerSelected(gucPlace);
//                }
//                return true;
//            }
//        });
//
//        progressDialog = ProgressDialog.show(getContext(), "Please wait.",
//                "Fetching route information.", true);
//        Routing routing = new Routing.Builder()
//                .travelMode(AbstractRouting.TravelMode.DRIVING)
//                .withListener(this)
//                .alternativeRoutes(false)
//                .waypoints(GucPoints.GATE_1.getLatLng(), GucPoints.C6_U_AREA.getLatLng())
//                .build();
//        routing.execute();
//
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
//            if (ContextCompat.checkSelfPermission(getActivity(),
//                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                checkLocationPermission();
//            }else{
//                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
//                mMap.setMyLocationEnabled(true);
//            }
//        }else {
//            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
//            mMap.setMyLocationEnabled(true);
//        }
//    }
//
//    public void addMarkersToMap(){
//        markers.put( GucPoints.D4_U_AREA.getName(),
//                mMap.addMarker(new MarkerOptions().position(GucPoints.D4_U_AREA.getLatLng())
//                .title(GucPoints.D4_U_AREA.getName()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
//        );
//        markers.put( GucPoints.C3_U_AREA.getName(),
//                mMap.addMarker(new MarkerOptions().position(GucPoints.C3_U_AREA.getLatLng())
//                .title(GucPoints.C3_U_AREA.getName()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
//        );
//        markers.put( GucPoints.C6_U_AREA.getName(),
//                mMap.addMarker(new MarkerOptions().position(GucPoints.C6_U_AREA.getLatLng())
//                .title(GucPoints.C6_U_AREA.getName()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
//        );
//        markers.put( GucPoints.B4_U_AREA.getName(),
//                mMap.addMarker(new MarkerOptions().position(GucPoints.B4_U_AREA.getLatLng())
//                .title(GucPoints.B4_U_AREA.getName()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
//        );
//        markers.put( GucPoints.GUC_GYM.getName(),
//                mMap.addMarker(new MarkerOptions().position(GucPoints.GUC_GYM.getLatLng())
//                        .title(GucPoints.GUC_GYM.getName()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
//        );
//        markers.put( GucPoints.GATE_1.getName(),
//                mMap.addMarker(new MarkerOptions().position(GucPoints.GATE_1.getLatLng())
//                        .title(GucPoints.GATE_1.getName()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
//        );
//        markers.put( GucPoints.GATE_3.getName(),
//                mMap.addMarker(new MarkerOptions().position(GucPoints.GATE_3.getLatLng())
//                        .title(GucPoints.GATE_3.getName()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
//        );
//        markers.put( GucPoints.GATE_4.getName(),
//                mMap.addMarker(new MarkerOptions().position(GucPoints.GATE_4.getLatLng())
//                        .title(GucPoints.GATE_4.getName()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
//        );
//
//    }
//
//    LocationCallback locationCallback = new LocationCallback(){
//        @Override
//        public void onLocationResult(LocationResult locationResult) {
//            super.onLocationResult(locationResult);
//            Log.v("LocationCallback", "UPDATING LOCATION");
//            for (Location location : locationResult.getLocations()){
//
//                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//                //LatLng latLng = new LatLng(29.986926, 31.440630);
//
//                if(! GUC.contains(latLng)){
//                    Toast.makeText(getContext(), "You are not inside the GUC campus", Toast.LENGTH_LONG).show();
//                }
//            }
//        }
//    };
//
//    private void checkLocationPermission() {
//        if (ContextCompat.checkSelfPermission(getActivity(),
//                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//
//           if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)){
//               new AlertDialog.Builder(getContext())
//                       .setTitle("Permission Missing")
//                       .setMessage("Please give the missing permissions for the app to function")
//                       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                           @Override
//                           public void onClick(DialogInterface dialogInterface, int i) {
//                               requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//                           }
//                       })
//                       .create().show();
//           }else {
//              requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//           }
//        }
//        else{
//            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
//            mMap.setMyLocationEnabled(true);
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        switch (requestCode){
//            case 1: if (grantResults.length > 0
//                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//
//                if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ){
//                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
//                    mMap.setMyLocationEnabled(true);
//                }
//
//            } else {
//                Toast.makeText(getContext(), "Please provide location permission", Toast.LENGTH_LONG).show();
//            }
//                break;
//        }
//    }
//
//    @Override
//    public void onRoutingFailure(RouteException e) {
//        progressDialog.dismiss();
//        if(e != null) {
//            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
//        }else {
//            Toast.makeText(getContext(), "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    @Override
//    public void onRoutingStart() {
//
//    }
//
//    @Override
//    public void onRoutingSuccess(List<Route> route, int shortestRouteIndex) {
//        progressDialog.dismiss();
//
//        if(polylines.size()>0) {
//            for (Polyline poly : polylines) {
//                poly.remove();
//            }
//        }
//
//        polylines = new ArrayList<>();
//        //add route(s) to the map.
//        for (int i = 0; i <route.size(); i++) {
//
//            PolylineOptions polyOptions = new PolylineOptions();
//            polyOptions.color(getResources().getColor(R.color.colorAccent));
//            polyOptions.width(10 + i * 3);
//            polyOptions.addAll(route.get(i).getPoints());
//            Polyline polyline = mMap.addPolyline(polyOptions);
//            polylines.add(polyline);
//
//            Toast.makeText(getContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    @Override
//    public void onRoutingCancelled() {
//
//    }
//}
