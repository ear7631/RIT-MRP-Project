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

    // Number of points in particle cloud
    static final int K = 100;
    static final Map map = new Map("map.png");
    static final Random rand = new Random();
    static final double PROB_THRESHOLD = 0.005;
    static final double LOC_THRESHOLD = 0.1;

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
            System.out.println("Let's try that again...");
            //laser = pc.requestInterfaceRanger(0, PlayerConstants.PLAYER_OPEN_MODE);
            sonar = pc.requestInterfaceSonar(0, PlayerConstants.PLAYER_OPEN_MODE);
        }
        System.out.println(sonar);

		Scanner fileReader = null;
		try {
			fileReader = new Scanner(new File(filename));
		} catch(FileNotFoundException e) {
			System.out.printf("Input file %s not found\n", filename);
			return;
		}

        distribution = getNewDistribution();;
        
        Point offset = null;
        while(offset == null) {
            safeWander();
            System.out.println("Trying to find where we are...");
            //figure out where we are
            Point p = whereAreWe(distribution);
            offset = p;
        }
        System.out.printf("I think we're at %s\n", offset);

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
            System.out.println("Waiting on ranger");
            pc.readAll();
        } while(!sonar.isDataReady());

        double[] ranges = rangerToArr();
        
        if (ranges[1] < 0.5) {
            // Oh god we're going to crash
            fwd = 0;
            if (ranges[0] < ranges[2]) {
                //Turn to the right
                turnrate = -1*omega;
            } else {
                // Turn to the left
                turnrate = omega;
            }
        } else {
            fwd = 0.25;
            if (ranges[0] < 1.0) {
                turnrate = -1*omega;
            } else if (ranges[2] < 1.0) {
                turnrate = omega;
            }
        }
        
        pos.setSpeed(fwd,turnrate);
    }
    
    public static LinkedList<Point> getNewDistribution() {
    	LinkedList<Point> distribution = new LinkedList<Point>();
        for(int i = 0; i < K; i++) {
            int x = 0;
            int y = 0;
            do {
                x = (int)(rand.nextDouble() * map.width);
                y = (int)(rand.nextDouble() * map.height);
            } while(!map.valid(x, y));
            distribution.add(new Point(x, y, (double)1/K));
            //System.out.printf("(%d, %d)", x, y);
        }
        return distribution;
    }

    public static Point whereAreWe(LinkedList<Point> distribution) {
        do {
            System.out.println("Waiting on ranger to localise");
            pc.readAll();
        } while(!sonar.isDataReady());
        double[] ranges = rangerToArr();
        
        double prob_tot = 0;
        LinkedList<Point> toRemove = new LinkedList<Point>();
        for(Point p : distribution) {
            //scale likelihood to map
            int[] cardinalValues = map.checkHere(p);
            double guess = Integer.MAX_VALUE;
            for(int i=0; i<cardinalValues.length; i++) {
                double left = cardinalValues[i] - ranges[0];
                double front = cardinalValues[(i+1)%4] - ranges[1];
                double right = cardinalValues[(i+2)%4] - ranges[2];
                double current = Math.sqrt(left*left + front*front + right*right);
                if(current < guess) {
                    guess = current;
                }
            }
            prob_tot += p.prob;
        }
        for(Point p : distribution) {
            if(p.prob < PROB_THRESHOLD) {
                toRemove.add(p);
            }
        }
        System.out.printf("Removing %d particles.\n", toRemove.size());
        // Out of loop to avoid ConcurrentModificationException
        for(Point p : toRemove) {
            distribution.remove(p);
        }
        // Scale probabilities to 1
        for(Point p : distribution) {
            p.prob /= prob_tot;
        }

        // Make some new points for all the ones we took out
        LinkedList<Point> additions = new LinkedList<Point>();
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
            prob_tot += p.prob;
            distribution.add(p);
        }
        for(Point p : distribution) {
            p.prob /= prob_tot;
        }

        // Return a guess if we have one, otherwise null
        Point bestPoint = new Point(0, 0, 0);
        for(Point p : distribution) {
            if(p.prob > bestPoint.prob) {
                bestPoint = p;
            }
        }
        System.out.printf("I think we're at %s\n", bestPoint);
        if(bestPoint.prob > LOC_THRESHOLD) {
            return bestPoint;
        } else {
            return null;
        }
    }

    private static double[] rangerToArr() {
        //double[] ranges = laser.getData().getRanges();
        float[] ranges = sonar.getData().getRanges();
        double[] retval = new double[3];
        
        /* Ranger values!
        retval[0] = (ranges[85]+ranges[90]) / 2.0;
        retval[1] = (ranges[592]+ranges[597]) / 2.0;
        retval[2] = (ranges[340]+ranges[345]) / 2.0;
        */
        /* Sonar values! */
        retval[0] = (ranges[1]+ranges[2]) / 2.0;
        retval[1] = (ranges[3]+ranges[4]) / 2.0;
        retval[2] = (ranges[5]+ranges[6]) / 2.0;

        return retval;
    }
}
