import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.image.BufferedImage;
import java.awt.*;

public class GridMap extends JFrame {

    private BufferedImage theMap;
    private int imwidth, imheight;
    private double scale;
    private MapPanel mp;

    public GridMap(int width, int height) {
        imwidth = width;
        imheight = height;
        theMap = new BufferedImage(imwidth,imheight,
                                   BufferedImage.TYPE_INT_ARGB);
        int midgray = (0xff << 24) | (180 << 16) | (180 << 8) | (180);
        for (int x = 0; x < imwidth; x++)
            for (int y = 0; y < imheight; y++)
                theMap.setRGB(x,y,midgray);

        mp = new MapPanel();
        add(mp);
        this.setBounds(100, 100, imwidth + 10, imheight + 35);
        this.setVisible(true);
    }

    void setPixel(int imx, int imy, int value) {
        if (imx >= 0 && imx < imwidth && imy >= 0 && imy < imheight)
            theMap.setRGB(imx,imy,value);
    }
    
    void setLine(int srcx, int srcy, int destx, int desty) {
    	Graphics2D g2d = theMap.createGraphics();
    	g2d.setColor(Color.RED);
    	BasicStroke bs = new BasicStroke(2);
    	g2d.setStroke(bs);
    	g2d.drawLine(srcx, srcy, destx, desty);
    }
    
    void reset(Map map) {
    	for(int i = 0; i < map.height; i++) {
    		for(int j = 0; j < map.width; j++) {
    			if(map.map[i][j] == 255) {
    				setPixel(i, j, 0xFFFFFFFF);
    			} else {
    				setPixel(i, j, 0xFF000000);
    			}
    		}
    	}
    }

    void touch() { mp.repaint(); }

    class MapPanel extends JPanel {

        protected void paintComponent(Graphics g) {
            g.drawImage(theMap,0,0,null);
        }
        public Dimension getPreferredSize() {
            return new Dimension(imwidth,imheight);
        }
    }

}
