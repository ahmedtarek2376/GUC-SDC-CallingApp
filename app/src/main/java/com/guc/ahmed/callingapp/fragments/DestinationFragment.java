package com.guc.ahmed.callingapp.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.guc.ahmed.callingapp.adapter.EditItemTouchHelperCallback;
import com.guc.ahmed.callingapp.adapter.ItemTouchHelperAdapter;
import com.guc.ahmed.callingapp.adapter.OnStartDragListener;
import com.guc.ahmed.callingapp.classes.Trip;
import com.guc.ahmed.callingapp.gucpoints.GucPlace;
import com.guc.ahmed.callingapp.gucpoints.GucPoints;
import com.guc.ahmed.callingapp.map.CustomMarker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DestinationFragment extends Fragment
        implements OnMapReadyCallback, OnStartDragListener {

    private GoogleMap mMap;
    private View view;

    private Location lastLocation;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private HashMap<String,Marker> markers;
    private NiftyDialogBuilder dialogBuilder;
    private ProgressDialog progressDialog;
    private List<Polyline> polylines;
    private Marker selectedMarker;
    private Marker pickupMarker;
    private DestinationFragment.OnDestinationLocationListener onDestinationLocationListener;

    private TextView destinationTxt;
    private GucPlace destinationLocation;
    private ActionProcessButton button;
    private Trip requestTrip;

    private AppCompatActivity activity;
    private ActionBar actionBar;

    private RecyclerView mRecyclerView;
    private DestinationFragment.MyAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ArrayList<GucPlace> chosenDestinations;
    private ItemTouchHelper mItemTouchHelper;

    public DestinationFragment() {
        // Required empty public constructor
    }

    public interface OnDestinationLocationListener {
        void onDestinationConfirmed(ArrayList<GucPlace> gucPlace);
        void onEditPickupClicked();
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_destination, container, false);
        Log.v("DESTINATION", "onCreateView");


        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

        dialogBuilder=NiftyDialogBuilder.getInstance(getContext());

        destinationTxt = view.findViewById(R.id.destination_txt);

        button = (ActionProcessButton) view.findViewById(R.id.destination_btn);
        button.setMode(ActionProcessButton.Mode.ENDLESS);
        button.setOnClickListener(confirmPickupOnClickListener);
        Typeface roboBlack = Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Bold.ttf");
        button.setTypeface(roboBlack);

        activity = (AppCompatActivity) getActivity();
        actionBar = activity.getSupportActionBar();

        try {
            onDestinationLocationListener = (DestinationFragment.OnDestinationLocationListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement OnHeadlineSelectedListener");
        }

        ////////////////////////////////Recycler View//////////////////////////////////////
        mRecyclerView = (RecyclerView) view.findViewById(R.id.destination_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext(), LinearLayout.HORIZONTAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);

        if(requestTrip != null && requestTrip.getDestinations() != null){
            ArrayList<GucPlace> arrayList = new ArrayList<>();
            for (LatLng latLng : requestTrip.getDestinations()){
               arrayList.add( GucPoints.getGucPlaceByLatLng(latLng) );
            }
            destinationTxt.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
            chosenDestinations = arrayList;
        }else{
            chosenDestinations = new ArrayList<>();
        }
        // specify an adapter
        mAdapter = new MyAdapter(this);

        ItemTouchHelper.Callback callback =
                new EditItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        mRecyclerView.setAdapter(mAdapter);
        ///////////////////////////////////////////////////////////////////////////////////

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

        actionBar.setTitle("Choose Your Destination");
        Log.v("DESTINATION", "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        Log.v("DESTINATION", "onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        Log.v("DESTINATION", "onStop");
    }

    public void setRequestTrip(Trip trip){
        this.requestTrip = trip;
        if(mMap != null)
            onMapReady(mMap);
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
        Log.v("DESTINATION", "onMapCreated");
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
            Log.e("DestinationFragment", "Style parsing failed.");
        }

        // Constrain the camera target to the Adelaide bounds.
        mMap.setLatLngBoundsForCameraTarget(GucPoints.GUC);

        addMarkersToMap();

        mMap.setOnInfoWindowClickListener(onInfoWindowClickListener);

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
        LatLng latLng = new LatLng(29.9859, 31.4401);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));


        mMap.setOnMarkerClickListener(onMarkerClickListener);

