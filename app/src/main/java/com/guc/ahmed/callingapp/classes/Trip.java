package com.guc.ahmed.callingapp.classes;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Trip {
    private String id;
	private Date requestTime;
	private Date startTime;
	private Date endTime;
	private Date cancelTime;
	
	private LatLng pickupLocation;
	private ArrayList<LatLng> destinations;
	
	private String carID;
	private String userID;
	
	public Trip() {
	}
	
	public Trip(String userID, LatLng pickupLocation, ArrayList<LatLng> destinations) {
		this.userID = userID;
		this.pickupLocation = pickupLocation;
		this.destinations = destinations;
	}

	public Trip(Date requestTime, Date startTime, Date endTime, Date cancelTime, LatLng pickupLocation, ArrayList<LatLng> destinations) {
		this.requestTime = requestTime;
		this.startTime = startTime;
		this.endTime = endTime;
		this.cancelTime = cancelTime;
		this.pickupLocation = pickupLocation;
		this.destinations = destinations;
	}

	public String getId() {
		return id;
	}

	public Date getRequestTime() {
		return requestTime;
	}

	public void setRequestTime(Date requestTime) {
		this.requestTime = requestTime;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public LatLng getPickupLocation() {
		return pickupLocation;
	}

	public void setPickupLocation(LatLng pickupLocation) {
		this.pickupLocation = pickupLocation;
	}

	public ArrayList<LatLng> getDestinations() {
		return destinations;
	}

	public void setDestinations(ArrayList<LatLng> destinations) {
		this.destinations = destinations;
	}

	public String getCarID() {
		return carID;
	}

	public void setCarID(String carID) {
		this.carID = carID;
	}

	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public Date getCancelTime() {
		return cancelTime;
	}

	public void setCancelTime(Date cancelTime) {
		this.cancelTime = cancelTime;
	}

}
