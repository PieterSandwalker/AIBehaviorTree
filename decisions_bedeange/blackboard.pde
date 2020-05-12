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
