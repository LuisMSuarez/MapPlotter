import java.awt.Image;
import java.awt.Point;

public class PlotterMap {
  private double xratio;
  private double yratio;
  private double zeroLat,maxLat,minLat;
  private double zeroLon,maxLon,minLon;
  private double area;
  private int w;
  private int h;
  private String mapName;
  private Image mapImage;
  private String mapFileName;

  public PlotterMap() {
    xratio=0;
    yratio=0;
    zeroLat=0;
    zeroLon=0;
  }

  public PlotterMap(int pxWidth, int pxHeight, int pxXstart, int pxYstart, int pxFullWidth, int pxFullHeight, String mapName, String fileName) {
    xratio=(double)(pxFullWidth/(double)360.0);
    yratio=(double)(pxFullHeight/(double)180.0);

    zeroLon= (double)(-180.0) + (double)((pxXstart+(pxWidth/2.0))/xratio);
    zeroLat= (double)(90.0) - (double)((pxYstart+(pxHeight/2.0))/yratio);

    maxLon = zeroLon + (pxWidth/2.0)/xratio;
    minLon = zeroLon - (pxWidth/2.0)/xratio;
    maxLat = zeroLat + (pxHeight/2.0)/yratio;
    minLat = zeroLat - (pxHeight/2.0)/yratio;

    area = (maxLon-minLon)*(maxLat-minLat);

    this.mapName=mapName;
    mapFileName=fileName;
    w = pxWidth;
    h = pxHeight;
    mapImage=null;
    //System.out.println("map " + mapName + "xratio:" + xratio + " yratio " + yratio + " zeroLat " + zeroLat + " zeroLon " + zeroLon);
  }

  public void setImage(Image img) {    mapImage=img;}
  public Image getMapImage() {    return mapImage;}
  public String getMapName() {    return mapName;}
  public String getMapFile() {    return mapFileName;}
  public double getArea() {    return area;}

  public boolean plot(double lat, double lon, Point p) {
    double x = (double)(xratio*(lon-zeroLon)+w/2);
    double y = (double)(h/2-yratio*(lat-zeroLat));

    p.x = (int)x;
    p.y = (int)y;

    return ((minLat<=lat)&&(lat<=maxLat)&&(minLon<=lon)&&(lon<=maxLon));
  }
}
