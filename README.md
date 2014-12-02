GOSS-Server
===========
Current GOSS build status: ![GOSS build status](https://travis-ci.org/GridOPTICS/GOSS-Server.svg?branch=master)

The GOSS-Server module is available for local testing through your ide or from the command line.

 1. Clone the repository.
 2. Edit the sample property/configuration files in the src/main/resources folder (Pay close attention to the ports in the sampole-goss-core.cfg and sample-broker files)
 3. Open a command window to the root of the repository.
 4. execute ./gradlew build (Linux) or gradlew.bat (Windows)
 5. execute ./gradlew installApp (Creates install with libs and executable in build/install/goss-server)
 6. execute build/install/goss-server/bin/goss-server --coreCfg=build/resources/main/sample-goss-core.properties
 7. execute build/install/goss-server/bin/goss-server --help (To get available options)
 
You should now have the server running on your local system. 


