/*
    Copyright (c) 2014, Battelle Memorial Institute
    All rights reserved.
    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:
    1. Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.
    2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.
    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
    ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
    WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE

    DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
    ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
    (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
    LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
    ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
    The views and conclusions contained in the software and documentation are those
    of the authors and should not be interpreted as representing official policies,
    either expressed or implied, of the FreeBSD Project.
    This material was prepared as an account of work sponsored by an
    agency of the United States Government. Neither the United States
    Government nor the United States Department of Energy, nor Battelle,
    nor any of their employees, nor any jurisdiction or organization
    that has cooperated in the development of these materials, makes
    any warranty, express or implied, or assumes any legal liability
    or responsibility for the accuracy, completeness, or usefulness or
    any information, apparatus, product, software, or process disclosed,
    or represents that its use would not infringe privately owned rights.
    Reference herein to any specific commercial product, process, or
    service by trade name, trademark, manufacturer, or otherwise does
    not necessarily constitute or imply its endorsement, recommendation,
    or favoring by the United States Government or any agency thereof,
    or Battelle Memorial Institute. The views and opinions of authors
    expressed herein do not necessarily state or reflect those of the
    United States Government or any agency thereof.
    PACIFIC NORTHWEST NATIONAL LABORATORY
    operated by BATTELLE for the UNITED STATES DEPARTMENT OF ENERGY
    under Contract DE-AC05-76RL01830
*/
package pnnl.goss.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pnnl.goss.core.GossCoreContants;
import pnnl.goss.core.server.GossDataServices;
import pnnl.goss.core.server.internal.BasicDataSourceCreatorImpl;
import pnnl.goss.core.server.internal.GossDataServicesImpl;
import pnnl.goss.core.server.internal.GossRequestHandlerRegistrationImpl;
import pnnl.goss.core.server.internal.GridOpticsServer;
import pnnl.goss.util.Utilities;


public class ServerMain {
    private static String CMD_DATACFG = "dataCfg";
    private static String CMD_ACTIVEMQ = "activemqCfg";
    private static String CMD_CORECFG = "coreCfg";

    private static Logger log = LoggerFactory.getLogger(ServerMain.class);

