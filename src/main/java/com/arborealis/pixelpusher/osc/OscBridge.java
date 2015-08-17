package com.arborealis.pixelpusher.osc;

import com.heroicrobot.dropbit.registry.DeviceRegistry;

public class OscBridge {	
  private static PixelPusherObserver observer;
  static DeviceRegistry registry;

  /**
   * @param args
   */
  public static void main(String[] args) {
    observer = new PixelPusherObserver();
    registry = new DeviceRegistry();
    registry.addObserver(observer);
    registry.startPushing();
    registry.setAutoThrottle(false);
  }
}
