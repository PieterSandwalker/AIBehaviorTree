// Knight class with the path from a_star
public class Knight extends KeyLocation {
  
  private Node path;
  
  public Knight(JSONArray loc) {
    super("knight", loc);
  }
  
  public void update() {
    if (getPath() != null) {
      setHome(path.getSelf());
      setPath(getPath().getParent());
    }
  }
  
  public void setPath(Node n) {
    path = n;
  }
  
  public Node getPath() {
     return path; 
  }
  
}

// Key Locations (i.e. Forge, Cave, etc) 
public class KeyLocation {

  private Cell home;
  private PVector location;
  private PImage sprite;
  private String name;

  public KeyLocation(String spr, JSONArray loc) {
    setSprite(spr);
    setLoc(new PVector(loc.getInt(0), loc.getInt(1)));
    setName(spr);
    setHome(grid[(int)getLoc().x / w][(int)getLoc().y / h]);
  }

  public void drawObj() {
    image(getSprite(), getLoc().x, getLoc().y);
  }

  public void setHome (Cell c) {
    home = c;
    setLoc(c.getLoc());
  }
  
  public Cell getHome() {
    return home;
  }

  public void setSprite(String spr) {
    sprite = loadImage(spr + ".jpg");
    if (sprite == null) {
      sprite = loadImage("default.jpg");
    }
    sprite.resize(w, h);
  }

  public void setName(String spr) {
    name = spr;
  }

  public String getName() {
    return name;
  }

  public PImage getSprite() {
    return sprite;
  }

  public void setLoc(PVector loc) {
    location = loc;
  }

  public PVector getLoc() {
    return location;
  }
}

// Items (i.e Fenrir, Wolfsbane, etc)
public class Item implements Comparable<Item> {
  
  private String name;
  private int priority;
  private boolean craftable;
  private boolean exchangable;
  private ArrayList<Item> ingredients;
  private NPC owner;
  
  public Item(String item, boolean craft) {
    setName(item);
    setPriority(0);
    setCraftable(craft);
    setExchange(false);
    
    ingredients = new ArrayList<Item>();
  }
  
  public void setName(String item) {
    name = item;
  }
  
  public String getName() {
    return name; 
  }
  
  public void setPriority(int x) {
    priority = x;
  }
  
  public int getPriority() {
    return priority;
  }
  
  public void setCraftable(boolean c) {
    craftable = c; 
  }
  
  public boolean getCraftable() {
    return craftable; 
  }
  
  public void setExchange(boolean e) {
    exchangable = e; 
  }
  
  public boolean getExchange() {
    return exchangable; 
  }
  
  public void addIngredient(Item i) {
    ingredients.add(i);
  }
  
  public ArrayList<Item> getIngredients() {
    return ingredients;
  }
  
  public void setOwner(NPC n) {
    owner = n;
  }
    
  public NPC getOwner() {
    return owner;
  }
  
  public int compareTo(Item i) {
    return getPriority() - i.getPriority();
  }
  
}

// NPC
public class NPC {
 
  private ArrayList<Item> wants;
  private ArrayList<Item> have;
  private String name;
  
  public NPC(String n) {
    setName(n);
    
    wants = new ArrayList<Item>();
    have = new ArrayList<Item>();
  }
  
  public void setName(String n){
    name = n;
  }
  
  public String getName() {
    return name; 
  }
  
  public void addWant(Item i) {
    wants.add(i);
  }
  
  public ArrayList<Item> getWants() {
    return wants; 
  }
  
  public void addHave(Item i) {
    have.add(i);
    i.setOwner(this);
    i.setExchange(true);
  }
  
  public ArrayList<Item> getHave() {
    return have;
  }
  
}

// Obstacles (i.e. Forest, Lake, Mountain)
public class Obstacle {

  private int[] obsColor;
  private ArrayList<PVector> shape;
  private Polygon bounds;

  public Obstacle(String name, JSONArray points) {
    setColor(name);
    setShape(points);
    setBounds(points);
  }

  public void drawObj() {
    beginShape();
    fill(getColor()[0], getColor()[1], getColor()[2]);
    for (PVector x : getShape()) {
      vertex(x.x, x.y);
    }
    endShape(CLOSE);
  }

  public void setColor(String name) {
    if (name.contains("lake")) {
      obsColor = new int[] {0, 0, 255};
    } else if (name.contains("forest")) {
      obsColor = new int[] {0, 255, 0};
    } else if (name.contains("mountain")) {
      obsColor = new int[] {139, 69, 12};
    }
  }

  public int[] getColor() {
    return obsColor;
  }

  public void setShape(JSONArray points) {
    shape = new ArrayList<PVector>();
    for (int i = 0; i < points.size(); i++) {
      JSONArray point = points.getJSONArray(i);
      shape.add(new PVector(point.getInt(0), point.getInt(1)));
    }
  }

  public ArrayList<PVector> getShape() {   
    return shape;
  }

  public void setBounds(JSONArray points) {
    bounds = new Polygon();
    for (int i = 0; i < points.size(); i++) {
      JSONArray point = points.getJSONArray(i);
      bounds.addPoint(point.getInt(0), point.getInt(1));
    }
  }

  public Polygon getBounds() {
    return bounds;
  }
}

// Cells of grid
public class Cell {

  private PVector location;
  private int desI;
  private int desJ;
  private Point center;
  private boolean obstacle;
  private boolean target;
  private int[] clr;

  public Cell(int x, int y, int i, int j) {
    setLoc(x, y);
    setDesignation(i, j);
    setColor(new int[] {125, 125, 125});
    setObs(false);
    setTar(false);
  }

  public void drawCell() {
    fill(getColor()[0], getColor()[1], getColor()[2]);
    rect(location.x, location.y, w, h);
  }

  public void setColor(int[] newClr) {
    clr = newClr;
  }

  public int[] getColor() {
    return clr;
  }

    public void setObs(boolean block) {
      obstacle = block;
      if (getObs()) {
        setColor(new int[] {0, 0, 0});
      }
    }

    public boolean getObs() {
      return obstacle;
    }

    public void setTar(boolean tar) {
      target = tar;
      if (getTar()) {
        setColor(new int[] {255, 215, 0});
      } else {
        setColor(new int[] {125, 125, 125});
      }
    }

    public boolean getTar() {
      return target;
    }

    public void setLoc(int x, int y) {
      center = new Point(x + w/2, y + h/2);
      location = new PVector(x, y);
    }

    public PVector getLoc() {
      return location;
    }

    public Point getCenter() {
      return center;
    }

    public void setDesignation(int i, int j) {
      desI = i;
      desJ = j;
    }

    public int getDesI() {
      return desI;
    }
    
    public int getDesJ() {
      return desJ;
    }
}
