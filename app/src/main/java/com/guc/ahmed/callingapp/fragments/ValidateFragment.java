package com.guc.ahmed.callingapp.fragments;


import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.dd.processbutton.iml.ActionProcessButton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guc.ahmed.callingapp.MainActivity;
import com.guc.ahmed.callingapp.R;
import com.guc.ahmed.callingapp.MyVolleySingleton;
import com.guc.ahmed.callingapp.objects.Profile;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * A simple {@link Fragment} subclass.
 */
public class ValidateFragment extends Fragment {
    private EditText gucMail;
    private ActionProcessButton verify_btn;
    private View view;
    private Gson gson;
    private static final String TAG = "ValidateFragment";

    public ValidateFragment() {

    }

    @Override
    public void onStop() {
        super.onStop();
        if (MyVolleySingleton.getInstance(getActivity()).getRequestQueue() != null) {
            MyVolleySingleton.getInstance(getActivity()).getRequestQueue().cancelAll(TAG);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_validate, container, false);

        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Verify Account");

        gucMail = view.findViewById(R.id.guc_mail_edit_text);

        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();


        verify_btn = view.findViewById(R.id.verify_button);
        verify_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                verify_btn.setProgress(1);

                Profile profile = new Profile();
                profile.setGmail(MainActivity.mAuth.getCurrentUser().getEmail());
                profile.setGucMail(gucMail.getText().toString());

                String str = gson.toJson(profile);
                JSONObject profilePost = new JSONObject();
                try {
                    profilePost = new JSONObject(str);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String url = getResources().getString(R.string.url_verify_profile);
                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                        (Request.Method.POST, url, profilePost, new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(JSONObject response) {
                                String message = "";
                                try {
                                    message = response.getString("message");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                Toast toast = Toast.makeText(getContext(), message, Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL, 0,0);
                                toast.show();
                                verify_btn.setProgress(0);
                            }
                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {
                                if(getContext()!=null){
                                    verify_btn.setProgress(0);
                                }
                            }
                        });

                jsonObjectRequest.setTag(TAG);
                MyVolleySingleton.getInstance(getActivity()).addToRequestQueue(jsonObjectRequest);

                Snackbar.make(getView(), "Sending verification mail...", Snackbar.LENGTH_LONG).show();
            }
        });

        return view;
    }

}
