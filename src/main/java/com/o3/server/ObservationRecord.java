package com.o3.server;

public class ObservationRecord {
    private String identifier;
    private String description;
    private String payload;
    private String rightAscension;
    private String declination; 

    public ObservationRecord(String identifier, String description, String payload, String rightAscension, String declination) {
        this.identifier = identifier;
        this.description = description;
        this.payload = payload;
        this.rightAscension = rightAscension;
        this.declination = declination;
    }

    public String getIdentifier() {return identifier;}
    public String getDescription() {return description;}
    public String getPayload() {return payload;}
    public String getRightAscension() {return rightAscension;}
    public String getDeclination() {return declination;}
}
