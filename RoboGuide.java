public class RoboGuide {

	public final double PI = Math.PI;
	public final double NEGPI = -1 * Math.PI;
	public final double TURN_SLOW = 10*Math.PI/180;
	public final double TURN_MEDIUM = 20*Math.PI/180;
	public final double TURN_FAST = 30*Math.PI/180;
	public final double FWD_SLOW = 0.05;
	public final double FWD_MEDIUM = 0.1;
	public final double FWD_FAST = 0.2;
	public final double LINEUP_THRESHOLD = 0.05;
		
	Point pos;
	
	public RoboGuide(Point pos) {
		this.pos = pos;
	}
	
	public boolean GoTo(GuidePoint p) {
		return false;
	}


	public double distanceTo(double x, double y) {
		return distanceTo(new GuidePoint(x, y));
	}

	public double distanceTo(GuidePoint p) {
		double a = Math.pow(p.getX() - pos.x, 2);
		double b = Math.pow(p.getY() - pos.y, 2);
		return Math.sqrt( (a + b) );
	}
	
	public double getAngleSonar(int index, int size) {
		double angle = 90 - (180/size) * index;
		return (angle * PI/180);
	}

	public double angleCorrection(double angle) {
		if (angle > PI) {
			angle = angle - (PI * 2);
		} else if (angle < NEGPI) {
			angle = angle + (PI * 2);
		}
		return angle;
	}
	
	public double[] collect_laser_samples(double[] ranges, int num_samples) {
		int sample_index = 0; //index for sampling
		double dps = 240.0/ranges.length; //degrees per sample
		double[] samples = new double[num_samples]; //sample array
		int end_index = (int)(30.0/dps); //normally you'd start at 0 to 240, but this is here...
		int start_index = (int)(210.0/dps); //...because we're going backwards from -90 to 90		
		int gap = (int)((180.0/(num_samples-1))/dps); //index gap between sensors.
		
		for (int i = start_index; i >= end_index; i = i - gap){
			int num_avg_samples = 5; // we'll take an average of 5 samples
			double avg = 0.0;
			
			for (int j = 0; j < num_avg_samples; j++){
				avg = avg + ranges[i+j];
			}
			avg = avg / num_avg_samples;
			samples[sample_index] = avg;
			sample_index++;
		}
		
		return samples;
	}
}
