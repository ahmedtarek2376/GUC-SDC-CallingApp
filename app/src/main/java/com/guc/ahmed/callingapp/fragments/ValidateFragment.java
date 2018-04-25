package com.guc.ahmed.callingapp.fragments;


import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.guc.ahmed.callingapp.apiclasses.ApiCall;
import com.guc.ahmed.callingapp.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;


/**
 * A simple {@link Fragment} subclass.
 */
public class ValidateFragment extends Fragment {
    EditText gucMail;
    Button verify_btn;
    View inflatedView;
    Bundle bundle;
    ProgressDialog progressDialog;

    public ValidateFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        this.inflatedView = inflater.inflate(R.layout.fragment_validate, container, false);

        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Verify Account");

        progressDialog = new ProgressDialog(inflatedView.getContext()) ;
        progressDialog.setCancelable ( false ) ;
        progressDialog.setMessage ( "Loading..." ) ;
        progressDialog.setTitle ( "Please wait" ) ;
        progressDialog.setIndeterminate ( true ) ;

        bundle = getArguments();

        gucMail = (EditText) inflatedView.findViewById(R.id.guc_mail_edit_text);

        verify_btn = (Button) inflatedView.findViewById(R.id.verify_button);
        verify_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Object dataTransfer[] = new Object[3];
                String url = "http://10.0.2.2:8080/profile/verify";
                dataTransfer[0] = url;
                dataTransfer[1] = gucMail.getText().toString();
                dataTransfer[2]= bundle.getString("gmail");
                VerificationAsync verificationAsync = new VerificationAsync(getActivity());
                verificationAsync.execute(dataTransfer);
                Toast.makeText(getActivity(), "Sending verification mail...", Toast.LENGTH_SHORT).show();
                progressDialog.show();
            }
        });

        return inflatedView;
    }

    class VerificationAsync extends AsyncTask<Object,String,String> {
        String url;
        WeakReference<Activity> mWeakActivity;
        Activity activity;
        String data;

        public VerificationAsync(Activity activity){
            mWeakActivity = new WeakReference<Activity>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            activity = mWeakActivity.get();
        }

        @Override
        protected String doInBackground(Object... objects) {
            url = (String)objects[0];

            JSONObject params = new JSONObject();
            try {
                params.put("gucMail", (String)objects[1]);
                params.put("gmail", (String)objects[2]);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            ApiCall apiCall = new ApiCall();
            try {
                data = apiCall.readUrl(url,params);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return data;
        }

        protected void onPostExecute(String data) {
            progressDialog.dismiss();
            JSONObject jsonObject;
            String message = "";
            try {
                //make the string a jsonObject to get the results from it
                jsonObject = new JSONObject(data);
                message = jsonObject.getString("message");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Toast.makeText(inflatedView.getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }


}
