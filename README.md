GOSS-Server
===========
Current GOSS build status: ![GOSS build status](https://travis-ci.org/GridOPTICS/GOSS-Server.svg?branch=master)

The GOSS-Server module is available for local testing through your ide or from the command line.

 1. Clone the repository.
 2. Edit the sample property/configuration files in the src/main/resources folder (Pay close attention to the ports in the sampole-goss-core.cfg and sample-broker files)
 3. Open a command window to the root of the repository.
 4. execute ./gradlew build (Linux) or gradlew.bat (Windows)
 5. execute ./gradlew installApp (Creates install with libs and executable in build/install/goss-server)
 6. Linux 
  * build/install/goss-server/bin/goss-server --coreCfg=build/resources/main/sample-goss-core.properties --activemqCfg=build/resources/main/sample-broker-nosecurity.xml --dataCfg=build/resources/main/sample-datasources.cfg
 6. Windows
  * build\install\goss-server\bin\goss-server.bat --coreCfg=build/resources/main/sample-goss-core.properties --activemqCfg=build/resources/main/sample-broker-nosecurity.xml --dataCfg=build/resources/main/sample-datasources.cfg
 7. execute build/install/goss-server/bin/goss-server --help (To get available options)
 
You should now have the server running on your local system. In the startup log you should see the lines DEBUG Step: Starting Server Consumer and DEBUG Step: Server Consumer started, this means that it started successfully.

See [Core Server Configuration](https://github.com/GridOPTICS/GOSS-Server/wiki/Core-Server-Config) in order to build your own custom configuraiton.


