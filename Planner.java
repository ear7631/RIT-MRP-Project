import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Map.Entry;


public class Planner {
	
	private static boolean DEBUG = false;
	
	private HashMap<Point, HashSet<Point>> roadmap;
	private HashMap<String, Point> points;
	
	private HashMap<Point, Point> boundaries;
	
	public static final String FILENAME = "provided_bytes.dat";
	private GridMap painter;
	private int[][] pixels;
	
	public Planner() {
		this.roadmap = new HashMap<Point, HashSet<Point>>();
		this.points = new HashMap<String, Point>();
		this.boundaries = new HashMap<Point, Point>();
		this.generateRoadmapPoints();
		this.generateRoadmapEdges();
		this.setSimulationBoundaries();
		painter = new GridMap(2000, 700);
		this.painter.setVisible(false);
		
		File file = new File(FILENAME);
		
		byte[] bytes = new byte[(int) file.length()];
		try {
			InputStream input = null;
			try {
				input = new BufferedInputStream(new FileInputStream(file));
				input.read(bytes);
			} finally {
				input.close();
			}
		} catch (FileNotFoundException ex) {
		} catch (IOException ex) {
		}
		
		pixels = new int[700][2000];
		int x = 0;
		for(int i = 0; i < 700; i++) {
			for(int j = 0; j < 2000; j++) {
				if((int)bytes[x] == 0) {
					pixels[i][j] = 0xFF000000;
				} else {
					pixels[i][j] = 0xFFFFFFFF;
				}
				x++;
			}
		}
		
		for(Point p : this.points.values()) {
			this.drawPoint(p);
		}
		
		for(Point p : this.roadmap.keySet()) {
			HashSet<Point> neighbors = this.roadmap.get(p);
			for(Point neighbor : neighbors) {
				continue;//this.drawEdge(p, neighbor);
			}
		}
	}

	
	private Point findClosestRoadmapNode(Point origin) {
		// If the point is not in our roadmap, check which roadmap point is closest
		// Use that node as the neighbor
		if(this.points.values().contains(origin)) {
			return origin;
		}
		
		double minimum = Double.MAX_VALUE;
		Point neighbor = null;
		
		for(Point candidate : this.points.values()) {
			double d = Math.sqrt(
					((candidate.x - origin.x) * (candidate.x - origin.x)) + 
					((candidate.y - origin.y) * (candidate.y - origin.y)));
			
			if(d < minimum && !this.lineClips(origin, candidate)) {
				neighbor = candidate;
				minimum = d;
			}
		}
		
		// uhoh case
		if(neighbor == null) {
			minimum = Double.MAX_VALUE;
						
			for(Point candidate : this.points.values()) {
				double d = Math.sqrt(
						((candidate.x - origin.x) * (candidate.x - origin.x)) + 
						((candidate.y - origin.y) * (candidate.y - origin.y)));
				
				if(d < minimum) {
					neighbor = candidate;
					minimum = d;
				}
			}
		}
		
		return neighbor;
	}
	
