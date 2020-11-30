package uk.ac.ed.inf.aqmaps;

import java.util.Comparator;

public class NodeComparator implements Comparator<Node> {

	@Override
	public int compare(Node node1, Node node2) {
		if (node1.getFCost() < node2.getFCost()) {
			return -1;
		}
		if (node1.getFCost() > node2.getFCost()) {
			return 1;
		}
		return 0;
	}

}
