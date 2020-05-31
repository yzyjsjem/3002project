import java.util.*;
import java.util.concurrent.TimeUnit;
import java.io.*;
import java.net.*;

public class M implements Runnable {
    ArrayList<String> neinfo;// not necessary right now
    ArrayList<String> nextstop;// all next stop
    ArrayList<String> stopinfo;// specific timetable of each next stop
    String routine;// the combined timetable for pass
    String finalway;// the completed routine
    String name;// self name
    String end;// the last stop
    String arrivetime;// the arrive time recived
    String arrivestop;// the arrivestop get from routine
    int ath;// arrive hour
    int atm;// arrive minute
    int aim;// udp port of the first server.
    int tcport;
    int udport;
    boolean runtcp;//keep tcp running
    boolean browserequest;// browser request to find the routine
    boolean findway;//if find the way it will return the correct answer 
    boolean initialstop;//check if the initalstop and decide how to handle udp information
    boolean getend;//if the current station could go to the destination
    boolean havepassed;//if the current station already been passed in the routine
    boolean directway;//if the inital station could directly go to the destination


    public void run() {
        try {
            if (!runtcp) {//keep tcp runing and let the next one do udp
                runtcp = true;
                TCPS();
            }
            UDP();

        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    // divide input information and store in order.
    public M(String[] arg) {
        name = arg[0];
        tcport = Integer.parseInt(arg[1]);
        udport = Integer.parseInt(arg[2]);
        neinfo = new ArrayList<String>();
        nextstop = new ArrayList<String>();
        stopinfo = new ArrayList<String>();
        routine = "";
        finalway = "";
        end = "";
        for (int i = 2; i < arg.length; i++) {
            neinfo.add(arg[i]);
        }

    }

    // give next bus stop from timetable
    public void getns() throws Exception {
        Scanner in = new Scanner(new FileReader("tt-" + name));
        in.next();
        String stop;
        while (in.hasNext()) {
            String Cline = in.next();// current line
            String[] res = Cline.split(",");
            String startime = res[0];
            String[] st = startime.split(":");
            int sth = Integer.parseInt(st[0]);
            int stm = Integer.parseInt(st[1]);

            stop = res[res.length - 1];// next stop
            if (sth > ath || (sth == ath && stm >= atm)) {
                Boolean repeat = false;
                for (int i = 0; i < nextstop.size(); i++) {
                    if (nextstop.get(i).equals(stop)) {
                        repeat = true;
                        break;
                    }
                }
                if (!repeat) {//save the earlist routine and their nextstop, each stop have one earlist routine
                    nextstop.add(stop);
                    stopinfo.add(Cline);
                }
            } else {
                in.next();
            }
        }
        in.close();
    }

    public void TCPS() throws Exception {

        ServerSocket ss = new ServerSocket(tcport);

        byte[] bys = new byte[1024];
        boolean go = true;
        while (go) {
            Socket s = ss.accept();
            InputStream is = s.getInputStream();
            OutputStream os = s.getOutputStream();
            Calendar now = Calendar.getInstance();
            ath = now.get(Calendar.HOUR_OF_DAY);//get the hour and minute of right time and will be used in getns to choose the earlist routine
            atm = now.get(Calendar.MINUTE);
            is.read(bys);
            String browser = new String(bys);
            if (browser.contains("to=")) {//if get the order of destination
                browserequest = true;
                System.out.println("get the order");
                String[] browser2 = browser.split("to=");
                String browser3 = browser2[1];
                String[] browser4 = browser3.split(" HTTP/1.1");
                end = browser4[0];
            } else {//if no order
                String error = "HTTP/1.1 404 Not Found \n" + "Content-Type: text/html\n" + "Content-Length: 29" + "\n\n"
                        + "<h1>Request Not Correct!</h1>";
                os.write(error.getBytes());

                continue;
            }
            getns();//read timetable
            for (int i = 0; i < nextstop.size(); i++) {//if could directly go to the destination
                if (nextstop.get(i).equals(end)) {
                    finalway=stopinfo.get(i);
                    directway=true;
                } 

            }

            if (directway) {//just send the routine back to browser
                String response = "HTTP/1.1 200 ok \n" + "Content-Type: text/html\n" + "Content-Length: "
                    + finalway.length() + "\n\n" + finalway;
            os.write(response.getBytes());

            s.close();
            } else {
                // 1.The first station, get the request from browser which contains a
                // terminal. And the start port and terminal name to the routine and send to its
                // neighour.
                InetAddress loc = InetAddress.getLocalHost();
                DatagramSocket socket = new DatagramSocket();
                for (int i = 0; i < nextstop.size(); i++) {
                    routine = udport + "," + end + "," + stopinfo.get(i);
                    for (int j = 0; j < neinfo.size(); j++) {
                        DatagramPacket packet = new DatagramPacket(routine.getBytes(), routine.getBytes().length, loc,
                                Integer.parseInt(neinfo.get(j)));

                        socket.send(packet);
                        

                    }
                    System.out.println(routine);
                }
                initialstop=true;
                socket.close();

            }
            byte[] bytes = new byte[1024];
            TimeUnit.SECONDS.sleep(3);//wait for three minutes
            while (!findway) {//if not find a way return no way
                String noway="there is no way";
                String response = "HTTP/1.1 200 ok \n" + "Content-Type: text/html\n" + "Content-Length: "
                + noway.length() + "\n\n" + noway;
                os.write(response.getBytes());

                s.close();
            }
            //if find the way, return the finalway
            String response = "HTTP/1.1 200 ok \n" + "Content-Type: text/html\n" + "Content-Length: "
                    + finalway.length() + "\n\n" + finalway;
            os.write(response.getBytes());

            s.close();

        }
        ss.close();

    }

    public void UDP() throws Exception {//run udp
        InetAddress loc = InetAddress.getLocalHost();
        DatagramSocket socket = new DatagramSocket(udport);
        boolean Urgo = true;
        while (Urgo) {//keep going
            DatagramPacket packetr = new DatagramPacket(new byte[1024], 1024);

            socket.receive(packetr);

            routine = new String(packetr.getData(),0,packetr.getLength());// get data set as routine
            String[] arrive = routine.split(",");
            // if this is the finalway that start at a time
            if (arrive[0].contains(":")) {
                finalway = finalway+routine;
                findway = true;
                System.out.println("find the way");

                continue;
            } else {

                arrivetime = arrive[arrive.length - 2];// get the arrivetime
                arrivestop = arrive[arrive.length - 1];
                end = arrive[1];// get the end information
                String[] at = arrivetime.split(":");
                ath = Integer.parseInt(at[0]);
                atm = Integer.parseInt(at[1]);
            }
            // before send udprequest, remake the timetable
            getns();
            for (int i = 0; i < nextstop.size(); i++) {
                if (nextstop.get(i).equals(end)) {
                    getend=true;
                } 

            }
            String[]check=routine.split(",");//check if the station has been passed, check the routine except the arrivestop and first two important information
            for (int i = 2; i < check.length-1; i++) {
                if (check[i].equals(name)) {
                    havepassed=true;
                }
            }



            // 2.if the stop has already in the routine, just abandon.
            if (havepassed||initialstop) {
                havepassed=false;
                //System.out.println("already pass "+routine);
                continue;
                // 3. If this station is not last stop in the routine,send the message to
                // neighbour.
            } else if (!arrivestop.equals(name)){
                for (int i = 0; i < neinfo.size(); i++) {
                    DatagramPacket packet = new DatagramPacket(routine.getBytes(), routine.getBytes().length, loc,
                            Integer.parseInt(neinfo.get(i)));

                    socket.send(packet);
                }

                // 4.IF the stop is the last stop of the routine and can go to the terminal.
                // rewrite the routine and send back to the start.
            } else if (arrivestop.equals(name) && getend) {
                getend=false;
                for (int i = 0; i < nextstop.size(); i++) {
                    if (nextstop.get(i).equals(end)) {
                        routine = routine + stopinfo.get(i);
                    }
                }
                String[] split = routine.split(",");
                aim = Integer.parseInt(split[0]);
                for (int j = 2; j < split.length; j++) {
                    finalway = finalway + split[j];
                }
                DatagramPacket packet = new DatagramPacket(finalway.getBytes(), finalway.getBytes().length, loc, aim);

                socket.send(packet);
                System.out.println("ready to finish "+finalway);

            } else // 5. the station is the last stop of the routine but can't go to the terminal,
                   // send all possible routine to its neighour.
            {
                for (int i = 0; i < nextstop.size(); i++) {
                    routine = routine + stopinfo.get(i);
                    for (int j = 0; j < neinfo.size(); j++) {
                        DatagramPacket packet = new DatagramPacket(routine.getBytes(), routine.getBytes().length, loc,
                                Integer.parseInt(neinfo.get(j)));

                        socket.send(packet);
                    }

                }
            }

        }

        socket.close();
    }

    public static void main(String[] args) {

        M station = new M(args);
        Thread tcp = new Thread(station);
        Thread udp = new Thread(station);
        tcp.start();
        udp.start();

    }

}