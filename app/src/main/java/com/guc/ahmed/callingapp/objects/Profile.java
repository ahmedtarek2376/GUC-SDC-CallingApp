package com.guc.ahmed.callingapp.objects;

import java.util.List;

public class Profile {

    private String id;

    private String gmail;

    private String gucMail;

    private boolean verified;

    private String confirmationGmail;

    private String confirmationToken;

    private List<RequestTrip> tripHistory;

    private RequestTrip currentTrip;

    public Profile() {
    }

    public Profile(String gucMail) {
        this.gucMail = gucMail;
        this.verified = false;
    }


    public String getGmail() {
        return gmail;
    }

    public void setGmail(String gmail) {
        this.gmail = gmail;
    }

    public String getConfirmationGmail() {
        return confirmationGmail;
    }

    public void setConfirmationGmail(String confirmationGmail) {
        this.confirmationGmail = confirmationGmail;
    }

    public List<RequestTrip> getTripHistory() {
        return tripHistory;
    }

    public void setTripHistory(List<RequestTrip> tripHistory) {
        this.tripHistory = tripHistory;
    }

    public String getGucMail() {
        return gucMail;
    }

    public void setGucMail(String gucMail) {
        this.gucMail = gucMail;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean enabled) {
        this.verified = enabled;
    }

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public void setConfirmationToken(String string) {
        this.confirmationToken = string;
    }

    public String getId() {
        return id;
    }

    public RequestTrip getCurrentTrip() {
        return currentTrip;
    }

    public void setCurrentTrip(RequestTrip currentTrip) {
        this.currentTrip = currentTrip;
    }



}
