import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.sql.Savepoint;

public class n2 {
    ArrayList<String> sinfo;
    String tt;
    ArrayList<String> nextstop;
    String name;

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
        // tt = sb.toString();

    }

    public n2(String[] arg) throws IOException {
        name = arg[0];
        sinfo = new ArrayList<String>();
        nextstop = new ArrayList<String>();
        for (int i = 1; i < arg.length; i++) {
            sinfo.add(arg[i]);
        }
        getns(arg);

    }

    public static void main(String[] args) throws IOException {

        n2 station = new n2(args);

        System.out.println(station.nextstop);
    }

}
