package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;


public class LineSegment {

	private Boolean isVertical;
	private double gradient;
	private double yIntercept;
	private Point endPoint1;
	private Point endPoint2;
	
	public LineSegment(Point endPoint1, Point endPoint2) {
		this.endPoint1 = endPoint1;
		this.endPoint2 = endPoint2;
		if (endPoint1.longitude() == endPoint2.longitude()) {
			isVertical = true;
		}
		else {
			isVertical = false;
			gradient = (endPoint2.latitude() - endPoint1.latitude()) / (endPoint2.longitude() - endPoint1.longitude());
		}
		yIntercept = endPoint1.latitude() - gradient * endPoint1.longitude();// y = mx + c => c = y - mx
	}
	
	private Boolean isVertical() {
		return isVertical;
	}
	
	private Point getEndPoint1() {
		return endPoint1;
	}
	
	private Point getEndPoint2() {
		return endPoint2;
	}
	
	private double getGradient() {
		return gradient;
	}
	
	private double getYIntercept() {
		return yIntercept;
	}
	
	public Boolean intersectsWith(LineSegment otherLine) {
		
		if (isVertical == true && otherLine.isVertical()) {
			if (endPoint1.longitude() == otherLine.getEndPoint1().longitude()) {
				return true; // Lines are both vertical and overlapping
			}
			else return false; // Both are vertical lines and so they are parallel and dont intersect
		}
		// Two cases where one of the lines are vertical
		else if (isVertical && !otherLine.isVertical()) {
			double thisX = endPoint1.longitude();
			double yIntersectionPoint = 
					otherLine.getGradient() * thisX + otherLine.getYIntercept();
			if (this.isPointOnLineSegment(thisX, yIntersectionPoint) && 
					otherLine.isPointOnLineSegment(thisX, yIntersectionPoint)){
				return true; // Intersection lies within endpoints of the line segment.
			}
			else return false; 
		}
		else if (!isVertical && otherLine.isVertical()) {
			double otherX = otherLine.getEndPoint1().longitude();
			double yIntersectionPoint = gradient * otherX + yIntercept;
			if (this.isPointOnLineSegment(otherX, yIntersectionPoint) && 
					otherLine.isPointOnLineSegment(otherX, yIntersectionPoint)){
				return true; // Intersection lies within endpoints of the line segment.
			}
			else return false; 
		}
		// The final case is that both lines are non vertical and so they both
		// have gradients.
		// y = m1x + c1 and y = m2x + c2 => m1x + c1 = m2x + c2
		// => m1x - m2x = c2 - c1 => (m1-m2)x = c2-c1 => x = (c2-c1)/(m1-m2)
		else {
			if (gradient == otherLine.getGradient() && 
					yIntercept == otherLine.getYIntercept()) {
				return this.doEndPointsOverlap(otherLine);
			}
			if (gradient == otherLine.getGradient() && 
					yIntercept != otherLine.getYIntercept()) {
				return false; // Parallel Lines never intersect
			}
			double xIntersectionPoint = (otherLine.getYIntercept()-yIntercept)
					/ (gradient - otherLine.getGradient());
			double yIntersectionPoint = 
					xIntersectionPoint * gradient + yIntercept;
			if (this.isPointOnLineSegment(xIntersectionPoint, 
					yIntersectionPoint) && otherLine.isPointOnLineSegment(
							xIntersectionPoint, yIntersectionPoint)){
				return true; // Intersection lies within endpoints of the line segment.
			}
			else return false; 
		}

	}
	
	private Boolean isPointOnLineSegment(double lng, double lat) {
		if (isVertical) {
			if ((lat <= endPoint1.latitude() && lat >= endPoint2.latitude()) ||
			 (lat >= endPoint1.latitude() && lat <= endPoint2.latitude())) {
				return true;
			}
		}
		else if ((lng <= endPoint1.longitude() && lng >= endPoint2.longitude()) 
				|| 
				(lng >= endPoint1.longitude() && lng<= endPoint2.longitude())) {
			if ((lat <= endPoint1.latitude() && lat >= endPoint2.latitude()) ||
			 (lat >= endPoint1.latitude() && lat <= endPoint2.latitude())){
				return true;
			}
		}
		
		return false;
	}
	
	// returns True if any of this line's endPoints lie on the other line segment
	// and false otherwise.
	private Boolean doEndPointsOverlap(LineSegment otherLine) {
		// If neither of these are true then the none of the endpoints of this
		// Line Segment lie on otherLine
		if (otherLine.isPointOnLineSegment(endPoint1.longitude(), 
				endPoint1.latitude()) ||
				otherLine.isPointOnLineSegment(endPoint2.longitude(), 
						endPoint2.latitude()) ||
				this.isPointOnLineSegment(otherLine.getEndPoint1().longitude(), 
						otherLine.getEndPoint1().latitude()) ||
				this.isPointOnLineSegment(otherLine.getEndPoint2().longitude(), 
						otherLine.getEndPoint2().latitude())) {
			return true;
		}
		return false;
	}
	
}
