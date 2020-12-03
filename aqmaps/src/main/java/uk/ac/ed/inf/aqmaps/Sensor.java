package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;
import com.google.gson.Gson;

public class Sensor {
	
	private String location; // What3words location
	private double battery;
	private double reading;
	private Point position;
	private String rgbString;
	private String markerSymbol;
	
	public double getReading() {
		return reading;
	}
	
	public double getBattery() {
		return battery;
	}
	
	public Point getPosition() {
		return position;
	}
	
	public String getLocation() {
		return location;
	}
	
	public String getRgbString() {
		return rgbString;
	}
	
	public String getMarkerSymbol() {
		return markerSymbol;
	}
	
	// Sets the position according to the sensor's what3words location
	public void setPosition(int webServerPort) {
		var urlString = 
				"http://localhost:" + webServerPort + "/words/" 
						+ location.replaceAll("\\.","/") + "/details.json";

		var jsonDetailsString = DroneController.getResponseBody(urlString);
		
		var details = new Gson().fromJson(jsonDetailsString, Details.class);
		
		position = Point.fromLngLat(details.getLng(), details.getLat());
	}
	
	public void setMarkerProperties(String rgbString, String markerSymbol) {
		this.rgbString = rgbString;
		this.markerSymbol = markerSymbol;
		
	}

}
