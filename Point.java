public class Point {
    public int x;
    public int y;
    public double prob;

    public Point(int x, int y, double prob) {
        this.x = x;
        this.y = y;
        this.prob = prob;
    }
    public Point(Point p) {
        this.x = p.x;
        this.y = p.y;
        this.prob = p.prob;
    }
    public String toString() {
        return "(" + this.x + ", " + this.y + ")";
    }
}
