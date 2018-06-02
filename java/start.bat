REM args: me host:port, bootstrap host:port
start java -jar .\target\botnet_p2p-1.0-SNAPSHOT-jar-with-dependencies.jar 127.0.0.1:3000
start java -jar .\target\botnet_p2p-1.0-SNAPSHOT-jar-with-dependencies.jar 127.0.0.1:4000 127.0.0.1:3000


REM start java -jar .\target\botnet_p2p-1.0-SNAPSHOT-jar-with-dependencies.jar 127.0.0.1:5000 127.0.0.1:3000
REM start java -jar .\target\botnet_p2p-1.0-SNAPSHOT-jar-with-dependencies.jar 127.0.0.1:6000 127.0.0.1:3000