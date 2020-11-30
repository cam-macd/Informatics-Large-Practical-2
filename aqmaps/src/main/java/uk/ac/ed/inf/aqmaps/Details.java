package uk.ac.ed.inf.aqmaps;

@SuppressWarnings("unused")
public class Details {
	private String country;
	
	private Square square;
	public static class Square {
		private Coordinates southWestCorner;
		private Coordinates northEastCorner;

	}
	public static class Coordinates {
		double lng;
		double lat;
	}
	
	private Coordinates coordinates;

	
	private String nearestPlace;
	private String words;
	private String language;
	private String map;
	
	// Returns longitude of the sensor
	public double getLng() {
		return coordinates.lng;
	}
	
	// Returns latitude of the sensor
	public double getLat() {
		return coordinates.lat;
	}
	
	public String getWords() {
		return words;
	}
	
}