	public Point nextLocalWaypoint(Point goal) {
		Point start = null;
		
		if(DEBUG) {
			System.out.println("Debug is on, using preset points.");
			start = this.generateRandomPoint(new Random());
			this.drawPoint(start, 0xFF0000FF);
			this.refreshImage();
			//start = this.point.get("G4");
			goal = this.points.get("C1");
		} else {
			start = Navigator.whereAreWe(Navigator.distribution);
			this.drawPoint(start, 0xFF0000FF);
			this.refreshImage();
		}
		
		// Create a copy of the roadmap.
		// Do this because we add in neighbors for this iteration only.
		HashMap<Point, HashSet<Point>> copymap = new HashMap<Point, HashSet<Point>>(this.roadmap);
		
		// Necessary collections for A*
		HashSet<Point> closed = new HashSet<Point>();
		HashSet<Point> open = new HashSet<Point>();
		HashMap<Point, Point> came_from = new HashMap<Point, Point>();
		HashMap<Point, Double> score = new HashMap<Point, Double>();

		
		// If the start isn't in the roadmap...
		// Add in the start node to our copied roadmap. Add in the closest node as a neighbor.
		if(!this.roadmap.keySet().contains(start)) {
			Point start_neighbor = findClosestRoadmapNode(start);
			copymap.get(start_neighbor).add(start);
			copymap.put(start, new HashSet<Point>());
			copymap.get(start).add(start_neighbor);
		}
		
		// Do the same for the end goal
		if(!this.roadmap.keySet().contains(goal)) {
			Point goal_neighbor = findClosestRoadmapNode(goal);
			copymap.put(goal, new HashSet<Point>());
			copymap.get(goal).add(goal_neighbor);
		}
		
		
		// Initialize the start node's score to 0, add it to the open set.
		score.put(start, 0.0);
		open.add(start);
		
		// Do A*
		while(!open.isEmpty()) {
			Point current = null;
			double minimum = Double.MAX_VALUE;
			for(Point candidate : open) {
				if(score.get(candidate) < minimum) {
					current = candidate;
					minimum = score.get(candidate);
				}
			}
			
			// If we reach the end, generate the path we took to get there
			if(current == goal) {
				boolean done = false;
				while(!done) {
					Point prev = came_from.get(current);
					String prevName = "";
					String currName = "";
					
					for(Entry<String, Point> entry : this.points.entrySet()) {
						if(entry.getValue() == prev) {
							prevName = entry.getKey();
						}
						if(entry.getValue() == current) {
							currName = entry.getKey();
						}
					}
					if(prev == start) {
						prevName = "Outside Map Start: " + start.toString();
					} else if(currName.equals("")) {
						currName = "Outside Map End: " + current.toString();
					}
					
					if(DEBUG){
						System.out.println(prevName + " --> " + currName);	
					}
					
					this.drawEdge(prev, current);
					if(prev == start) {
						return current;
					}
					current = prev;
				}
			}
			
			open.remove(current);
			closed.add(current);
			
			// Handle neighbors
			for(Point neighbor : copymap.get(current)) {
				if(closed.contains(neighbor)) {
					continue;
				}
				double tentative_score = score.get(current) + Math.sqrt(
						((neighbor.x - current.x) * (neighbor.x - current.x)) + 
						((neighbor.y - current.y) * (neighbor.y - current.y)));
				if(!open.contains(neighbor) || tentative_score < score.get(neighbor)) {
					open.add(neighbor);
					came_from.put(neighbor, current);
					score.put(neighbor, tentative_score);
				}
			}
		}
		
		return null;
	}
	
	public void drawPoint(Point p) {
		drawPoint(p, 0xFF00FF00);	
	}
	
	public void drawPoint(Point p, int color) {
		for(int i = 0; i < 7; i++) {
			for(int j=0; j < 7; j++) {
				pixels[p.y - (3 - i)][p.x - (3 - j)] = color;
			}
		}
	}
	
	public void drawEdge(Point src, Point dest) {
		painter.setLine(src.x, src.y, dest.x, dest.y);
	}
	
	public void refreshImage() {
		for(int i = 0; i < pixels.length; i++) {
			for(int j = 0; j < pixels[i].length; j++) {
				painter.setPixel(j, i, pixels[i][j]);
			}
		}
		painter.touch();
	}
	
