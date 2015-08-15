# pixelpusher-osc
Use OSC to control a PixelPusher.

Directions:

$ mvn install:install-file -Dfile=lib/PixelPusher.jar -DgroupId=com.heroicrobot -DartifactId=PixelPusher -Dversion=1.0 -Dpackaging=jar

$ mvn compile

$ mvn package

$ java -cp target/OscBridge-1.0-SNAPSHOT-jar-with-dependencies.jar com.arborealis.pixelpusher.osc.OscBridge
