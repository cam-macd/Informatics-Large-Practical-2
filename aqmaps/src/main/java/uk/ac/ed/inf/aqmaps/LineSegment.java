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
			// Line Segments have booleans "isVertical" so that there aren't
			// problems with setting the undefined gradient for a vertical line
			isVertical = true;
		}
		else {
			isVertical = false;
			gradient = (endPoint2.latitude() - endPoint1.latitude()) / (endPoint2.longitude() - endPoint1.longitude());
			yIntercept = endPoint1.latitude() - gradient * endPoint1.longitude();// y = mx + c => c = y - mx
		}
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
	
	// Returns true if the two line segments intersect and false otherwise.
	public Boolean intersectsWith(LineSegment otherLine) {
		// First case: where both line segments are vertical
		if (isVertical == true && otherLine.isVertical()) {
			return this.doEndPointsOverlap(otherLine);
		}
		
		// Second case: where this line segment is vertical and the other line
		// segment is not.
		else if (isVertical && !otherLine.isVertical()) {
			var thisX = endPoint1.longitude();
			// yIntersectionPoint is the y coordinate of the other line segment,
			// considered as a line, at the x coordinate of this vertical line 
			// segment.
			var yIntersectionPoint = 
					otherLine.getGradient() * thisX + otherLine.getYIntercept();
			
			if (this.isPointOnLineSegment(thisX, yIntersectionPoint) && 
					otherLine.isPointOnLineSegment(thisX, yIntersectionPoint)){
				// The intersection point lies within endpoints of both line
				// segments so the lines intersect.
				return true; 
			}
			// The intersection point does not lie within endpoints of both line
			// segments so the lines intersect.
			else return false; 
		}
		
		// Third case: where this line segment is not vertical but the other 
		// line segment is.
		else if (!isVertical && otherLine.isVertical()) {
			var otherX = otherLine.getEndPoint1().longitude();
			// yIntersectionPoint is the y coordinate of this line segment,
			// considered as a line, at the x coordinate of the other vertical 
			// line segment.
			var yIntersectionPoint = gradient * otherX + yIntercept;
			if (this.isPointOnLineSegment(otherX, yIntersectionPoint) && 
					otherLine.isPointOnLineSegment(otherX, yIntersectionPoint)){
				return true;
				// The intersection point lies within endpoints of both line
				// segments so the lines intersect.
			}
			// The intersection point does not lie within endpoints of both line
			// segments so the lines intersect.
			else return false; 
		}
		
		// Fourth case: where both lines are non vertical and so they both have
		// gradients.
		// y = m1x + c1 and y = m2x + c2 => m1x + c1 = m2x + c2
		// => m1x - m2x = c2 - c1 => (m1-m2)x = c2-c1 => x = (c2-c1)/(m1-m2)
		else {
			if (gradient == otherLine.getGradient()) {
				// Both line segments are either parallel or part of the same
				// line so they are either overlapping or not overlapping.
				return this.doEndPointsOverlap(otherLine);
			}
			// Both line segments are not parallel so if we consider them
			// both as full lines they will have a point of intersection
			// with xIntersectionPoint and yIntersectionPoint representing
			// the coordinates of that intersection.
			var xIntersectionPoint = (otherLine.getYIntercept()-yIntercept)
					/ (gradient - otherLine.getGradient());
			var yIntersectionPoint = 
					xIntersectionPoint * gradient + yIntercept;
			
			if (this.isPointOnLineSegment(xIntersectionPoint, 
					yIntersectionPoint) && otherLine.isPointOnLineSegment(
							xIntersectionPoint, yIntersectionPoint)){
				// The point of intersection lies within the endpoints of both
				// lines so the line segments intersect.
				return true;
			}
			else return false;
		}

	}
	
	// Returns true if the (x,y) point given by (lng,lat) is on this line
	// segment.
	private Boolean isPointOnLineSegment(double lng, double lat) {
		if (isVertical) {
			if ((lat <= endPoint1.latitude() && lat >= endPoint2.latitude() &&
				lng == endPoint1.longitude()) || (lat >= endPoint1.latitude() &&
				lat <= endPoint2.latitude() && lng == endPoint1.longitude())) {
				// The point lies within the endpoints of this vertical line 
				// segment.
				return true;
			}
		}
		else if ((lng <= endPoint1.longitude() && lng >= endPoint2.longitude()) 
				|| 
				(lng >= endPoint1.longitude() && lng<= endPoint2.longitude())) {
			// The x coordinate of the point lies within the x range of the
			// line segment
			if ((lat <= endPoint1.latitude() && lat >= endPoint2.latitude()) ||
			 (lat >= endPoint1.latitude() && lat <= endPoint2.latitude())){
				// The y coordinate of the point lies within the y range of the
				// line segment as well so the point is on the line segment.
				return true;
			}
		}
		
		return false;
	}
	
	// Returns true if any of this line segment's endPoints lie on the other 
	// line segment or if any of the other line segment's endPoints lie on this 
	// line segment and false otherwise.
	private Boolean doEndPointsOverlap(LineSegment otherLine) {
		// If neither of these are true then none of the endpoints of this
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
