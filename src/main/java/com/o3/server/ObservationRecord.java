package com.o3.server;

public class ObservationRecord {
    private Integer id;

    private String identifier;
    private String description;
    private String payload;
    private String rightAscension;
    private String declination; 
    private String timeReceived;
    private String owner;

    private boolean isObservatoryPresent;
    private String observatoryName;
    private Double latitude;
    private Double longitude;

    private boolean isWeatherPresent;
    private Double temperatureInKelvins;
    private Double cloudinessPercentance;
    private Double bagroundLightVolume;

    private String ownerUsername;
    private String updateReason;
    private String modified;

    public ObservationRecord (
        Integer id,

        String identifier, 
        String description, 
        String payload, 
        String rightAscension, 
        String declination,
        String timeReceived,
        String owner,

        boolean isObservatoryPresent,
        String observatoryName,
        Double latitude,
        Double longitude,

        boolean isWeatherPresent,
        Double temperatureInKelvins,
        Double cloudinessPercentance,
        Double bagroundLightVolume,

        String ownerUsername,
        String updateReason,
        String modified
    ) {
        this.id = id;

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

        this.isWeatherPresent = isWeatherPresent;
        this.temperatureInKelvins = temperatureInKelvins;
        this.cloudinessPercentance = cloudinessPercentance;
        this.bagroundLightVolume = bagroundLightVolume;

        this.ownerUsername = ownerUsername;
        this.updateReason = updateReason;
        this.modified = modified;
    }

    public Integer getId() {return id;}

    public String getIdentifier() {return identifier;}
    public String getDescription() {return description;}
    public String getPayload() {return payload;}
    public String getRightAscension() {return rightAscension;}
    public String getDeclination() {return declination;}
    public String getTimeReceived() {return timeReceived;}
    public String getOwner() {return owner;}

    public boolean getIsObservatoryPresent() {return isObservatoryPresent;}
    public String getObservatoryName() {return observatoryName;}
    public Double getLatitude() {return latitude;}
    public Double getLongitude() {return longitude;}

    public boolean getIsWeatherPresent() {return isWeatherPresent;}
    public Double getTemperatureInKelvins() {return temperatureInKelvins;}
    public Double getCloudinessPercentance() {return cloudinessPercentance;}
    public Double getBagroundLightVolume() {return bagroundLightVolume;}

    public String getOwnerUsername() {return ownerUsername;}
    public String getUpdateReason() {return updateReason;}
    public String getModified() {return modified;}
}
