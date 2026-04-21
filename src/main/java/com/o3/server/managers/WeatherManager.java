package com.o3.server.managers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONObject;
import org.w3c.dom.*;
import java.io.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class WeatherManager {

    public static JSONObject getWeather(double lat, double lon, String time) {
        try {
            String url = String.format("http://localhost:4001/wfs?latlon=%s,%s" 
                + "&parameters=temperatureInKelvins,cloudinessPercentance,bagroundLightVolume"
                + "&starttime=%s", String.valueOf(lat), String.valueOf(lon), time);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept","application/xml")
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            //System.out.println(response.body());
            return pasrseXML(response.body());
        } catch (Exception e) {
            System.out.println("WeatherManager > getWeather > Exception");
        }
        return null;
    }

    private static JSONObject pasrseXML(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));

            NodeList elements = doc.getElementsByTagName("BsWfs:BsWfsElement");
            //Element element = (Element) elements.item(0);
            //String parameterValue = element.getElementsByTagName("BsWfs:ParameterValue").item(0).getTextContent();
            //return Double.parseDouble(parameterValue.replace(",", "."));

            JSONObject weather = new JSONObject();

            for (int i = 0; i < elements.getLength(); i++) {
                Element element = (Element) elements.item(i);
                String parameterName = element.getElementsByTagName("BsWfs:ParameterName").item(0).getTextContent();
                String parameterValue = element.getElementsByTagName("BsWfs:ParameterValue").item(0).getTextContent();
                weather.put(parameterName, parameterValue.replace(",", "."));
                //System.out.println("Parameter: " + parameterName + " | Value: " + parameterValue);
            }
            return weather;
        } catch (Exception e) {
            System.out.println("WeatherManager > parseXML > Exception \n" + e.getMessage());
        }
        return null;
    }
}
