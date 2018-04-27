package com.guc.ahmed.callingapp.apiclasses;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.TextView;

import com.guc.ahmed.callingapp.R;
import com.guc.ahmed.callingapp.apiclasses.ApiCall;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Created by ahmed on 17-Mar-18.
 */

public class VerificationAsync extends AsyncTask<Object,String,String> {

    private String sygicData;
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
        String url = (String)objects[0];

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

    protected void onPostExecute(String s) {

        JSONObject jsonObject;
        try {
            //make the string a jsonObject to get the results from it
            jsonObject = new JSONObject(s);
            String message = jsonObject.getString("message");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //TextView textView = (TextView) activity.findViewById(R.id.textView);
    }
}
