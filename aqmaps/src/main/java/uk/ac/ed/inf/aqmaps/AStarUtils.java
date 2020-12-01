package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import com.mapbox.geojson.Point;


public class AStarUtils {
	// Pseudocode cited in the report from Wikipedia was used in the coding
	// of the reconstructPath function. This function is used to reconstruct
	// the cheapest path found from the aStar function given cameFrom, which
	// should map each point to the previous point which it came from in the
	// cheapest known path to that point, and current, which is considered
	// the target point or a point close enough to the target.
	private static List<Point> reconstructPath(Map<Point,Point> cameFrom, 
			Point current) {
		List<Point> totalPath = new ArrayList<>();
		totalPath.add(current);
		
		while (cameFrom.containsKey(current)) {
			current = cameFrom.get(current);
			totalPath.add(0,current);
		}
		
		return totalPath;
	}	
	
	// Pseudocode cited in the report from Wikipedia was used in the coding
	// and commenting of the aStar function. The aStar function takes a start
	// Point and a target Point as well as noFlyLineSegments and returns a path
	// from the start to the target which does not intersect with any of the 
	// noFlyLineSegments. When the distance between a point and the target
	// is less than the closeEnough value, this point is considered to be
	// close enough to the target such that the point has reached the goal.
	public static List<Point> aStar(Point start, Point target, 
			List<LineSegment> noFlyLineSegments, double closeEnough) {
		Comparator<Node> comparator = new NodeComparator();
		// openSet should be the set of nodes to be expanded in the order set by
		// the priority queue - see NodeComparator for more info.
		PriorityQueue<Node> openSet = new PriorityQueue<Node>(11,comparator);
		
		// cameFrom should map each point to the previous point which it came 
		// from in the cheapest known path to that point.
		Map<Point, Point> cameFrom = new HashMap<>();
		
		// gScore should map each point to it's gScore (cheapest known path from 
		// the starting point to the point in the Map)
		Map<Point, Double> gScore = new HashMap<>();
		gScore.put(start,(double) 0);
		
		// fScore should map each point to it's fScore (gScore + heuristic
		// score of the point). Note that we use the straight line distance as
		// the heuristic function for this implementation of the A* function, so
		// the heuristic score of a point is the straight line distance from the
		// point to the target. This is also admissible as shown in the report.
		Map<Point, Double> fScore = new HashMap<>();
		fScore.put(start,PointUtils.findDistanceBetween(start,target));
		
		// Add the start Point to the openSet priority queue as a Node to allow
		// ordering in terms of fScore.
		openSet.add(new Node(start,fScore.get(start)));
		
		while (openSet.peek() != null) {
			
			// Consider the Node with the lowest fScore.
			Node current = openSet.poll();
			// If the point of the current Node is close enough to the target,
			// we have found the cheapest path to the target and so we return
			// that path.
			if (PointUtils.findDistanceBetween(current.getPoint(),target) 
					< closeEnough && !cameFrom.keySet().isEmpty()) {
				return reconstructPath(cameFrom, current.getPoint());
			}
			
			List<Point> neighbours = 
					getNeighbours(current.getPoint(), noFlyLineSegments);
			
			for (int i = 0; i < neighbours.size(); i++) {
				Point currentNeighbour = neighbours.get(i);
				// tentative_gScore is the cost of the path from the start to 
				// currentNeighbour through the Point of current.
				double tentative_gScore = 
						gScore.get(current.getPoint()) + 
						PointUtils.findDistanceBetween(current.getPoint(), 
								currentNeighbour);
				
				// if gScore already contains a path to currentNeighbour
				// we need to check if the current path has a lower gScore
				// than the existing one, in which case it is a better path, and 
				// update cameFrom, gScore, fScore and openSet if it does.
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
				// if gScore does not contain a path to currentNeighbour
				// we must update cameFrom, gScore, fScore and openSet to 
				// record this path.
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
	
	// Returns the list of points which can be visited with a single move
	// from the given point such that the line segments between each of the 
	// returned points and the given point does not intersect with any of the
	// line segments given by noFlyLineSegments.
	private static List<Point> getNeighbours(Point point, 
			List<LineSegment> noFlyLineSegments) {
		List<Point> neighbours = new ArrayList<>();
		for (int i = 0; i < 36; i++) {
			Boolean isLegalMove = true;
			Point possibleNeighbour = PointUtils.pointAfterMove(point, i*10);
			LineSegment possibleMove = new LineSegment(point,possibleNeighbour);
			for (int j = 0; j < noFlyLineSegments.size(); j++) {
				if (possibleMove.intersectsWith(noFlyLineSegments.get(j))) {
					isLegalMove = false;
				}
			}
			if (isLegalMove) {
				neighbours.add(possibleNeighbour);
			}
		}
		return neighbours;
	}

}