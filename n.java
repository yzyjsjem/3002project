import java.util.*;
import java.io.*;
import java.net.*;

public class n2 {
    ArrayList<String> sinfo;
    ArrayList<String> nextstop;//all next stop
    String name;
    int tcport;
    int udport;

    public void getns(String[] arg) throws IOException {
        Scanner in = new Scanner(new FileReader("tt-" + arg[0]));
        
        in.next();
        String stop;
        while (in.hasNext()) {
            String sbbb = in.next();
            String[] res = sbbb.split(",");
            stop = res[res.length - 1];
            Boolean repeat = false;
            for (int i = 0; i < nextstop.size(); i++) {
                if (nextstop.get(i).equals(stop)) {
                    repeat = true;
                    break;
                }
            }
            if (!repeat) {           
                nextstop.add(stop);
            }
        }
        in.close();
    }

    public n2(String[] arg) throws IOException {
        name = arg[0];
        tcport=Integer.parseInt(arg[1]);
        udport=Integer.parseInt(arg[2]);
        sinfo = new ArrayList<String>();
        nextstop = new ArrayList<String>();
        for (int i = 1; i < arg.length; i++) {
            sinfo.add(arg[i]);
        }
        getns(arg);

    }
    public void TCP(){

    }

    public static void main(String[] args) throws IOException {

        n2 station = new n2(args);

        System.out.println(station.name);
    }
}
public void TCPC()throws IOException{
    Socket s = new Socket("127.0.0.1",62850);
    OutputStream os = s.getOutputStream();
    String str = "This TCP,im comming";
    os.write(str.getBytes());
    s.close();
}