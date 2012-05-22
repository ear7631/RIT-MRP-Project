/** 
 * Navigator
 * 
 * Oh god I've been kidnapped help...
 * 
 * @author Nathaniel Case
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;
import java.util.Stack;

//Player/Stage imports
import javaclient3.*;
import javaclient3.structures.ranger.*;
import javaclient3.structures.PlayerConstants;
import javaclient3.structures.graphics2d.*;
import javaclient3.structures.PlayerPoint2d;
import javaclient3.structures.PlayerColor;

public class Navigator {

	static SafeGoto safeGoto = new SafeGoto();
	static GridMap painter = new GridMap(2000, 700);
	static PlayerClient pc;
	static Position2DInterface pos;
	static RangerInterface laser;
	static SonarInterface sonar;
	static LinkedList<Point> distribution;
	static final double tolerance = 0.25;
    static double lastx = 0;
    static double lasty = 0;
    static double lastyaw = 0;

    // Number of points in particle cloud
    static final int K = 2000;
    static final Map map = new Map("map.png");
    static final Random rand = new Random();
    static final double PROB_THRESHOLD = 35;
    static final double LOC_THRESHOLD = 0.3;
    
    public static void main(String[] args) {
		String filename;
        painter.reset(map);

		if (args.length == 3) {
			pc = new PlayerClient(args[0], Integer.parseInt(args[1]));
			filename = args[2];
		} else {
			System.out.println("Usage: java Navigator <hostname> <port> <points-file>");
			return;
		}

		pos = pc.requestInterfacePosition2D(0, PlayerConstants.PLAYER_OPEN_MODE);
        pos.setMotorPower(1);
        while(sonar == null) {
            //laser = pc.requestInterfaceRanger(0, PlayerConstants.PLAYER_OPEN_MODE);
            sonar = pc.requestInterfaceSonar(0, PlayerConstants.PLAYER_OPEN_MODE);
        }

        LinkedList<Point> destinations = new LinkedList<Point>();
		Scanner fileReader = null;
		try {
			fileReader = new Scanner(new File(filename));
		} catch(FileNotFoundException e) {
			System.out.printf("Input file %s not found\n", filename);
			return;
		}
		
		// Get the points, convert to pixel coordinates
		while(fileReader.hasNextLine()) {
			String[] pointdata = fileReader.nextLine().split(" ");
			double x = ((Double.parseDouble(pointdata[0]) * 100) / 6.6) + 1000;
			double y = 350 - ((Double.parseDouble(pointdata[1]) * 100) / 6.6);
			destinations.add(new Point(x, y, 0, 0));
		}

        distribution = getNewDistribution();
        Planner planner = new Planner();
        
        Point offset = null;
        int rateLimit = 0;
        Point waypoint = destinations.removeFirst();
        boolean reachedWaypoint = false;
        boolean done = false;
        while(!done) {
        	
        	// If we reached a waypoint, get the next one, or finish.
        	// TODO: pause for project requirements
        	if(reachedWaypoint) {
        		reachedWaypoint = false;
        		if(destinations.isEmpty()) {
        			System.out.println("All finished reaching goals!");
        			return;
        		} else {
        			System.out.println("Destination reached, on to the next one...");
        			waypoint = destinations.removeFirst();
        		}
        	}
        	
        	// if we are not localized confidently enough...
        	if(offset == null) {
        		safeWander();
        	} else {
            	// Get the next "step" in the path with our roadmap
            	// Argument is the GOAL destination. It uses whereAreWe as the START.
            	Point localDestination = planner.nextLocalWaypoint(offset, waypoint);
            	
            	//Arguments, sonar interface, current position, goal, position2d (for movement)
            	//TODO: make it work
            	safeGoto.move(sonar, offset, localDestination, pos);
        	}

            if(rateLimit == 5) {
                //figure out where we are
                offset = whereAreWe();
                rateLimit = 0;
            }
            rateLimit++;
        }
	}

	private static boolean isTolerable(double x, double y) {
		double deltaX = Math.abs(pos.getX() - x);
		double deltaY = Math.abs(pos.getY() - y);
		return deltaX < tolerance && deltaY < tolerance;
	}

    /**
     * Wander for one tick.
     */
    private static void safeWander() {
        double turnrate = 0, fwd = 0.2;
        double omega = 20*Math.PI/180; 

        double[] ranges = rangerToArr();
        double left = (ranges[1] + ranges[2]) / 2;
        double front = (ranges[3] + ranges[4]) / 2;
        double right = (ranges[5] + ranges[6]) / 2;
        
        if (front < 0.5) {
            // Oh god we're going to crash
            fwd = 0;
            if (left < right) {
                //Turn to the right
                turnrate = -1*omega;
            } else {
                // Turn to the left
                turnrate = omega;
            }
        } else {
            fwd = 0.25;
            if (left < 1.0) {
                turnrate = -1*omega;
            } else if (right < 1.0) {
                turnrate = omega;
            }
        }
        
        pos.setSpeed(fwd,turnrate);
    }
    
    public static LinkedList<Point> getNewDistribution() {
    	LinkedList<Point> dist = new LinkedList<Point>();
        for(int i = 0; i < K; i++) {
            dist.add(new Point(K));
        }
        return dist;
    }

    public static Point whereAreWe() {
        double[] ranges = rangerToArr();
        double currx = pos.getX();
        double curry = pos.getY();
        double curryaw = pos.getYaw();

        
        LinkedList<Point> toRemove = new LinkedList<Point>();
        Point bestPoint = new Point(0, 0);
        double count = 0;
        for(Point p : distribution) {
            painter.setPixel((int)p.x, (int)p.y, 0xFFFFFFFF);
            // Randomly kill about half the points.
            if(rand.nextDouble() < 0.5) {
                toRemove.add(p);
                continue;
            }
            p.yaw += lastyaw - curryaw + rand.nextDouble() * 0.02 - 0.01;
            double[] translated = Map.robotToMap(lastx - currx, lasty - curry, p.yaw);
            p.x += translated[0] + rand.nextDouble() * 2 - 1;
            p.y += translated[1] + rand.nextDouble() * 2 - 1;

            //scale likelihood to map
            double[] readings = map.checkHere(p);
            double distance = 0;
            for(int i=0; i<readings.length; i++) {
                distance += Math.abs(readings[i] - ranges[i]);
            }
            p.prob = 40 - distance;
            if(p.prob < PROB_THRESHOLD || !map.valid(p)) {
                toRemove.add(p);
            } else if(p.prob > 39) {
                // Only average points that make the cut.
                count++;
                bestPoint.x += p.x;
                bestPoint.y += p.y;
            }
        }
        bestPoint.x /= count;
        bestPoint.y /= count;
        
        lastx = currx;
        lasty = curry;
        lastyaw = curryaw;

        distribution = scale(distribution);
        // Out of loop to avoid ConcurrentModificationException
        for(Point p : toRemove) {
            distribution.remove(p);
        }

        // Make some new points for all the ones we took out
        if(distribution.size() == 0) {
            distribution = getNewDistribution();
        }
        distribution = scale(distribution);
        while(distribution.size() < K) {
            if(rand.nextDouble() < 0.5) {
                distribution.add(new Point(K));
            }
            double temp_tot = 0;
            double target = rand.nextDouble();
            for(Point p : distribution) {
                temp_tot += p.prob;
                if(temp_tot > target) {
                    distribution.add(new Point(p));
                    break;
                }
            }
        }

        distribution = scale(distribution);
        for(Point p : distribution) {
        	// Render the distribution of particles to the gridmap
            painter.setPixel((int)p.x, (int)p.y, 0xFFFF0000);
        }

        int radius = 4;
        for(int i=-radius; i<=radius; i++) {
            for(int j=-radius; j<=radius; j++) {
                painter.setPixel((int)bestPoint.x+i, (int)bestPoint.y+j, 0xFF00FF00);
            }
        }

        painter.repaint();
        if(map.valid(bestPoint)) {
            return bestPoint;
        } else {
            return null;
        }
    }

    private static LinkedList<Point> scale(LinkedList<Point> dist) {
        double total = 0;
        for(Point p : dist) {
            total += p.prob;
        }
        for(Point p : dist) {
            p.prob /= total;
        }
        return dist;
    }

    private static double[] rangerToArr() {
        do {
            pc.readAll();
        } while(!sonar.isDataReady());

        //double[] ranges = laser.getData().getRanges();
        float[] ranges = sonar.getData().getRanges();
        double[] retVal = new double[8];
        
        /* Ranger values!
        retVal[0] = (ranges[85]+ranges[90]) / 2.0;
        retVal[1] = (ranges[592]+ranges[597]) / 2.0;
        retVal[2] = (ranges[340]+ranges[345]) / 2.0;
        */
        /* Sonar values! */
        for(int i=0; i<8; i++) {
            retVal[i] = ranges[i];
        }

        return retVal;
    }
}
