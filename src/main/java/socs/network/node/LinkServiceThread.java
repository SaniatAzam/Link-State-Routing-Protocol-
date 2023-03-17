package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.SOSPFPacket;

import java.io.*;
import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

class LinkServiceThread implements Runnable{
    Socket clientSocket; // The global Client Socket
    protected ObjectInputStream in = null; //Instantiate the ObjectOutput Stream
    protected ObjectOutputStream out = null; //Instantiate the ObjectOutput Stream
    private Router router; // Used to store the parent Router of the client thread
    private Link link; // Used to store the link this client thread is concerned with
    int service;  // Used to specify the type of service this client thread is going to need. More on this later.
    String clientSIP; // Used to store the simulated IP of the destination router.
    boolean threadRunning = true;  // Used to specify that the thread is still running
    public LinkServiceThread() { // Basic constructor
        super();
    }
    int portSlotNum; // Used to store the index of the link (in the router's ports array) this thread is concered with


    /***
     * @param s - the client socket
     * @param service - the service we require - 0 in this case
     * @param hostRouter - the parent router from which this thread is executed
     * @throws IOException
     *
     * Initializes the client thread for service: 0
     * Service 0 happens when the server socket of the parent router accepts another router's attach request.
     * As a result the LinkServiceThread must act like a server and wait for Hello packets.
     * This also initializes the Object IO streams
     */
    LinkServiceThread(Socket s, int service, Router hostRouter) throws IOException{
        clientSocket = s;
        out = new ObjectOutputStream(clientSocket.getOutputStream());
        in = new ObjectInputStream(clientSocket.getInputStream());
        this.service = service;
        this.router = hostRouter;
    }

    /***
     * @param s - the client socket
     * @param service - the service we require - 1 in this case
     * @param hostRouter - the parent router from which this thread is executed
     * @param link - the link with all the information of the destination router
     * @throws IOException
     *
     * Initializes the client thread for service: 1
     * Service 01happens when the parent router tries attaching to another router
     * As a result the LinkServiceThread must act like a client and have all the link information to be ready
     * to send out Hello messages.
     * This also initializes the Object IO streams
     */
    LinkServiceThread(Socket s, int service, Router hostRouter, Link link) throws IOException{
        clientSocket = s;
        out = new ObjectOutputStream(clientSocket.getOutputStream());
        in = new ObjectInputStream(clientSocket.getInputStream());
        this.service = service;
        this.router = hostRouter;
        this.link = link;
    }

