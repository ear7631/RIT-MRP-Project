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
	static final double tolerance = 0.25;
    static final double[] sonar_yaw = {90, 50, 30, 10, -10, -30, -50, -90};

    // Number of points in particle cloud
    static final int K = 100;
    static final Map map = new Map("map.png");
    static final Random rand = new Random();
    static final double PROB_THRESHOLD = 0.005;

    public static void main(String[] args) {
		String filename;

		if (args.length == 3) {
			pc = new PlayerClient(args[0], Integer.parseInt(args[1]));
			filename = args[2];
		} else {
			System.out.println("Usage: java Navigator <hostname> <port> <points-file>");
			return;
		}

		Scanner fileReader = null;
		try {
			fileReader = new Scanner(new File(filename));
		} catch(FileNotFoundException e) {
			System.out.printf("Input file %s not found\n", filename);
			return;
		}

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
        
		pos = pc.requestInterfacePosition2D(0,PlayerConstants.PLAYER_OPEN_MODE);
        sonar = pc.requestInterfaceSonar(0,PlayerConstants.PLAYER_OPEN_MODE);
        pos.setMotorPower(1);

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

        pc.readAll();
        if (!sonar.isDataReady()) {
            System.out.println("Waiting on sonar");
            return;
        }

        float[] ranges = sonar.getData().getRanges();
        
        double leftval =  (ranges[1]+ranges[2]) / 2.0;
        double frontval = (ranges[3]+ranges[4]) / 2.0;
        double rightval = (ranges[5]+ranges[6]) / 2.0;

        if (frontval < 0.5) {
            // Oh god we're going to crash
            fwd = 0;
            if (leftval < rightval) {
                //Turn to the right
                turnrate = -1*omega;
            } else {
                // Turn to the left
                turnrate = omega;
            }
        } else {
            fwd = 0.25;
            if (leftval < 1.0) {
                turnrate = -1*omega;
            } else if (rightval < 1.0) {
                turnrate = omega;
            }
        }
        
        pos.setSpeed(fwd,turnrate);
    }

    private static Point whereAreWe(LinkedList<Point> distribution) {
        double prob_tot = 0;
        for(Point p : distribution) {
            //scale likelihood to map.. somehow
            if(p.prob < PROB_THRESHOLD) {
                distribution.remove(p);
            } else {
                prob_tot += p.prob;
            }
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

        // Add the new points and scale again
        for(Point p : additions) {
            prob_tot += p.prob;
            distribution.add(p);
        }
        for(Point p : distribution) {
            p.prob /= prob_tot;
        }

        // Return a guess if we have one, otherwise null
        return null;
    }
}

