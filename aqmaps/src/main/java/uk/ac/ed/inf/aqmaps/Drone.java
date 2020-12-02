package uk.ac.ed.inf.aqmaps;

import java.util.HashMap;
import java.util.Map;

import com.mapbox.geojson.Point;


public class Drone {

	private Point position;
	private int moveAllowance; // number of moves the drone is allowed to make
	// Colour map for the different ranges of sensor readings
	private Map<Integer, String> colourMap = new HashMap<>();
	// Symbol map for the different ranges of sensor readings
	private Map<Integer, String> symbolMap = new HashMap<>();
	
	
	public Drone(Point position, int moveAllowance) {
		this.position = position;
		this.moveAllowance = moveAllowance;
		setColours();
		setSymbols();
	}
	
	// If no move allowance is passed to the constructor, then moveAllowance 
	// will default to 150
	public Drone(Point position) {
		this.position = position;
		moveAllowance = 150;
	}
	
	public int getMoveAllowance() {
		return moveAllowance;
	}
	
	public Point getPosition() {
		return position;
	}
	
	// Sets the colour map
	private void setColours() {
		colourMap.put(0, "#00ff00");
		colourMap.put(1, "#40ff00");
		colourMap.put(2, "#80ff00");
		colourMap.put(3, "#c0ff00");
		colourMap.put(4, "#ffc000");
		colourMap.put(5, "#ff8000");
		colourMap.put(6, "#ff4000");
		colourMap.put(7, "#ff0000");
	}
	
	// Sets the symbol map
	private void setSymbols() {
		symbolMap.put(0, "lighthouse");
		symbolMap.put(1, "lighthouse");
		symbolMap.put(2, "lighthouse");
		symbolMap.put(3, "lighthouse");
		symbolMap.put(4, "danger");
		symbolMap.put(5, "danger");
		symbolMap.put(6, "danger");
		symbolMap.put(7, "danger");
	}
	
	// Reads the sensor and sets it's marker properties.
	// This function should only be called if the drone is within 0.0002 degrees
	// of the sensor.
	public void readSensor(Sensor sensor) {
		if (PointUtils.findDistanceBetween(position, sensor.getPosition()) >= 
				0.0002) {
			throw new IllegalStateException(
					"Distance between sensor and drone position should be less"
					+ "than 0.0002 degrees of the drone but was "
					+ PointUtils.findDistanceBetween(
							position, sensor.getPosition())
					+ " degrees.");
		}
		
		if (sensor.getBattery() >= 10) {
			double reading = sensor.getReading();
			var readingKey = (int) Math.floor(reading/32.0);
			sensor.setMarkerProperties(
					colourMap.get(readingKey), symbolMap.get(readingKey));
		}
		
		else sensor.setMarkerProperties("000000", "cross");
	}
	
	// Moves the drone to a new position.
	// This magnitude (length) of the move should be 0.0003 degrees.
	// The new position should be the result of a move at an angle of 0-350 
	// degrees (inclusive) and the angle should be a multiple of 10.
	// The drone should have a move allowance greater than 0.
	// An argument passed to this function which does not meet these 
	// requirements is an illegal argument.
	public void move(Point newPosition) {
		// magnitude is affected by floating point errors
		var magnitude = PointUtils.findDistanceBetween(position,newPosition);
		// Due to floating point errors the magnitude cannot be compared 
		// directly with 0.0003.
		if (Math.abs(magnitude - 0.0003) > 1*Math.pow(10,-14)) { 
			throw new IllegalArgumentException("Magnitude of the move must be"
					+ "0.0003 degrees, but was " + magnitude);
		}
		
		// angle will also be affected by floating point errors
		var angle = 
				Math.toDegrees(PointUtils.angleBetweenPoints(position, 
						newPosition));
		var roundedAngle = (int) Math.round(angle);
		var nearestTen = (int) (Math.round(angle/10) * 10);
		if (Math.abs(nearestTen - angle) > 0.00001 || 
				roundedAngle % 10 != 0 || angle < 0 || angle > 350) {
			throw new IllegalArgumentException("Angle must be a multiple of 10 "
					+ "between 0 and 350 (inclusive), but was " + angle + 
					"(unrounded)" );
		}
		
		if (moveAllowance <= 0) { 
			throw new IllegalArgumentException("Drone cannot perform a move "
					+ "unless it's move allowance is greater than 0 whereas"
					+ "move was called with moveAllowance = " + moveAllowance);
		}
		
		moveAllowance = moveAllowance - 1;
		position = newPosition;
	}


}
