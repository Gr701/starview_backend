package com.o3.server.models;

import org.json.JSONObject;
import org.json.JSONException;

import com.o3.server.managers.WeatherManager;

import static com.o3.server.utils.JsonUtils.*;

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

    private Integer viewCount;
    private Double rating;
    private Integer ratingCount;

    public ObservationRecord () {
        id = null;

        identifier = null;
        description = null;
        payload = null;
        rightAscension = null;
        declination = null;
        timeReceived = null;
        owner = null;

        isObservatoryPresent = false;
        observatoryName = null;
        latitude = 0.0;
        longitude = 0.0;

        isWeatherPresent = false;
        temperatureInKelvins = -1.0;
        cloudinessPercentance = 0.0;
        bagroundLightVolume = 0.0;

        ownerUsername = null;
        updateReason = null;
        modified = null;

        viewCount = 0;
        rating = 0.0; 
        ratingCount = 0;
    }

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
        String modified,

        Integer viewCount,
        Double rating,
        Integer ratingCount
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

        this.viewCount = viewCount;
        this.rating = rating;
        this.ratingCount = ratingCount;
    }

    //GETTERS
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

    public Integer getViewCount() {return viewCount;}
    public Double getRating() {return rating;}
    public Integer getRatingCount() {return ratingCount;}

    //SETTERS
    public void setIdentifier (String identifier) {this.identifier = identifier;}
    public void setDescription (String description) {this.description = description;}
    public void setPayload (String payload) {this.payload = payload;}
    public void setRightAscension (String rightAscension) {this.rightAscension = rightAscension;}
    public void setDeclination (String declination) {this.declination = declination;}
    public void setTimeReceived (String timeReceived) {this.timeReceived = timeReceived;}
    public void setOwner (String owner) {this.owner = owner;}

    public void setIsObservatoryPresent (boolean isObservatoryPresent) 
        {this.isObservatoryPresent = isObservatoryPresent;}
    public void setObservatoryName (String observatoryName) {this.observatoryName = observatoryName;}
    public void setLatitude (Double latitude) {this.latitude = latitude;}
    public void setLongitude (Double longitude) {this.longitude = longitude;}

    public void setIsWeatherPresent (boolean isWeatherPresent) 
        {this.isWeatherPresent = isWeatherPresent;}
    public void setTemperatureInKelvins (Double temperatureInKelvins) 
        {this.temperatureInKelvins = temperatureInKelvins;}
    public void setCloudinessPercentance (Double cloudinessPercentance) 
        {this.cloudinessPercentance = cloudinessPercentance;}
    public void setBagroundLightVolume (Double bagroundLightVolume) 
        {this.bagroundLightVolume = bagroundLightVolume;}

    public void setOwnerUsername (String ownerUsername) {this.ownerUsername = ownerUsername;}
    public void setUpdateReason (String updateReason) {this.updateReason = updateReason;}
    public void setModified (String modified) {this.modified = modified;}

    //OTHER
    public void addView() {
        viewCount += 1;
    }

    public void updateRating(int rating) {
        this.rating = (this.rating * ratingCount + rating) / (ratingCount + 1);
        ratingCount += 1;
    }

    public JSONObject getJson() {
        JSONObject json = new JSONObject();

        json.put("id", id);
        json.put("recordIdentifier", identifier);
        json.put("recordDescription", description);
        json.put("recordPayload", payload);
        json.put("recordRightAscension", rightAscension);
        json.put("recordDeclination", declination);
        json.put("recordTimeReceived", timeReceived);
        json.put("recordOwner", owner);
        json.put("recordViewCount", viewCount);
        json.put("recordRating", rating);
        json.put("recordRatingCount", ratingCount);

        if (isObservatoryPresent) {
            JSONObject observatory = new JSONObject();
            observatory.put("observatoryName", observatoryName);
            observatory.put("latitude", latitude);
            observatory.put("longitude", longitude);
            json.put("observatory", observatory);
        }

        if (isWeatherPresent) {
            JSONObject observatoryWeather = new JSONObject();
            observatoryWeather.put("temperatureInKelvins", temperatureInKelvins);
            observatoryWeather.put("cloudinessPercentance", cloudinessPercentance);
            observatoryWeather.put("bagroundLightVolume", bagroundLightVolume);
            json.put("observatoryWeather", observatoryWeather);
        }

        if (modified != null) {
            json.put("updateReason", updateReason);
            json.put ("modified", modified);
        }

        return json;
    }

    private void setWeather() {
        JSONObject weatherJson = WeatherManager.getWeather(
            latitude, 
            longitude, 
            timeReceived
        );
        
        if (weatherJson == null) {
            throw new RuntimeException("Error adding weather to the observation");
        }

        try {
            isWeatherPresent = true;
            temperatureInKelvins = weatherJson.getDouble("temperatureInKelvins");
            cloudinessPercentance = weatherJson.getDouble("cloudinessPercentance");
            bagroundLightVolume = weatherJson.getDouble("bagroundLightVolume");
        } catch (JSONException e) {
            throw new RuntimeException("Error adding weather to the observation");
        }
    }

    private void setObservatory(JSONObject json) {
        isObservatoryPresent = true;
        observatoryName = getStringFromJson(json, "observatoryName");
        latitude = getDoubleFromJson(json, "latitude");
        longitude = getDoubleFromJson(json, "longitude");

        if (json.has("observatoryWeather")) {
            setWeather();
        }
    }

    public void setJson(JSONObject json) {
        //BASICS
        identifier = getStringFromJson(json, "recordIdentifier");
        description = getStringFromJson(json, "recordDescription");
        payload = getStringFromJson(json, "recordPayload");
        rightAscension = getStringFromJson(json, "recordRightAscension");
        declination = getStringFromJson(json, "recordDeclination");

        //OBSERVATORY AND WEATHER
        if (json.has("observatory")) {
            try { 
                JSONObject observatoryJson = json.getJSONObject("observatory");
                setObservatory(observatoryJson);
            } catch (JSONException e) {
                throw new IllegalArgumentException("Json observatory field must be a JSONObject");
            }
        }
    }
}
