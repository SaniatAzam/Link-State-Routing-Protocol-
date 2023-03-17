package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

public class LinkStateDatabase {

  //linkID => LSAInstance
  HashMap<String, LSA> _store = new HashMap<String, LSA>();

  private RouterDescription rd = null;

  public LinkStateDatabase(RouterDescription routerDescription) {
    rd = routerDescription;
    LSA l = initLinkStateDatabase();
    _store.put(l.linkStateID, l);
  }

  public void localUpdate(Link link){
//    System.out.println("Doing Local Update for " + rd.simulatedIPAddress);
    LSA lsa = _store.get(rd.simulatedIPAddress);
    LinkDescription ld = new LinkDescription();
    ld.linkID = link.router2.simulatedIPAddress;
    ld.portNum = link.router2.processPortNumber;
    ld.tosMetrics = link.cost;
    lsa.links.add(ld);
    lsa.lsaSeqNumber++;
  }

  public boolean lsaReceiveUpdate(Vector<LSA> lsaArray) {
//    System.out.println("Received a packet of LSA");
    boolean received_new = false;
    for(LSA lsa: lsaArray){
      if(!_store.containsKey(lsa.linkStateID)) {
        _store.put(lsa.linkStateID, lsa);
        received_new = true;
      } else if ((_store.containsKey(lsa.linkStateID)) && _store.get(lsa.linkStateID).lsaSeqNumber < lsa.lsaSeqNumber) {
        _store.replace(lsa.linkStateID, lsa);
        received_new = true;
//        _store.get(lsa.linkStateID).lsaSeqNumber++;
      }
    }
//    System.out.println("LSDB Size is now: " + _store.size());
    return received_new;
  }

  /**
   * output the shortest path from this router to the destination with the given IP address
   */
  String getShortestPath(String destinationIP) {

    // Create a Graph
    ArrayList<WeighedGraph.Node> vertices = new ArrayList<>();
    for(String LID: _store.keySet()){
      if (LID.equals(rd.simulatedIPAddress)) vertices.add(new WeighedGraph.Node(LID, true, false)); // Start Node
      else if(LID.equals(destinationIP)) vertices.add(new WeighedGraph.Node(LID, false, true)); // Destination Node
      else vertices.add(new WeighedGraph.Node(LID, false, false)); // Every other node

    }
    WeighedGraph g = new WeighedGraph(vertices);

    //Add Edges
    for(String key: _store.keySet()){
      LSA l = _store.get(key);
      for(LinkDescription ld: l.links){
        if (ld.linkID.equals(key)) continue;
        g.addEdge(g.getNode(key), g.getNode(ld.linkID), ld.tosMetrics);
      }
    }

    //Initialize Dijkstra
    for(WeighedGraph.Node n: g.nodes){
      n.costEstimate = Double.POSITIVE_INFINITY;
      n.predecessor = null;
    }
    WeighedGraph.Node start = g.getNode(rd.simulatedIPAddress);
    start.costEstimate = 0;

    ArrayList<WeighedGraph.Node> S = new ArrayList<>(g.nodes.size());
    NodePriorityQ Q = new NodePriorityQ(g.nodes);

    //Start
    while(!Q.isEmpty()){
      WeighedGraph.Node u = Q.removeMin();
      S.add(u);
      if(u.nodeID.equals(destinationIP)) break;
      relax(Q, u, g);
    }
    ArrayList<WeighedGraph.Node> Res = new ArrayList<>();
    int o = 0;
    WeighedGraph.Node dest = S.get(o);
    while(!dest.nodeID.equals(destinationIP)){
      o++;
      dest = S.get(o);
    }
    WeighedGraph.Node org = dest;
    Res.add(dest);
    while(org != start){
      org = org.predecessor;
      Res.add(org);
    }
    ArrayList<WeighedGraph.Node> Final = new ArrayList<>();
    for(int i = Res.size() -1 ; i >= 0; i--){
      Final.add(Res.get(i));
    }

    //Prepare Output
    StringBuilder s = new StringBuilder("");
    for (int i =0; i < Final.size()-1; i++){
      WeighedGraph.Node a = Final.get(i);
      WeighedGraph.Node b = Final.get(i+1);
      s.append(a.nodeID + " ->(" + g.getWeight(a, b) + ") ");
      if(b.isDestination) s.append(b.nodeID);
    }
    return s.toString();
  }

  public void relax(NodePriorityQ q, WeighedGraph.Node u, WeighedGraph g) {
    for(WeighedGraph.Node v: u.neighbors){
      //RELAX
      Double w = g.getWeight(u,v);
//      if(v.isDestination) System.out.println("*****************The tile " + v + " is a destination. with CE: " + v.costEstimate + " Parent "  + u + " CE: " + u.costEstimate + " Weight: " + w);
      if( v.costEstimate > u.costEstimate + w)
        q.updateKeys(v,u,u.costEstimate+w);
    }
  }

  //initialize the linkstate database by adding an entry about the router itself
  private LSA initLinkStateDatabase() {
    LSA lsa = new LSA();
    lsa.linkStateID = rd.simulatedIPAddress;
    lsa.lsaSeqNumber = Integer.MIN_VALUE;
    LinkDescription ld = new LinkDescription();
    ld.linkID = rd.simulatedIPAddress;
    ld.portNum = -1;
    ld.tosMetrics = 0;
    lsa.links.add(ld);
    return lsa;
  }


  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (LSA lsa: _store.values()) {
      sb.append(lsa.linkStateID).append("(" + lsa.lsaSeqNumber + ")").append(":\t");
      for (LinkDescription ld : lsa.links) {
        sb.append(ld.linkID).append(",").append(ld.portNum).append(",").
                append(ld.tosMetrics).append("\t");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

}
