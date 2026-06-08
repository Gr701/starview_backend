package com.o3.server;

import com.sun.net.httpserver.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.o3.server.managers.AuthenticationManager;
import com.o3.server.handlers.DatarecordHandler;
import com.o3.server.handlers.ProfileHandler;
import com.o3.server.handlers.RegistrationHandler;
import com.o3.server.handlers.SearchHandler;
import com.o3.server.handlers.CommentHandler;
import com.o3.server.handlers.CollectionHandler;

public class Server {
    private Server() {}

    /*
     * Https stuff, SSL 
     */
    private static SSLContext myServerSSLContext(String[] args) throws Exception {
        //char[] passphrase = "progr3key".toCharArray();
        char[] passphrase = args[1].toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(args[0]), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ssl;
    }

    public static void main(String[] args) throws Exception {
        try {
            //System.out.println(WeatherManager.getWeather(2.0, 4.0, "2025-03-19T07:28:20.065Z"));

            //create the http server to port 8001 with default logger
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001),0);


            SSLContext sslContext = myServerSSLContext(args);
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                    //InetSocketAddress remote = params.getClientAddress();
                    SSLContext c = getSSLContext();
                    SSLParameters sslparams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslparams);
                }
            });

            AuthenticationManager authenticationManager = new AuthenticationManager();

            //create context that defines path for the resource
            server.createContext("/registration", new RegistrationHandler()); 

            HttpContext datarecordContext = 
                server.createContext("/datarecord", new DatarecordHandler());
            datarecordContext.setAuthenticator(authenticationManager);

            HttpContext searchContext = server.createContext("/search", new SearchHandler());
            searchContext.setAuthenticator(authenticationManager);
            
            HttpContext profileContext = server.createContext("/profile", new ProfileHandler());
            profileContext.setAuthenticator(authenticationManager);

            HttpContext commentContext = server.createContext("/comment", new CommentHandler());
            commentContext.setAuthenticator(authenticationManager);

            HttpContext collectionContext = 
                server.createContext("/collection", new CollectionHandler());
            collectionContext.setAuthenticator(authenticationManager);

            // creates a default executor
            //server.setExecutor(null); 
            server.setExecutor(Executors.newCachedThreadPool()); 
            server.start(); 
            System.out.println("\nServer started");
        } catch (FileNotFoundException e) {
            System.out.println("Certificate not found");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