    public void attachShutdownHook(){
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Shutdown server main");
            }
        });

    }

    @SuppressWarnings("rawtypes")
    public static void outputConfig(Dictionary dictionary){

        Enumeration dictEnumeration = dictionary.keys();
        boolean hasOne = false;

        while(dictEnumeration.hasMoreElements()){
            String element = (String)dictEnumeration.nextElement();
            log.debug("\t"+element+" => "+ dictionary.get(element));
            hasOne = true;
        }

        if (!hasOne){
            log.debug("\tNo config elements present");
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void replacePropertiesFromHome(Dictionary toReplace, String propertiesFile){
        File gossProperties = new File(System.getProperty("user.home")+"\\.goss\\"+propertiesFile);

        if(!gossProperties.exists()){
            log.error("Properties File Doesn't exist!\n\t"+gossProperties.toString());
            return;
        }

        Dictionary privateProperties = Utilities.loadProperties(gossProperties.getAbsolutePath(), false);

        Enumeration propEnum = toReplace.keys();

        while(propEnum.hasMoreElements()){
            String k = (String) propEnum.nextElement();
            String v = (String) toReplace.get(k);

            if (v != null && v.startsWith("${") && v.endsWith("}")){
                String keyInPrivate = v.substring(2, v.length() - 1);
                if (privateProperties.get(keyInPrivate) != null){
                    toReplace.put(k, privateProperties.get(keyInPrivate));
                }
            }
        }
    }

    @SuppressWarnings("static-access")
    public Options buildOptions(){

        Options options = new Options();

        options.addOption(OptionBuilder.withArgName("help")
                .withDescription("Prints this message.")
                .create("help"));


        options.addOption(OptionBuilder.withArgName("dataCfg")
                .hasArg()
                .withDescription( "Specifies the datasource configuration file.")
                .create(CMD_DATACFG));

        options.addOption(OptionBuilder.withArgName("broker")
                .hasArg()
                .withDescription("An activemq broker configuration file.")
                .create(CMD_ACTIVEMQ));

        options.addOption(OptionBuilder.withArgName("coreCfg")
                .hasArg()
                .withDescription("Specifies core configuration parameters.")
                .create(CMD_CORECFG));



        return options;
    }

    @SuppressWarnings("rawtypes")
    public Dictionary loadProperties(String fileName) {
        if (fileName == null){
            return null;
        }

        boolean exceptionHandled = false;
        File file = new File(fileName);
        Dictionary dict = new Hashtable();
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(file));
        } catch (IOException e) {
            exceptionHandled = true;
        }
        if (exceptionHandled){
            return null;
        }
         return props;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void main(String[] args) throws ParseException {

        ServerMain serverMain = new ServerMain();
        Options options = serverMain.buildOptions();
        String brokerFile = null;
        String coreFile = null;
        String datasourceFile = null;
        boolean startBroker = false;

        //CommandLineParser parser = new PosixParser().parse(options,args);
        CommandLine cmd = null;
        try{
            cmd = new PosixParser().parse(options, args); //parser.parse(options, args,true);

            if (cmd.hasOption("help")){
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( "ant", options );
                return;
            }

            if (!cmd.hasOption(CMD_CORECFG)) {
                Option ops = options.getOption(CMD_CORECFG);
                System.err.println("Required "+ ops.getArgName());
                return;
            }

            coreFile = cmd.getOptionValue(CMD_CORECFG);

            if (cmd.hasOption(CMD_ACTIVEMQ)){
                brokerFile = cmd.getOptionValue(CMD_ACTIVEMQ);
            }

            if (cmd.hasOption(CMD_DATACFG)){
                datasourceFile = cmd.getOptionValue(CMD_DATACFG);
            }

        }
        catch( ParseException exp ) {
            System.err.println( "Unexpected exception: " + exp.getMessage() );
            return;
        }

        Option ops = null;
        Dictionary dataSourcesCfg = serverMain.loadProperties(datasourceFile);
        if (dataSourcesCfg == null){
            dataSourcesCfg = new Hashtable();
        }


        Dictionary coreConfig = serverMain.loadProperties(coreFile);
        if (coreConfig == null){
            ops = options.getOption(CMD_CORECFG);
            log.error("Invalid "+ops.getArgName() + " path\n\t"+
                    coreFile);
            return;
        }

        if (brokerFile != null){
            File bf = new File(brokerFile);
            if (!bf.exists()){
                ops = options.getOption(CMD_ACTIVEMQ);
                log.error("Invalid "+ops.getArgName() + " path\n\t"+
                        brokerFile);
                return;
            }
        }

        serverMain.attachShutdownHook();

        log.debug("CORE CONFIGURATION");
        outputConfig(coreConfig);

        log.debug("DATASOURCES CONFIGURATION");
        outputConfig(dataSourcesCfg);

        GossDataServices dataServices = new GossDataServicesImpl(
                new BasicDataSourceCreatorImpl(), dataSourcesCfg);
        GossRequestHandlerRegistrationImpl registrationService =
                new GossRequestHandlerRegistrationImpl(dataServices);

        registrationService.addHandlersFromClassPath();

        if (brokerFile != null){
            log.debug("BROKER FILE: "+ brokerFile);
            //log.debug("REPLACED: " + brokerFile.replaceAll("\\\\", "/"));
            coreConfig.put(GossCoreContants.PROP_ACTIVEMQ_CONFIG,
                    brokerFile.replaceAll("\\\\", "/"));
            startBroker = true;
        }

        GridOpticsServer server = new GridOpticsServer(registrationService,
                coreConfig, startBroker);

//        Dictionary dataSourcesConfig = Utilities.loadProperties(PROP_DATASOURCES_CONFIG);
//        // Replaces the ${..} with values from the goss.properties file.
//        replacePropertiesFromHome(dataSourcesConfig, "goss.properties");
//
//        Dictionary coreConfig = Utilities.loadProperties(PROP_CORE_CONFIG);


//        log.debug("CORE CONFIGURATION");
//        outputConfig(coreConfig);
//
//        log.debug("DATASOURCES CONFIGURATION");
//        outputConfig(dataSourcesConfig);
//
//        // Pass the datasourcesconfig to the constructor so that it is available.
//        GossDataServices dataServices = new GossDataServicesImpl(dataSourcesConfig);
//
//        GossRequestHandlerRegistrationImpl registrationService = new GossRequestHandlerRegistrationImpl(dataServices);
//        registrationService.setCoreServerConfig(coreConfig);
//        registrationService.addHandlersFromClassPath();
//
//        @SuppressWarnings("unused")
//        GridOpticsServer server = new GridOpticsServer(registrationService, true);



        //PowergridModel t = new PowergridModel();

        //PowergridServerActivator pgActivator = new PowergridServerActivator(registrationService, dataServices);
        //RequestHandlerFinder finder = new RequestHandlerFinder(registrationService);
//		FusionDBServerActivator fusionDBServerActivator = new FusionDBServerActivator(registrationService, dataServices);
//		fusionDBServerActivator.update(dataSourcesConfig);
//		fusionDBServerActivator.start();
//
//
//		MDARTServerActivator mdartServerActivator = new MDARTServerActivator(registrationService, dataServices);
//		mdartServerActivator.update(dataSourcesConfig);
//		mdartServerActivator.start();
//
//		KairosDBServerActivator kairosDBServerActivator = new KairosDBServerActivator(registrationService, dataServices);
//		kairosDBServerActivator.update(dataSourcesConfig);
//		kairosDBServerActivator.start();
//
//		GridMWServerActivator gridmwServerActivator = new GridMWServerActivator(registrationService, dataServices);
//		gridmwServerActivator.update(dataSourcesConfig);
//		gridmwServerActivator.start();
//
//
//		PowergridServerActivator pgActivator = new PowergridServerActivator(registrationService, dataServices);
//		pgActivator.start();
//
//		GridpackServerActivator gridpackActivator = new GridpackServerActivator(registrationService, dataServices);
//		gridpackActivator.start();

//		GossSecurityDemoActivator demoActivator = new GossSecurityDemoActivator(registrationService, dataServices);
//		demoActivator.updated(dataSourcesConfig);
//		demoActivator.start();
        //Not sure that the webapp will run under this instance

        //GridpackServerActivator gridpackActivator = new GridpackServerActivator()

        //Fusion Launchers----------------------------------------------
//		DataStreamLauncher launcher = new DataStreamLauncher();
//		launcher.startLauncher();


        //TODO: All the lines below to be removed after all the bundles are tested.
        //GossRequestHandlerRegistrationService handlers = new GossRequestHandlerRegistrationImpl(dataServices);

        //------------------------------------Powergrid(CA)----------------------------------------
        /*handlers.addHandlerMapping(RequestPowergrid.class, RequestPowergridHandler.class);
        handlers.addHandlerMapping(RequestPowergridTimeStep.class, RequestPowergridHandler.class);


        //---------------------------Performance Testing-------------------------------------------
        handlers.addHandlerMapping(RequestGridMWTest.class, RequestGridMWTestHandler.class);
        handlers.addHandlerMapping(RequestGridMWAsyncTest.class, RequestGridMWTestHandler.class);
        handlers.addHandlerMapping(RequestKairosTest.class, RequestKairosTestHandler.class);
        handlers.addHandlerMapping(RequestKairosAsyncTest.class, RequestKairosTestHandler.class);
        handlers.addHandlerMapping(RequestLineLoadTest.class, RequestLineLoadTestHandler.class);
        handlers.addHandlerMapping(RequestLineLoadAsyncTest.class, RequestLineLoadTestHandler.class);

        //---------------------------Performance Testing Security-----------------------------------
        handlers.addSecurityMapping(RequestGridMWTest.class, AccessControlHandlerAllowAll.class);
        handlers.addSecurityMapping(RequestGridMWAsyncTest.class, AccessControlHandlerAllowAll.class);
        handlers.addSecurityMapping(RequestKairosTest.class, AccessControlHandlerAllowAll.class);
        handlers.addSecurityMapping(RequestKairosAsyncTest.class, AccessControlHandlerAllowAll.class);
        handlers.addSecurityMapping(RequestLineLoadTest.class, AccessControlHandlerAllowAll.class);
        handlers.addSecurityMapping(RequestLineLoadAsyncTest.class, AccessControlHandlerAllowAll.class);

        //-------------------------------------PMU(GridMW)-----------------------------------------
        handlers.addHandlerMapping(RequestPMU.class, RequestPMUHandler.class);
        //handlers.addSecurityMapping(RequestPMU.class, AccessControlHandlerPMU.class);
        handlers.addHandlerMapping(RequestPMUMetaData.class, RequestPMUMetadataHandler.class);
        handlers.addHandlerMapping(RequestPMUKairos.class, RequestPMUKairosHandler.class);


        //--------------------------------Shared Perspective---------------------------------------
        handlers.addHandlerMapping(RequestTopology.class, RequestTopologyHandler.class);
        handlers.addHandlerMapping(RequestLineLoadTest.class, RequestLineLoadTestHandler.class);
        handlers.addHandlerMapping(RequestContingencyResult.class, RequestContingencyResultHandler.class);
        handlers.addHandlerMapping(RequestLineLoad.class, RequestLineLoadHandler.class);
        handlers.addHandlerMapping(RequestContingencyResult.class, RequestContingencyResultHandler.class);
        handlers.addHandlerMapping(RequestAlertContext.class, RequestAlertHandler.class);
        handlers.addHandlerMapping(RequestAlerts.class, RequestAlertHandler.class);

        //--------------------------------Shared Perspective Security---------------------------------------
        //handlers.addSecurityMapping(RequestLineLoadTest.class, AccessControlHandlerAllowAll.class);

        //-------------------------------------HPC-------------------------------------------------
        //handlers.addHandlerMapping(ExecuteRequest.class, ExecuteHPCHandler.class);




        //-------------------------------------Fusion Security----------------------------------------------
        handlers.addSecurityMapping(RequestActualTotal.class, AccessControlHandlerAllowAll.class);
        handlers.addSecurityMapping(RequestCapacityRequirement.class, AccessControlHandlerAllowAll.class);
        handlers.addSecurityMapping(RequestForecastTotal.class, AccessControlHandlerAllowAll.class);
        handlers.addSecurityMapping(RequestHAInterchangeSchedule.class, AccessControlHandlerAllowAll.class);
        handlers.addSecurityMapping(RequestRTEDSchedule.class, AccessControlHandlerAllowAll.class);
        handlers.addSecurityMapping(RequestPowergrid.class, AccessControlHandlerAllowAll.class);
        handlers.addSecurityMapping(RequestGeneratorData.class, AccessControlHandlerAllowAll.class);


        //-------------------------------------Tutorial----------------------------------------------
        handlers.addUploadHandlerMapping("Tutorial", TutorialDesktopHandler.class);
        handlers.addHandlerMapping(TutorialDownloadRequestSync.class, TutorialDesktopDownloadHandler.class);
        //Start launcher and aggregators
        String[] arguments = new String[] {};
        //Start aggregator and generator listening so they can be started by web ui
         *
         */

        //try {
            //Dictionary config = Utilities.loadProperties(powergridDatasourceConfig);

            //PowergridDataSources.instance().addConnections(config, "datasource");
            //Dictionary coreConfig = Utilities.loadProperties("config.properties");
            //handlers.setCoreServerConfig(coreConfig);
            //@SuppressWarnings("unused")
            //GridOpticsServer server = new GridOpticsServer(registrationService, true);

            //Launch the generator and aggregator listeners
            //new AggregatorLauncher().start();
            //new GeneratorLauncher().start();
            //new PythonJavaGatewayLauncher().start();

        /*} catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidDatasourceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/


    }

}
