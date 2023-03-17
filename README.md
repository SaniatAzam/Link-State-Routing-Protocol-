# Link-State-Routing-Protocol-


This link state rrouting protocol emulator is written in Java and compiled using maven. We have six router configurations with identifiable characteristics: the simulated IP address and the port number. Each router can connect to the other and send two types of packets, the first being the "Hello" message and the second one being the "LSAUpdate" message. LSA is Link State Advertisement. A link is a connection between two Router instances. Each router has a Link State Database, which stores the information on the network's topology. This topology updates when we create a link; the routers in the new link send 'LSAUpdate' packets to their neighbors. The neighboring routers use this information to update their Link State Database. This database helps us find the shortest path to a given simulated IP address from any router. Each link in this network has a weight (which simulates the real-world cost of sending packets from one host to another). Knowing the cost between each link, the routers can find the shortest path to a given simulated IP; this is implemented using Dijkstra's algorithm. 

The connectivity logic between each router is implemented using multi-threaeded client-server programming. You will find this in the Router and LinkServiceThread classes.


Set Up Instrructions:
  
  • Download the folder.
  • Make sure your maven configurations is on par with the pom.xml
  • On your terminal type "mvn clean"
  • On your terminal type "mvn compile assembly:single"
  • This updates the target folder


For each router instances we want to start, we need to open up a seperate terminal.
Running the router Processes.

  • To start Router 1: java -cp target/COMP535-1.0-SNAPSHOT-jar-with-dependencies.jar socs.network.Main conf/router1.conf
  
  • To start Router 2: java -cp target/COMP535-1.0-SNAPSHOT-jar-with-dependencies.jar socs.network.Main conf/router2.conf
  
  • To start Router 3: java -cp target/COMP535-1.0-SNAPSHOT-jar-with-dependencies.jar socs.network.Main conf/router3.conf
  
  ...and so on
  
  This will run a little console for each router instance that takes in custom commands.


  
Attaching syntax: attach | realIP port_of_router_being_attached_to | simulated_IP_of_the_router_being_attached_to | link_weight

  The following attach examples follow the configuration files found under the conf folder and the link weights are randomly set.
  
  • To attach any router to R2: attach 192.168.0.181 4002 192.168.1.100 4
  
  • To attach any router to R3: attach 192.168.0.181 4003 192.168.2.1 3
  
  • To attach any router to R4: attach 192.168.0.181 4004 192.168.3.1 15
  
  ..and so on
  
If we attach R1 to R2: R1 acts like a client and binds itself to the server socket of R2. Here R1 only knows the R2's SimulatedIP information and only R1w will add R2 to it's neighbor list.

To send a Hello message from a client router, we type "start" on our console. This will send a hello message to each neighbo. This will aslo start updating the network's topology.

We can now find the shortest part to another router, for example if we want to find the shortest path to R4 -- we can do so by typing:
  
  "detect 192.168.3.1"
  
This will give a String output that will show the path it takes from the sourrce router to the destination router! It also includes the weight of each path.
  


  
