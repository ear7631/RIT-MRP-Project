import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class PathPlanner {
	
	// The scale of this map is 6.6 cm / pixel in SIMULATION.
	public static final String FILENAME = "provided_bytes.dat";
	private GridMap painter;
	private int[][] pixels;
	HashMap<Point, Point> boundaries;
	public PathPlanner() {
		boundaries = new HashMap<Point, Point>();
		painter = new GridMap(2000, 700);
		// read through the image data and plot it for displaying
		File file = new File(FILENAME);
		
		byte[] bytes = new byte[(int) file.length()];
		System.out.println(bytes.length);
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
	}
	
	public void generateRoadmap(int numNodes) {
		LinkedList<Point> vertices = new LinkedList<Point>();
		HashMap<Point, Point> edges = new HashMap<Point, Point>();
		int v = 0;
		int e = 0;
		int k = 5; // number of neighbors in map generation
		
		this.setSimulationBoundaries();
		
		Random random = new Random();
		
		System.out.println("Placing preset points...");
		LinkedList<Point> presets = getPresetPoints();
		for(Point preset : presets) {
			vertices.add(preset);
			drawSpecialPoint(preset);
		}
		
		
		System.out.println("Generating random points...");
		while( v < numNodes ) {
			boolean goodPoint = false;
			while(!goodPoint) {
				Point q = generateRandomPoint(random);
				
				if (isCollisionFree(q)) {
					//System.out.println("Generated collision-free point: (" + (q.x) + ", " + (q.y) +")!");
					drawPoint(q);
					vertices.add(q);
					v++;
					goodPoint = true;
				}
			}
		}
		System.out.println("Generating edges...");
		for(Point q : vertices) {
			for(int i = 0; i < k; i++) {
				Point dest = findClosestNeighbor(q, vertices, edges, this.boundaries);
				if(dest == null) {
					i = k;
					continue;
				}
				edges.put(dest, q);
				edges.put(q, dest);
				drawEdge(q, dest);
				e++;
			}
		}
		System.out.println("Done!");
	}
	
	
	/**
	 * Finds the closest neightbor of a given point.
	 *   - Returns a point to which there is no edge yet, and the edge has no collision.
	 *   - Returns null if there are no candidates.
	 */
	public Point findClosestNeighbor(Point src, LinkedList<Point> candidates, 
			HashMap<Point, Point> edges, HashMap<Point, Point> boundaries) {
		
		TreeMap<Double, Point> distances = new TreeMap<Double, Point>();
		for(Point candidate : candidates) {
			if(src == candidate) {
				continue;
			}
			
			double d = Math.sqrt( 
					((candidate.x - src.x) * (candidate.x - src.x)) + 
					((candidate.y - src.y) * (candidate.y - src.y))
					);
			distances.put(d, candidate);
		}
		
		Map.Entry<Double,PathPlanner.Point> closest = distances.firstEntry();
		boolean done = false;
		while(!done) {
			// does the edge already exist?
			if( edges.get(src) == closest.getValue() || edges.get(closest.getValue()) == src ) {
				if(distances.higherEntry(closest.getKey()) == null) {
					done = true;
					closest = null;
				} else {
					closest = distances.higherEntry(closest.getKey());
				}
			} 
			// is there a collision on the edge?
			else if (lineClips(src, closest.getValue())) {
				if(distances.higherEntry(closest.getKey()) == null) {
					done = true;
					closest = null;
					System.out.println("OUT OF CANDIDATES");
				} else {
					closest = distances.higherEntry(closest.getKey());
				}
			} 
			// if the edge is sexy, we're done here
			else {
				done = true;
			}
		}
		
		if(closest == null) {
			return null;
		}
		
		return closest.getValue();
	}
	
	
	public Point generateRandomPoint(Random random) {
		int x = random.nextInt(1998);
		int y = random.nextInt(698);
		return new Point(x+1, y+1);
	}
	
	public LinkedList<Point> getPresetPoints() {
		LinkedList<Point> points = new LinkedList<Point>();
		points.add(new Point(108, 302));
		points.add(new Point(108, 244));
		points.add(new Point(147, 244));
		points.add(new Point(147, 153));
		points.add(new Point(1000, 150));
		points.add(new Point(1319, 196));
		points.add(new Point(1356, 231));
		points.add(new Point(1626, 503));
		points.add(new Point(1685, 505));
		points.add(new Point(276, 147));
		points.add(new Point(277, 497));
		points.add(new Point(159, 502));
		points.add(new Point(1127, 148));
		points.add(new Point(1634, 225));
		points.add(new Point(1672, 250));
		points.add(new Point(1129, 242));
		points.add(new Point(1129, 400));
		points.add(new Point(1128, 493));
		points.add(new Point(1421, 488));
		points.add(new Point(1616, 308));
		points.add(new Point(1435, 218));
		points.add(new Point(1423, 149));
		points.add(new Point(1316, 149));
		points.add(new Point(1621, 143));
		points.add(new Point(1316, 173));
		points.add(new Point(277, 248));
		points.add(new Point(277, 380));
		points.add(new Point(192, 499));
		points.add(new Point(394, 151));
		points.add(new Point(555, 154));
		points.add(new Point(895, 150));
		points.add(new Point(983, 496));
		points.add(new Point(405, 496));
		points.add(new Point(750, 155));
		points.add(new Point(610, 500));
		points.add(new Point(750, 500));
		points.add(new Point(270, 395));
		points.add(new Point(1275, 500));
		points.add(new Point(1440, 350));
		points.add(new Point(1235, 150));
		points.add(new Point(1530, 150));
		
		return points;
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
				System.out.println("(" + src.x + ", " + src.y + ") -- (" + dest.x + ", " + dest.y + ")  " +
						"AGAINST  (" + p.x + ", " + p.y + ") -- (" + boundaries.get(p).x + ", " + boundaries.get(p).y + ")"); 
				return true;
			}
		}
		
		return false;
	}
	
	
	public boolean isCollisionFree(Point q) {
		return pixels[q.y][q.x] == 0xFFFFFFFF;
	}
	
	
	public void refreshImage() {
		for(int i = 0; i < pixels.length; i++) {
			for(int j = 0; j < pixels[i].length; j++) {
				painter.setPixel(j, i, pixels[i][j]);
			}
		}
		painter.touch();
	}
	
	
	public void drawPoint(Point p) {
		int col = 0xFF00FF00;
		for(int i = 0; i < 7; i++) {
			for(int j=0; j < 7; j++) {
				pixels[p.y - (3 - i)][p.x - (3 - j)] = col;
			}
		}
		refreshImage();
	}
	
	public void drawSpecialPoint(Point p) {
		int col = 0xFF0000FF;
		for(int i = 0; i < 7; i++) {
			for(int j=0; j < 7; j++) {
				pixels[p.y - (3 - i)][p.x - (3 - j)] = col;
			}
		}
		refreshImage();
	}
	
	public void drawEdge(Point src, Point dest) {
		painter.setLine(src.x, src.y, dest.x, dest.y);
	}
	
	public void drawEdge(int a, int b, int c, int d) {
		painter.setLine(a, b, c, d);
	}
	
	
	public void setSimulationBoundaries() {
		HashMap<Point, Point> b = this.boundaries;
		ap(194, 110, 196, 136, b);
		ap(196, 136, 224, 136, b);
		ap(160, 185, 200, 185, b);
		ap(200, 185, 200, 174, b);
		ap(127, 151, 127, 175, b);
		ap(127, 175, 96, 175, b);
		ap(240, 176, 257, 176, b);
		ap(257, 176, 257, 190, b);
		ap(298, 187, 298, 172, b);
		ap(298, 172, 315, 172, b);
		
		ap(254, 461, 254, 472, b);
		ap(254, 472, 226, 472, b);
		ap(198, 540, 198, 524, b);
		ap(198, 524, 228, 524, b);
		ap(298, 462, 298, 477, b);
		ap(298, 477, 315, 477, b);
		
		ap(1087, 174, 1107, 174, b);
		ap(1107, 174, 1107, 187, b);
		ap(1160, 170, 1144, 170, b);
		ap(1144, 170, 1144, 186, b);
		
		ap(1109, 455, 1109, 477, b);
		ap(1109, 477, 1088, 477, b);
		ap(1147, 458, 1147, 477, b);
		ap(1147, 477, 1165, 477, b);
		
		ap(1325, 173, 1392, 173, b);
		ap(1392, 173, 1415, 294, b);
		ap(1495, 179, 1456, 179, b);
		ap(1456, 179, 1456, 202, b);
		
		ap(1398, 454, 1389, 475, b);
		ap(1389, 475, 1377, 477, b);
		
		ap(1653, 151, 1653, 182, b);
		ap(1653, 182, 1685, 182, b);
		ap(1660, 320, 1638, 320, b);
		ap(1638, 320, 1638, 346, b);
		ap(1651, 372, 1745, 371, b);
		ap(1643, 407, 1643, 490, b);
		ap(1643, 420, 1676, 420, b);
		ap(1658, 522, 1658, 545, b);
		
		//Big Box (renders some edges somewhat redundant)
		ap(300, 191, 300, 460, b);
		ap(300, 191, 1100, 191, b);
		ap(300, 460, 1100, 460, b);
		ap(1100, 460, 1100, 191, b);
		
		ap(1148, 172, 1305, 172, b);
		ap(1305, 172, 1305, 270, b);
		ap(1305, 270, 1410, 270, b);
		ap(1410, 270, 1390, 477, b);
		ap(1148, 172, 1148, 476, b);
		ap(1148, 476, 1390, 477, b);
		
		ap(1464, 182, 1593, 182, b);
		ap(1593, 182, 1593, 514, b);
		ap(1593, 514, 1464, 514, b);
		ap(1464, 514, 1464, 182, b);
		
		ap(202, 181, 254, 181, b);
		ap(254, 181, 254, 462, b);
		ap(254, 462, 86, 460, b);
		ap(86, 460, 86, 330, b);
		ap(86, 330, 202, 330, b);
		ap(202, 330, 202, 181, b);
		
		//Special tweaks
		ap(391, 174, 391, 475, b);
		ap(197, 516, 197, 570, b);
		
	}
	
	public void ap(int x1, int y1, int x2, int y2, HashMap<Point, Point> b) {
		//drawEdge(x1, y1, x2, y2);
		b.put(new Point(x1, y1), new Point(x2, y2));
	}
	
	
	public static void main(String args[]) {
		PathPlanner planner = new PathPlanner();
		planner.refreshImage();
		planner.generateRoadmap(80);
		planner.painter.repaint();
	}
	
	
	public class Point {
		public int x, y;
		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

}
