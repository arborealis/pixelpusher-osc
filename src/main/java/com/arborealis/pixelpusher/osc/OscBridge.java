package com.arborealis.pixelpusher.osc;

import com.heroicrobot.dropbit.registry.DeviceRegistry;

public class OscBridge {
  private static PixelPusherObserver observer;
  static DeviceRegistry registry;

  /**
   * @param args
   */
  public static void main(String[] args) {
    boolean debug = true;
    if (args.length > 0) {
      debug = Boolean.parseBoolean(args[0]);
    }
    if (debug) {
      System.out.println("Debug output on.");
    }

    observer = new PixelPusherObserver();
    observer.debug = debug;
    registry = new DeviceRegistry();
    registry.setLogging(debug);
    registry.addObserver(observer);
    registry.startPushing();
    registry.setAutoThrottle(false);
  }
}
