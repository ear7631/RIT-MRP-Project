public class Point {
    public double x;
    public double y;
    public double yaw;
    public double prob;
    
    public Point(int K) {
        this.prob = 1.0 / K;
        do {
            this.x = Navigator.rand.nextDouble() * Navigator.map.width;
            this.y = Navigator.rand.nextDouble() * Navigator.map.height;
            this.yaw = Navigator.rand.nextDouble() * (2 * Math.PI) - Math.PI;
        } while(!Navigator.map.valid((int)this.x, (int)this.y));
    }   

    public Point(double x, double y, double yaw, double prob) {
    	this.x = x;
        this.y = y;
        this.yaw = yaw;
        this.prob = prob;
    }

    public Point(int x, int y, double yaw, double prob) {
        this((double)x, (double)y, yaw, prob);
    }
    
    public Point(int x, int y) {
    	this(x, y, 0, 0);
    }
    
    public Point(Point p) {
        this(p.x, p.y, p.yaw, p.prob);
    }

    public String toString() {
        return "(" + this.x + ", " + this.y + ")";
    }
}
