package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;

public class PointUtils {
	// Returns the Pythagorean distance between two points.
	public static double findDistanceBetween(Point point1, Point point2) {
		return Math.sqrt(Math.pow(point1.longitude() - point2.longitude(),2) + 
				Math.pow(point1.latitude() - point2.latitude(),2)); 
	}
	
	// Returns a new Point given a starting point and angle assuming the 
	// magnitude of the move is 0.0003 degrees.
	// Takes an angle in degrees as the argument, with 0 representing a move
	// east, 90 representing south, 180 representing west and 270 representing
	// north.
	public static Point pointAfterMove(Point startPoint, int angle) {
		double radiansAngle = Math.toRadians(angle);

		double newLng = startPoint.longitude() + Math.cos(radiansAngle)*0.0003;
		double newLat = startPoint.latitude() - Math.sin(radiansAngle)*0.0003;
		return Point.fromLngLat(newLng, newLat);
	}
	
	// Returns a Point with the coordinates of point1 - point2
	public static Point subtractPoints(Point point1, Point point2) {
		return Point.fromLngLat(point1.longitude() - point2.longitude(),
				point1.latitude() - point2.latitude());
	}
	
	// Returns the magnitude of a point as if it represented a vector from the
	// origin to its coordinates.
	public static double pointMagnitude(Point point) {
		return Math.sqrt(
				Math.pow(point.longitude(), 2) + Math.pow(point.latitude(), 2));
	}
	
	
	// Returns the angle (as radians) of the move from a startPoint to a 
	// newPoint assuming that the move is of length 0.0003 degrees.
	public static double angleBetweenPoints(Point startPoint, Point newPoint) {
		// eastPointOrigin is the point that the drone would end in if it moved 
		// at an angle of 0 degrees with a magnitude of 0.0003 degrees starting 
		// from the origin.
		Point eastPointOrigin = Point.fromLngLat(0.0003, 0);
		Point newPointOrigin = subtractPoints(newPoint,startPoint); // The new point relative to the origin
		double dotProduct = 
				eastPointOrigin.longitude()*newPointOrigin.longitude() 
				+ eastPointOrigin.latitude()*newPointOrigin.latitude();
		return Math.acos(dotProduct/(pointMagnitude(eastPointOrigin)*pointMagnitude(newPointOrigin)));
		// The above expression is derived from the equation of the dot product.
	}
	
}
