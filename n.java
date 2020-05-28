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
   // divide input information and store in order.

   public void run(String[] arg) throws IOException{
       if (!runtcp) {
           runtcp=true;
           TCPS();
       }
       UDPR();
       getns(arg);
       UDPS();
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

    public void TCPS() throws IOException {
            
            ServerSocket ss = new ServerSocket(tcport);
            boolean go=true;
            InputStream is = s.getInputStream();
                OutputStream os = s.getOutputStream();
                byte[] bys = new byte[1024];
            while (go) {
                Socket s = ss.accept();
                Calendar now = Calendar.getInstance();
                ath=now.get(Calendar.HOUR_OF_DAY);
                atm=now.get(Calendar.MINUTE);
                is.read(bys);
                String browser= new String(bys);
                if (browser.contains("to=")) {
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
    }

    public void UDPR() throws IOException{
        DatagramSocket socketUDP = new DatagramSocket(udport);
        boolean Urgo=true;
        while (Urgo) {
            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
    
            socketUDP.receive(packet);
    
            byte[] arr = packet.getData();
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

            
        }

        socketUDP.close();
    }

    public void UDPS() throws IOException{
        InetAddress loc = InetAddress.getLocalHost(); 
         DatagramSocket socket = new DatagramSocket();
           boolean usgp=true;
           while (usgp) {
               
               //为初始 终点站是否有登记end不是null因为没有收到过udp的话ath和atm都为默认的0......后期需要改
              if (ath==0) {
                  for (int i = 0; i < nextstop.size(); i++) {
                      routine=udport+","+end+","+stopinfo.get(i);
                      for (int j = 0; j < neinfo.size(); j++) {
                          DatagramPacket packet =
                          new DatagramPacket(routine.getBytes(), routine.getBytes().length, loc, Integer.parseInt(neinfo.get(j)));
                           
                       socket.send(packet);}
                  }
                   
              } else if (routine.contains(name)) {
                 continue;
                  //自己接不上直接传
              } else if (arrivestop != name) {
                  for (int i = 0; i < neinfo.size(); i++) {
                      DatagramPacket packet =
                      new DatagramPacket(routine.getBytes(), routine.getBytes().length, loc, Integer.parseInt(neinfo.get(i)));
                       
                   socket.send(packet);}
                 
                //自己接的上，可以到终点
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
             
                  //routine 开始分割发送后半部分
              } else //接的上，接上自己的然后发送 可能需要两个for loop  一个i一个j时间没有考虑
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
        Thread udpr=new Thread(station);
        Thread udps=new Thread(station);
        tcp.start();
        udpr.start();
        udps.start();   

    
    }

}
