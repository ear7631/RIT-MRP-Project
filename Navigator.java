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

//Player/Stage imports
import javaclient3.*;
import javaclient3.structures.ranger.*;
import javaclient3.structures.PlayerConstants;
import javaclient3.structures.graphics2d.*;
import javaclient3.structures.PlayerPoint2d;
import javaclient3.structures.PlayerColor;

public class Navigator {

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
    static final int K = 1000;
    static final Map map = new Map("map.png");
    static final Random rand = new Random();
    static final double PROB_THRESHOLD = 38; //1.0 / (2 * K);
    static final double LOC_THRESHOLD = 0.3;
    
    public static void main(String[] args) {
		String filename;

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

		Scanner fileReader = null;
		try {
			fileReader = new Scanner(new File(filename));
		} catch(FileNotFoundException e) {
			System.out.printf("Input file %s not found\n", filename);
			return;
		}

        distribution = getNewDistribution();
        
        Point offset = null;
        int rateLimit = 0;
        while(offset == null) {
            safeWander();
            if(rateLimit == 5) {
                //figure out where we are
                Point p = whereAreWe();
                offset = p;
                rateLimit = 0;
            }
            rateLimit++;
        }
        System.out.printf("I *really* think we're at %s\n", offset);

        // Translate the offset by where we think we are now.
        offset.x -= pos.getX();
        offset.y -= pos.getY();

        //TODO: pathfind
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
        System.out.println(Arrays.toString(ranges));
        
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
        for(Point p : distribution) {
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
            }
        }
        
        lastx = currx;
        lasty = curry;
        lastyaw = curryaw;

        distribution = scale(distribution);
        System.out.printf("Removing %d particles.\n", toRemove.size());
        // Out of loop to avoid ConcurrentModificationException
        for(Point p : toRemove) {
            distribution.remove(p);
        }

        // Make some new points for all the ones we took out
        if(distribution.size() == 0) {
            distribution = getNewDistribution();
        }
        distribution = scale(distribution);
        System.out.printf("Adding %d particles back.\n", K - distribution.size());
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
        Point bestPoint = new Point(0, 0);
        painter.reset(map);
        for(Point p : distribution) {
        	// Render the distribution of particles to the gridmap
        	for(int i = 0; i < 5; i++) {
    			for(int j=0; j < 5; j++) {
    				painter.setPixel((int)p.x - (2-j), (int)p.y - (2-i), 0xFFFF0000);
    			}
    		}
            
            
            if(p.prob > bestPoint.prob) {
                bestPoint = p;
            }
        }
        for(int i = 0; i < 9; i++) {
			for(int j=0; j < 9; j++) {
				painter.setPixel((int)bestPoint.x - (4-j), (int)bestPoint.y - (4-i), 0xFF00FF00);
			}
		}
        painter.repaint();
        System.out.printf("I think we're at %s\n", bestPoint);
        /*if(bestPoint.prob > LOC_THRESHOLD) {
            System.out.println(bestPoint.prob);
            return bestPoint;
        } else {*/
            return null;
        //}
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
