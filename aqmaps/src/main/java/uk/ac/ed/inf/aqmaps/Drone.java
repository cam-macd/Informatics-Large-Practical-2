package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;


public class Drone {

	private Point position;
	private int moveAllowance; // number of moves the drone is allowed to make
	
	public Drone(Point position, int moveAllowance) {
		this.position = position;
		this.moveAllowance = moveAllowance;
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
			if (reading >= 0 && reading < 32) 
				sensor.setMarkerProperties("#00ff00", "lighthouse");
			else if (reading >= 32 && reading < 64)
				sensor.setMarkerProperties("#40ff00", "lighthouse");
			else if (reading >= 64 && reading < 96)
				sensor.setMarkerProperties("#80ff00", "lighthouse");
			else if (reading >= 96 && reading < 128)
				sensor.setMarkerProperties("#c0ff00", "lighthouse");
			else if (reading >= 128 && reading < 160)
				sensor.setMarkerProperties("#ffc000", "danger");
			else if (reading >= 160 && reading < 192)
				sensor.setMarkerProperties("#ff8000", "danger");
			else if (reading >= 192 && reading < 224)
				sensor.setMarkerProperties("#ff4000", "danger");
			else if (reading >= 224 && reading < 256)
				sensor.setMarkerProperties("#ff0000", "danger");
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
		double magnitude = PointUtils.findDistanceBetween(position,newPosition);
		// Due to floating point errors the magnitude cannot be compared 
		// directly with 0.0003.
		// This threshold gives less than a cm difference between magnitude and
		// the expected magnitude of the move.
		if (magnitude - 0.0003 > 0.000001) { 
			throw new IllegalArgumentException("Magnitude of the move must be"
					+ "0.0003 degrees, but was " + magnitude);
		}
		
		// angle will also be affected by floating point errors
		double angle = 
				Math.toDegrees(PointUtils.angleBetweenPoints(position, 
						newPosition));
		int roundedAngle = (int) Math.round(angle);
		int nearestTen = (int) (Math.round(angle/10) * 10);
		if (nearestTen - angle > 0.0001 || roundedAngle % 10 != 0 || angle < 0 
				|| angle > 350) {
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