    public void run() {
        /***
         * Service 0: When the server socket accepts a client.
         * Service 1: When the client thread is created through processAttach().
         */
        if (service == 0){
           try {

//               System.out.println(clientSocket.getLocalPort());
//               System.out.println(router.rd.processPortNumber);
               //Check if by any chance a router connected to a different port than the parent router's.
               if(clientSocket.getLocalPort() != router.rd.processPortNumber){
                    out.close();
                    in.close();
                    clientSocket.close();
                    throw new IllegalArgumentException("A client has been rejected for wrong port information");
               }

               // Create a dummy link with a dummy router description but without weight
               RouterDescription rd2 = new RouterDescription();
               rd2.simulatedIPAddress = null;
               rd2.processIPAddress = clientSocket.getInetAddress().getHostAddress();
               rd2.processPortNumber = (short) clientSocket.getLocalPort();
               this.link = new Link(router.rd, rd2);

               //Add a dummy Link, without the weight to the parent router's port.
               assignLink();

               // Add this thread to the client socket stores in the parent router's class.
               router.client_sockets.add(this);

               while (threadRunning) {

                   // Try reading the packet
                   SOSPFPacket packet = null;
                   packet = (SOSPFPacket) in.readObject();
                   if (packet == null) continue;

                   // If the header is 0/Hello we start processing outputs.
                   // For now the 1/LSD header processing is unimplemented.
                   if(packet.sospfType == 0){
                       System.out.println("received HELLO from " + packet.srcIP + ";");
                       if (clientSIP == null){
                           // If we have no clientSIP yet then this is the first Hello message,
                           // and therefore we extract the SIP and weight information and update the link.
                           clientSIP = packet.srcIP;
                           this.link.setSIP(packet.srcIP);
                           this.link.setWeight(packet.linkWeight);
                           updateLink();
                       }

                       // Check the RouterStatus from the link and update it accordingly and log the change.
                       if(this.link.router2.status != RouterStatus.INIT){
                           this.link.router2.status = RouterStatus.INIT;
                           updateLink();
                           System.out.println("set " + packet.srcIP + " STATE to INIT;");
                           // Send the feedback/reply
                           router.feedback(this);
                       } else {
                           this.link.router2.status = RouterStatus.TWO_WAY;
                           System.out.println("set " + packet.srcIP + " STATE to TWO_WAY;");
                           updateLink();


                           this.router.lsd.localUpdate(this.link); //Create self LSA
                           for(LinkServiceThread lst: this.router.client_sockets){
//                               if(lst != this)
                               delay();
                               this.router.lsaUpdate(lst); //Send LSP to all
                           }


                       }
                   }
                   else if (packet.sospfType == 1 && this.link.router2.status == RouterStatus.TWO_WAY) {
//                       System.out.println("Synchronizing Database (Server)");
                       if(packet.lsaArray.size() != 0) {
                           if (this.router.lsd.lsaReceiveUpdate(packet.lsaArray)){ //If a new packet has been received
                               for(LinkServiceThread lst: this.router.client_sockets){
                                   if(lst.clientSIP.equals(this.clientSIP)) continue;
                                   delay();
                                   this.router.lsaUpdate(lst); //Send LSP to all besides the client
                               }
                           }
                       }
                   }
               }
           } catch (IOException ioe) {
               System.err.println("IO exception in client handler");
               System.err.println(ioe.getStackTrace());
           } catch (ClassNotFoundException e) {
               throw new RuntimeException(e);
           }catch (IllegalArgumentException iae){
               System.out.println("Port Mismatch from client");
           } finally { //Clean up
               router.client_sockets.remove(this);
               //Delete the link
               router.ports[portSlotNum] = null;
               try {
                   out.close();
                   in.close();
                   clientSocket.close();
               } catch (IOException e) {
                   throw new RuntimeException(e);
               }
           }
       } else { // Service == 1
            try {
                // Add Link with all the info and the client socket like before.
                assignLink();
                router.client_sockets.add(this);

                // As this service only concerns with being a client who sent out the initial attach request --
                // we can assume that if a packet with header 0 comes in, it will be after we have started and sent our
                // initial HELLO message, so we can directly change the state to TWO_WAY for the destination router.
                while (threadRunning) {
                    SOSPFPacket packet = (SOSPFPacket) in.readObject();
                    if (packet.sospfType == 0) {
                        System.out.println("received HELLO from " + packet.srcIP + ";");
                        this.link.setSIP(packet.srcIP);
                        this.link.router2.status = RouterStatus.TWO_WAY;
                        updateLink();

                        this.router.lsd.localUpdate(this.link); // Update the LSA of this router


                        System.out.println("set " + packet.srcIP + " STATE to TWO_WAY;");
                        // Send another hello message so the destination router can change their state to TWO_WAY
                        router.feedback(this);
                        //Send lsaUpdate to all neighbors
                        //Assumption is this is right after processStart() from the client end


                        for(LinkServiceThread lst: router.client_sockets) {
                            delay();
                            router.lsaUpdate(lst);
                        }


                    }  else if (packet.sospfType == 1) {
//                        System.out.println("Synchronizing Database. (Client)");
                        if(packet.lsaArray.size() != 0) {
                            if (this.router.lsd.lsaReceiveUpdate(packet.lsaArray)){ //If a new packet has been received
                                for(LinkServiceThread lst: this.router.client_sockets){
                                    if(lst.clientSIP.equals(this.clientSIP)) continue;
                                    delay();
                                    this.router.lsaUpdate(lst); //Send LSP to all besides the client
                                }
                            }
                       }
                    }

                }
            } catch (IOException ioe) {
                System.err.println("IO exception in client handler");
                ioe.printStackTrace();
            }  catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } finally { // Clean up
                router.client_sockets.remove(this);
                //Delete the link
                router.ports[portSlotNum] = null;
                try {
                    out.close();
                    in.close();
                    clientSocket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /***
     * Assigns the link to an empty router.ports slot, while checking again if there is an empty slot.
     */
    private void assignLink() {
        int count = 0;
        for(Link ls: router.ports) {
            if(ls == null) {
                router.ports[count] = this.link;
                if(router.ports[count].router2.simulatedIPAddress != null)
                    clientSIP = router.ports[count].router2.simulatedIPAddress;
                portSlotNum = count;
                break;
            } else if(count >= 3){
                try {
                    out.close();
                    in.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                throw new RuntimeException("There are no ports empty.");
            }
            count++;
        }
    }

    // Maybe redundant but this updates the corresponding link object in the router class.
    private void updateLink(){
        router.ports[portSlotNum] = this.link;
    }

    public void delay(){
        Random random = new Random();
        int delay = random.nextInt(2000); //0 to 2 seconds delay
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}