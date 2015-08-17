package com.arborealis.pixelpusher.osc;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPort;
import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.utility.OSCByteArrayToJavaConverter;

import com.heroicrobot.dropbit.devices.pixelpusher.PixelPusher;
import com.heroicrobot.dropbit.devices.pixelpusher.Strip;
import com.heroicrobot.dropbit.registry.DeviceRegistry;


public class OscMapping {
  OSCPortIn receiver;
  List<OSCListener> listeners;

  OscMapping () {
    receiver = null;
    listeners = new ArrayList<OSCListener>();
  }

  public static double scale(final double valueIn, final double baseMin, final double baseMax, final double limitMin, final double limitMax) {
    return ((limitMax - limitMin) * (valueIn - baseMin) / (baseMax - baseMin)) + limitMin;
  }

  public void generateMapping(List<PixelPusher> pushers) {
    if (receiver != null) {
      receiver.stopListening();
      listeners.clear();
    }

    int oscPortNum = OSCPort.defaultSCOSCPort();
    try {
      receiver = new OSCPortIn(oscPortNum);
    } catch (java.net.SocketException e) {
      System.out.println("Could not connect to OSC port: " + oscPortNum);
      return;
    }

    for (final PixelPusher pusher : pushers) {
      int tree = pusher.getGroupOrdinal();
      int controller = pusher.getControllerOrdinal();
      int numberOfStrips = pusher.getNumberOfStrips();
      int pixelsPerStrip = pusher.getPixelsPerStrip();

      for (int s = 0; s < numberOfStrips; ++s) {
        final int stripIndex = s;
        final int branch = (controller * 8) + s;
        for (int p = 0; p < pixelsPerStrip; ++p) {
          final int pixelIndex = p;
          OSCListener listener = new OSCListener() {
            public void acceptMessage(java.util.Date time, OSCMessage message) {
              List<Object> arguments = message.getArguments();
              if (arguments.size() != 3) {
                System.out.println("Expected 3 doubles instead got:" + arguments.size());
                return;
              }
              for (int i=0; i<arguments.size(); ++i) {
                if (!(arguments.get(i) instanceof Double)) {
                  System.out.println("Expected double at position " + i + " instead got " + arguments.get(i).getClass());
                  return;
                }
              }

              double unscaledHue = (Double)arguments.get(0);
              double unscaledSaturation = (Double)arguments.get(1);
              double unscaledLightness = (Double)arguments.get(1);
              double hue = scale(unscaledHue, 0.0, 12.0, 0.0, 360.0);
              double saturation = scale(unscaledSaturation, 0.0, 1.0, 0.0, 100.0);
              double lightness = scale(unscaledLightness, 0.0, 2.0, 0.0, 100.0);
              HSLColor hslColor = new HSLColor(hue, saturation, lightness, 1.0);

              Color rgbColor = hslColor.getRGB();
              Strip strip = pusher.getStrip(stripIndex);
              strip.setPixelRed((byte) rgbColor.getRed(), pixelIndex);
              strip.setPixelBlue((byte) rgbColor.getBlue(), pixelIndex);
              strip.setPixelGreen((byte) rgbColor.getGreen(), pixelIndex);
            }
          };
          receiver.addListener("/ArborTree/" + tree + "/" + branch + "/0/" + p + "/", listener);
          listeners.add(listener);
        }
      }
    }
    receiver.startListening();
  }
}
