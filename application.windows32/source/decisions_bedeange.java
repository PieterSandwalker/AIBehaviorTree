import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.Iterator; 
import java.util.Set; 
import java.awt.Polygon; 
import java.awt.Point; 
import java.util.PriorityQueue; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class decisions_bedeange extends PApplet {







ArrayList<KeyLocation> key_locations;
ArrayList<Obstacle> obstacles;
Knight character;
Selector behavior_tree;

Cell[][] grid;
int w = 16;
int h = 16;
int cols = width/w;
int rows = height/h;

public void setup() {
  
  frameRate(15);
  
  cols = width/w;
  rows = height/h;
  
  JSONObject json = loadJSONObject("map.json");
  
  // Make Grid Map
  obstacles = new ArrayList<Obstacle>();
  JSONObject obs = json.getJSONObject("obstacles");
  
  Iterator<String> keys2 = obs.keys().iterator();
  while(keys2.hasNext()) {
    String obstacle = keys2.next();
    obstacles.add(new Obstacle(obstacle, obs.getJSONArray(obstacle)));
  }
  
  grid = new Cell[cols][rows];
    for (int i = 0; i < cols; i++) {
      for (int j = 0; j < rows; j++) {
        grid[i][j] = new Cell(i*w, j*h, i, j);
   
        for (Obstacle o : obstacles) {
          if (o.getBounds().contains(grid[i][j].getCenter().x, grid[i][j].getCenter().y)) {
            grid[i][j].setObs(true);
          } 
        }
      }
    }
  
  // Add Knight
  JSONArray knightLoc = json.getJSONArray("knight_start");
  character = new Knight(knightLoc);
  
  // Add Key Locations
  key_locations = new ArrayList<KeyLocation>();
  JSONObject keyLocs = json.getJSONObject("key_locations");
  
  Iterator<String> keys = keyLocs.keys().iterator();
  while(keys.hasNext()) {
    String keyLocation = keys.next();
    key_locations.add(new KeyLocation(keyLocation, keyLocs.getJSONArray(keyLocation)));
  }
  
  // Blackboard
  Blackboard b = new Blackboard();
  b.setState(json.getJSONObject("state_of_world"));
  b.setGold(json.getInt("greet_king"));
  
  // Build Tree
  behavior_tree = new Selector(b);
  Sequence master = new Sequence(b);
  behavior_tree.addChild(master);
  behavior_tree.addChild(new Fight(b));
  
  // Game Start Subtree
  Once start = new Once(b);
  start.setChild(new Start(b));
  master.addChild(start);
  
  // Move Subtree
  Selector move = new Selector(b);
  move.addChild(new Move(b));
  move.addChild(new Inaccessible(b));
  master.addChild(move);
  
  // Greet Subtree
  Once greet = new Once(b);
  greet.setChild(new Greet(b));
  master.addChild(greet);
  
  // Use Subtree
  // Fight
  Selector victory = new Selector(b);
  Sequence fight = new Sequence(b);
  Once ready = new Once(b);
  ready.setChild(new CanFight(b));
  fight.addChild(ready);
  fight.addChild(new Fight(b));
  victory.addChild(fight);
  // Get Item
  Sequence item = new Sequence(b);
  item.addChild(new FindBestItem(b));
  Selector craftExchange = new Selector(b);
  craftExchange.addChild(new Harvest(b));
  craftExchange.addChild(new Use(b));
  craftExchange.addChild(new Exchange(b));
  item.addChild(craftExchange);
  victory.addChild(item);
  master.addChild(victory);
  
}

public void draw() {
  background(64, 64, 64);
  
  // Behavior Tree
  if ( behavior_tree.run() == TASK_OVER ) {
    println("Game Over, the kingdom was saved!");
    noLoop();
  } else if ( behavior_tree.run() == TASK_OVER_FAIL ) {
    println("Game Over, Ramses has destroyed all!");
    noLoop();
  }
  
  // Draw grid
  for (int i = 0; i < cols; i++) {
    for (int j = 0; j < rows; j++) {
      grid[i][j].drawCell();
    }
  }
  
  // Draw obstacles
  for (Obstacle obs : obstacles) {
    obs.drawObj();
  }
  
 
  // Draw landmarks
  for (KeyLocation loc : key_locations) {
    loc.drawObj();
  }

  // Draw Character
  character.drawObj();
  
}
public class Node implements Comparable<Node> {
  
  private Node parent;
  private Cell self;
  private int g;
  private int h;
  
  public Node(Cell c, Node n) {
    setSelf(c);
    setParent(n);
    if (n != null) {
      setG(n.getG() + 1);
    } else {
      setG(0); 
    }
    setH(heuristic(getSelf()));
  }

  public void setG(int x) {
    g = x;
  }
  
  public void setH(int x) {
    h = x;
  }
  
  public int getG() {
    return g; 
  }
  
  public int getH() {
    return h; 
  }
  
  public int getF() {
    return g + h;
  }
  
  public void setParent(Node n) {
    parent = n;
  }
  
  public Node getParent() {
    return parent;
  }
  
  public void setSelf(Cell c) {
    self = c;
  }
  
  public Cell getSelf() {
    return self;
  }
  
  public int compareTo(Node n) {
    return getF() - n.getF();
  }
  
  public ArrayList<Node> getNeighbors() {
    ArrayList<Node> neighbors = new ArrayList<Node>();
    
    int x = getSelf().getDesI();
    int y = getSelf().getDesJ();
    if (x - 1 >= 0) {
      if (!grid[x-1][y].getObs()) {
        neighbors.add(new Node(grid[x-1][y], this));
      }
    }
    
    if (x + 1 < cols) {
      if (!grid[x+1][y].getObs()) {
        neighbors.add(new Node(grid[x+1][y], this));
      }
    }
    
    if (y - 1 >= 0) {
      if (!grid[x][y-1].getObs()) {
        neighbors.add(new Node(grid[x][y-1], this));
      }
    }
    
    if (y + 1 < rows) {
      if (!grid[x][y+1].getObs()) {
        neighbors.add(new Node(grid[x][y+1], this));
      }
    }
    
    return neighbors;
  }
  
}

public Node a_star(Cell target) {
  PriorityQueue<Node> openList = new PriorityQueue<Node>();
  ArrayList<Node> closedList = new ArrayList<Node>();
  
  Node start = new Node(target, null);
  openList.add(start);
  
  while (openList.size() != 0) {
    Node n = openList.poll();
    
    if (n.getSelf().equals(character.getHome())) {
      return n.getParent();
    }
    
    for ( Node x : n.getNeighbors()) {
      boolean add = true;
      for (Node x2 : openList) {
        if (x.getSelf().equals(x2.getSelf()) && x.getF() > x2.getF()) {
           add = false; 
        }
      }
      for (Node x2 : closedList) {
        if (x.getSelf().equals(x2.getSelf())) {
           add = false; 
        }
      }
      if (add) {
        openList.add(x);
      }
    }
    
    closedList.add(n);
  }

  return null;
  
}

public int heuristic(Cell c) {
  return (abs(c.getDesI() - character.getHome().getDesI()) + abs(c.getDesJ() - character.getHome().getDesJ())); 
}
final int TASK_SUCCESS= 0;
final int TASK_FAIL = 1;
final int TASK_IN_PROGRESS = 2;
final int TASK_OVER_FAIL = 3;
final int TASK_OVER = 4;

// Base Class
public abstract class Task {
 
  private Blackboard bb;
  
  public Task(Blackboard b) {
    setBB(b);
  }
  
  public abstract int run();
  
  public Blackboard getBB() {
    return bb;
  }
  
  public void setBB(Blackboard b) {
    bb = b;
  }
  
}

// Collection Tasks
public class Sequence extends Task {

  private ArrayList<Task> children;
  
  public Sequence(Blackboard b) {
    super(b);
    
    children = new ArrayList<Task>();
  }
  
  public int run() {
    for (Task t : children) {
      int status = t.run();
      if (status != TASK_SUCCESS) {
         return status; 
      }
    }
    return TASK_SUCCESS;
  }
  
  public void addChild(Task t) {
    children.add(t);
  }

}

public class Selector extends Task {
 
  private ArrayList<Task> children;
  
  public Selector(Blackboard b) {
    super(b);
    
    children = new ArrayList<Task>();
  }
  
  public int run() {
    for (Task t : children) {
      int status = t.run();
      if (status != TASK_FAIL) {
         return status; 
      }
    }
    return TASK_FAIL;
  }
  
  public void addChild(Task t) {
    children.add(t);
  }
  
}

// Decorators
public class Once extends Task {
  
  private Task child;
  private int count;
  
  public Once(Blackboard b) {
    super(b);
    
    count = 0;
  }
  
  public int run() {
    if (count == 1) {
      return TASK_SUCCESS;  
    } else {
      int child_result = child.run();
      if (child_result == TASK_SUCCESS) {
        count++;  
      }
      return child_result;
    }
    
  }
  
  public void setChild(Task t) {
    child = t;
  }
  
  public Task getChild() {
    return child; 
  }
  
}

// Actions
public class Move extends Task {
  
  public Move(Blackboard b) {
    super(b);
  }
  
  public int run() {
    if (character.getHome().equals(getBB().getTarget().getHome())) {
      return TASK_SUCCESS;
    }
    
    Node n = a_star(getBB().getTarget().getHome());
    if (n == null) {
      println("The " + getBB().getTarget().getName() + " is inaccesible.");
      return TASK_FAIL;
    } else {
      character.setPath(n);
      character.update();
      return TASK_IN_PROGRESS;
    }
  }
  
}

public class Greet extends Task {

  public Greet(Blackboard b) {
    super(b);  
  }
  
  public int run() {
    KeyLocation castle = null;
    
    for (KeyLocation k : key_locations) {
      if (k.getName().equals("castle")) {
        castle = k;
        break;
      }
    }
    
    if (castle == null) {
      return TASK_FAIL;  
    }
    
    if (castle.getHome().equals(character.getHome())) {
      for (int i = 0; i < getBB().getGold(); i++) {
        getBB().addToInventory(getBB().getIndex().getItem("1gold"));
      }
      println("The Knight greets the King.");
      println("The King gave the Knight a gift of " 
        + getBB().getGold() + " gold.");
      return TASK_SUCCESS;
    } else {
      return TASK_FAIL;
    }   
  }
}

public class Harvest extends Task {
  
  public Harvest(Blackboard b) {
    super(b);
  }
  
  public int run() {
    Item i = getBB().getTarItem();
    if (i != null && i.getName().equals("Wood")) {
      getBB().setTarget("tree");
      if (character.getHome().equals(getBB().getTarget().getHome())) {
        for (Item x : getBB().getInventory()) {
          if (x.getName().equals("Axe")) {
            getBB().getInventory().remove(x);
            break;
          }
        }
        getBB().addToInventory(i);
        getBB().setTarItem(null);
        println("The Knight gains a " + i.getName() +  ".");
          
        for (NPC n : getBB().getNPCs()) {
          if (n.getName().equals("Wood Spirit")) {
            for (Item y : n.getHave()) {
              y.setOwner(null);
              y.setExchange(false);
            }
            getBB().getNPCs().remove(n);
          }
        }
        println("The Wood Spirit is dead.");
        
      } else {
        println("The Knight sets off for the tree.");
      }
      
      return TASK_SUCCESS;
     }
    return TASK_FAIL;
  }
  
}

public class Use extends Task {
 
  public Use(Blackboard b) {
    super(b); 
  }
  
  public int run() {
    Item i = getBB().getTarItem();
    if (i != null && i.getCraftable() && !i.getName().equals("Wood"))  {
      for (Item x : i.getIngredients()) {
        for (Item y : getBB().getInventory()) {
          if (y.getName().equals(x.getName())) {
            getBB().getInventory().remove(y);
            break;
          }
        }
      }
      
      getBB().addToInventory(i);
      getBB().setTarItem(null);
      println("The Knight crafted a " + i.getName() + ".");
      return TASK_SUCCESS;
    }
    return TASK_FAIL; 
  }
}

public class Fight extends Task {
  
  public Fight(Blackboard b) {
    super(b);
  }
  
  public int run() {
    getBB().setTarget("tar_pit");
      if (character.getHome().equals(getBB().getTarget().getHome())) {
        println("The Knight takes on Ramses!");
        
        if (getBB().have(getBB().getIndex().getItem("Fenrir"))) {
          println("The Knight defeated Ramses with Fenrir.");
          return TASK_OVER;
        } else if (getBB().have(getBB().getIndex().getItem("Fire"))) {
          println("The Knight defeated Ramses with Fire.");
          return TASK_OVER; 
        } else if (getBB().have(getBB().getIndex().getItem("Poisoned Fenrir"))) {
          println("The Knight defeated Ramses with Poisoned Fenrir.");
          return TASK_OVER;
        } else if (getBB().have(getBB().getIndex().getItem("Poisoned Sword"))) {
          println("The Knight defeated Ramses with Poisoned Sword.");
          return TASK_OVER; 
        }
        
        println("Ramses proved to strong. The Knight was defeated.");
        return TASK_OVER;
      }
      return TASK_SUCCESS;
  }
}

public class Exchange extends Task {
  
  public Exchange(Blackboard b) {
    super(b);
  }
  
  public int run() {
    Item i = getBB().getTarItem();
    if (i != null && i.getExchange()) {
       
      NPC n = i.getOwner();
      KeyLocation k = getLocation(n);
      if (k != null ) {
        getBB().setTarget(k.getName());
        if (character.getHome().equals(getBB().getTarget().getHome())) {
          for (Item x : n.getWants()) {
            if (getBB().have(x)) {
              println(n.getName() + " wants " + x.getName() + ".");
              getBB().getInventory().remove(x);
              break;
            }
          }
          for (Item y : n.getHave()) {
            if (y.getName().equals(i.getName())) {
              if (y.getName().equals("Fenrir")) {
                y.setOwner(null);
                y.setExchange(false);
                n.getHave().remove(y);
              }
              getBB().addToInventory(y);
              getBB().setTarItem(null);
              println("The Knight recieved the " + y.getName() + ".");
              break;
            }
          }
        } else {
          println("The Knight sets off for the " + k.getName());
        }
          
      } else {
        return TASK_FAIL;
      }
      
      return TASK_SUCCESS;
    }
    return TASK_FAIL;
  }
  
  public KeyLocation getLocation(NPC n) {
    if (n.getName().equals("Blacksmith")) {
      for (KeyLocation k : key_locations) {
        if (k.getName().equals("forge")) {
          return k; 
        }
      }
    } 
    
    else if (n.getName().equals("Innkeeper")) {
      for (KeyLocation k : key_locations) {
        if (k.getName().equals("tavern")) {
          return k; 
        }
      }
    }
    
    else if (n.getName().equals("Lady Lupa")) {
      for (KeyLocation k : key_locations) {
        if (k.getName().equals("cave")) {
          return k; 
        }
      }
    }
    
    else if (n.getName().equals("Tree Spirit")) {
      for (KeyLocation k : key_locations) {
        if (k.getName().equals("tree")) {
          return k; 
        }
      }
    }
    
    return null;
    
  }
  
}

// Deciders
public class CanFight extends Task {
  
  public CanFight(Blackboard b) {
    super(b);
  }
  
  public int run() {
    if (getBB().have(getBB().getIndex().getItem("Fenrir"))) {
      println("The Knight is ready to fight Ramses.");
      println("The Knight sets forth for the tar pit.");
      return TASK_SUCCESS;
    } else if (getBB().have(getBB().getIndex().getItem("Fire"))) {
      println("The Knight is ready to fight Ramses.");
      println("The Knight sets forth for the tar pit.");
      return TASK_SUCCESS; 
    } else if (getBB().have(getBB().getIndex().getItem("Poisoned Fenrir"))) {
      println("The Knight is ready to fight Ramses.");
      println("The Knight sets forth for the tar pit.");
      return TASK_SUCCESS; 
    } else if (getBB().have(getBB().getIndex().getItem("Poisoned Sword"))) {
      println("The Knight is ready to fight Ramses.");
      println("The Knight sets forth for the tar pit.");
      return TASK_SUCCESS; 
    }
    return TASK_FAIL;
  }
  
}

// Blackboard Read & Write
public class Start extends Task {
 
  public Start(Blackboard b) {
    super(b);
  }
  
  public int run() {
    println("The Knight sets off for the Castle");
    getBB().setTarget("castle");
    return 0;
  }
  
}

public class FindBestItem extends Task {
  
  public FindBestItem(Blackboard b) {
    super(b);
  }
  
  public int run() {
    if (getBB().getTarItem() != null) {
      return TASK_SUCCESS;
    }
    
    PriorityQueue<Item> openList = new PriorityQueue<Item>();
    ArrayList<Item> closedList = new ArrayList<Item>();
    
    openList.add(getBB().getIndex().getItem("Fenrir"));
    openList.add(getBB().getIndex().getItem("Poisoned Fenrir"));
    openList.add(getBB().getIndex().getItem("Poisoned Sword"));
    openList.add(getBB().getIndex().getItem("Fire"));
    
    while(openList.size() != 0) {
      Item i = openList.poll();
      
        // Check if have ingredients, otherwise add ingredients to openList
        if (i.getCraftable()) {
          
          int needIngredients = 0;
          
          // check if have ingredients
          for (Item x : i.getIngredients()) {
            if (!getBB().have(x)) {
              needIngredients++;
            }
          }
          
          // if have both ingredients, target item is I
          if (needIngredients == 0) {
            getBB().setTarItem(i);
            println("The Knight decides to make " + i.getName());
            return TASK_SUCCESS; 
          }
          
          // check to see if in a list already
          for (Item x : i.getIngredients()) {
            boolean add = true;
            if (getBB().have(x)) {
              add = false; 
            }
            for (Item z : openList) {
              if (z.getName().equals(x.getName())) {
                add = false; 
              }
            }
            for (Item z : closedList) {
              if (z.getName().equals(x.getName())) {
                add = false; 
              }
            }
            
            // if not in either list, add to openlist
            if (add) {
              x.setPriority(i.getPriority() + needIngredients);
              openList.add(x);
            }
          }
        }
        
        // check to see if item can be bought
        if (i.getExchange()) {
           NPC n = i.getOwner();
           for (Item x : n.getWants()) {
             if (getBB().have(x)) {
               getBB().setTarItem(i);
               println("The Knight decides to trade for " + i.getName());
               return TASK_SUCCESS;
             } else {
               boolean add = true;
               for (Item z : openList) {
                 if (z.getName().equals(x.getName())) {
                   add = false; 
                 }
               }
               for (Item z : closedList) {
                 if (z.getName().equals(x.getName())) {
                   add = false; 
                 }
               }
               if (add) {
                 x.setPriority(i.getPriority() + 1);
                 openList.add(x);
               }
             }
           }
        }
      
        closedList.add(i);
    }
    
    return TASK_FAIL;
  }
  
}

public class Inaccessible extends Task {
  
  public Inaccessible(Blackboard b) {
    super(b);
  }
  
  public int run() {
    println("The way to the " + getBB().getTarget().getName() + " is blocked.");
    if (getBB().getTarget().getName().equals("tree")) {
      Item wood = getBB().getIndex().getItem("Wood");
      wood.craftable = false;
      for (NPC n : getBB().getNPCs()) {
        if (n.getName().equals("Tree Spirit")) {
          for (Item i : n.getHave()) {
            i.setExchange(false);
          }
        }
      }
    }
    
    if (getBB().getTarget().getName().equals("forge")) {
      for (NPC n : getBB().getNPCs()) {
        if (n.getName().equals("Blacksmith")) {
          for (Item i : n.getHave()) {
            i.setExchange(false);
          }
        }
      }
    }
    
    if (getBB().getTarget().getName().equals("tavern")) {
      for (NPC n : getBB().getNPCs()) {
        if (n.getName().equals("Innkeeper")) {
          for (Item i : n.getHave()) {
            i.setExchange(false);
          }
        }
      }
    }
    
    if (getBB().getTarget().getName().equals("cave")) {
      for (NPC n : getBB().getNPCs()) {
        if (n.getName().equals("Lady Lupa")) {
          for (Item i : n.getHave()) {
            i.setExchange(false);         
          }
        }
      }
    }
    
    return TASK_SUCCESS;
  }
}
public class Blackboard {
  
  ArrayList<Item> inventory;
  KeyLocation target;
  int gold;
  ArrayList<NPC> NPCs;
  ItemDatabase index;
  Item targetItem;
  boolean atLocation;
  
  public Blackboard() {
    inventory = new ArrayList<Item>();
    index = new ItemDatabase();
    
    // set NPCs
    NPCs = new ArrayList<NPC>();
    NPCs.add(new NPC("Blacksmith"));
    NPCs.add(new NPC("Lady Lupa"));
    NPCs.add(new NPC("Innkeeper"));
    NPCs.add(new NPC("Tree Spirit"));
    
    atLocation = false;
  }
  
  public void setState(JSONObject json) {
    JSONArray has = json.getJSONArray("Has");
    JSONArray wants = json.getJSONArray("Wants");
    
    for (int i = 0; i < has.size(); i++) {
      JSONArray have = has.getJSONArray(i);
      String item = have.getString(1);
      Item x = index.getItem(item);
      String owner = have.getString(0);
      for (NPC n : NPCs) {
        if (n.getName().equals(owner)) {
          n.addHave(x);
        }
      }
    }
    
    for (int i = 0; i < wants.size(); i++) {
      JSONArray want = wants.getJSONArray(i);
      String item = want.getString(1);
      Item x = index.getItem(item);
      String owner = want.getString(0);
      for (NPC n : NPCs) {
        if (n.getName().equals(owner)) {
          n.addWant(x);
        }
      }
    }
  }
  
  public void setTarItem(Item i) {
    targetItem = i;  
  }
  
  public Item getTarItem() {
    return targetItem; 
  }
  
  public void setAtLocation(boolean b) {
    atLocation = b;
  }
  
  public boolean atLocation() {
    return atLocation; 
  }
  
  public KeyLocation getTarget() {
    return target;
  }
  
  public void setTarget(String location) {
    for (KeyLocation k : key_locations) {
      if (k.getName().equals(location)) {
        target = k;
        break;
      }
    }
  }
  
  public int getGold() {
    return gold;  
  }
  
  public void setGold(int g) {
    gold = g;
  }
  
  public void addToInventory(Item i) {
    inventory.add(i);
  }
  
  public ArrayList<Item> getInventory() {
    return inventory;
  }
  
  public ItemDatabase getIndex() {
    return index;
  }
  
  public ArrayList<NPC> getNPCs() {
    return NPCs;
  }
  
  public boolean have(Item i) {
    for (Item x : inventory) {
      if (x.getName().equals(i.getName())) {
        return true;  
      }
    }
    return false;
  }
}

public class ItemDatabase {
  
  private ArrayList<Item> index;
  
  ItemDatabase() {
    index = new ArrayList<Item>();
    index.add(new Item("1gold", false));
    index.add(new Item("Axe", false));
    index.add(new Item("Blade", false));
    index.add(new Item("Wolfsbane", false));
    index.add(new Item("Fenrir", false));
    index.add(new Item("Water", false));
    index.add(new Item("Ale", false));
    addCraftables();
    
  }
  
  public void addCraftables() {
    Item i = new Item("Wood", true);
    i.addIngredient(getItem("Axe"));
    index.add(i);
    
    i = new Item("Sword", true);
    i.addIngredient(getItem("Blade"));
    i.addIngredient(getItem("Wood"));
    index.add(i);
    
    i = new Item("Poisoned Sword", true);
    i.addIngredient(getItem("Sword"));
    i.addIngredient(getItem("Wolfsbane"));
    index.add(i);
    
    i = new Item("Poisoned Fenrir", true);
    i.addIngredient(getItem("Fenrir"));
    i.addIngredient(getItem("Wolfsbane"));
    index.add(i);
    
    i = new Item("Fire", true);
    i.addIngredient(getItem("Ale"));
    i.addIngredient(getItem("Wood"));
    index.add(i);
    println("Database intitialized.");
  }
  
  public Item getItem(String name) {
    for (Item i : index) {
      if (i.getName().equals(name)) {
        return i;
      }
    }
    return null;
  }
  
}
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
  public void settings() {  size(640, 480); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "decisions_bedeange" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