	private void generateRoadmapPoints() {
		if(this.points.isEmpty()) {
			// generate points
			this.points.put("A0", new Point(163, 110));
			this.points.put("A1", new Point(167, 152));
			this.points.put("A2", new Point(146, 181));
			this.points.put("A3", new Point(150, 227));
			this.points.put("A4", new Point(108, 276));
			this.points.put("A5", new Point(230, 152));
			
			this.points.put("B0", new Point(279, 153));
			this.points.put("B1", new Point(279, 235));
			this.points.put("B2", new Point(279, 337));
			this.points.put("B3", new Point(279, 440));
			this.points.put("B4", new Point(279, 500));
			
			this.points.put("C0", new Point(198, 496));
			this.points.put("C1", new Point(165, 527));
			
			this.points.put("D0", new Point(347, 150));
			this.points.put("D1", new Point(399, 150));
			this.points.put("D2", new Point(474, 150));
			this.points.put("D3", new Point(553, 150));
			this.points.put("D4", new Point(641, 150));
			this.points.put("D5", new Point(745, 150));
			this.points.put("D6", new Point(855, 150));
			this.points.put("D7", new Point(972, 150));
			this.points.put("D8", new Point(1062, 150));
			this.points.put("D9", new Point(1129, 150));
			
			this.points.put("E0", new Point(347, 498));
			this.points.put("E1", new Point(399, 498));
			this.points.put("E2", new Point(474, 498));
			this.points.put("E3", new Point(553, 498));
			this.points.put("E4", new Point(641, 498));
			this.points.put("E5", new Point(745, 498));
			this.points.put("E6", new Point(855, 498));
			this.points.put("E7", new Point(972, 498));
			this.points.put("E8", new Point(1062, 498));
			this.points.put("E9", new Point(1129, 498));

			this.points.put("F0", new Point(1129, 235));
			this.points.put("F1", new Point(1129, 337));
			this.points.put("F2", new Point(1129, 440));
			
			this.points.put("G0", new Point(1185, 148));
			this.points.put("G1", new Point(1250, 148));
			this.points.put("G2", new Point(1315, 148));
			this.points.put("G3", new Point(1318, 185));
			this.points.put("G4", new Point(1352, 226));
			this.points.put("G5", new Point(1366, 148));
			this.points.put("G6", new Point(1424, 153));
			this.points.put("G7", new Point(1494, 145));
			this.points.put("G8", new Point(1567, 147));
			this.points.put("G9", new Point(1618, 146));
			
			this.points.put("H0", new Point(1434, 224));
			this.points.put("H1", new Point(1442, 298));
			this.points.put("H2", new Point(1441, 388));
			this.points.put("H3", new Point(1426, 491));
			this.points.put("H4", new Point(1372, 498));
			this.points.put("H5", new Point(1298, 498));
			this.points.put("H6", new Point(1204, 498));
			
			this.points.put("I0", new Point(1624, 212));
			this.points.put("I1", new Point(1692, 249));
			this.points.put("I2", new Point(1618, 305));
			this.points.put("I3", new Point(1618, 372));
			this.points.put("I4", new Point(1618, 466));
			this.points.put("I5", new Point(1631, 503));
			this.points.put("I6", new Point(1680, 503));
			this.points.put("I7", new Point(1734, 457));
			this.points.put("I8", new Point(1724, 560));
		} else {
			this.points.clear();
			generateRoadmapPoints();
		};
	}

	private void generateRoadmapEdges() {
		this.addBothWays("A0", "A1");
		this.addBothWays("A1", "A5");
		this.addBothWays("A1", "A2");
		this.addBothWays("A2", "A3");
		this.addBothWays("A3", "A4");
		this.addBothWays("A5", "B0");
		
		this.addBothWays("B0", "B1");
		this.addBothWays("B0", "D0");
		this.addBothWays("B1", "B2");
		this.addBothWays("B2", "B3");
		this.addBothWays("B3", "B4");
		this.addBothWays("B4", "C0");
		this.addBothWays("B4", "E0");
		
		this.addBothWays("C0", "C1");
		
		this.addBothWays("D0", "D1");
		this.addBothWays("D1", "D2");
		this.addBothWays("D2", "D3");
		this.addBothWays("D3", "D4");
		this.addBothWays("D4", "D5");
		this.addBothWays("D5", "D6");
		this.addBothWays("D6", "D7");
		this.addBothWays("D7", "D8");
		this.addBothWays("D8", "D9");
		this.addBothWays("D9", "F0");
		this.addBothWays("D9", "G0");
		
		this.addBothWays("E0", "E1");
		this.addBothWays("E1", "E2");
		this.addBothWays("E2", "E3");
		this.addBothWays("E3", "E4");
		this.addBothWays("E4", "E5");
		this.addBothWays("E5", "E6");
		this.addBothWays("E6", "E7");
		this.addBothWays("E7", "E8");
		this.addBothWays("E8", "E9");
		this.addBothWays("E9", "F2");
		this.addBothWays("E9", "H6");
		
		this.addBothWays("F0", "F1");
		this.addBothWays("F1", "F2");
		
		this.addBothWays("G0", "G1");
		this.addBothWays("G1", "G2");
		this.addBothWays("G2", "G3");
		this.addBothWays("G2", "G5");
		this.addBothWays("G3", "G4");
		this.addBothWays("G5", "G6");
		this.addBothWays("G6", "H0");
		this.addBothWays("G6", "G7");
		this.addBothWays("G7", "G8");
		this.addBothWays("G8", "G9");
		this.addBothWays("G9", "I0");
		
		this.addBothWays("H0", "H1");
		this.addBothWays("H1", "H2");
		this.addBothWays("H2", "H3");
		this.addBothWays("H3", "H4");
		this.addBothWays("H4", "H5");
		this.addBothWays("H5", "H6");
	
		this.addBothWays("I0", "I1");
		this.addBothWays("I0", "I2");
		this.addBothWays("I1", "I2");
		this.addBothWays("I2", "I3");
		this.addBothWays("I3", "I4");
		this.addBothWays("I4", "I5");
		this.addBothWays("I5", "I6");
		this.addBothWays("I6", "I7");
		this.addBothWays("I6", "I8");
		this.addBothWays("I7", "I8");
	}
	
