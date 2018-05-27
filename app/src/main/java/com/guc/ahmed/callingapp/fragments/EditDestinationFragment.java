package com.guc.ahmed.callingapp.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
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
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guc.ahmed.callingapp.MainActivity;
import com.guc.ahmed.callingapp.R;
import com.guc.ahmed.callingapp.adapter.ItemTouchHelperAdapter;
import com.guc.ahmed.callingapp.adapter.ItemTouchHelperCallback;
import com.guc.ahmed.callingapp.adapter.OnStartDragListener;
import com.guc.ahmed.callingapp.MyVolleySingleton;
import com.guc.ahmed.callingapp.gucpoints.GucPlace;
import com.guc.ahmed.callingapp.gucpoints.GucPoints;
import com.guc.ahmed.callingapp.map.CustomMarker;
import com.guc.ahmed.callingapp.objects.Trip;
import com.guc.ahmed.callingapp.objects.TripDestination;
import com.guc.ahmed.callingapp.objects.TripEvent;
import com.tapadoo.alerter.Alert;
import com.tapadoo.alerter.Alerter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EditDestinationFragment extends Fragment
        implements OnMapReadyCallback, OnStartDragListener {

    private GoogleMap mMap;
    private View view;

    private LatLng lastLocation;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private HashMap<String,Marker> markers;
    private Marker pickupMarker;

    private TextView destinationTxt;
    private ActionProcessButton button;
    private Trip requestTrip;

    private AppCompatActivity activity;
    private ActionBar actionBar;

    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ArrayList<GucPlace> chosenDestinations;
    private ItemTouchHelper mItemTouchHelper;
    private LinearLayout recyclerLayout;
    private Alert alert;
    private Gson gson;

    private static final String TAG = "EDITDESTINATIONFRAGMENT";

    public EditDestinationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_edit_destination, container, false);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

        destinationTxt = view.findViewById(R.id.destination_txt);

        button = (ActionProcessButton) view.findViewById(R.id.destination_btn);
        button.setMode(ActionProcessButton.Mode.ENDLESS);
        button.setOnClickListener(confirmPickupOnClickListener);
        Typeface roboBlack = Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Regular.ttf");
        button.setTypeface(roboBlack);

        activity = (AppCompatActivity) getActivity();
        actionBar = activity.getSupportActionBar();


        ////////////////////////////////Recycler View//////////////////////////////////////
        recyclerLayout = view.findViewById(R.id.recycler_layout);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.destination_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext(), LinearLayout.HORIZONTAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);

        if(requestTrip != null && requestTrip.getDestinations() != null){
            ArrayList<GucPlace> arrayList = new ArrayList<>();
            for (TripDestination tripDestination : requestTrip.getDestinations()){
                arrayList.add( GucPoints.getGucPlaceByLatLng(tripDestination.getLocation()));
            }
            destinationTxt.setVisibility(View.GONE);
            recyclerLayout.setVisibility(View.VISIBLE);
            chosenDestinations = arrayList;
        }else{
            chosenDestinations = new ArrayList<>();
        }
        // specify an adapter
        mAdapter = new MyAdapter(this);

        ItemTouchHelper.Callback callback =
                new ItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        mRecyclerView.setAdapter(mAdapter);
        ///////////////////////////////////////////////////////////////////////////////////

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        markers = new HashMap<>();

        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();

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
        actionBar.setTitle("Edit Destinations");

        CoordinatorLayout coordinatorLayout = getActivity().findViewById(R.id.edit_destination_fragment);
        Snackbar snackbar = Snackbar.make(coordinatorLayout, "You can remove, add, and reorder unreached destinations", Snackbar.LENGTH_INDEFINITE);
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
        mMap = googleMap;

        mMap.clear();

        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setMinZoomPreference(16.0f);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        mMap.setPadding(0,150,0,0);

        boolean success = googleMap.setMapStyle(new MapStyleOptions(getResources()
                .getString(R.string.style_json)));

        if (!success) {
            Log.e("EditDestinationFragment", "Style parsing failed.");
        }

        // Constrain the camera target to the Adelaide bounds.
        mMap.setLatLngBoundsForCameraTarget(GucPoints.GUC);

        addMarkersToMap();

        mMap.setOnInfoWindowClickListener(onInfoWindowClickListener);

        locationRequest = new LocationRequest();
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //to be removed
        LatLng latLng = new LatLng(29.986654, 31.440191);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17));
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
            recyclerLayout.setVisibility(View.VISIBLE);
            return true;
        }else {
            Snackbar.make(getView(),"You can choose maximum 3 destinations",Snackbar.LENGTH_SHORT).show();
            return false;
        }
    }

    private View.OnClickListener confirmPickupOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(chosenDestinations.size() == 0){
                CoordinatorLayout coordinatorLayout = getActivity().findViewById(R.id.edit_destination_fragment);
                Snackbar snackbar = Snackbar.make(coordinatorLayout, "Please select a destination", Snackbar.LENGTH_SHORT);
                View view = snackbar.getView();
                view.setBackgroundColor(getResources().getColor(R.color.red_error));
                TextView textView = view.findViewById(android.support.design.R.id.snackbar_text);
                textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                CoordinatorLayout.LayoutParams params=(CoordinatorLayout.LayoutParams)view.getLayoutParams();
                params.gravity = Gravity.TOP;
                view.setLayoutParams(params);
                snackbar.show();
            }else{
                button.setProgress(1);
                changeDestinations();
            }
        }
    };

    private void changeDestinations() {
        List<TripDestination> newDestinations = new ArrayList<>();
        for( GucPlace gucPlace : chosenDestinations){
            newDestinations.add(new TripDestination(gucPlace.getLatLng()));
        }

        for (int i=0 ; i < requestTrip.getDestinations().size() ; i++){
            if(requestTrip.getDestinations().get(i).isArrived()){
                newDestinations.get(i).setArrived(true);
            }
        }
        requestTrip.setDestinations(newDestinations);

        String str = gson.toJson(requestTrip);
        JSONObject trip = new JSONObject();
        try {
            trip = new JSONObject(str);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = getResources().getString(R.string.url_trip_change)+ MainActivity.mAuth.getCurrentUser().getEmail();
        JsonObjectRequest tripRequest = new JsonObjectRequest
                (Request.Method.POST, url, trip, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        Trip tripp = gson.fromJson(response.toString(), Trip.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("EVENT", TripEvent.CHANGE_DESTINATION.name());
                        bundle.putString("CAR_ID",tripp.getCarID());
                        bundle.putString("TRIP_ID",tripp.getId());

                        button.setProgress(0);

                        if(getContext()==null){
                            return;
                        }
                        ((MainActivity)getActivity()).showOnTripFragment(bundle);
                        Snackbar.make(getActivity().getCurrentFocus(),"You have successfuly modified your trip's destination(s)", Snackbar.LENGTH_SHORT);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        button.setProgress(0);
                        if(getContext()==null){
                            return;
                        }
                        Toast.makeText(getContext(),"Error, please try again.", Toast.LENGTH_LONG).show();
                    }
                });

        MyVolleySingleton.getInstance(getContext()).addToRequestQueue(tripRequest);
    }

    public void addMarkersToMap(){

        mMap.clear();

        CustomMarker customMarker = new CustomMarker(getContext());
        customMarker.setImage(R.drawable.ic_marker_black);

        for (GucPlace place : MainActivity.gucPlaces){
            customMarker.setText(place.getName());
            markers.put( place.getName(),
                    mMap.addMarker(new MarkerOptions().position(place.getLatLng())
                            .title(place.getName()).icon(BitmapDescriptorFactory.fromBitmap(customMarker.createBitmapFromView())))
            );
        }

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

    public void setTrip(Trip trip) {
        requestTrip = new Trip();
        requestTrip.setPickupLocation(trip.getPickupLocation());
        requestTrip.setDestinations(trip.getDestinations());
    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> implements ItemTouchHelperAdapter {
        private RecyclerView mRecyclerView;
        private final OnStartDragListener mDragStartListener;

        public MyAdapter(EditDestinationFragment editDestinationFragment){
            mDragStartListener = editDestinationFragment;
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
            return new MyAdapter.ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.destinationName.setText(chosenDestinations.get(position).getName());

            if(position<requestTrip.getDestinations().size() && requestTrip.getDestinations().get(position).isArrived()){
                holder.handleView.setVisibility(View.VISIBLE);
                holder.deleteView.setVisibility(View.GONE);
                holder.destinationName.setTextColor(getResources().getColor(R.color.text_grey_light));
            } else {
                holder.handleView.setVisibility(View.INVISIBLE);
                holder.deleteView.setVisibility(View.VISIBLE);
                holder.destinationName.setTextColor(getResources().getColor(R.color.text_grey_dark));

                holder.handleView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                            mDragStartListener.onStartDrag(holder);
                        }
                        return false;
                    }
                });

                holder.destinationName.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                            mDragStartListener.onStartDrag(holder);
                        }
                        return false;
                    }
                });

            }

            if(chosenDestinations.size()>0){
                destinationTxt.setVisibility(View.GONE);
            }

        }

        @Override
        public int getItemCount() {
            return chosenDestinations.size();
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            mRecyclerView = recyclerView;
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
                            recyclerLayout.setVisibility(View.GONE);
                            destinationTxt.setVisibility(View.VISIBLE);
                        }
                        addMarkersToMap();
                    }
                });

                destinationName = itemView.findViewById(R.id.destination_item_name);

                handleView = (ImageView) itemView.findViewById(R.id.handle);

            }

            @Override
            public void onClick(View view) {

            }

        }
    }

}
