import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.Arrays;

public class Map {
    public int width;
    public int height;
    public int[][] map;
    final double[] sonar_yaw = {90, 50, 30, 10, -10, -30, -50, -90};
    public Map(String image_file) {
        // Read map
        Raster data = null;
        try {
            data = ImageIO.read(new File(image_file)).getData();
        } catch(IOException e) {
            System.out.println("oh noesss!");
            System.exit(1);
        }

        // Set size
        this.width = data.getWidth();
        this.height = data.getHeight();

        // convert to 2d bitmatrix
        int[] buffer = new int[this.height];
        map = new int[this.width][this.height];
        for(int i = 0; i < this.width; i++) {
            map[i] = data.getPixels(i, 0, 1, this.height, buffer).clone();
        }
    }

    public boolean valid(int x, int y) {
        try {
            return this.map[x][y] == 255;
        } catch(ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    public boolean valid(Point p) {
        return this.valid((int)p.x, (int)p.y);
    }

    public static double[] robotToMap(double x, double y, double yaw){
        double[] retVal = new double[2];
        double d = Math.sqrt(x*x + y*y);
        retVal[0] = Math.cos(yaw) * d * 15;
        retVal[1] = Math.sin(yaw) * d * 15;
        return retVal;
    }
    public static double[] mapToRobot(int x, int y){
        double[] retVal = new double[2];
        retVal[0] = (double)x / 15;
        retVal[1] = (double)y / 15;
        return retVal;
    }

    public double[] checkHere(Point p) {
        double[] retVal = new double[8];
        int x = 0;
        int y = 0;
        double yaw;
        for(int i=0; i<8; i++) {
            yaw = p.yaw + Math.toRadians(sonar_yaw[i]);
            for(int j=1; j<=75; j++) {
                x = (int)p.x + (int)(Math.cos(yaw) * j);
                y = (int)p.y + (int)(Math.sin(yaw) * j);
                if(!valid(x, y)) {
                    retVal[i] = (double)(j - 1) / 15;
                    break;
                } else if(j == 75) {
                    retVal[i] = 5;
                }
            }
        }
        return retVal;
    }

    public static void main(String args[]) {
        Map map = new Map("map.png");
        System.out.printf("(%d, %d)\n", map.width, map.height);
    }
}
