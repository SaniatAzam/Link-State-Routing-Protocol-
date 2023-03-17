package socs.network.node;

import java.util.ArrayList;

public class WeighedGraph {

    ArrayList<Node> nodes = new ArrayList<>();
    ArrayList<Edge> edges = new ArrayList<>();

    public WeighedGraph(ArrayList<Node> vertices) {
        this.nodes.addAll(vertices);
    }

    public void addEdge(Node origin, Node destination, int weight){
        Edge e = new Edge(origin, destination, weight);
        if (this.edges.contains(e)) return;

        // Add edges and update neighbor list
        edges.add(e);
        if(!origin.neighbors.contains(destination) || !destination.neighbors.contains(origin)){
            origin.addNeighbor(destination);
        }
    }

    public Node getNode(String id) {
        for(Node n: this.nodes){
            if (id.equals(n.nodeID)) return n;
        }
        return null;
    }

    public double getWeight(Node start, Node dest){

//        System.out.println("Getting Edge from: " + start + " to: " + dest);
        Edge e = null;
        for(Edge et: edges){
            if(et.origin == start && et.destination == dest || et.destination == start && et.origin == dest) e = et;
        }
        if (e == null) throw new IllegalArgumentException("Could Not Find This Edge.");
        return e.weight;
    }

    public Node getStart(){
        Node start = null;
        for(Node n: nodes){
            if (n.isStart) {
                start = n;
                break;
            }
        }
        return start;
    }


    public static class Node {

        public String nodeID;
        public boolean isStart = false;
        public boolean isDestination = false;
        public boolean isVisited = false;

        public Node predecessor;
        public double costEstimate;

        public ArrayList<Node> neighbors = new ArrayList<>();

        public Node(String nodeID, boolean isStart, boolean isDestination){
            this.nodeID = nodeID;
            this.isStart = isStart;
            this.isDestination = isDestination;
        }

        public void addNeighbor(Node node) {
            if (neighbors.contains(node)) return;
            neighbors.add(node);
            node.addNeighbor(this);
        }


    }

    public class Edge {
        Node origin;
        Node destination;
        int weight;

        public Edge(Node o, Node d, int weight){
            this.origin = o;
            this.destination = d;
            this.weight = weight;
        }
    }
}
