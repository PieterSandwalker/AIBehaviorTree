import java.util.Iterator;
import java.util.Set;
import java.awt.Polygon;
import java.awt.Point;
import java.util.PriorityQueue;

ArrayList<KeyLocation> key_locations;
ArrayList<Obstacle> obstacles;
Knight character;
Selector behavior_tree;

Cell[][] grid;
int w = 16;
int h = 16;
int cols = width/w;
int rows = height/h;

void setup() {
  size(640, 480);
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

void draw() {
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
