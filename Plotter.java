import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

public class Plotter extends JApplet implements Runnable {

  private volatile Thread plotterThread = null;
  private volatile boolean stopThread;
  private volatile boolean painting=false;

  // pre-loading code in a thread context
  public void run() {
    MediaTracker mediatracker = new MediaTracker(this);
    for(int i=1;i<maps.length;i++) {
      if(stopThread) return;
      showStatus("loading map " + i + "/" + (maps.length-1));
      maps[i].setImage(getImage(getClass().getResource(maps[i].getMapFile())));  
      mediatracker.addImage(maps[i].getMapImage(),i);
      try {
        mediatracker.waitForID(i);
      }
      catch(InterruptedException ex) {
      }
      if(mediatracker.isErrorID(i))
      System.err.println("Failed to load map " + maps[i].getMapFile());   
      progress.setValue(progress.getValue()+1);
    } 
    //mapChooser.setEnabled(true);

    // now load vector data
    if(stopThread) return;
    showStatus("loading nations boundary data");
    nations.loadFile();
    progress.setValue(progress.getValue()+1); 
    if(stopThread) return;
    showStatus("loading regional boundary data");
    states.loadFile();
    progress.setValue(progress.getValue()+1); 
    //checkBoundaries.setEnabled(true);

    showStatus("");
    controlPanel.remove(progress);  
    progress=null;
    controlPanel.validate();

    // this loop clears the flickering that appears when viewing in a browser
    while(!stopThread) {
      if(!painting) repaint();
      try {
        Thread.sleep(1000);
      }catch(Exception e) {      }

    } 
  }

  public void start() {
    if (plotterThread == null) {
      stopThread=false;
      plotterThread = new Thread(this, "Plotter");
      plotterThread.start();  // creates a thread and invokes "run"
    }
  }

  public void stop() {
    stopThread=true;
  }

  int pointSize=7;
  double lats[];
  double lons[];
  String names[];
  int numberPlots;

  final int w = 800;
  final int h = 400;

  Color textColor=Color.WHITE;
  boolean showLabels = true;
  boolean plotAll=true;
  boolean connectPoints=false;
  boolean boundaries=true;
  boolean countries=false;
  int plotIndex=0;

  JCheckBox checkLabels = new JCheckBox("Labels",true);
  JCheckBox checkConnect = new JCheckBox("Connect",false);
  JCheckBox checkBoundaries = new JCheckBox("Boundaries",false);
  JCheckBox checkCountries = new JCheckBox("Countries",false);
  JComboBox plotChooser = new JComboBox();
  JComboBox mapChooser = new JComboBox();
  JProgressBar progress = new JProgressBar();

  PlotterMap maps[];
  PlotterMap oldMap,theMap;
  Panel controlPanel;

  PlotterVector nations,states;
  PlotterCountries countryNames= new PlotterCountries();

  Image bufferImage; // buffer image for double-buffering graphics
  Graphics gAux;
  Graphics2D ggAux;
  Image controlImage=null;

  // event handler that calls a repaint
  private class CheckLabelListner implements ItemListener {
    private Plotter p;

    public CheckLabelListner(Plotter thePlotter) {
      p=thePlotter;
    }
    public void itemStateChanged(ItemEvent e) {
      p.repaint();
    }
  }

  private Point drawPoint(Graphics g,double lat, double lon, String caption) {
    Point p = new Point();
    if(theMap.plot(lat,lon,p)) {
      Color aux=g.getColor();
      g.setColor(Color.DARK_GRAY);
      g.fillOval(p.x-(pointSize+1)/2,p.y-(pointSize+1)/2,(pointSize+1),(pointSize+1));    
      g.setColor(Color.red);
      g.fillOval(p.x-pointSize/2,p.y-pointSize/2,pointSize,pointSize);
      g.setColor(Color.black);
      if(showLabels) {
        g.drawString(caption,p.x+3,p.y-5);
        //g.drawString(caption,p.x-2,p.y-2);
        g.setColor(textColor);
        g.drawString(caption,p.x+4,p.y-4);
      }
      g.setColor(aux);
    }  
    return p;
  }

