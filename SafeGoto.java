/** 
 * SafeGoto.java
 * 
 * Attempts to avoid objects while going to a goal using potential field analysis.
 *
 * @author Eitan Romanoff
 */

import javaclient3.*;

public class SafeGoto {

    @SuppressWarnings("deprecation")
	public boolean move(SonarInterface sonar, Point pos, Point nextDest, Position2DInterface robot) {
		RoboGuide guide = new RoboGuide(pos);

		double[] sonars = Navigator.rangerToArr();
		
		// GuidePoints greater than this are thrown away.
		double GOAL_WEIGHT = 2.0;
		double OBJECT_WEIGHT = 1.0;
		
		// also keep track of how many recorded samples you have. This will modify the coefficient.
		int num_samples = 0;
		
		// add up all the object vectors
		double sum_x = 0;
		double sum_y = 0;
		
		/*for (int i = 0; i < sonars.length; i++) {
			double sensor_d = sonars[i];
			if (sensor_d > 1.0 || sensor_d == 0.0) {
				continue;
			}
			num_samples++;
			double angle_to_object = guide.getAngleSonar(i, sonars.length);
			double obj_x = sensor_d * Math.cos(angle_to_object);
			double obj_y = sensor_d * Math.sin(angle_to_object);
			
			// this adjusts these values to be inverse to the distance to object
			sum_x = sum_x - (obj_x / (obj_x * obj_x + obj_y * obj_y));
			sum_y = sum_y - (obj_y / (obj_x * obj_x + obj_y * obj_y));
		}
		*/
		
		double goal_vector_x = (nextDest.x - pos.x) / guide.distanceTo(nextDest);
		double goal_vector_y = (nextDest.y - pos.y) / guide.distanceTo(nextDest);
		
		double final_vector_x = (GOAL_WEIGHT * goal_vector_x) + (OBJECT_WEIGHT * sum_x);
		double final_vector_y = (GOAL_WEIGHT * goal_vector_y) + (OBJECT_WEIGHT * sum_y);
		
		double dYaw = Math.atan2(final_vector_y, final_vector_x);
		dYaw = guide.angleCorrection(dYaw - pos.yaw);
		
		// g_angle is what we want the yaw to be (global scale)
		
		double speed = 0, turn = guide.TURN_MEDIUM;
		if(sonars[3] + sonars[4] < 1.0) {
			// Try not to crash into things
			speed = 0;
		} else if (Math.abs(dYaw) < (guide.PI / 32)){
			speed = guide.FWD_FAST;
			turn = 0;
		} else if (Math.abs(dYaw) < (guide.PI / 4)) {
			speed = guide.FWD_MEDIUM;
		} else if (Math.abs(dYaw) < (guide.PI / 2)) {
			speed = guide.FWD_SLOW;
		}
		
		System.out.println(dYaw);
		if(dYaw < 0) {
			turn = -turn;
		}
		
		robot.setSpeed(speed, turn);
		
		double dX = nextDest.x - pos.x;
		double dY = nextDest.y - pos.y;
		if (Math.abs(dX) < .25 && Math.abs(dY) < .25) {
			return true;
		} else {
			return false;
		}
	}
}
