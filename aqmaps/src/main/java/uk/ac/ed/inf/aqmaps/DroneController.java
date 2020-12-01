package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;


public class DroneController {

	private LineString confinementArea;
	private List<Polygon> noFlyZones = new ArrayList<>();
	// NoFlyLineSegments are the line segments which the drone must not
	// intersect, not to be mistaken with the line segments representing the no
	// fly zones.
	private List<LineSegment> noFlyLineSegments = new ArrayList<>();
	private List<Sensor> sensorList = new ArrayList<>();
	private List<Feature> featureList = new ArrayList<>();
	private Drone drone;
	private String flightPathString;
	private String year;
	private String month;
	private String day;
	
	public DroneController(LineString confinementArea, int year, int month, int
			day, Drone drone) {
		
		String dayString = Integer.toString(day);
		if (day < 10) {
			dayString = "0" + dayString;
		}
		this.day = dayString;
		
		String monthString = Integer.toString(month);
		if (month < 10) {
			monthString = "0" + monthString;
		}
		this.month = monthString;
		
		String yearString = Integer.toString(year);
		this.year = yearString;
		
		flightPathString = "";
		
		setSensorList(this.year, this.month, this.day);
		
		setNoFlyZones();
		
		this.confinementArea = confinementArea;
		featureList.add(Feature.fromGeometry((Geometry)confinementArea));
		
		// Note that the confinement area is also added to the noFlyLineSegments
		// since the drone path should not intersect the lines defining the 
		// confinement area
		setNoFlyLineSegments(); 
		
		this.drone = drone;
	}
	
	// Setter for sensorList
	private void setSensorList(String year, String month, String day) {
		// Must format the appropriate date inputs in a way that allows access
		// to the appropriate web server directory.
		String urlString = 
				"http://localhost:80/maps/" + year + "/" + month + "/" + day 
				+ "/air-quality-data.json";
		
		String jsonMapString = getResponseBody(urlString);
		jsonMapString = jsonMapString.replaceAll("\"null\"", "\"NaN\""); // Surround with brackets incase a What3Words encoding can contain the word null
		
		Type listType = new TypeToken<ArrayList<Sensor>>() {}.getType();
		sensorList = new Gson().fromJson(jsonMapString, listType);
		
		for (int i = 0; i < sensorList.size(); i++) {
			sensorList.get(i).setPosition();
			sensorList.get(i).setMarkerProperties("#aaaaaa", ""); // All markers start as unvisited.
		}

	}
	
	// Setter for noFlyZones, and initialises featureList to be equal to the
	// Feature List containing all of the no-fly-zones
	private void setNoFlyZones() {
		
		String urlString = "http://localhost:80/buildings/no-fly-zones.geojson";
		List<Feature> noFlyZoneFeatures = 
				FeatureCollection.fromJson(getResponseBody(urlString)).
				features();
		
		// Initialise featureList to contain the no fly zones and confinement
		// area is added to this list in the constructor.
		featureList = noFlyZoneFeatures;
		
		for (int i = 0; i < noFlyZoneFeatures.size(); i++) {
			noFlyZones.add((Polygon)noFlyZoneFeatures.get(i).geometry());
		}
	}
	
	// Getter for drone
	private Drone getDrone() {
		return drone;
	}
	
	private List<LineSegment> getNoFlyLineSegments() {
		return noFlyLineSegments;
	}
	
