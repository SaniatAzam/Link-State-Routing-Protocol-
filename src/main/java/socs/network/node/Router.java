package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.*;

public class Router {
  ServerSocket serverSocket; // The global server socket for the Router instance
  protected LinkStateDatabase lsd;
  boolean serverRunning = true; // Might be useful in later assignments
  private Executor executor; // The thread executor
  protected ArrayList<LinkServiceThread> client_sockets = new ArrayList<>(4); //Stores the "Client Threads"
  RouterDescription rd = new RouterDescription(); // The router description
  Link[] ports = new Link[4]; // The Links - the router can have 4 ports for now

  public Router(Configuration config) {
    //Initialize the RouterDescription object's field.
    rd.simulatedIPAddress = config.getString("socs.network.router.ip");
    rd.processPortNumber = config.getShort("socs.network.router.process_port");
    try {
      rd.processIPAddress = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      System.out.println("Exception at getting process IP. ");
      e.printStackTrace();
    }

    // Initialize the LinkStateDatabase field.
    lsd = new LinkStateDatabase(rd);

    //Initialize Thread Executor with 4 processes
    this.executor = Executors.newFixedThreadPool(4);

    //Instantiate the ServerSocket
    try {
      this.serverSocket = new ServerSocket(rd.processPortNumber);
    } catch (IOException e) {
      System.out.println("Could not create server socket on port " + rd.processPortNumber + ". Quitting.");
      System.exit(-1);
    }
    //See next code block
    startServer();
  }

