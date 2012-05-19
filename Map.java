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
        return this.map[x][y] == 255;
    }

    public int[] checkHere(Point p) {
        int[] retVal = new int[3];
        int x = 0;
        int y = 0;
        double yaw = p.yaw - (Math.PI / 2);
        for(int i=0; i<3; i++) {
            for(int j=5; j<=0; j--) {
                x = (int)Math.cos(p.yaw) * j;
                y = (int)Math.sin(p.yaw) * j;
                if(valid(x, y)) {
                    retVal[i] = j;
                }
            }
            yaw += (Math.PI / 2);
        }
        return retVal;
    }

    public static void main(String args[]) {
        Map map = new Map("map.png");
        System.out.printf("(%d, %d)\n", map.width, map.height);
    }
}
