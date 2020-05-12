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
