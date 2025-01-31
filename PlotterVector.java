import java.io.*;
import java.net.*;
import java.awt.*;

class PlotterVector {
  URL bvd;
  int nfeatures;
  float features[][][];
  volatile boolean mapLoaded;
  Color color;
  PlotterMap currentMap;
  int pointsx[][];
  int pointsy[][];

  public void loadFile() {
    try {
      URLConnection urlconnection = bvd.openConnection();  
      urlconnection.setDoInput(true);
      urlconnection.setUseCaches(true);
      DataInputStream datainputstream = new DataInputStream(new BufferedInputStream(urlconnection.getInputStream()));

      nfeatures=datainputstream.readShort();
      //System.out.println(urlconnection);
      //System.out.println("Features: " + nfeatures);

      features = new float[nfeatures][][];
      pointsx = new int[nfeatures][];
      pointsy = new int[nfeatures][];
      for(int feature=0;feature<nfeatures;feature++) {
        int featureVerts = datainputstream.readShort();
        //System.out.println("Feature: " + feature + " has " + featureVerts + " vertices");
        features[feature] = new float[featureVerts][2];
        pointsx[feature] = new int[featureVerts];
        pointsy[feature] = new int[featureVerts];
        for(int vert=0;vert<featureVerts;vert++) {
          features[feature][vert][0]=datainputstream.readFloat();
          features[feature][vert][1]=datainputstream.readFloat();
          //System.err.println("Feature: " + feature + "; Vertex: " + vert + "; Lat: " + features[feature][vert][0] + "; Lon: " + features[feature][vert][0]);
        }
      }    
      mapLoaded=true; 
    } 
    catch(Exception e) {
      System.out.println("Could not load boundary file");
    }  
  } 

  private void calcPoints() {
    Point p = new Point();
    for(int feature=0;feature<features.length;feature++) {
      for(int vert=0;vert<features[feature].length;vert++) {
        currentMap.plot(features[feature][vert][0],features[feature][vert][1],p);
        pointsx[feature][vert]=p.x;
        pointsy[feature][vert]=p.y;                
      }
    }  
  }

  public void paint(Graphics g,PlotterMap map) {
    if(!mapLoaded)
    return;
    if(map!=currentMap) {
      currentMap=map;
      calcPoints();
    }      
    g.setColor(color);
    for(int feature=0;feature<features.length;feature++)
    g.drawPolyline(pointsx[feature],pointsy[feature],pointsx[feature].length);
  }

  PlotterVector(URL bvd, Color plotColor) {
    this.bvd=bvd;
    mapLoaded=false;
    color=plotColor;
    currentMap=null;
  }
}
