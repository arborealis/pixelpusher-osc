package com.arborealis.pixelpusher.osc;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Observable;

import java.util.Observer;

import com.heroicrobot.dropbit.devices.pixelpusher.PixelPusher;
import com.heroicrobot.dropbit.registry.DeviceRegistry;

class PixelPusherObserver implements Observer {
  public boolean debug = false;
  public boolean hasStrips = false;
  public OscMapping mapping = new OscMapping();
  public HashMap<String, PixelPusher> knownPushers = new HashMap<String, PixelPusher>();
  
  public boolean hasSignificantChange(PixelPusher updatedDevice) {
    PixelPusher known = knownPushers.get(updatedDevice.getMacAddress());
    if (known == null) {
      knownPushers.put(updatedDevice.getMacAddress(), updatedDevice);
      return true;
    }
    if (known.getNumberOfStrips() != updatedDevice.getNumberOfStrips()) {
      knownPushers.put(updatedDevice.getMacAddress(), updatedDevice);
      return true;
    }
    if (known.getPixelsPerStrip() != updatedDevice.getPixelsPerStrip()) {
      knownPushers.put(updatedDevice.getMacAddress(), updatedDevice);
      return true;
    }

    // otherwise it's not a significant enough change to trigger a remap,
    // but we should remember that it changed.
    knownPushers.put(updatedDevice.getMacAddress(), updatedDevice);
    return false;
  }
  
  public void update(Observable registry, Object updatedDevice) {
     //logging.info("Registry changed!");
    DeviceRegistry deviceRegistry = (DeviceRegistry) registry;
    if (updatedDevice != null) {
      if (updatedDevice instanceof PixelPusher) {
        if (hasSignificantChange((PixelPusher)updatedDevice)) {
          mapping.generateMapping(deviceRegistry.getPushers(), debug);
          System.out.println("Device change: " + updatedDevice);
        }
      } else {
        System.out.println("Registry:  updated device was not a PixelPusher!");
      }
    }
    this.hasStrips = true;
  }
}
