package com.arborealis.pixelpusher.osc;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
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
  static int LEDS_PER_PIECE = 64;

  OSCPortIn receiver;
  List<OSCListener> listeners;
  HashMap<PixelIndex, HSLColor> currentColors;

  OscMapping () {
    receiver = null;
    listeners = new ArrayList<OSCListener>();
    currentColors = new HashMap<PixelIndex, HSLColor>();
  }

  public class PixelIndex {
    private int tree;
    private int branch;
    private int pixel;

    public PixelIndex(int tree, int branch, int pixel) {
      this.tree = tree;
      this.branch = branch;
      this.pixel = pixel;
    }

    @Override
    public int hashCode() {
      return this.tree ^ this.branch ^ this.pixel;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      PixelIndex other = (PixelIndex) obj;
      if (this.tree != other.tree)
        return false;
      if (this.branch != other.branch)
        return false;
      if (this.pixel != other.pixel)
        return false;
      return true;
    }
  }

  public static double scale(final double valueIn, final double baseMin, final double baseMax,
                             final double limitMin, final double limitMax) {
    return ((limitMax - limitMin) * (valueIn - baseMin) / (baseMax - baseMin)) + limitMin;
  }

  private static List<String> splitAddress(String address) {
    final List<String> parts = new ArrayList<String>(Arrays.asList(address.split("/", -1)));
    if (address.startsWith("/")) {
      // as "/hello" gets split into {"", "hello"}, we remove the first empty entry,
      // so we end up with {"hello"}
      parts.remove(0);
    }
    if (address.endsWith("/")) {
      // as "hello/" gets split into {"hello", ""}, we also remove the last empty entry,
      // so we end up with {"hello"}
      parts.remove(parts.size() - 1);
    }
    return Collections.unmodifiableList(parts);
  }

  public double scaleHue(double hue) {
    return scale(hue, 0.0, 12.0, 0.0, 360.0);
  }
  
  public double scaleSaturation(double saturation) {
    return scale(saturation, 0.0, 1.0, 0.0, 100.0);
  }

  public double scaleLightness(double lightness) {
    return scale(lightness, 0.0, 2.0, 0.0, 100.0);
  }

  public void setUnscaledPixel(Strip strip, int tree, int branch, int pixel,
                               double hue, double saturation, double lightness) {
    HSLColor hslColor;
    try {
      hslColor = new HSLColor(scaleHue(hue), scaleSaturation(saturation),
                              scaleLightness(lightness), 1.0);
    } catch (java.lang.IllegalArgumentException e) {
      System.out.println(e);
      return;
    }

    setHSLPixel(strip, tree, branch, pixel, hslColor);
  }

  public HSLColor getHSLPixel(int tree, int branch, int pixel) {
    return currentColors.get(new PixelIndex(tree, branch, pixel));
  }

  public void setHSLPixel(Strip strip, int tree, int branch, int pixel, HSLColor hslColor) {
    if (strip == null) {
      System.out.println("Invalid strip for tree #" + tree + " branch #" + branch + " pixel #" + pixel);
      return;
    }

    if (hslColor == null) {
      System.out.println("Invalid HSL color for tree #" + tree + " branch #" + branch + " pixel #" + pixel);
      return;
    }

    Color rgbColor =  hslColor.getRGB();
    if (rgbColor == null) {
      System.out.println("Invalid RGB color for tree #" + tree + " branch #" + branch + " pixel #" + pixel);
      return;
    }

    currentColors.put(new PixelIndex(tree, branch, pixel), hslColor);

    //System.out.println("Setting tree #" + tree + " branch #" + branch + " pixel #" + pixel +
    //  " to HSL: " + hslColor.getHue() + ", " + hslColor.getSaturation() + ", " + hslColor.getLuminance() + 
    //  " or RGB: " + rgbColor.getRed() + ", " + rgbColor.getGreen() + ", " + rgbColor.getBlue());
    strip.setPixelRed((byte) rgbColor.getRed(), pixel);
    strip.setPixelGreen((byte) rgbColor.getGreen(), pixel);
    strip.setPixelBlue((byte) rgbColor.getBlue(), pixel);
  }
  
  public void registerStrips(final PixelPusher pusher, final int tree, final boolean debug) {
    OSCListener listener = new OSCListener() {
      public void acceptMessage(java.util.Date time, OSCMessage message) {
        if (debug) {
          System.out.println("Recieved OSC Message to: " + message.getAddress() +
                             " with: " + message.getArguments());
        }

        // /ArborTree/[0-2]/[0-7]/[0-100]/{H,S,L}
        List<String> addressParts = splitAddress(message.getAddress());
        if (addressParts.size() < 5) {
          System.out.println("Expected 5 address parts instead got:" + addressParts.size());
          return;
        }
        final int branch = Integer.parseInt(addressParts.get(2));
        final int piece = Integer.parseInt(addressParts.get(3));
        final String component = addressParts.get(4);

        List<Object> arguments = message.getArguments();
        List<Double> doubleArguments = new ArrayList<Double>();
        for (int i=0; i<arguments.size(); ++i) {
          if (arguments.get(i) instanceof Double) {
            doubleArguments.add((Double) arguments.get(i));
          } else if (arguments.get(i) instanceof Float) {
            doubleArguments.add(((Float) arguments.get(i)).doubleValue());
          } else if (arguments.get(i) instanceof Integer) {
            doubleArguments.add(((Integer) arguments.get(i)).doubleValue());
          } else {
            System.out.println("Expected double, float, or int at position " + i + " instead got " + arguments.get(i).getClass());
            return;
          }

          if (!(arguments.get(i) instanceof Double) && !(arguments.get(i) instanceof Float) && !(arguments.get(i) instanceof Integer)) {
            System.out.println("Expected double at position " + i + " instead got " + arguments.get(i).getClass());
            return;
          }
        }

        int startPixel = piece * LEDS_PER_PIECE;
        for (int p=0; p<doubleArguments.size(); ++p) {
          int pixel = startPixel + p;
          HSLColor hslColor = getHSLPixel(tree, branch, pixel);
          if (hslColor == null) {
            hslColor = new HSLColor(180.0, 100.0, 100.0);
          }
          
          try {
            if (component.equalsIgnoreCase("H")) {
              hslColor.setHue(scaleHue(doubleArguments.get(p)));
            } else if (component.equalsIgnoreCase("S")) {
              hslColor.setSaturation(scaleSaturation(doubleArguments.get(p)));
            } else if (component.equalsIgnoreCase("L")) {
              hslColor.setLuminance(scaleLightness(doubleArguments.get(p)));
            } else {
              System.out.println("Component must be H, S, or L. Not '" + component + "'.");
              return;
            }
          } catch (java.lang.IllegalArgumentException e) {
            System.out.println(e);
            return;
          }

          Strip strip;
          try {
            strip = pusher.getStrip(branch);
          } catch (java.lang.ArrayIndexOutOfBoundsException e) {
            System.out.println("No branch configured for: " + branch);
            return;
          }
          if (strip == null) {
            System.out.println("No strip at position: " + branch);
            return;
          }
          setHSLPixel(strip, tree, branch, pixel, hslColor);
        }
      }
    };
    String oscAddress = "/ArborTree/" + tree + "/[0-7]/[0-1000]/{H,S,L}";
    System.out.println("Registered OSC address: " + oscAddress);
    receiver.addListener(oscAddress, listener);
    listeners.add(listener);
  }

  public void registerPixels(final PixelPusher pusher, final int tree, final boolean debug) {
    OSCListener listener = new OSCListener() {
      public void acceptMessage(java.util.Date time, OSCMessage message) {
        if (debug) {
          System.out.println("Recieved OSC Message to: " + message.getAddress() +
                             " with: " + message.getArguments());
        }

        // /ArborTree/[0-2]/[0-7]/*/[0-179]/HSL
        List<String> addressParts = splitAddress(message.getAddress());
        if (addressParts.size() < 6) {
          System.out.println("Expected 6 address parts instead got:" + addressParts.size());
          return;
        }
        final int branch = Integer.parseInt(addressParts.get(2));
        final int piece = Integer.parseInt(addressParts.get(3));
        final int pixelInPiece = Integer.parseInt(addressParts.get(4));
        final int pixel = piece * LEDS_PER_PIECE + pixelInPiece;

        List<Object> arguments = message.getArguments();
        if (arguments.size() < 3) {
          System.out.println("Expected at least 3 arguments instead got:" + arguments.size());
          return;
        }
        List<Double> doubleArguments = new ArrayList<Double>();
        for (int i=0; i<arguments.size(); ++i) {
          if (arguments.get(i) instanceof Double) {
            doubleArguments.add((Double) arguments.get(i));
          } else if (arguments.get(i) instanceof Float) {
            doubleArguments.add(((Float) arguments.get(i)).doubleValue());
          } else if (arguments.get(i) instanceof Integer) {
            doubleArguments.add(((Integer) arguments.get(i)).doubleValue());
          } else {
            System.out.println("Expected double, float, or int at position " + i + " instead got " + arguments.get(i).getClass());
            return;
          }

          if (!(arguments.get(i) instanceof Double) &&
              !(arguments.get(i) instanceof Float) &&
              !(arguments.get(i) instanceof Integer)) {
            System.out.println("Expected double at position " + i + " instead got " + arguments.get(i).getClass());
            return;
          }
        }

        double unscaledHue = doubleArguments.get(0);
        double unscaledSaturation = doubleArguments.get(1);
        double unscaledLightness = doubleArguments.get(2);

        Strip strip;
        try {
          strip = pusher.getStrip(branch);
        } catch (java.lang.ArrayIndexOutOfBoundsException e) {
          System.out.println("No branch configured for: " + branch);
          return;
        }
        if (strip == null) {
          System.out.println("No strip at position: " + branch);
          return;
        }
        setUnscaledPixel(strip, tree, branch, pixel,
                         unscaledHue, unscaledSaturation, unscaledLightness);
      }
    };
    String oscAddress = "/ArborTree/" + tree + "/[0-8]/[0-1000]/[0-1000]/HSL";
    System.out.println("Registered OSC address: " + oscAddress);
    receiver.addListener(oscAddress, listener);
    listeners.add(listener);
  }

  public void generateMapping(List<PixelPusher> pushers, boolean debug) {
    if (receiver != null) {
      receiver.stopListening();
      listeners.clear();
    }

    int oscPortNum = 7000;//OSCPort.defaultSCOSCPort();
    try {
      receiver = new OSCPortIn(oscPortNum);
    } catch (java.net.SocketException e) {
      System.out.println("Could not connect to OSC port: " + oscPortNum);
      return;
    }

    for (final PixelPusher pusher : pushers) {
      int tree = pusher.getControllerOrdinal();
      registerPixels(pusher, tree, debug);
      registerStrips(pusher, tree, debug);
    }
    receiver.startListening();
  }
}
