package com.arborealis.pixelpusher.osc;

import java.net.InetAddress;

public class OscLocation {

  private int universe;
  private int channel;
  private InetAddress multicast;

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + channel;
    result = prime * result + universe;
    return result;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    OscLocation other = (OscLocation) obj;
    if (channel != other.channel)
      return false;
    if (universe != other.universe)
      return false;
    return true;
  }

  /**
   * @return the universe
   */
  public int getUniverse() {
    return universe;
  }

  /**
   * @param universe
   *          the universe to set
   */
  public void setUniverse(int universe) {
    this.universe = universe;
  }

  /**
   * @return the channel
   */
  public int getChannel() {
    return channel;
  }

  /**
   * @param channel
   *          the channel to set
   */
  public void setChannel(int channel) {
    this.channel = channel;
  }

  public OscLocation(int universe, int channel, InetAddress location) {
    this.universe = universe;
    this.channel = channel;
    this.multicast = location;
  }

public InetAddress getMulticast() {
	return multicast;
}

public void setMulticast(InetAddress multicast) {
	this.multicast = multicast;
}

}
