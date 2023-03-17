package socs.network;

import socs.network.node.Router;
import socs.network.util.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {

  public static void main(String[] args) throws UnknownHostException {
    if (args.length != 1) {
      System.out.println("usage: program conf_path");
      System.exit(1);
    }
    // Start the Router
    Router r = new Router(new Configuration(args[0]));
  }
}

