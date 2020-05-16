import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.sql.Savepoint;

public class n {
    ArrayList<String> sinfo;
   
    
    public  n(String[] arg){
        sinfo=new ArrayList<String>();
            for(int i = 1; i < arg.length; i++) {
         sinfo.add(arg[i]);
            }
       }
    public static void main(String[] args) {
        n station =new n(args);
      
      
       
     
     System.out.println(station.sinfo);
    }
   
    
   }
   
   