package socs.network.node;

public class Link {

  RouterDescription router1;
  RouterDescription router2;

  short cost;

  public Link(RouterDescription r1, RouterDescription r2, short aCost) {
    router1 = r1;
    router2 = r2;
    cost = aCost;
  }

  public Link(RouterDescription r1, RouterDescription r2) {
    router1 = r1;
    router2 = r2;
  }

  public void setWeight(short aCost){
    cost = aCost;
  }

  public void setSIP(String aSIP){
    router2.simulatedIPAddress = aSIP;
  }
}
