import java.util.*;
import java.io.*;
import java.net.*;

public class n2 extends Thread {
    ArrayList<String> neinfo;// not necessary right now
    ArrayList<String> nextstop;// all next stop
    ArrayList<String> stopinfo;//specific timetable of each next stop
    String routine;//the combined timetable for pass
    String finalway;//the completed routine
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
            stop = res[res.length - 1];//next stop
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
        routine="";
        finalway="";
        end="";
        aim=0;
        for (int i = 2; i < arg.length; i++) {
            neinfo.add(arg[i]);
        }
        getns(arg);
        TCPS();
        UDPR();//receiver
        UDPS();//sender

    }

    public void TCPS() throws IOException {
        ServerSocket ss = new ServerSocket(tcport);
        Socket s = ss.accept();
        InputStream is = s.getInputStream();
        byte[] bys = new byte[1024];
        int len;
        len = is.read(bys);
        InetAddress address = s.getInetAddress();
        System.out.println("sender:" + address);
        System.out.println(new String(bys, 0, len));//得到的终点站的信息为名字
        s.close();
    }
//............................................................................................................
    public void UDPR() throws IOException{
        DatagramSocket socketUDP = new DatagramSocket(udport);

        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);

        socketUDP.receive(packet);

        byte[] arr = packet.getData();



        socketUDP.close();
    }

    public void UDPS() throws IOException{
        InetAddress loc = InetAddress.getLocalHost(); 
         DatagramSocket socket = new DatagramSocket();
           

        if (为初始 终点站是否有登记end不是null) {
            for (int i = 0; i < nextstop.size(); i++) {
                routine=udport+","+end+","+stopinfo.get(i);
                for (int j = 0; j < neinfo.size(); j++) {
                    DatagramPacket packet =
                    new DatagramPacket(routine.getBytes(), routine.getBytes().length, loc, Integer.parseInt(neinfo.get(j)));
                     
                 socket.send(packet);}
            }
            socket.close();  
        } else if (路径已经有自己，需要考虑第二站传回自己的情况) {
            socket.close();  
        } else if (自己接不上，不以当前车站名结尾，不改直接传) {
            for (int i = 0; i < neinfo.size(); i++) {
                DatagramPacket packet =
                new DatagramPacket(routine.getBytes(), routine.getBytes().length, loc, Integer.parseInt(neinfo.get(i)));
                 
             socket.send(packet);}
            socket.close();  


        } else if (自己接的上，下一站包括终点站 缝合之后routine 分隔出头两个，第一个取出为目标，后面一堆发送) {
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
        socket.close();  
            //routine 开始分割发送后半部分
        } else //接的上，接上自己的然后发送 可能需要两个for loop  一个i一个j
        {
            for (int i = 0; i < nextstop.size(); i++) {
                routine=routine+stopinfo.get(i);
                for (int j = 0; j < neinfo.size(); j++) {
                    DatagramPacket packet =
                    new DatagramPacket(routine.getBytes(), routine.getBytes().length, loc, Integer.parseInt(neinfo.get(j)));
                     
                 socket.send(packet);}
            }
            socket.close();  
        }





         //放在最后一块

          for (int i = 0; i < neinfo.size(); i++) {
            DatagramPacket packet =
            new DatagramPacket(routine.getBytes(), routine.getBytes().length, loc, Integer.parseInt(neinfo.get(i)));
             
         socket.send(packet);}
        socket.close();  

    }
//....................................................................................................................
    public static void main(String[] args) throws IOException {

        n2 station = new n2(args);
    

        System.out.println(station.stopinfo);
    }

}
