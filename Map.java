import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.Arrays;

public class Map {
    public int width;
    public int height;
    public int[][] map;
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
        return this.valid(p.x, p.y);
    }

    public static int[] robotToMap(double x, double y){
        int[] retVal = new int[2];
        retVal[0] = (int)x * 15;
        retVal[1] = (int)y * 15;
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
        double yaw = p.yaw - (3 * (Math.PI / 4));
        for(int i=0; i<8; i++) {
            for(int j=1; j<=75; j++) {
                x = p.x + (int)(Math.cos(p.yaw) * j);
                y = p.y + (int)(Math.sin(p.yaw) * j);
                if(!valid(x, y)) {
                    retVal[i] = (double)(j - 1) / 15;
                    break;
                } else if(j == 100) {
                    retVal[i] = 5;
                }
            }
            yaw += 3 * (Math.PI / 16);
        }
        return retVal;
    }

    public static void main(String args[]) {
        Map map = new Map("map.png");
        System.out.printf("(%d, %d)\n", map.width, map.height);
    }
}
