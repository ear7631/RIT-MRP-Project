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

	static PlayerClient pc;
	static Position2DInterface pos;
	static RangerInterface laser;
	static SonarInterface sonar;
	static LinkedList<Point> distribution;
	static final double tolerance = 0.25;
    static final double[] sonar_yaw = {90, 50, 30, 10, -10, -30, -50, -90};
    static double lastx = 0;
    static double lasty = 0;
    static double lastyaw = 0;

    // Number of points in particle cloud
    static final int K = 100;
    static final Map map = new Map("map.png");
    static final Random rand = new Random();
    static final double PROB_THRESHOLD = 1.0 / (2 * K);
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
        while(offset == null) {
            safeWander();
            System.out.println("Trying to find where we are...");
            //figure out where we are
            Point p = whereAreWe();
            offset = p;
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

        do {
            pc.readAll();
        } while(!sonar.isDataReady());

        double[] ranges = rangerToArr();
        double left = (ranges[4] + ranges[5]) / 2;
        double front = (ranges[2] + ranges[3]) / 2;
        double right = (ranges[0] + ranges[1]) / 2;
        
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
            int x = 0;
            int y = 0;
            double yaw = 0;
            do {
                x = (int)(rand.nextDouble() * map.width);
                y = (int)(rand.nextDouble() * map.height);
                yaw = rand.nextDouble() * (2 * Math.PI) - Math.PI;
            } while(!map.valid(x, y));
            dist.add(new Point(x, y, yaw, (double)1/K));
        }
        return dist;
    }

    public static Point whereAreWe() {
        do {
            pc.readAll();
        } while(!sonar.isDataReady());
        double[] ranges = rangerToArr();
        double currx = pos.getX();
        double curry = pos.getY();
        double curryaw = pos.getYaw();
        
        LinkedList<Point> toRemove = new LinkedList<Point>();
        for(Point p : distribution) {
            int[] translated = Map.robotToMap(lastx - currx, lasty - curry);
            p.x += translated[0];
            p.y += translated[1];
            p.yaw += lastyaw - curryaw;

            //scale likelihood to map
            double[] readings = map.checkHere(p);
            double distance = 0;
            for(int i=0; i<readings.length; i++) {
                distance += Math.abs(readings[i] - ranges[i]);
            }
            p.prob = 1 - distance;
        }
        lastx = currx;
        lasty = curry;
        lastyaw = curryaw;
        distribution = scale(distribution);
        for(Point p : distribution) {
            if(p.prob < PROB_THRESHOLD || !map.valid(p)) {
                toRemove.add(p);
            }
        }
        System.out.printf("Removing %d particles.\n", toRemove.size());
        // Out of loop to avoid ConcurrentModificationException
        for(Point p : toRemove) {
            distribution.remove(p);
        }

        // Make some new points for all the ones we took out
        LinkedList<Point> additions = new LinkedList<Point>();
        if(distribution.size() == 0) {
            distribution = getNewDistribution();
        }
        distribution = scale(distribution);
        while(distribution.size() < K) {
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
        System.out.printf("Adding %d particles back.\n", additions.size());

        // Add the new points and scale again
        for(Point p : additions) {
            distribution.add(p);
        }
        distribution = scale(distribution);
        Point bestPoint = new Point(0, 0);
        for(Point p : distribution) {
            if(p.prob > bestPoint.prob) {
                bestPoint = p;
            }
        }
        System.out.printf("I think we're at %s\n", bestPoint);
        if(bestPoint.prob > LOC_THRESHOLD) {
            System.out.println(bestPoint.prob);
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
            p.prob /= total * K;
        }
        return dist;
    }

    private static double[] rangerToArr() {
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
