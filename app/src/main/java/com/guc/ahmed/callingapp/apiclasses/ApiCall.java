package com.guc.ahmed.callingapp.apiclasses;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class ApiCall {
    public String readUrl(String myUrl, JSONObject params) throws IOException
    {
        String data = "";
        InputStream inputStream = null;
        HttpURLConnection urlConnection = null;
        DataOutputStream out;

        try {
            URL url = new URL(myUrl);
            urlConnection=(HttpURLConnection) url.openConnection();
            urlConnection.setDoInput(true);
            if(params != null){
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type","application/json");

                out = new DataOutputStream(urlConnection.getOutputStream());
                out.writeBytes(params.toString());
                out.flush();
                out.close();
            }

            urlConnection.connect();
            //Log.v("HERE", "2");
            inputStream = urlConnection.getInputStream();
            //Log.v("HERE", "3");
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            //Log.v("HERE", "4");
            StringBuffer stringBuffer = new StringBuffer();

            String line = "";
            while((line = br.readLine()) != null)
            {
                stringBuffer.append(line);
            }

            data = stringBuffer.toString();
            br.close();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (inputStream != null)
                inputStream.close();
            if (urlConnection != null)
                urlConnection.disconnect();
        }
        Log.d("ApiCall","Returning data= "+data);

        return data;
    }

}
