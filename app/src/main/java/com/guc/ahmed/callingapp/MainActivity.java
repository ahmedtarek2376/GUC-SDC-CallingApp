package com.guc.ahmed.callingapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guc.ahmed.callingapp.fragments.ConfirmFragment;
import com.guc.ahmed.callingapp.fragments.DestinationFragment;
import com.guc.ahmed.callingapp.fragments.EditDestinationFragment;
import com.guc.ahmed.callingapp.fragments.OnTripFragment;
import com.guc.ahmed.callingapp.fragments.PickupFragment;
import com.guc.ahmed.callingapp.fragments.TripHistoryFragment;
import com.guc.ahmed.callingapp.fragments.ValidateFragment;
import com.guc.ahmed.callingapp.gucpoints.GucPlace;
import com.guc.ahmed.callingapp.objects.RequestTrip;
import com.guc.ahmed.callingapp.objects.Trip;
import com.tapadoo.alerter.Alert;
import com.tapadoo.alerter.Alerter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, PickupFragment.OnPickupLocationListener, DestinationFragment.OnDestinationLocationListener {

    private  FirebaseAuth.AuthStateListener mAuthStateListener;
    public static FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private RequestTrip requestTrip;
    private ConfirmFragment confirmFragment;
    private PickupFragment pickupFragment;
    private Alert alertInternet;

    private Gson gson;
    public static ArrayList<GucPlace> gucPlaces;

    private static final String TAG = "MainActivity";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Roboto-Regular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );

        requestTrip = new RequestTrip();
        gucPlaces = new ArrayList<>();
        getGucPlaces();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        checkPlayServices();

        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        pickupFragment = new PickupFragment();

        String action = getIntent().getAction();
        if(action!=null && action.equalsIgnoreCase("ONTRIP")){
            Bundle bundle = getIntent().getExtras();
            String status = bundle.getString("EVENT");
            if(status != null && status.length()>0 ){
                showOnTripFragment(bundle);
            }
        }else{
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, pickupFragment, "PICKUP_FRAGMENT").commit();
        }
        /////////////////////////w/////////////////////
        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mAuthStateListener = new FirebaseAuth.AuthStateListener(){
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(mAuth.getCurrentUser() == null){

                    //This code clears which account is connected to the app. To sign in again, the user must choose their account again.
                    mGoogleSignInClient.signOut()
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    updateUI();
                                }
                            });

                }
            }
        };

        /////////////////////////////////////////////

        /////////////////////////////////////////////
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);
        TextView navName = headerView.findViewById(R.id.nav_name);
        navName.setText(mAuth.getCurrentUser().getDisplayName());
        TextView navEmail = headerView.findViewById(R.id.nav_email);
        navEmail.setText(mAuth.getCurrentUser().getEmail());

        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();

    }

    private void getGucPlaces() {
        final ArrayList<GucPlace> arrayList = new ArrayList<>();
        String url = getResources().getString(R.string.url_get_pins);
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {

                        for(int i=0;i<response.length();i++){
                            try {
                                JSONObject object = response.getJSONObject(i);
                                GucPlace place = gson.fromJson(object.toString(), GucPlace.class);
                                arrayList.add(place);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        MainActivity.gucPlaces = arrayList;
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }){
            @Override
            public Priority getPriority() {
                return Priority.IMMEDIATE;
            }
        };

        jsonArrayRequest.setTag(TAG);
        MyVolleySingleton.getInstance(this).addToRequestQueue(jsonArrayRequest);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthStateListener);
        LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver),
                new IntentFilter("FcmData")
        );
        this.registerReceiver((internetReceiver),
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
        if(!isNetworkStatusAvailable(getApplicationContext())) {
            showNoInternetConncetion();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (MyVolleySingleton.getInstance(this).getRequestQueue() != null) {
            MyVolleySingleton.getInstance(this).getRequestQueue().cancelAll(TAG);
        }
    }

    public static boolean isNetworkStatusAvailable (Context context) {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null &&
                activeNetwork.isConnected();
    }

    //update UI on sign out
    private void updateUI() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Toast.makeText(MainActivity.this, "You have successfully signed out", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if(currentFragment instanceof PickupFragment){
                finish();
            } else if(currentFragment instanceof DestinationFragment){
                pickupFragment = new PickupFragment();
                pickupFragment.setRequestTrip(requestTrip);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, pickupFragment, "PICKUP_FRAGMENT").commit();
            } else if(currentFragment instanceof ConfirmFragment){
                destinationFragment = new DestinationFragment();
                destinationFragment.setRequestTrip(requestTrip);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, destinationFragment, "DESTINATION_FRAGMENT").commit();
            } else if(currentFragment instanceof EditDestinationFragment) {
                getSupportActionBar().setTitle("Your Ride");
                super.onBackPressed();
            }else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_verify) {
            showVerifyFragment();
        } else if (id == R.id.nav_history) {
            Bundle bundle = new Bundle();
            bundle.putString("gmail", mAuth.getCurrentUser().getEmail());
            TripHistoryFragment fragment = new TripHistoryFragment();
            fragment.setArguments(bundle);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment, "TRIP_HISTORY_FRAGMENT")
                    .addToBackStack(null).commit();
        } else if (id == R.id.nav_signout) {
            mAuth.signOut();
        } else if (id == R.id.nav_current_trip) {
            checkCurrentTrip();
        }else if (id == R.id.nav_home) {
            pickupFragment = new PickupFragment();
            pickupFragment.setRequestTrip(requestTrip);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, pickupFragment, "PICKUP_FRAGMENT").commit();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return false;
    }

    public void showVerifyFragment() {
        ValidateFragment fragment = new ValidateFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, "VALIDATE_FRAGMENT")
                .addToBackStack(null).commit();
    }

    private void checkCurrentTrip() {
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
                            if(getApplicationContext()==null){
                                return;
                            }
                            Snackbar snackbar = Snackbar.make(getCurrentFocus(),"You have no ongoing trip",Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }else {
                            String tripID = "";
                            try {
                                tripID = response.getString("TRIP_ID");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Bundle bundle = new Bundle();
                            bundle.putString("TRIP_ID", tripID);
                            showOnTripFragment(bundle);
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if(getApplicationContext()==null){
                            return;
                        }
                        Toast.makeText(getApplicationContext(),"Error, please try again.", Toast.LENGTH_LONG).show();
                    }
                });
        freeRequest.setTag(TAG);
        MyVolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(freeRequest);

    }


    private void checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, 9000)
                        .show();
                apiAvailability.makeGooglePlayServicesAvailable(this);
            } else {
                Log.i("MainActivity", "This device is not supported.");
                finish();
            }
        }
    }

    private DestinationFragment destinationFragment;

    @Override
    public void onPickupConfirmed(GucPlace gucPlace) {
        requestTrip.setPickupLocation(gucPlace.getLatLng());
        requestTrip.setDestinations(null);
        destinationFragment = new DestinationFragment();
        destinationFragment.setRequestTrip(requestTrip);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, destinationFragment, "DESTINATION_FRAGMENT").commit();
    }

    @Override
    public void onDestinationConfirmed(ArrayList<GucPlace> gucPlace) {
        ArrayList<LatLng> destinations = new ArrayList<>();
        for (GucPlace place : gucPlace){
            destinations.add(place.getLatLng());
        }
        requestTrip.setDestinations(destinations);

        confirmFragment = new ConfirmFragment();
        confirmFragment.setRequestTrip(requestTrip);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, confirmFragment, "CONFIRM_FRAGMENT").commit();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            String status = bundle.getString("EVENT");
            if(status != null && status.length()>0 ){
                showOnTripFragment(bundle);
            }
        }
    };

    private BroadcastReceiver internetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if(! isNetworkStatusAvailable(context)){
                showNoInternetConncetion();
            }else {
                removeNoInternetConnection();
            }
        }
    };

    private void removeNoInternetConnection() {
        if(alertInternet!=null){
            alertInternet.hide();
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            getSupportFragmentManager().beginTransaction().detach(currentFragment).attach(currentFragment).commitAllowingStateLoss();
        }
    }

    private void showNoInternetConncetion() {
        Alerter.clearCurrent(this);
        alertInternet = Alerter.create(this)
                .setTitle("No Internet Connection !!")
                .setText("The application needs Internet connection to function. This alert will automatically disappear when you are connected to the Internet.")
                .enableIconPulse(true)
                .disableOutsideTouch()
                .setIcon(R.drawable.ic_signal_wifi_off_black_24dp)
                .setBackgroundColorRes(R.color.red_error)
                .enableInfiniteDuration(true)
                .show();
    }

    public void showOnTripFragment(Bundle bundle) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if(currentFragment instanceof OnTripFragment){
            ((OnTripFragment)currentFragment).updateData(bundle);
        }else {
            OnTripFragment onTripFragment = new OnTripFragment();
            onTripFragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, onTripFragment, "ONTRIP_FRAGMENT").commitAllowingStateLoss();
        }
    }

    public void showEditDestinationFragment(Trip trip) {
        EditDestinationFragment editDestinationFragment = new EditDestinationFragment();
        editDestinationFragment.setTrip(trip);
        getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, editDestinationFragment, "EDIT_DESTINATION_FRAGMENT").addToBackStack(null).commit();
    }



}
