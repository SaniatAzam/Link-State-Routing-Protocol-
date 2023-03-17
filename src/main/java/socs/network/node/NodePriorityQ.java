package socs.network.node;

import java.util.ArrayList;
import java.util.Arrays;

public class NodePriorityQ {
    public WeighedGraph.Node[] queue;
    public int head;
    public int size = 0;

    public NodePriorityQ (ArrayList<WeighedGraph.Node> vertices) {

        this.queue = new WeighedGraph.Node[vertices.size()+1];
        this.head = 1;

        for(int i = 0; i < vertices.size(); i++) {
            add(vertices.get(i));
        }

    }

    private void add(WeighedGraph.Node n){
        size++;
        queue[size] =  n;
        int i = size;
        while(i >1 && queue[i].costEstimate < queue[i/2].costEstimate){
            WeighedGraph.Node tmp = queue[i];
            queue[i] = queue[i/2];
            queue[i/2] = tmp;
            i = i/2;
        }

    }

    private void downHeap( int startIndex, int maxIndex){
//		System.out.println("Down Heaping");
        int i = startIndex;
        while (2*i <= maxIndex){
            int child = 2*i;
            if (child < maxIndex) {
                if (queue[child+1].costEstimate < queue[child].costEstimate)
                    child = child +1;
            }
            if(queue[child].costEstimate < queue[i].costEstimate) {
                WeighedGraph.Node tmp = queue[child];
                queue[child] = queue[i];
                queue[i] = tmp;
                i = child;
            } else break;
        }
    }

    private void print(){
//        System.out.println("The size of the Queue is: " + size);
        if( size > 0) {
            StringBuilder s = new StringBuilder("");
            StringBuilder v = new StringBuilder("");
            for(int i = head; i <= size; i++){
                s.append(queue[i].costEstimate + " ");
                v.append(queue[i] + " ");
            }
            System.out.println(s.toString());
            System.out.println(v.toString());
        }

    }



    public WeighedGraph.Node removeMin() {
//		System.out.println("Removing min from");
//		print();
        if (this.queue[head] == null) return null;
        WeighedGraph.Node tmpNode = queue[1];
        queue[1] = queue[size];
        queue[size] = null;
        size--;
        downHeap(1, size);
        return tmpNode;
    }

    public void updateKeys(WeighedGraph.Node t, WeighedGraph.Node newPred, double newEstimate) {
//        System.out.println("Updating node " + t + " New Parent: " + newPred + " New estimate: " + newEstimate +  "The neighbor is the destination? " + t.isDestination);
        ArrayList<WeighedGraph.Node> check = new ArrayList<>(Arrays.asList(queue));
        t.costEstimate = newEstimate;
        t.predecessor = newPred;
        WeighedGraph.Node nodeToRemove = t;
        int index = -1;
        for (int i = 1; i <= size; i++) {
            if (queue[i] == nodeToRemove) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            for (int i = index; i <= size -1; i++) {
                queue[i] = queue[i + 1];
            }
            queue[size] = null;
        }
        size--;
        add(t);
//        System.out.println("New queue: "); print();
    }

    public boolean isEmpty(){
        return size == 0;
    }

}
