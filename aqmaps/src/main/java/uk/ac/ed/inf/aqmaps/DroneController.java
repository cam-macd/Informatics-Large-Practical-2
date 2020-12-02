package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
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
	
	public DroneController(LineString confinementArea, String year, 
			String month, String day, Drone drone, int webServerPort) {
		
		this.day = day;
		
		this.month = month;
		
		this.year = year;
		
		flightPathString = "";
		
		setSensorList(this.year, this.month, this.day, webServerPort);
		
		setNoFlyZones(webServerPort);
		
		this.confinementArea = confinementArea;
		
		// Note that the confinement area is also added to the noFlyLineSegments
		// since the drone path should not intersect the lines defining the 
		// confinement area
		setNoFlyLineSegments(); 
		
		this.drone = drone;
		
	}
	
	// setSensorList sets sensorList by reading the map for the given date
	// from the web server at the given port.
	private void setSensorList(String year, String month, String day, 
			int webServerPort) {
		var urlString = 
				"http://localhost:" + webServerPort + "/maps/" + year + "/" 
						+ month + "/" + day + "/air-quality-data.json";
		
		var jsonMapString = getResponseBody(urlString);
		jsonMapString = jsonMapString.replaceAll("\"null\"", "\"NaN\"");
		// Rather than just replacing all occurrences of null with NaN, we 
		// replace all occurrences of "null" with "NaN", since the what3Words
		// addresses might contain the word null.
		
		var listType = new TypeToken<ArrayList<Sensor>>() {}.getType();
		sensorList = new Gson().fromJson(jsonMapString, listType);
		
		for (int i = 0; i < sensorList.size(); i++) {
			sensorList.get(i).setPosition();
			sensorList.get(i).setMarkerProperties("#aaaaaa", ""); // All markers start as unvisited.
		}

	}
	
	// setNoFlyZones accesses the web server at the given port to set the 
	// noFlyZones
	private void setNoFlyZones(int webServerPort) {
		
		var urlString = "http://localhost:" + webServerPort 
				+ "/buildings/no-fly-zones.geojson";
		var noFlyZoneFeatures = 
				FeatureCollection.fromJson(getResponseBody(urlString)).
				features();
		
		for (int i = 0; i < noFlyZoneFeatures.size(); i++) {
			noFlyZones.add((Polygon)noFlyZoneFeatures.get(i).geometry());
		}
	}
	
	private Drone getDrone() {
		return drone;
	}
	
	private List<LineSegment> getNoFlyLineSegments() {
		return noFlyLineSegments;
	}
	
	// Sets noFlyLineSegments which is defined as the line segments which the
	// drone should never cross, not to be mistaken with, purely, the line 
	// segments defining the no-fly-zones these will also include the line 
	// segments defining the confinement area.
	private void setNoFlyLineSegments() {
		// Adding the line segments defining the no-fly-zones to 
		// noFlyLineSegments
		for (int i = 0; i < noFlyZones.size(); i++) {
			// noFlyZonePointList is the list of points which make up the 
			// LineStrings which represent the edges of no-fly-zones. Must use 
			// .get(0) because.coordinates() returns List<List<Point>> for a 
			// polygon.
			var noFlyZonePointList = 
					noFlyZones.get(i).coordinates().get(0);
			for (int j = 0; j < noFlyZonePointList.size() - 1; j++) {
				var point1 = 
						Point.fromLngLat(noFlyZonePointList.get(j).longitude(), 
								noFlyZonePointList.get(j).latitude());
				var point2 = 
						Point.fromLngLat(noFlyZonePointList.get(j+1).longitude(), 
								noFlyZonePointList.get(j+1).latitude());
				var noFlyLineSegment = new LineSegment(point1, point2);
				noFlyLineSegments.add(noFlyLineSegment);
			}
		}
		
		// Adding the line segments defining the confinement area to 
		// noFlyLineSegments
		var confinementAreaPointList = confinementArea.coordinates();
		for (int i = 0; i < confinementAreaPointList.size() - 1 ; i++) {
			var noFlyLineSegment = 
					new LineSegment(confinementAreaPointList.get(i), 
							confinementAreaPointList.get(i+1));
			noFlyLineSegments.add(noFlyLineSegment);
		}
	}

	// Writes the geojson readings file to contain the sensors with their
	// updated reading and the features in featureList which should contain the
	// path of the drone.
	// Should be called with the fileName "readings-DD-MM-YYYY.geojson" for the
	// appropriate date.
	private void writeReadings(String fileName) {
		for (int i = 0; i < sensorList.size(); i++) {
			var sensor = sensorList.get(i);
			var sensorPoint = sensorList.get(i).getPosition();
			var sensorFeature = Feature.fromGeometry((Geometry)sensorPoint);
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
        var jsonString = featureCollection.toJson();
		try {
			var file = new FileWriter(fileName);
	        file.write(jsonString);
	        file.close();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}
	
	// Writes the flightpath txt file to contain flightPathString.
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

	
	// Moves the drone and updates the flight path string of the drone.
	// The Point, newPosition, must meet the requirements of drone's move 
 	// function - this should be confirmed before it is passed to this function.
	private void moveDrone(Point newPosition) {
		// angle will be affected by floating point errors
		var angle = Math.toDegrees(PointUtils.angleBetweenPoints(
				drone.getPosition(), newPosition));
		var roundedAngle = (int) Math.round(angle);
		// Update flightPathString
		flightPathString = flightPathString + (151-drone.getMoveAllowance()) 
				+ "," + drone.getPosition().longitude() + "," 
				+ drone.getPosition().latitude() + "," + roundedAngle + "," 
				+ newPosition.longitude() + "," 
				+ newPosition.latitude() + ",";
		
		drone.move(newPosition);
	}
	
	// droneRead makes the drone read the sensor and updates the flight path
	// string accordingly.
	private void droneRead(Sensor sensor) {
		flightPathString = flightPathString + sensor.getLocation() + "\n";
		getDrone().readSensor(sensor);
	}
	
	// droneDontRead is used when the the drone is unable to read a sensor
	// or we dont want it to and updates the flight path string accordingly.
	private void droneDontRead() {
		flightPathString = flightPathString + "null\n";
	}
	
	// Sends the drone to visit all of the sensors on the given day, choosing
	// to travel from its startPosition to the sensor with the shortest
	// straight line distance between the two points, then from that sensor
	// to the next closest sensor according to the straight line distance and
	// so on without explicitly visiting the same sensor twice until the drone
	// has visited all the sensors. At this point the drone is sent to return
	// to the startPosition. This takes into account the drone's move allowance
	// and will not return to the start position if it runs out of moves before
	// visiting all sensors.
	public void greedyFlightPath() {
		var startPosition = drone.getPosition();
		var currentPosition = drone.getPosition();
		for (int i = 0; i < sensorList.size(); i++) {
			if (drone.getMoveAllowance() <= 0) {
				System.out.println("Flightpath runs out of moves on date "
						+ "DD/MM/YY: " + day + month + year + "- Cant visit all"
						+ " sensors");
				break;
			}
			
			// First we find the closest sensor according to the straight line
			// distance from currentPosition to the sensor.
			var bestDistance = Double.POSITIVE_INFINITY;
			var closestSensor = sensorList.get(0);
			for (int j = 0; j < sensorList.size(); j++) {
				var currentSensor = sensorList.get(j);
				var currentSensorDistance = 
						PointUtils.findDistanceBetween(currentPosition, 
								currentSensor.getPosition());
				if (currentSensorDistance < bestDistance && 
						currentSensor.getMarkerSymbol() == "") {
					bestDistance = currentSensorDistance;
					closestSensor = currentSensor;
				}
			}
			// The drone is sent to visit the closest sensor according to the
			// straight line distance.
			visitSensor(closestSensor);
			
			// The drone's current position is updated.
			currentPosition = drone.getPosition();
		}

		// Move the drone back to the start if it is not already close enough
		if (PointUtils.findDistanceBetween(currentPosition, startPosition) >= 0.0003) {
			returnDrone(startPosition);
		}
		
		// Write the flightpath txt file for the day.
		var flightPathFile = "flightpath-" + day + "-" + month + "-" + year
				+ ".txt";
		writeFlightPath(flightPathFile);
		
		// Write the geojson readings file for the day which contains the
		// path of the drone and the updated sensor readings.
		var readingsFile = "readings-" + day + "-" + month + "-" + year
				+ ".geojson";
		writeReadings(readingsFile);
	}
	
	// Uses AStarUtils' pathfinding function, findBestPath, to move the drone 
	// along the optimal path to a sensor and takes the sensor's readings.
	private void visitSensor(Sensor sensor) {
		
		var currentPosition = drone.getPosition();
		// First check that the drone is allowed to make moves.
		if (drone.getMoveAllowance() > 0) {
			List<Point> moves = 
					AStarUtils.findBestPath(currentPosition, 
							sensor.getPosition(), getNoFlyLineSegments(), 
							0.0002);
			for (int j = 0; j < moves.size() - 1; j++) {
				// The drone should not perform any more moves if it has already
				// reached its move limit.
				if (drone.getMoveAllowance() <= 0) {
					// Makes sure that the appropriate lines representing the 
					// drone's moves are added to the featureList for the 
					// geojson file later.
					moves = moves.subList(0,j);
					break;
				}
				
				// Since the first point in moves will be the drone's current 
				// position
				moveDrone(moves.get(j+1)); 
				
				if (PointUtils.findDistanceBetween(moves.get(j+1),
						sensor.getPosition()) >= 0.0002) {
					// if the next move doesnt bring the drone in range of the 
					// target sensor, the drone will not read a sensor. 
					droneDontRead();
				}
				else {
					droneRead(sensor); 
					// otherwise the drone is in range of the sensor and can 
					// take it's readings.
				}
			}
			var moveLines = LineString.fromLngLats(moves);
			featureList.add(Feature.fromGeometry((Geometry)moveLines));	
		}
	}
	
	// Returns the drone from its current position to the point from where it
	// was launched, using AStarUtils' pathfinding function, findBestPath, to 
	// find the path back
	private void returnDrone(Point startPosition) {
		var currentPosition = drone.getPosition();

		if (drone.getMoveAllowance() > 0) {
			List<Point> returnMoves = 
					AStarUtils.findBestPath(currentPosition, startPosition, 
							getNoFlyLineSegments(), 0.0003);
			for (int i = 0; i < returnMoves.size() - 1; i++) {
				// The drone should not perform any more moves if it has already
				// reached its move limit.
				if (drone.getMoveAllowance() <= 0) {
					// Makes sure that the appropriate lines representing the 
					// drone's moves are added to the featureList for the 
					// geojson file later.
					returnMoves = returnMoves.subList(0,i); 
					System.out.println("Flightpath runs out of moves on date "
							+ "DD/MM/YY: " + day + month + year + " - Cant "
							+ "return to start point.");
					break;
				}
				// Since the first point in returnMoves is the current position.
				moveDrone(returnMoves.get(i+1));
				
				droneDontRead();// we have read all sensors at this point
			}
			var moveLines = LineString.fromLngLats(returnMoves);
			featureList.add(Feature.fromGeometry((Geometry)moveLines));
		}
	}
	
	// Used to access the web server and return files as strings
	public static String getResponseBody(String urlString) {
		var responseString = "";
		
		var client = HttpClient.newHttpClient();
		var request = 
				HttpRequest.newBuilder().uri(URI.create(urlString)).build();

		try {
			var response = client.send(request, BodyHandlers.ofString());
			responseString = response.body();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		
		return responseString;
	}

	// Used for testing while optimizing some code
	public String getFlightPathString() {
		return flightPathString;
	}
	
	public static void main(String[] args) {
		// Setting up confinement area
		var point1 = Point.fromLngLat(-3.192473, 55.946233);
		var point2 = Point.fromLngLat(-3.184319,55.946233);
		var point3 = Point.fromLngLat(-3.184319, 55.942617);
		var point4 = Point.fromLngLat(-3.192473, 55.942617);
		var point5 = Point.fromLngLat(-3.192473, 55.946233);
		var pointList = new ArrayList<Point>();
		pointList.add(point1);
		pointList.add(point2);
		pointList.add(point3);
		pointList.add(point4);
		pointList.add(point5);
		var confinementArea = LineString.fromLngLats(pointList);		
		
		var day = args[0];
		var month = args[1];
		var year = args[2];
		
		var launchPosition = 
				Point.fromLngLat(Double.parseDouble(args[4]), 
						Double.parseDouble(args[3]));
		
		var webServerPort = Integer.parseInt(args[5]);
		
		var drone = new Drone(launchPosition, 150);
		
		var droneController = 
				new DroneController(confinementArea, year, month, day, drone, 
						webServerPort);
		
		droneController.greedyFlightPath();
		
		
	
	}
}
