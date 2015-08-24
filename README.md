# pixelpusher-osc
Use OSC to control a PixelPusher.

Setup:

If you have Yosemite or later it doesn't ship with java:
* $ brew install caskroom/cask/brew-cask
* $ brew cask install java
* $ brew install maven

Directions:

* $ mvn install:install-file -Dfile=lib/PixelPusher.jar -DgroupId=com.heroicrobot.dropbit.devices.pixelpusher -DartifactId=PixelPusher -Dversion=1.0 -Dpackaging=jar
* $ mvn compile
* $ mvn package
* $ java -cp target/OscBridge-1.0-SNAPSHOT-jar-with-dependencies.jar com.arborealis.pixelpusher.osc.OscBridge
