package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;

public class Node {
	
	private Point point;
	private double fCost;
	
	public Node(Point point, double fCost) {
		this.point = point;
		this.fCost = fCost;
	}
	
	public double getFCost() {
		return fCost;
	}
	
	public Point getPoint() {
		return point;
	}
	
}