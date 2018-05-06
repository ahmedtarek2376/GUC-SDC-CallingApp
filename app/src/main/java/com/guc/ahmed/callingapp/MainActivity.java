package com.guc.ahmed.callingapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
import com.guc.ahmed.callingapp.classes.Trip;
import com.guc.ahmed.callingapp.fragments.ConfirmFragment;
import com.guc.ahmed.callingapp.fragments.DestinationFragment;
import com.guc.ahmed.callingapp.fragments.OnTripFragment;
import com.guc.ahmed.callingapp.fragments.PickupFragment;
import com.guc.ahmed.callingapp.fragments.TripHistoryFragment;
import com.guc.ahmed.callingapp.fragments.ValidateFragment;
import com.guc.ahmed.callingapp.gucpoints.GucPlace;
import com.tapadoo.alerter.Alert;
import com.tapadoo.alerter.Alerter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, PickupFragment.OnPickupLocationListener, DestinationFragment.OnDestinationLocationListener {

    private NavigationView navigationView;
    private Toolbar toolbar;
    private  FirebaseAuth.AuthStateListener mAuthStateListener;
    public static FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private Trip requestTrip;
    private static boolean accountVerified;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private ConfirmFragment confirmFragment;
    private PickupFragment pickupFragment;
    private Alert alert;

    private TextView navName;
    private TextView navEmail;
    private Gson gson;
    public static ArrayList<GucPlace> gucPlaces;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Roboto-Regular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );

        sharedPref = getPreferences(Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        accountVerified = sharedPref.getBoolean("AccountVerified", false);

        Log.v("Account Verified = " ,accountVerified + "");

        requestTrip = new Trip();
        gucPlaces = new ArrayList<>();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        checkPlayServices();

        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.v("MainActivity", "FirebaseInstanceId Token: " + refreshedToken);

        pickupFragment = new PickupFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, pickupFragment, "PICKUP_FRAGMENT").commit();

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
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);
        navName =  headerView.findViewById(R.id.nav_name);
        navName.setText(mAuth.getCurrentUser().getDisplayName());
        navEmail = headerView.findViewById(R.id.nav_email);
        navEmail.setText(mAuth.getCurrentUser().getEmail());

        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();

    }

    @Override
    protected void onStart() {
        super.onStart();
        accountVerified = true;
        mAuth.addAuthStateListener(mAuthStateListener);
        LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver),
                new IntentFilter("FcmData")
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
        if(!isNetworkStatusAvialable(getApplicationContext())) {
            Alerter.clearCurrent(this);
            alert = Alerter.create(this)
                    .setTitle("No Internet Connection !!")
                    .setText("Please enable internet connection to proceed. Click to dismiss when internet connection is available.")
                    .enableIconPulse(true)
                    .disableOutsideTouch()
                    .setBackgroundColorRes(R.color.red_error)
                    .enableInfiniteDuration(true)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(isNetworkStatusAvialable(getApplicationContext())){
                                alert.hide();
                                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                                getSupportFragmentManager().beginTransaction().detach(currentFragment).attach(currentFragment).commit();
                            }
                        }
                    })
                    .show();
        }

    }

    public static boolean isNetworkStatusAvialable (Context context) {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        return isConnected;
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
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if(currentFragment instanceof PickupFragment){
                Log.v("TESTTT", "ANA pickup !!!");
                finish();
            } else if(currentFragment instanceof DestinationFragment){
                Log.v("TESTTT", "ANA destination !!!");
                pickupFragment = new PickupFragment();
                pickupFragment.setRequestTrip(requestTrip);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, pickupFragment, "PICKUP_FRAGMENT").commit();
                //getSupportActionBar().setTitle("Chose Your Destination");
            } else if(currentFragment instanceof ConfirmFragment){
                Log.v("TESTTT", "ANA confirm !!!");
                destinationFragment = new DestinationFragment();
                destinationFragment.setRequestTrip(requestTrip);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, destinationFragment, "DESTINATION_FRAGMENT").commit();
                //getSupportActionBar().setTitle("Chose Your Destination");
            } else {
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_verify) {
            Bundle bundle = new Bundle();
            bundle.putString("gmail", mAuth.getCurrentUser().getEmail());
            ValidateFragment fragment = new ValidateFragment();
            fragment.setArguments(bundle);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment, "VALIDATE_FRAGMENT")
                    .addToBackStack(null).commit();

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
        } else if (id == R.id.nav_share) {

        }else if (id == R.id.nav_home) {
            pickupFragment = new PickupFragment();
            pickupFragment.setRequestTrip(requestTrip);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, pickupFragment, "PICKUP_FRAGMENT").commit();
        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return false;
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

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        editor.putBoolean("AccountVerified", accountVerified);
        editor.commit();
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        editor.putBoolean("AccountVerified", accountVerified);
        editor.commit();
        super.onDestroy();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle map = intent.getExtras();
            String status = map.getString("STATUS");
            if(status != null && status.length()>0 ){
                showOnTripFragment(map.getString("TRIP_ID"));
            }
        }
    };

    public void showOnTripFragment(String tripID) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if(currentFragment instanceof OnTripFragment){
            ((OnTripFragment)currentFragment).updateData(tripID);
        }else {
            OnTripFragment onTripFragment = new OnTripFragment();
            Bundle bundle = new Bundle();
            bundle.putString("TRIP_ID", tripID);
            onTripFragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, onTripFragment, "PICKUP_FRAGMENT").commitAllowingStateLoss();
        }
    }

}
