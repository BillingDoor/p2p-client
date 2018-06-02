REM args: me host:port, bootstrap host:port
start java -Dme.host=127.0.0.1 -Dme.port=3000 -jar  .\target\botnet_p2p-1.0-SNAPSHOT-jar-with-dependencies.jar
start java -Dme.host=127.0.0.1 -Dme.port=4000 -Dbootstrap -Dboot.host=127.0.0.1 -Dboot.port=3000 -jar .\target\botnet_p2p-1.0-SNAPSHOT-jar-with-dependencies.jar


start java -Dme.host=127.0.0.1 -Dme.port=5000 -Dbootstrap -Dboot.host=127.0.0.1 -Dboot.port=3000 -jar .\target\botnet_p2p-1.0-SNAPSHOT-jar-with-dependencies.jar
REM start java -jar .\target\botnet_p2p-1.0-SNAPSHOT-jar-with-dependencies.jar 127.0.0.1:6000 127.0.0.1:3000