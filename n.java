import java.util.*;
import java.io.*;
import java.net.*;

public class n extends Thread {
    ArrayList<String> neinfo;// not necessary right now
    ArrayList<String> nextstop;// all next stop
    ArrayList<String> stopinfo;//specific timetable of each next stop
    String routine;//the combined timetable for pass
    String finalway;//the completed routine
    String name;//self name
    String end;//the last stop
    String arrivetime;//the arrive time recived
    String arrivestop;//the arrivestop get from routine
    int ath;//arrive hour
    int atm;//arrive minute
    int aim;// udp port of the first server.
    int tcport;
    int udport;
    boolean runtcp;
    boolean browserequest;//browser request to find the routine 
   // divide input information and store in order.

   public void run(String[] arg) throws IOException{
       if (!runtcp) {
           runtcp=true;
           TCPS(arg);
       }
       UDP(arg);
   }

    public n(String[] arg) throws IOException {
        name = arg[0];
        tcport = Integer.parseInt(arg[1]);
        udport = Integer.parseInt(arg[2]);
        neinfo = new ArrayList<String>();
        nextstop = new ArrayList<String>();
        stopinfo = new ArrayList<String>();
        routine="";
        finalway="";
        end="";
        aim=0;
        ath=0;//如果是初始站的话就要提前设置时间
        atm=0;
        for (int i = 2; i < arg.length; i++) {
            neinfo.add(arg[i]);
        }
        //getns(arg);
        //TCPS();
        //UDPR();
        //UDPS();//sender

    }
     // give next bus stop from timetable
     public void getns(String[] arg) throws IOException {
        Scanner in = new Scanner(new FileReader("tt-" +name));
        in.next();
        String stop;
        while (in.hasNext()) {
            String Cline = in.next();//current line
            String[] res = Cline.split(",");
            String startime = res[0];
            String[] st=startime.split(":");
            int sth=Integer.parseInt(st[0]);
            int stm=Integer.parseInt(st[1]);

            stop = res[res.length - 1];//next stop
            if (sth>ath||(sth==ath&&stm>=atm)) {
                Boolean repeat = false;
                for (int i = 0; i < nextstop.size(); i++) {
                    if (nextstop.get(i).equals(stop)) {
                        repeat = true;
                        break;
                    }
                }
                if (!repeat) {
                    nextstop.add(stop);
                    stopinfo.add(Cline);
                }
            } else {
                in.next();
            }
        }
        in.close();
    }

    public void TCPS(String[] arg) throws IOException {
            
            ServerSocket ss = new ServerSocket(tcport);
            Socket s = ss.accept();
            InputStream is = s.getInputStream();
            OutputStream os = s.getOutputStream();
            byte[] bys = new byte[1024];
            boolean go=true;
            while (go) {
                
                Calendar now = Calendar.getInstance();
                ath=now.get(Calendar.HOUR_OF_DAY);
                atm=now.get(Calendar.MINUTE);
                is.read(bys);
                String browser= new String(bys);
                if (browser.contains("to=")) {
                    browserequest=true;
                    String[]browser2=browser.split("to=");
                    String browser3=browser2[1];
                    String[] browser4 = browser3.split(" HTTP/1.1");
                    end =browser4[0]; 
                }
                byte[] bytes = new byte[1024];
         
                    String response = "HTTP/1.1 200 ok \n" + "Content-Type: text/html\n" + "Content-Length: "
                    + finalway.length() + "\n\n" + finalway;
                        os.write(response.getBytes());
    
                s.close();
            }
        ss.close();
        getns(arg);
//1.The first station, if get the request from browser which contains a terminal. And the start port and terminal name to the routine and send to its neighour.
        InetAddress loc = InetAddress.getLocalHost(); 
        DatagramSocket socket = new DatagramSocket();
        for (int i = 0; i < nextstop.size(); i++) {
            routine=udport+","+end+","+stopinfo.get(i);
            for (int j = 0; j < neinfo.size(); j++) {
                DatagramPacket packet =
                new DatagramPacket(routine.getBytes(), routine.getBytes().length, loc, Integer.parseInt(neinfo.get(j)));
                 
             socket.send(packet);}
        }
        socket.close();  

    }

    public void UDP(String[] arg) throws IOException{
        InetAddress loc = InetAddress.getLocalHost(); 
        DatagramSocket socket = new DatagramSocket(udport);
        boolean Urgo=true;
        while (Urgo) {
            DatagramPacket packetr = new DatagramPacket(new byte[1024], 1024);
    
            socket.receive(packetr);
    
            byte[] arr = packetr.getData();
            routine=new String(arr);//get data set as routine
            String[] arrive=routine.split(",");
            //if this is the finalway that start at a time
            if (arrive[0].contains(":")) {
                finalway=routine+finalway;
            } else {
                
                arrivetime =arrive[arrive.length-2];//get the arrivetime
                arrivestop = arrive[arrive.length-1];
                end = arrive[1];//get the end information
                String[] at=arrivetime.split(":");
                ath=Integer.parseInt(at[0]);
                atm=Integer.parseInt(at[1]);
            }
            //before send udprequest, remake the timetable
            getns(arg);
            
              
                    //2.if the stop has already in the routine, just abandon.
                if (routine.contains(name)) {
                  continue;
                   //3. If this station is not last stop in the routine,send the message to neighbour.
               } else if (arrivestop != name) {
                   for (int i = 0; i < neinfo.size(); i++) {
                       DatagramPacket packet =
                       new DatagramPacket(routine.getBytes(), routine.getBytes().length, loc, Integer.parseInt(neinfo.get(i)));
                        
                    socket.send(packet);}
                  
                 //4.IF the stop is the last stop of the routine and can go to the terminal. rewrite the routine and send back to the start.
               } else if (arrivestop==name&nextstop.contains(end)) {
                   for (int i = 0; i < nextstop.size(); i++) {
                       if (nextstop.get(i)==end) {
                           routine = routine + stopinfo.get(i);
                       }
                   }
                   String[] split=routine.split(","); 
                   aim= Integer.parseInt(split[0]);
                   for (int j = 2; j < split.length; j++) {
                       finalway = finalway + split[j];
                   } 
                   DatagramPacket packet =
                   new DatagramPacket(finalway.getBytes(), finalway.getBytes().length, loc, aim);
                    
               socket.send(packet);
              
                 
               } else //5. the station is the last stop of the routine but can't go to the terminal, send all possible routine to its neighour.
               {
                   for (int i = 0; i < nextstop.size(); i++) {
                       routine=routine+stopinfo.get(i);
                       for (int j = 0; j < neinfo.size(); j++) {
                           DatagramPacket packet =
                           new DatagramPacket(routine.getBytes(), routine.getBytes().length, loc, Integer.parseInt(neinfo.get(j)));
                            
                        socket.send(packet);}
                   }
               }
            
            
        }
        



        socket.close();
    }

    public static void main(String[] args) throws IOException {

        n station = new n(args);
        Thread tcp= new Thread(station);
        Thread udp=new Thread(station);
        tcp.start();
        udp.start();

    
    }

}
