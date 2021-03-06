package com.guc.ahmed.callingapp.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guc.ahmed.callingapp.MainActivity;
import com.guc.ahmed.callingapp.R;
import com.guc.ahmed.callingapp.MyVolleySingleton;
import com.guc.ahmed.callingapp.gucpoints.GucPoints;
import com.guc.ahmed.callingapp.objects.Profile;
import com.guc.ahmed.callingapp.objects.RequestTrip;
import com.guc.ahmed.callingapp.gucpoints.GucPlace;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class TripHistoryFragment extends Fragment {


    private View view;
    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;
    private List<RequestTrip> tripHistory;
    private LinearLayout noHistory;
    private TextView historyMessage;
    private ProgressBar progressBar;

    private String gmail;
    private Profile profile;
    private Gson gson;
    private static final String TAG = "TripHistoryFragment";

    public TripHistoryFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_trip_history, container, false);

        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Ride History");

        noHistory = view.findViewById(R.id.no_history);
        historyMessage = view.findViewById(R.id.history_message);
        progressBar = view.findViewById(R.id.progressBar);

        tripHistory = new ArrayList<>();
        mRecyclerView = view.findViewById(R.id.trip_history_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext(), LinearLayout.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new MyAdapter();
        mRecyclerView.setAdapter(mAdapter);

        gmail = MainActivity.mAuth.getCurrentUser().getEmail();
        profile = getProfile(gmail);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();
        
        return view;
    }

    private Profile getProfile(String gmail) {
        progressBar.setVisibility(View.VISIBLE);
        String url = getResources().getString(R.string.url_get_profile) + gmail;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        progressBar.setVisibility(View.GONE);
                        profile = gson.fromJson(response.toString(), Profile.class);
                        tripHistory = getTripHistory();
                        if(tripHistory.size()>0){
                            mAdapter.notifyDataSetChanged();
                            noHistory.setVisibility(View.GONE);
                            mRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            historyMessage.setText("You have no previous rides");
                            noHistory.setVisibility(View.VISIBLE);
                            mRecyclerView.setVisibility(View.GONE);
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.GONE);
                        noHistory.setVisibility(View.VISIBLE);
                        mRecyclerView.setVisibility(View.GONE);
                        historyMessage.setText("Error loading your ride history");
                    }
                });

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(2500,2,1) );
        jsonObjectRequest.setTag(TAG);
        MyVolleySingleton.getInstance(getActivity()).addToRequestQueue(jsonObjectRequest);

        return null;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (MyVolleySingleton.getInstance(getActivity()).getRequestQueue() != null) {
            MyVolleySingleton.getInstance(getActivity()).getRequestQueue().cancelAll(TAG);
        }
    }

    private List<RequestTrip> getTripHistory() {
        return profile.getTripHistory();
    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private RecyclerView mRecyclerView;

        public MyAdapter(){
        }
        // Create new views (invoked by the layout manager)
        @Override
        public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
            // create a new view
            CardView v = (CardView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.trip_history_item, parent, false);

            return new MyAdapter.ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            RequestTrip trip = tripHistory.get(position);
            holder.pickupTxt.setText(GucPoints.getGucPlaceByLatLng(trip.getPickupLocation()).getName());

            String time = new SimpleDateFormat("hh:mm a").format(trip.getRequestTime());
            String date = new SimpleDateFormat("dd/MM/yyyy").format(trip.getRequestTime());
            holder.tripDate.setText(date + " at " + time);

            if(trip.getCancelTime() != null){
                holder.tripStatus.setText("Canceled");
            }else if(tripHistory.get(position).getEndTime() != null){
                holder.tripStatus.setText("Completed");
            }
            holder.destination1Txt.setText(GucPoints.getGucPlaceByLatLng(trip.getDestinations().get(0)).getName());
            if(trip.getDestinations().size() == 2){
                holder.destination2Txt.setText(GucPoints.getGucPlaceByLatLng(trip.getDestinations().get(1)).getName());
                holder.destination2.setVisibility(View.VISIBLE);
                holder.dot1to2.setVisibility(View.VISIBLE);
            }else if(trip.getDestinations().size() == 3){
                holder.destination2Txt.setText(GucPoints.getGucPlaceByLatLng(trip.getDestinations().get(1)).getName());
                holder.destination3Txt.setText(GucPoints.getGucPlaceByLatLng(trip.getDestinations().get(2)).getName());
                holder.destination2.setVisibility(View.VISIBLE);
                holder.dot1to2.setVisibility(View.VISIBLE);
                holder.destination3.setVisibility(View.VISIBLE);
                holder.dot2to3.setVisibility(View.VISIBLE);
            }

        }

        @Override
        public int getItemCount() {
            return tripHistory.size();
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            mRecyclerView = recyclerView;
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            private TextView tripDate;
            private TextView tripStatus;
            private TextView pickupTxt;
            private TextView destination1Txt;
            private TextView destination2Txt;
            private TextView destination3Txt;
            private LinearLayout destination2;
            private LinearLayout destination3;
            private LinearLayout dot1to2;
            private LinearLayout dot2to3;

            public ViewHolder(final View itemView) {
                super(itemView);

                tripDate = itemView.findViewById(R.id.trip_history_date);
                tripStatus = itemView.findViewById(R.id.trip_history_status);
                pickupTxt = itemView.findViewById(R.id.pickup_txt);
                destination1Txt = itemView.findViewById(R.id.destination_1_txt);
                destination2Txt = itemView.findViewById(R.id.destination_2_txt);
                destination3Txt = itemView.findViewById(R.id.destination_3_txt);
                destination2 = itemView.findViewById(R.id.summary_destination_2);
                destination3 = itemView.findViewById(R.id.summary_destination_3);
                dot1to2 = itemView.findViewById(R.id.dot_1_2);
                dot2to3 = itemView.findViewById(R.id.dot_2_3);
            }

            @Override
            public void onClick(View view) {

            }

        }
    }

}
