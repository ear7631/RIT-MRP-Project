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
        //System.out.printf("Calling valid on (%d, %d)\n", x, y);
        return this.map[x][y] == 255;
    }

    public int[] checkHere(Point p) {
        int[] retVal = new int[4];
        for(int i=0; i<4; i++) {
            int x = i/2==0 ? (i%2==0 ? -5 : 5) : 0;
            int y = i/2==1 ? (i%2==0 ? -5 : 5) : 0;
            while(!valid(p.x+x, p.y+y)) {
                x = x<0 ? x+1 : x>0 ? x-1 : x;
                y = y<0 ? y+1 : y>0 ? y-1 : y;
            }
            retVal[i] = Math.abs(x+y);
        }
        return retVal;
    }

    public static void main(String args[]) {
        Map map = new Map("map.png");
        System.out.printf("(%d, %d)\n", map.width, map.height);
    }
}
