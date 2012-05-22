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

		float[] sonars = sonar.getData().getRanges();
		//double[] many_ranges = ranger.getData().getRanges();
		
		// GuidePoints greater than this are thrown away.
		double GOAL_WEIGHT = 1.0;
		double OBJECT_WEIGHT = 1.0;
		
		// also keep track of how many recorded samples you have. This will modify the coefficient.
		int num_samples = 0;
		
		// add up all the object vectors
		double sum_x = 0;
		double sum_y = 0;
		
		for (int i = 0; i < sonar.getData().getRanges_count(); i++) {
			float sensor_d = sonars[i];
			if (sensor_d > 1.0 || sensor_d == 0.0) {
				continue;
			}
			num_samples = num_samples + 1;
			double angle_to_object = guide.getAngleSonar(i, sonar.getData().getRanges_count());
			double obj_x = sensor_d * Math.cos(angle_to_object);
			double obj_y = sensor_d * Math.sin(angle_to_object);
			
			// this adjusts these values to be inverse to the distance to object
			sum_x = sum_x - (obj_x / (Math.pow(obj_x, 2) + Math.pow(obj_y, 2)));
			sum_y = sum_y - (obj_y / (Math.pow(obj_x, 2) + Math.pow(obj_y, 2)));
		}
		
		//double o_mag = Math.sqrt(Math.pow(sum_x, 2) + Math.pow(sum_y, 2));
		double o_angle = (Math.atan2(sum_y, sum_x) + pos.yaw);
		o_angle = guide.angleCorrection(o_angle);
		
		/*boolean tooClose = false;
		if(o_mag > 9) {
			GOAL_WEIGHT = 0;
			tooClose = true;
		}*/
			
		if (sum_x == 0 && sum_y == 0) {
			o_angle = 0;
		}
		
		double goal_vector_x = (nextDest.x - pos.x) / guide.distanceTo(nextDest);
		double goal_vector_y = (nextDest.y - pos.y) / guide.distanceTo(nextDest);
		
		double final_vector_x = (GOAL_WEIGHT * goal_vector_x) + (OBJECT_WEIGHT * sum_x);
		double final_vector_y = (GOAL_WEIGHT * goal_vector_y) + (OBJECT_WEIGHT * sum_y);
		
		double g_angle = Math.atan2(goal_vector_y, goal_vector_x);
		g_angle = guide.angleCorrection(g_angle);
		
		double f_angle = Math.atan2(final_vector_y, final_vector_x);
		f_angle = guide.angleCorrection(f_angle);
		
		// g_angle is what we want the yaw to be (global scale)
		double dYaw = f_angle - pos.yaw;
		// The distance of the vector is the magnitude
		//double final_mag = Math.sqrt(Math.pow(final_vector_x, 2) + Math.pow(final_vector_y, 2));
		
		double speed = 0;
		double turn = 0;
		// If outside a PI/16 cone, stop and turn
		if (Math.abs(dYaw) < (guide.PI / 32)){
			speed = guide.FWD_FAST;
			turn = guide.TURN_SLOW;
		} else if (Math.abs(dYaw) < (guide.PI / 4)) {
			speed = guide.FWD_FAST;
			turn = guide.TURN_SLOW;
		} else {
			speed = guide.FWD_MEDIUM;
			turn = guide.TURN_FAST;
		}
		
		dYaw = guide.angleCorrection(dYaw);
		if (dYaw < 0) {
			robot.setSpeed(speed, -1 * turn);
		} else {
			robot.setSpeed(speed, turn);
		}
		
		double dX = nextDest.x - pos.x;
		double dY = nextDest.y - pos.y;
		if (Math.abs(dX) < .25 && Math.abs(dY) < .25) {
			return true;
		} else {
			return false;
		}
	}
}