  public void paint(Graphics g) {
    painting=true;
    String copyright = "Plotter by Luis M. Suárez";   
    Font f = gAux.getFont();
    gAux.setFont(new Font("SansSerif",Font.PLAIN,10));

    gAux.setColor(Color.white);
    if (theMap.getMapImage()!=null)
      gAux.drawImage(theMap.getMapImage(),0,0,this);
    Color aux=gAux.getColor();
    gAux.setColor(Color.black);
    gAux.drawString(copyright,-1,9);
    gAux.drawString(copyright,1,11);
    gAux.setColor(Color.white);
    gAux.drawString(copyright,0,10);
    gAux.setColor(aux);
    gAux.setFont(new Font("SansSerif",Font.BOLD,12));

    if(boundaries) {
      nations.paint(gAux,theMap);
      states.paint(gAux,theMap);        
    }
    if(countries)
    countryNames.paint(gAux,theMap);
    Point newPoint,last=null;  

    if(plotAll)
    for(int i=0;i<numberPlots;i++) {
      newPoint=drawPoint(gAux,lats[i],lons[i],names[i]);
      if(connectPoints && (last!=null)) {
        gAux.setColor(Color.red);
        gAux.drawLine((int)last.getX(),(int)last.getY(),(int)newPoint.getX(),(int)newPoint.getY());        
      } 
      last=newPoint; 
    }
    else
    drawPoint(gAux,lats[plotIndex],lons[plotIndex],names[plotIndex]);
    gAux.setColor(aux);  
    gAux.setFont(f);  
    // repaint controls: important: object cig must exist!!!
    int cw=(int)(controlPanel.getBounds().getWidth());
    int ch=(int)(controlPanel.getBounds().getHeight());
    if((cw>0)&&(ch>0)) {
      if(controlImage==null)
      controlImage = createImage(cw,ch); // buffer image to draw controls
      Graphics cig = controlImage.getGraphics();
      cig.setColor(checkConnect.getBackground());
      cig.fillRect(0,0,cw,ch);    
      controlPanel.paint(cig);
      controlPanel.getGraphics().drawImage(controlImage,0,0,null); //controlImage,(int)(controlPanel.getBounds().getLocation().getX()),(int)(controlPanel.getBounds().getLocation().getY()-20) ,null);
      cig=null;
    }  
    //switch buffer and done!!!
    g.drawImage(bufferImage,0,0,null);
    painting=false;
  }