//        progressDialog = ProgressDialog.show(getContext(), "Please wait.",
//                "Fetching route information.", true);
//        Routing routing = new Routing.Builder()
//                .travelMode(AbstractRouting.TravelMode.DRIVING)
//                .withListener(this)
//                .alternativeRoutes(false)
//                .waypoints(GucPoints.GATE_1.getLatLng(), GucPoints.C6_U_AREA.getLatLng())
//                .build();
//        routing.execute();

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

    private GoogleMap.OnInfoWindowClickListener onInfoWindowClickListener = new GoogleMap.OnInfoWindowClickListener() {
        @Override
        public void onInfoWindowClick(Marker marker) {

        }
    };

    private GoogleMap.OnMarkerClickListener onMarkerClickListener = new GoogleMap.OnMarkerClickListener() {

        @Override
        public boolean onMarkerClick(Marker marker) {

            if(! marker.getTitle().equalsIgnoreCase(pickupMarker.getTitle() ) ) {
                GucPlace gucPlace = GucPoints.getGucPlaceByName(marker.getTitle());

                if(addToChosenDestinations(gucPlace)){
                    addMarkersToMap();
                }
            } else {
                marker.showInfoWindow();
            }

            return true;
        }
    };

    private boolean addToChosenDestinations(GucPlace gucPlace) {

        if(chosenDestinations.size() != 3 && !(chosenDestinations.contains(gucPlace))){
            chosenDestinations.add(gucPlace);
            mAdapter.notifyItemInserted(chosenDestinations.size() - 1);
            mRecyclerView.setVisibility(View.VISIBLE);
            return true;
        }else {
            return false;
        }
    }

    private View.OnClickListener confirmPickupOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(chosenDestinations.size() == 0){
                Toast.makeText(getActivity(), "Please select a destination", Toast.LENGTH_SHORT).show();
            }else{
                button.setProgress(1);
                onDestinationLocationListener.onDestinationConfirmed(chosenDestinations);
                button.setProgress(0);
            }
        }
    };

    public void addMarkersToMap(){

        mMap.clear();

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

        pickupMarker = markers.get(GucPoints.getGucPlaceByLatLng(requestTrip.getPickupLocation()).getName());
        customMarker.setImage(R.drawable.custom_marker_start);
        customMarker.setText(pickupMarker.getTitle());
        pickupMarker.setIcon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView()));
        pickupMarker.setTitle("Pickup Location");
        Marker destination;

        for(GucPlace place : chosenDestinations){
            destination = markers.get(place.getName());
            customMarker.setImage(R.drawable.custom_marker_end);
            customMarker.setText(place.getName());
            destination.setIcon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView()));
        }
    }

    private LocationCallback locationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            for (Location location : locationResult.getLocations()){

                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                //LatLng latLng = new LatLng(29.986926, 31.440630);

                if(! GucPoints.GUC.contains(latLng)){
                    Toast.makeText(getContext(), "You are not inside the GUC campus", Toast.LENGTH_LONG).show();
                }
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

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> implements ItemTouchHelperAdapter {
        private RecyclerView mRecyclerView;
        private final OnStartDragListener mDragStartListener;

        public MyAdapter(DestinationFragment destinationFragment){
            mDragStartListener = destinationFragment;
        }
        // Create new views (invoked by the layout manager)
        @Override
        public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
            // create a new view
            CardView v = (CardView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.destination_item, parent, false);

            if(chosenDestinations.size()>0){
                parent.setVisibility(View.VISIBLE);
            }

            Log.v("ADAPTER" , "onCreateViewHolder");

           return new MyAdapter.ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.destinationName.setText(chosenDestinations.get(position).getName());

            holder.handleView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        mDragStartListener.onStartDrag(holder);
                    }
                    return false;
                }
            });

            if(chosenDestinations.size()>0){
                destinationTxt.setVisibility(View.GONE);
            }

            Log.v("ADAPTER" , "onBindViewHolder");
        }

        @Override
        public int getItemCount() {
            return chosenDestinations.size();
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            mRecyclerView = recyclerView;
            Log.v("ADAPTER" , "onAttachedToRecyclerView");
        }

        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
            GucPlace place = chosenDestinations.get(fromPosition);
            chosenDestinations.remove(fromPosition);
            chosenDestinations.add(toPosition, place);
            notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            private TextView destinationName;
            private ImageView handleView;
            private ImageView deleteView;

            public ViewHolder(final View itemView) {
                super(itemView);

                deleteView = itemView.findViewById(R.id.delete);

                deleteView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        chosenDestinations.remove(getAdapterPosition());
                        notifyItemRemoved(getAdapterPosition());
                        notifyItemRangeChanged(getAdapterPosition(),chosenDestinations.size());
                        if(getItemCount()==0){
                            mRecyclerView.setVisibility(View.GONE);
                            destinationTxt.setVisibility(View.VISIBLE);
                        }
                        addMarkersToMap();
                    }
                });

                destinationName = itemView.findViewById(R.id.destination_item_name);

                handleView = (ImageView) itemView.findViewById(R.id.handle);

                Log.v("ADAPTER" , "ViewHolder");
            }

            @Override
            public void onClick(View view) {

            }

        }
    }

}
