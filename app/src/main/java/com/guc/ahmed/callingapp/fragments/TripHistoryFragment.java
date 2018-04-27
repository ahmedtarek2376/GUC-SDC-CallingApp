package com.guc.ahmed.callingapp.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guc.ahmed.callingapp.R;
import com.guc.ahmed.callingapp.adapter.EditItemTouchHelperCallback;
import com.guc.ahmed.callingapp.apiclasses.MyVolleySingleton;
import com.guc.ahmed.callingapp.classes.Profile;
import com.guc.ahmed.callingapp.classes.Trip;
import com.guc.ahmed.callingapp.gucpoints.GucPlace;
import com.guc.ahmed.callingapp.gucpoints.GucPoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class TripHistoryFragment extends Fragment {


    private View view;
    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;
    private List<Trip> tripHistory;

    private String gmail;
    private Profile profile;
    private Gson gson;

    public TripHistoryFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_trip_history, container, false);

        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Trip History");

        gmail = getArguments().getString("gmail");
        profile = getProfile(gmail);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();
        
        tripHistory = new ArrayList<>();
        mRecyclerView = view.findViewById(R.id.trip_history_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext(), LinearLayout.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new MyAdapter();
        mRecyclerView.setAdapter(mAdapter);
        
        return view;
    }

    private Profile getProfile(String gmail) {
        String url = getResources().getString(R.string.url_get_profile) + gmail;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        profile = gson.fromJson(response.toString(), Profile.class);
                        tripHistory = getTripHistory();
                        mAdapter.notifyDataSetChanged();
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });

// Access the RequestQueue through your singleton class.
        MyVolleySingleton.getInstance(getActivity()).addToRequestQueue(jsonObjectRequest);

        return null;
    }

    private List<Trip> getTripHistory() {

//        final LatLng b3 = GucPoints.getGucPlaceByName(GucPoints.B3_U_AREA.getName()).getLatLng();
//        final LatLng gym = GucPoints.getGucPlaceByName(GucPoints.GUC_GYM.getName()).getLatLng();
//        final LatLng gate1= GucPoints.getGucPlaceByName(GucPoints.GATE_1.getName()).getLatLng();
//        final LatLng d4 = GucPoints.getGucPlaceByName(GucPoints.D4_U_AREA.getName()).getLatLng();
//
//        Trip trip1 = new Trip(new Date(), new Date(), new Date(), null, b3 , new ArrayList<LatLng>(){{ add(gym);}} );
//        Trip trip2 = new Trip(new Date(), new Date(), null, new Date(), gate1 , new ArrayList<LatLng>(){{ add(d4);}} );
//        Trip trip3 = new Trip(new Date(), new Date(), null, new Date(), d4 , new ArrayList<LatLng>(){{ add(gate1); add(gym); add(b3);}} );
//        Trip trip4 = new Trip(new Date(), new Date(), new Date(), null, b3 , new ArrayList<LatLng>(){{ add(gate1);}} );
//        Trip trip5 = new Trip(new Date(), new Date(), null, new Date(), gym , new ArrayList<LatLng>(){{ add(d4); add(gate1);}} );
//        Trip trip6 = new Trip(new Date(), new Date(), new Date(), null, d4 , new ArrayList<LatLng>(){{ add(gate1);}} );
//        Trip trip7 = new Trip(new Date(), new Date(), new Date(), null, b3 , new ArrayList<LatLng>(){{ add(gym);}} );
//        Trip trip8 = new Trip(new Date(), new Date(), new Date(), null, gate1 , new ArrayList<LatLng>(){{ add(d4); add(b3); add(gym);}} );
//
//        ArrayList<Trip> arrayList = new ArrayList<>();
//        arrayList.addAll(Arrays.asList(trip1,trip2,trip3,trip4,trip5,trip6,trip7,trip8));
//
//        return arrayList;
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
            Trip trip = tripHistory.get(position);
            holder.pickupTxt.setText(GucPoints.getGucPlaceByLatLng(trip.getPickupLocation()).getName());
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