  public void blendMaps(Graphics g, PlotterMap oldMap, PlotterMap newMap) {
    painting=true;
    float oldMapAlpha=1.0f;
    float alphaDelta=0.05f;
    while(oldMapAlpha>0.0f) {
      ggAux.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, oldMapAlpha));
      if(oldMap!=null)
      ggAux.drawImage(oldMap.getMapImage(),0,0,null);
      ggAux.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f-oldMapAlpha));
      ggAux.drawImage(newMap.getMapImage(),0,0,null);
      g.drawImage(bufferImage,0,0,this);        
      oldMapAlpha=oldMapAlpha-alphaDelta;         
    }  
    painting=false;
  }

  public void repaint() {
    showLabels=checkLabels.isSelected(); 
    connectPoints = checkConnect.isSelected(); 
    boundaries = checkBoundaries.isSelected();
    countries = checkCountries.isSelected();

    plotIndex=plotChooser.getSelectedIndex()-1;     
    plotAll=(plotChooser.getSelectedIndex()==0);

    int mapIndex = mapChooser.getSelectedIndex();

    oldMap=theMap;

    theMap= maps[mapIndex];
    if(theMap.getMapImage()==null) {
      try {
        theMap.setImage(getImage(getClass().getResource(theMap.getMapFile())));          
      }catch(Exception e) {
        System.out.println("Error loading map " + theMap.getMapFile()); }
    }    

    if (oldMap!=theMap)
    blendMaps(this.getGraphics(),oldMap,theMap);  

    paint(this.getGraphics());       
  }     

  public void initMaps() {
    maps = new PlotterMap[16];
    maps[0] = new PlotterMap(800,400,0,0,800,400,"World","earth.jpg");
    maps[1] = new PlotterMap(800,400,0,0,800,400,"World - Night","night.jpg");
    maps[2] = new PlotterMap(800,400,0,0,800,400,"World - Clouds","clouds.jpg");
    maps[3] = new PlotterMap(800,400,741,504,4915,2458,"USA","USA.jpg");
    maps[4] = new PlotterMap(800,400,1860,938,8192,4096,"USA - East","USA-East.jpg");
    maps[5] = new PlotterMap(800,400,1235,938,8192,4096,"USA - West","USA-West.jpg");
    maps[6] = new PlotterMap(800,400,1976,1385,8192,4096,"Caribbean","caribbean.jpg");
    maps[7] = new PlotterMap(800,400,1769,229,4096,2048,"Europe","europe.jpg");
    maps[8] = new PlotterMap(800,400,3875,866,8192,4096,"Europe - South","southEurope.jpg");
    maps[9] = new PlotterMap(800,400,3848,520,8192,4096,"Europe - North","northEurope.jpg");
    maps[10] = new PlotterMap(800,400,25,43,2048,1024,"North America","northAmerica.jpg");
    maps[11] = new PlotterMap(800,400,277,436,2048,1024,"South America","southAmerica.jpg");      
    maps[12] = new PlotterMap(800,400,757,309,2048,1024,"Africa","africa.jpg");
    maps[13] = new PlotterMap(800,400,1248,68,2048,1024,"Asia","asia.jpg");
    maps[14] = new PlotterMap(800,400,6808,956,8192,4096,"Japan and Korea","japanKorea.jpg");
    maps[15] = new PlotterMap(800,400,1248,403,2048,1024,"Australasia","australasia.jpg");

    for(int i=0;i<maps.length;i++) { //maps are loaded in a separate thread
      mapChooser.addItem(maps[i].getMapName());
    }   
  }

  // sets the initial map to the smallest map that contains all points
  private void setInitialMap() {
    if(numberPlots<=0)
    mapChooser.setSelectedIndex(0);
    else {
      int smallest=0;
      double smallestArea=360*180+1;
      boolean allIn;
      Point p=new Point();

      for(int mapIndex=0;mapIndex<maps.length;mapIndex++) {
        allIn=true;
        for(int pointIndex=0;(pointIndex<numberPlots)&&allIn;pointIndex++)
        allIn=maps[mapIndex].plot(lats[pointIndex],lons[pointIndex],p);
        if(allIn && (maps[mapIndex].getArea()<smallestArea)) {
          smallestArea=maps[mapIndex].getArea();
          smallest=mapIndex;
        }
      }  
      mapChooser.setSelectedIndex(smallest);
    }
  }

  // the entry point for the applet
  // the start method is invoked automatically after the init completes
  // we run pre-loading code in the run method by creating a thread in start
  public void init() {
    initMaps();
    getContentPane().setLayout(new BorderLayout());

    plotChooser.addItem("All");

    try {
      numberPlots = Integer.parseInt(getParameter("points"));
      lats = new double[numberPlots];
      lons = new double[numberPlots];
      names = new String[numberPlots];
      int nameLength;
      for(int i=1;i<=numberPlots;i++) {
        lats[i-1]=Double.parseDouble(getParameter("lat" + i));
        lons[i-1]=Double.parseDouble(getParameter("lon" + i));
        names[i-1]=this.getParameter("name"+i);
        nameLength = names[i-1].length();
        plotChooser.addItem(names[i-1].substring(0,Math.min(nameLength,50)));    
      }
      checkLabels.setText(getParameter("capLabels"));
      checkConnect.setText(getParameter("capConnect"));
      checkBoundaries.setText(getParameter("capBoundaries"));
      checkCountries.setText(getParameter("capCountries"));
    }catch(Exception e) {
      System.out.println("error parsing parameters!!!! " + e.getMessage());
    }

    setInitialMap();  

    nations= new PlotterVector(getClass().getResource("nation.bvd"),Color.WHITE);
    states= new PlotterVector(getClass().getResource("state.bvd"),Color.GREEN);

    controlPanel = new Panel();//new GridLayout(0,3));

    CheckLabelListner cll= new CheckLabelListner(this);

    checkLabels.addItemListener(cll);
    controlPanel.add(checkLabels);

    checkConnect.addItemListener(cll);
    controlPanel.add(checkConnect);

    checkBoundaries.addItemListener(cll);
    //checkBoundaries.setEnabled(false);
    controlPanel.add(checkBoundaries);

    checkCountries.addItemListener(cll);
    controlPanel.add(checkCountries);

    plotChooser.addItemListener(cll);
    plotChooser.setLightWeightPopupEnabled(false);
    controlPanel.add(plotChooser);

    mapChooser.addItemListener(cll);
    mapChooser.setLightWeightPopupEnabled(false);
    //mapChooser.setEnabled(false);
    controlPanel.add(mapChooser);

    progress.setOrientation(JProgressBar.HORIZONTAL);
    progress.setMinimum(0);
    progress.setMaximum(maps.length+1);
    // controlPanel.add(progress);  

    try {
      UIManager.setLookAndFeel(
      //  UIManager.getCrossPlatformLookAndFeelClassName());
      UIManager.getSystemLookAndFeelClassName());
      //  UIManager.getInstalledLookAndFeels()[4].getClassName());
      SwingUtilities.updateComponentTreeUI(controlPanel);                
    } catch (Exception e) {    }

    controlPanel.setBackground(checkConnect.getBackground());
    getContentPane().add(controlPanel,BorderLayout.SOUTH);   

    // init the double-buffering   
    bufferImage = createImage(w,h);
    gAux =  bufferImage.getGraphics();
    ggAux = (Graphics2D)gAux;   

    repaint();
  }
}
