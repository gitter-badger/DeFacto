/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.defacto.webservices;

/**
 *
 * @author ngonga
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;

import org.aksw.defacto.Defacto;
import org.aksw.defacto.config.DefactoConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author ngonga
 */
public class ServiceMain {

    static Logger log = Logger.getLogger(ServiceMain.class.toString());
    

    private static URI getBaseURI() {
    	
    	try {
			Defacto.DEFACTO_CONFIG = new DefactoConfig(new Ini(new File("defacto.ini")));
		} catch (InvalidFileFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
        return UriBuilder.fromUri(Defacto.DEFACTO_CONFIG.getStringSetting("server", "ip")).
        		port(Defacto.DEFACTO_CONFIG.getIntegerSetting("server", "port")).build();
    }
    public static final URI BASE_URI = getBaseURI();

    protected static HttpServer startServer() throws IOException {
        System.out.println("Starting grizzly...");
        ResourceConfig rc = new PackagesResourceConfig("org.aksw.defacto.webservices");
        return GrizzlyServerFactory.createHttpServer(BASE_URI, rc);
    }

    public static void main(String[] args) throws IOException {
        try {
        	
            FileHandler fh = new FileHandler("DeFacto.log");
            log.addHandler(fh);
            //logger.setLevel(Level.ALL);  
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);

            // the following statement is used to log any messages  
            log.info("Service started");
            HttpServer httpServer = startServer();
            System.out.println(String.format("Jersey app started with WADL available at "
                    + "%sapplication.wadl\nTry out %sgetdefactoscore"
                    + "\nHit enter to stop it...",
                    BASE_URI, BASE_URI));
            System.in.read();
            httpServer.stop();
        } catch (Exception e) {
            log.warning("Messed up handlers");
            e.printStackTrace();
        }
    }
//    public static void old_main(String[] args) throws IOException {
//        // Port can be pulled from an environmental variable
//        // Ex: System.getenv("SERVICE_PORT")
//        Main m = new Main();
//        final Map<String, String> initParams = new HashMap<String, String>();
//        // Classes under resources and its subpackages will be included when routing requests
//        initParams.put("com.sun.jersey.config.property.packages", "org.aksw.simba.rdf2nl");
//        final String baseUri = "http://localhost:9998/";
//        SelectorThread st = GrizzlyWebContainerFactory.create(baseUri, initParams);
//        System.in.read();
//        st.stopEndpoint();
//        System.exit(0);
//    }
}
