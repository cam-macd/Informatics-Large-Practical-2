package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import com.mapbox.geojson.Point;


public class AStarUtils {
	
	public static List<Point> reconstructPath(Map<Point,Point> cameFrom, 
			Point current) {
		List<Point> totalPath = new ArrayList<>();
		totalPath.add(current);
		
		while (cameFrom.containsKey(current)) {
			current = cameFrom.get(current);
			totalPath.add(0,current);
		}
		
		return totalPath;
	}	
	
	public static List<Point> aStar(Point start, Point target, 
			List<LineSegment> noFlyZones) {
		Comparator<Node> comparator = new NodeComparator();
		PriorityQueue<Node> openSet = new PriorityQueue<Node>(11,comparator);
		
		
		Map<Point, Point> cameFrom = new HashMap<>();
		
		Map<Point, Double> gScore = new HashMap<>();
		gScore.put(start,(double) 0);
		
		Map<Point, Double> fScore = new HashMap<>();
		fScore.put(start,PointUtils.findDistanceBetween(start,target));
		
		openSet.add(new Node(start,fScore.get(start)));
		
		while (openSet.peek() != null) {
			
			Node current = 
					new Node(openSet.peek().getPoint(), 
							openSet.peek().getFCost());
			if (PointUtils.findDistanceBetween(current.getPoint(),target) 
					< 0.0002 && !cameFrom.keySet().isEmpty()) {
				return reconstructPath(cameFrom, current.getPoint());
			}
			
			openSet.poll();
			
			List<Point> neighbours = 
					getNeighbours(current.getPoint(), noFlyZones);
			
			for (int i = 0; i < neighbours.size(); i++) {
				Point currentNeighbour = neighbours.get(i);
				double tentative_gScore = 
						gScore.get(current.getPoint()) + 
						PointUtils.findDistanceBetween(current.getPoint(), 
								currentNeighbour);
				
				if (gScore.containsKey(currentNeighbour)) {
					if (tentative_gScore < gScore.get(currentNeighbour)) {
						cameFrom.put(currentNeighbour,current.getPoint());
						gScore.put(currentNeighbour, tentative_gScore);
						fScore.put(currentNeighbour, tentative_gScore + 
								PointUtils.findDistanceBetween(
										currentNeighbour,target));
						Node currentNeighbourNode = 
								new Node(currentNeighbour,fScore.get(
										currentNeighbour));
						if (!openSet.contains(currentNeighbourNode)) {
							openSet.add(currentNeighbourNode);
						}
					}
				}
				else {
					cameFrom.put(currentNeighbour,current.getPoint());
					gScore.put(currentNeighbour, tentative_gScore);
					fScore.put(currentNeighbour, tentative_gScore + 
							PointUtils.findDistanceBetween(
									currentNeighbour, target));
					Node currentNeighbourNode = 
							new Node(currentNeighbour,fScore.get(
									currentNeighbour));
					if (!openSet.contains(currentNeighbourNode)) {
						openSet.add(currentNeighbourNode);
					}
				}
			}
		}
		
		return null;

	}
	
	public static List<Point> getNeighbours(Point point, 
			List<LineSegment> noFlyZones) {
		List<Point> neighbours = new ArrayList<>();
		for (int i = 0; i < 36; i++) {
			Boolean checkBool = false;
			Point possibleNeighbour = PointUtils.pointAfterMove(point, i*10);
			LineSegment possibleLine = new LineSegment(point,possibleNeighbour);
			for (int j = 0; j < noFlyZones.size(); j++) {
				if (possibleLine.intersectsWith(noFlyZones.get(j))) {
					checkBool = true;
				}
			}
			if (checkBool == false) {
				neighbours.add(possibleNeighbour);
			}
		}
		return neighbours;
	}

}