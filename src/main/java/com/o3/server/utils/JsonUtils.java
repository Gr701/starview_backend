package com.o3.server.utils;

import org.json.JSONException;
import org.json.JSONObject;

import com.o3.server.models.ObservationRecord;

public final class JsonUtils {
    private JsonUtils () {}

    public static void checkJsonField (JSONObject json, String key) {
        if (!json.has(key) || json.isNull(key)) {
            throw new IllegalArgumentException("Json field " + key + " is missing or null");
        }
    }

    public static String getStringFromJson(JSONObject json, String key) {
        checkJsonField(json, key);
        try {
            return json.getString(key);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Json value at " + key + " must be a string");
        }
    }

    public static Integer getIntegerFromJson(JSONObject json, String key) {
        checkJsonField(json, key);
        try {
            return json.getInt(key);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Json value at " + key + " must be an integer");
        }
    }

    public static Double getDoubleFromJson(JSONObject json, String key) {
        checkJsonField(json, key);
        try {
            return json.getDouble(key);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Json value at " + key + " must be a double");
        }
    }
}
