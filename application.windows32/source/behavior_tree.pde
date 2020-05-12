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