	private void setNoFlyLineSegments() {
		// Adding the no-fly-zones to noFlyLineSegments
		for (int i = 0; i < noFlyZones.size(); i++) {
			// noFlyZonePoints is the list of points which make up the LineStrings
			// which represent the edges of no-fly-zones. Must use get(0) because
			// .coordinates() returns List<List<Point>> for a polygon.
			List<Point> noFlyZonePoints = 
					noFlyZones.get(i).coordinates().get(0);
			for (int j = 0; j < noFlyZonePoints.size() - 1; j++) {
				Point point1 = 
						Point.fromLngLat(noFlyZonePoints.get(j).longitude(), 
								noFlyZonePoints.get(j).latitude());
				Point point2 = 
						Point.fromLngLat(noFlyZonePoints.get(j+1).longitude(), 
								noFlyZonePoints.get(j+1).latitude());
				LineSegment noFlyLineSegment = new LineSegment(point1, point2);
				noFlyLineSegments.add(noFlyLineSegment);
			}
		}
		
		// Adding the confinement area to noFlyLineSegments
		List<Point> confinementAreaPoints = confinementArea.coordinates();
		for (int i = 0; i < confinementAreaPoints.size() - 1 ; i++) {
			LineSegment noFlyLineSegment = 
					new LineSegment(confinementAreaPoints.get(i), 
							confinementAreaPoints.get(i+1));
			noFlyLineSegments.add(noFlyLineSegment);
		}
	}

	
	// Should be called with the fileName "readings-DD-MM-YYYY.geojson" for the
	// appropriate date.
	private void writeReadings(String fileName) {
		for (int i = 0; i < sensorList.size(); i++) {
			Sensor sensor = sensorList.get(i);
			Point sensorPoint = sensorList.get(i).getPosition();
			Feature sensorFeature = Feature.fromGeometry((Geometry)sensorPoint);
			sensorFeature.addStringProperty("location", sensor.getLocation());
			sensorFeature.addStringProperty("rgb-string", 
					sensor.getRgbString());
			sensorFeature.addStringProperty("marker-color", 
					sensor.getRgbString());
			sensorFeature.addStringProperty("marker-symbol", 
					sensor.getMarkerSymbol());
			featureList.add(sensorFeature);
		}
        var featureCollection = FeatureCollection.fromFeatures(featureList);
        String jsonString = featureCollection.toJson();
		try {
			var file = new FileWriter(fileName);
	        file.write(jsonString);
	        file.close();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}
	
	// Should be called with the fileName "flightpath-DD-MM-YYYY.txt" for the
	// appropriate date.
	private void writeFlightPath(String fileName) {
		try {
			var file = new FileWriter(fileName);
	        file.write(flightPathString);
	        file.close();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}
	
	// Used for testing while optimizing some code
	public String getFlightPathString() {
		return flightPathString;
	}
	
	// Used for testing
 	public void printSensors() {
		System.out.println("Number of sensors in list: " + sensorList.size());
		for (int i = 0; i < sensorList.size(); i++) {
			Sensor sensor = sensorList.get(i);
			System.out.println("What3Words Location: " + sensor.getLocation());
			System.out.println("Battery: " + sensor.getBattery());
			System.out.println("Reading: " + sensor.getReading());
			System.out.println("Lng: " + sensor.getPosition().longitude() 
					+ ", Lat: " + sensor.getPosition().latitude());
		}
	} 
	
	// Moves the drone and updates the flight path string of the drone.
	// The Point, newPosition, must meet the requirements of drone's move 
 	// function.
	private void moveDrone(Point newPosition) {
		// angle will be affected by floating point errors
		double angle = Math.toDegrees(PointUtils.angleBetweenPoints(
				drone.getPosition(), newPosition));
		int roundedAngle = (int) Math.round(angle);
		
		flightPathString = flightPathString + (151-drone.getMoveAllowance()) 
				+ "," + drone.getPosition().longitude() + "," 
				+ drone.getPosition().latitude() + "," + roundedAngle + "," 
				+ newPosition.longitude() + "," 
				+ newPosition.latitude() + ",";
		
		drone.move(newPosition);
	}
	
	// Used to read a sensor
	private void droneRead(Sensor sensor) {
		flightPathString = flightPathString + sensor.getLocation() + "\n";
		getDrone().readSensor(sensor);
	}
	
	// Used when we cant/dont want to read a sensor
	private void droneDontRead() {
		flightPathString = flightPathString + "null\n";
	}
	

	public void greedyFlightPath(Point startPosition) {
		Point currentPosition = startPosition;
		for (int i = 0; i < sensorList.size(); i++) {
			if (drone.getMoveAllowance() <= 0) {
				System.out.println("Flightpath runs out of moves on date "
						+ "DD/MM/YY: " + day + month + year + "- Cant visit all"
						+ " sensors");
				break;
			}
			
			double bestDistance = Double.POSITIVE_INFINITY;
			Sensor closestSensor = sensorList.get(0);
			for (int j = 0; j < sensorList.size(); j++) {
				Sensor currentSensor = sensorList.get(j);
				double currentSensorDistance = 
						PointUtils.findDistanceBetween(currentPosition, 
								currentSensor.getPosition());
				if (currentSensorDistance < bestDistance && 
						currentSensor.getMarkerSymbol() == "") {
					bestDistance = currentSensorDistance;
					closestSensor = currentSensor;
				}
			}
			// The drone is sent to visit the closest sensor according to
			// the straight line distance between its position and the sensor.
			visitSensor(closestSensor);
			
			// The drone's current point is updated.
			currentPosition = drone.getPosition();
		}

		// Moving the drone back
		returnDrone(startPosition);
		
		//System.out.println(drone.getMoveAllowance());
		
		String flightPathFile = "flightpath-" + day + "-" + month + "-" + year
				+ ".txt";
		writeFlightPath(flightPathFile);
		
		String readingsFile = "readings-" + day + "-" + month + "-" + year
				+ ".geojson";
		writeReadings(readingsFile);
	}
	
	// Uses AStarUtils' aStar function to move the drone along the optimal
	// path to a sensor
	private void visitSensor(Sensor sensor) {
		Point currentPosition = drone.getPosition();
		List<Point> moves = 
				AStarUtils.aStar(currentPosition, sensor.getPosition(), 
						getNoFlyLineSegments(), 0.0002);

		for (int j = 0; j < moves.size() - 1; j++) {
			// The drone should not perform any more moves if it has already
			// reached its move limit.
			if (drone.getMoveAllowance() <= 0) {
				// Makes sure that the appropriate lines representing the 
				// drone's moves are added to the featureList for the geojson 
				// file later.
				// Before this function is called, there should be an if
				// statement making sure that moveAllowance is at least 1, so we 
				// can be confident that the second argument of subList will be
				// at least 1 and so moves will always represent at least one
				// LineString on the geojson map.
				moves = moves.subList(0,j);
				break;
			}
			
			moveDrone(moves.get(j+1)); // Since the first point in moves will be the drone's current position
			if (PointUtils.findDistanceBetween(moves.get(j+1),
					sensor.getPosition()) >= 0.0002) {
				droneDontRead(); // if the next move doesnt bring the drone in range of the target sensor, the drone will not read a sensor. 
			}
		}
		LineString lines = LineString.fromLngLats(moves);
		featureList.add(Feature.fromGeometry((Geometry)lines));
		// Need this if condition incase the drone runs out of moves before reaching the sensor.
		if (PointUtils.findDistanceBetween(drone.getPosition(),
				sensor.getPosition()) < 0.0002) {
			droneRead(sensor);
		}
		
	}
	
	// Returns the drone from its current position to the point from where it
	// was launched, using AStarUtils' aStar function to find the path back
	private void returnDrone(Point startPosition) {
		Point currentPosition = drone.getPosition();

		if (drone.getMoveAllowance() > 0) {
			List<Point> returnMoves = 
					AStarUtils.aStar(currentPosition, startPosition, 
							getNoFlyLineSegments(), 0.0003);
			for (int i = 0; i < returnMoves.size() - 1; i++) {
				// The drone should not perform any more moves if it has already
				// reached its move limit.
				if (drone.getMoveAllowance() <= 0) {
					// Before going into this for loop, there is an if statement
					// making sure that  moveAllowance is least 1, so we can be
					// confident that the second argument of subList will be
					// at least 1.
					returnMoves = returnMoves.subList(0,i); 
					System.out.println("Flightpath runs out of moves on date "
							+ "DD/MM/YY: " + day + month + year + " - Cant "
							+ "return to start point.");
					break;
				}
				moveDrone(returnMoves.get(i+1));
				droneDontRead();// we have read all sensors at this point
			}
			LineString returnLines = LineString.fromLngLats(returnMoves);
			featureList.add(Feature.fromGeometry((Geometry)returnLines));
		}
	}
	
	// Used to access the web server and return files as strings
	public static String getResponseBody(String urlString) {
		String responseString = "";
		
		var client = HttpClient.newHttpClient();
		// HttpClient assumes that it is a GET request by default.
		var request = 
				HttpRequest.newBuilder().uri(URI.create(urlString)).build();
		// The response object is of class HttpResponse<String>
		try {
			var response = client.send(request, BodyHandlers.ofString());
			responseString = response.body();
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return responseString;
	}

	
	public static void main(String[] args) {
		// Setting up confinement area
		Point point1 = Point.fromLngLat(-3.192473, 55.946233);
		Point point2 = Point.fromLngLat(-3.184319,55.946233);
		Point point3 = Point.fromLngLat(-3.184319, 55.942617);
		Point point4 = Point.fromLngLat(-3.192473, 55.942617);
		Point point5 = Point.fromLngLat(-3.192473, 55.946233);
		List<Point> pointList = new ArrayList<>();
		pointList.add(point1);
		pointList.add(point2);
		pointList.add(point3);
		pointList.add(point4);
		pointList.add(point5);
		LineString confinementArea = LineString.fromLngLats(pointList);		
		
		Point launchPoint = Point.fromLngLat(-3.1878, 55.9444);
		
		int[] minDate = new int[3];
		int minMoves = 100000;
		int[] maxDate = new int[3];
		int maxMoves = 0;
		double sum = 0;
		int[] years = {2020,2021};
		int[] months = {1,2,3,4,5,6,7,8,9,10,11,12};
		int[] days = 
			{31,29,31,30,31,30,31,31,30,31,30,31,31,28,31,30,31,30,31,31,30,31,
					30,31}; // for month 1-12 between 2020 AND 2021
		
		for (int i = 0; i < years.length; i++) {
			for (int j = 0; j < months.length; j++) {
				for (int k = 1; k < days[j + i*12]+1; k++) {
					Drone drone = new Drone(launchPoint, 150);
					DroneController dc = new DroneController(confinementArea, years[i], months[j], k, drone);

					dc.greedyFlightPath(launchPoint);
					
					// Start of refactor testing
					
					String dayString = Integer.toString(k);
					if (k < 10) {
						dayString = "0" + dayString;
					}
					String monthString = Integer.toString(months[j]);
					if (months[j] < 10) {
						monthString = "0" + monthString;
					}
					String yearString = Integer.toString(years[i]);
					
					var filePathCorrect = Path.of("Answers/flightpath-" + dayString + "-" + monthString + "-" + yearString + ".txt");

					try {
						String newFlightPathString = dc.getFlightPathString();
						String correctFlightPathString = Files.readString(filePathCorrect);
						if (correctFlightPathString.equals(newFlightPathString) == false) {
							System.out.println(newFlightPathString.length());
							System.out.println(correctFlightPathString.length());
							throw new IllegalStateException("flightpath-" + dayString + "-" + monthString + "-" + yearString + ".txt" + " has changed.");
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					// End of refactor Testing
					
					sum = sum + dc.getDrone().getMoveAllowance();
					
					if (dc.getDrone().getMoveAllowance() < minMoves) {
						minMoves = dc.getDrone().getMoveAllowance();
						minDate[0] = years[i];
						minDate[1] = months[j];
						minDate[2] = k;
					}
					else if (dc.getDrone().getMoveAllowance() > maxMoves) {
						maxMoves = dc.getDrone().getMoveAllowance();
						maxDate[0] = years[i];
						maxDate[1] = months[j];
						maxDate[2] = k;
					}
				}
			}
		}
		
		System.out.println("Average moves remaining per map: " + sum/(365+366));
		System.out.println("Min moves remaining = " + minMoves + ", Date (DD-MM-YY) = " + minDate[2] +"-" +
		minDate[1] + "-" + minDate[0]);
		System.out.println("Max moves remaining = " + maxMoves + ", Date (DD-MM-YY) = " + maxDate[2] +"-" +
		maxDate[1] + "-" + maxDate[0]);
		

	
	}
}