	private void addBothWays(String p1, String p2) {
		if(this.roadmap.get(this.points.get(p1)) == null) {
			this.roadmap.put(this.points.get(p1), new HashSet<Point>());
		}
		if(this.roadmap.get(this.points.get(p2)) == null) {
			this.roadmap.put(this.points.get(p2), new HashSet<Point>());
		}
		
		this.roadmap.get(this.points.get(p1)).add(this.points.get(p2));
		this.roadmap.get(this.points.get(p2)).add(this.points.get(p1));
	}

	private void reveal() {
		this.painter.setVisible(true);
		this.painter.repaint();
	}

	public void setSimulationBoundaries() {
		HashMap<Point, Point> b = this.boundaries;
		addBoundaryEdge(335, 325, 1075, 325, b);
		addBoundaryEdge(226, 192, 226, 440, b);
		addBoundaryEdge(1167, 205, 1282, 205, b);
		addBoundaryEdge(1345, 172, 1387, 172, b);
		addBoundaryEdge(1387, 172, 1417, 337, b);
		addBoundaryEdge(1525, 198, 1525, 580, b);
		addBoundaryEdge(1650, 340, 1650, 457, b);
		addBoundaryEdge(1650, 372, 1760, 372, b);
		
	}
	
	public void addBoundaryEdge(int x1, int y1, int x2, int y2, HashMap<Point, Point> b) {
		//drawEdge(x1, y1, x2, y2);
		b.put(new Point(x1, y1), new Point(x2, y2));
	}
	
	public Point generateRandomPoint(Random random) {
		boolean done = false;
		int x = 0;
		int y = 0;
		while(!done) {
			x = random.nextInt(1998);
			y = random.nextInt(698);
			
			if(this.pixels[y][x] != 0xFFFFFFFF) {
				continue;
			}
			done = true;
		}
		
		return new Point(x, y);
	}
	
	/**
	 * Returns true if the proposed line intersects with a set of lines in a given map.
	 */
	public boolean lineClips(Point src, Point dest) {
		
		for(Point p : this.boundaries.keySet()) {
			boolean intersects = java.awt.geom.Line2D.linesIntersect(
					src.x, src.y, dest.x, dest.y, 
					p.x, p.y, boundaries.get(p).x, boundaries.get(p).y);
			if(intersects) { 
				return true;
			}
		}
		
		return false;
	}
	
	
	public static void main(String args[]) {
		// Run this function if you want to see the roadmap.
		Planner planner = new Planner();
		Planner.DEBUG = true;
		planner.refreshImage();
		System.out.println("Next roadmap point to go to: " + planner.nextLocalWaypoint(null));
		planner.reveal();
	}

}
