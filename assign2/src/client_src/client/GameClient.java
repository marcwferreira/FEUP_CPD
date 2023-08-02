package client;

import java.net.*;
import java.sql.ClientInfoStatus;
import java.io.*;
import java.util.Scanner;

import javax.security.auth.login.LoginContext;

import org.json.simple.JSONArray;
import org.json.simple.parser.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import myutils.*;
 

public class GameClient {

    public static void main(String[] args){
        if(args.length < 2)
            return;

    
        String filePath = "default";
        if(args.length >= 3)
            filePath = "./classes/client/data/"+args[2]+".json";

        Boolean restoreActive = true;    
        if(args.length == 4 && args[3].equals("unchecked"))
            restoreActive = false;

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        boolean executionStatus = false;
        ClientHandler client = new ClientHandler(hostname, port, filePath);

        try{
            executionStatus = client.runClient(!restoreActive);
        } catch (Exception e){
            //e.printStackTrace();
        }
        
        if(executionStatus) System.exit(0);
        else System.exit(1);
    }
    
}