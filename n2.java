import java.util.*;
import java.io.*;
import java.net.*;

public class n2 extends Thread {
    ArrayList<String> neinfo;// not necessary right now
    ArrayList<String> nextstop;// all next stop
    ArrayList<String> stopinfo;//specific timetable of each next stop
    String routine;//the combined timetable for pass
    String name;//self name
    String end;//the last stop
    int aim;
    int tcport;
    int udport;

    // give next bus stop from timetable
    public void getns(String[] arg) throws IOException {
        Scanner in = new Scanner(new FileReader("tt-" + arg[0]));
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
            if (sth<7) {
                in.next();
            } else {
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
                
            }
        }
        in.close();
    }

    // divide input information and store in order.
    public n2(String[] arg) throws IOException {
        name = arg[0];
        tcport = Integer.parseInt(arg[1]);
        udport = Integer.parseInt(arg[2]);
        neinfo = new ArrayList<String>();
        nextstop = new ArrayList<String>();
        stopinfo = new ArrayList<String>();
        for (int i = 2; i < arg.length; i++) {
            neinfo.add(arg[i]);
        }
        //getns(arg);
        TCPS();
        //UDPR();//receiver
        //UDPS();//sender
    
    }
//拔出end
    public void TCPS() throws IOException {
        ServerSocket ss = new ServerSocket(tcport);
        Socket s = ss.accept();
        InputStream is = s.getInputStream();
        byte[] bys = new byte[1024];
        int len;
        len = is.read(bys);
        InetAddress address = s.getInetAddress();
        System.out.println("sender:" + address);
        System.out.println("the information is " + new String(bys, 0, len));//得到的终点站的信息为名字
        s.close();
    }

    public void UDPR() throws IOException{
        DatagramSocket socketUDP = new DatagramSocket(udport);

        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);

        socketUDP.receive(packet);

        byte[] arr = packet.getData();

        int len = packet.getLength();

        System.out.println(new String(arr, 0, len));

        socketUDP.close();
    }

    public void UDPS() throws IOException{
        InetAddress loc = InetAddress.getLocalHost();
 
        Scanner sc = new Scanner(System.in);
             
         DatagramSocket socket = new DatagramSocket();
             while(true) {
             
                String line = sc.nextLine();
                if("quit".equals(line)) {
                break;
                }

          for (int i = 0; i < neinfo.size()-1; i++) {
            DatagramPacket packet =
            new DatagramPacket(line.getBytes(), line.getBytes().length, loc, Integer.parseInt(neinfo.get(i)));//要改为邻居地址
             
         socket.send(packet);}
        }
        socket.close();  

    }

    public static void main(String[] args) throws IOException {

        n2 station = new n2(args);
        System.out.println(station.end);
     
    }

}
