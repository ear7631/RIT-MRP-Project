public class Point {
    public int x;
    public int y;
    public double yaw;
    public double prob;

    public Point(int x, int y, double yaw, double prob) {
        this.x = x;
        this.y = y;
        this.yaw = yaw;
        this.prob = prob;
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
