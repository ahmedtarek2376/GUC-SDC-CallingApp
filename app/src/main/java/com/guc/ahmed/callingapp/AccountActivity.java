package com.guc.ahmed.callingapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;

public class AccountActivity extends AppCompatActivity {

    private Button mLogoutBtn;
    private  FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private TextView username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        username = (TextView) findViewById(R.id.username_text);
        username.setText("Hello, " + getIntent().getStringExtra("username"));
        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient= GoogleSignIn.getClient(this, gso);

        mAuthStateListener = new FirebaseAuth.AuthStateListener(){
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(mAuth.getCurrentUser() == null){
                    //This code clears which account is connected to the app. To sign in again, the user must choose their account again.
                    mGoogleSignInClient.signOut();

                    updateUI();
                }
            }
        };

        mLogoutBtn = (Button) findViewById(R.id.logout_btn);
        mLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthStateListener);
    }

    private void updateUI() {
        Intent intent = new Intent(AccountActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Toast.makeText(AccountActivity.this, "You have successfully signed out.", Toast.LENGTH_SHORT).show();
    }
}
