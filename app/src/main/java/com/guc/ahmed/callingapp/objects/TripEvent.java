package com.guc.ahmed.callingapp.objects;

public enum TripEvent {
    START("START"),
    END("END"),
    CANCEL("CANCEL"),
    CONTINUE("CONTINUE"),
    ARRIVE_PICKUP("ARRIVE_PICKUP"),
    ARRIVE_DESTINATION("ARRIVE_DESTINATION"),
    ARRIVE_FINAL("ARRIVE_FINAL"),
    CAR_ON_WAY("CAR_ON_WAY"),
    CHANGE_DESTINATION("CHANGE_DESTINATION");

    private String name;

    TripEvent(String event) {
        this.name = event;
    }
}