package com.o3.server;

public class ObservationRecord {
    private String identifier;
    private String description;
    private String payload;
    private String rightAscension;
    private String declination; 
    private String timeReceived;
    private String owner;

    private boolean isObservatoryPresent;
    private String observatoryName;
    private String latitude;
    private String longitude;

    public ObservationRecord (
        String identifier, 
        String description, 
        String payload, 
        String rightAscension, 
        String declination,
        String timeReceived,
        String owner,

        boolean isObservatoryPresent,
        String observatoryName,
        String latitude,
        String longitude
    ) {
        this.identifier = identifier;
        this.description = description;
        this.payload = payload;
        this.rightAscension = rightAscension;
        this.declination = declination;
        this.timeReceived = timeReceived;
        this.owner = owner;

        this.isObservatoryPresent = isObservatoryPresent;
        this.observatoryName = observatoryName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getIdentifier() {return identifier;}
    public String getDescription() {return description;}
    public String getPayload() {return payload;}
    public String getRightAscension() {return rightAscension;}
    public String getDeclination() {return declination;}
    public String getTimeReceived() {return timeReceived;}
    public String getOwner() {return owner;}

    public boolean getIsObservatoryPresent() {return isObservatoryPresent;}
    public String getObservatoryName() {return observatoryName;}
    public String getLatitude() {return latitude;}
    public String getLongitude() {return longitude;}
}