  /***
   * Threads the server start process & the terminal I/O
   */
  private void startServer() {
    // Start the terminal as a separate thread
    new Thread(() -> terminal()).start();

    // Start listening on the initialized ServerSocket on a separate thread to not block the terminal.
    new Thread(() -> {
      while (true) {
        try {
          Socket socket = serverSocket.accept();
          //Execute the LinkServiceThread in the thread pool -- See the class for more info
          executor.execute(new LinkServiceThread(socket, 0, this));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }).start();
  }
  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip address of the destination simulated router
   */
  private void processDetect(String destinationIP) {
    System.out.println(lsd.getShortestPath(destinationIP));
  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
   */
  private void processDisconnect(short portNumber) {

  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to identify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(String processIP, short processPort,
                             String simulatedIP, short weight) {

    /**
     * Create new router description to be used in creating a new Link object.
     */

    //Create a Link for the router to be connected to
    RouterDescription rd2 = new RouterDescription();
    rd2.processIPAddress = processIP;
    rd2.processPortNumber = processPort;
    rd2.simulatedIPAddress = simulatedIP;
    Link l = new Link(rd, rd2, weight);

    // Try connecting to the router's server socket
    try {
        //Check if we are trying to connect to the router to itself
        if(processPort == rd.processPortNumber || Objects.equals(simulatedIP, rd.simulatedIPAddress))
          throw new IllegalArgumentException();

        // Check if there are empty ports
        int count = 0;
        for(Link ls: ports){
          if(ls != null) count++;
        }
        if(count >= 4) throw new IllegalAccessException();
        Socket socket = new Socket(processIP, processPort);

        //Execute the LinkServiceThread in the thread pool -- See the class for more info
        executor.execute(new LinkServiceThread(socket, 1, this, l));
    } catch (IOException ioe) {
      System.out.println("Exception encountered on accept. Ignoring.");
      ioe.printStackTrace();
    } catch (IllegalAccessException iae){
      System.out.println("All ports are filled. Ignoring.");
    } catch (IllegalArgumentException iae) {
      System.out.println("The router can not connect to its own port or simulated IP address");
    }
  }


  /**
   * broadcast Hello to neighbors
   */
  private void processStart() throws IOException {

    // Go through the links
    for(Link l: ports){
      if(l == null) continue;

      // Isolate the simulated IP
      String r2SIP = l.router2.simulatedIPAddress;

      // Check if the simulated IP for the link is not null or if the link's destination router's
      // status is already TWO_WAY
      if(r2SIP != null && l.router2.status != RouterStatus.TWO_WAY) {

        // Create the HELLO packet with sospfType = 0
        SOSPFPacket packet0 = new SOSPFPacket();
        packet0.srcIP = packet0.routerID = rd.simulatedIPAddress;
        packet0.dstIP = packet0.neighborID = r2SIP;
        packet0.sospfType = 0;
        packet0.linkWeight = l.cost;

        //Find the matching client's/neighbor's output stream and write the object.
        for(LinkServiceThread lst: client_sockets){
          if(lst.clientSIP == r2SIP) {
            lst.out.writeObject(packet0);
          }
        }
      }
    }

  }


  /**
   * The feedback that is sent to a client when a client sends a hello message to establish TWO_WAY
   * @param aLst
   * @throws IOException
   */

  protected void feedback(LinkServiceThread aLst) throws IOException {
    int socket_idx = client_sockets.indexOf(aLst);
    LinkServiceThread lst = this.client_sockets.get(socket_idx);

    // Initialize the feedback hello
    SOSPFPacket packet = new SOSPFPacket();
    packet.srcIP = packet.routerID = rd.simulatedIPAddress;
    packet.dstIP = packet.neighborID = lst.clientSIP;
    packet.sospfType = 0;
    lst.out.writeObject(packet);
  }

  protected void lsaUpdate(LinkServiceThread aLst) throws IOException {
//    System.out.println("LSA Update on the ip: " + aLst.clientSIP);
    int socket_idx = client_sockets.indexOf(aLst);
    LinkServiceThread lst = this.client_sockets.get(socket_idx);

    // Initialize the lsaUpdate
    SOSPFPacket packet1 = new SOSPFPacket();
    packet1.srcIP = packet1.routerID = rd.simulatedIPAddress;
    packet1.dstIP = packet1.neighborID = lst.clientSIP;
    packet1.sospfType = 1;

    //Add the LSA's to the packet's vector
//    System.out.println(lsd._store.keySet().size());
    packet1.lsaArray = new Vector<>(lsd._store.keySet().size());
    for(String key: lsd._store.keySet()){
      packet1.lsaArray.add(lsd._store.get(key));
    }
//    System.out.println(lst.clientSIP);
    lst.out.writeObject(packet1);
  }



  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
   */
  private void processConnect(String processIP, short processPort,
                              String simulatedIP, short weight) {

  }

  /**
   * output the neighbors of the routers
   */
  private void processNeighbors() {
    System.out.println("------- Neighbors -------");
    for(Link l: ports){
      if(l != null && (l.router2.status == RouterStatus.TWO_WAY || l.router2.simulatedIPAddress != null)){
        System.out.println(l.router2.simulatedIPAddress);
      }
    }
  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit() {

  }

  /**
   * update the weight of an attached link
   */
  private void updateWeight(String processIP, short processPort,
                             String simulatedIP, short weight){

  }


  /**
   * Type "info" on terminal
   * It will show you the information of the current Router.
   * @throws UnknownHostException
   */

  private void info() throws UnknownHostException {
    System.out.println("------- Router Information -------");
    System.out.println("Process IP: " + rd.processIPAddress);
    System.out.println("Process Port: " + rd.processPortNumber);
    System.out.println("Simulated IP: " + rd.simulatedIPAddress);
    int count = 0;
    for(Link l: ports) {
      if (l != null) count++;
    }
    System.out.println("Number of Links: " + count);
    System.out.println("--Client Sockets--");
    for(LinkServiceThread lst: client_sockets){
      if(lst != null)
        System.out.println(lst);
    }
  }

  public void terminal() {
    try {
      InputStreamReader isReader = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(isReader);
      System.out.print(">> ");
      String command = br.readLine();
      while (true) {
        if (command.startsWith("detect ")) {
          String[] cmdLine = command.split(" ");
          processDetect(cmdLine[1]);
        } else if (command.startsWith("disconnect ")) {
          String[] cmdLine = command.split(" ");
          processDisconnect(Short.parseShort(cmdLine[1]));
        } else if (command.startsWith("quit")) {
          processQuit();
        } else if (command.startsWith("attach ")) {
          String[] cmdLine = command.split(" ");
          processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("start")) {
          processStart();
        } else if (command.equals("connect ")) {
          String[] cmdLine = command.split(" ");
          processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("neighbors")) {
          //output neighbors
          processNeighbors();
        } else if (command.equals("info")) {
          //output neighbors
          info();
        } else {
          //invalid command
          break;
        }
        System.out.print(">> ");
        command = br.readLine();
      }
      isReader.close();
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }




}
