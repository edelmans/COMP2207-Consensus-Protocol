import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Participant {
    private Socket s;
    private ServerSocket ss;
    //private Socket ls;
    private PrintWriter pr;
    private InputStreamReader in;
    private BufferedReader bf;

    private int cport, lport, pport, timeout;
    // <cport> port number that coordinator is listening on
    // <lport> port that logger server is listening on
    // <pport> port that this participant will listen on
    // <timeout> timeout in milliseconds

    public Participant(String[] args) throws IOException {
        if(args.length < 4){
            System.out.println("Insufficient arguments");
            return;
        }

        cport = Integer.parseInt(args[0]);
        lport = Integer.parseInt(args[1]);
        pport = Integer.parseInt(args[2]);
        timeout = Integer.parseInt(args[3]);

        s = new Socket("localhost", cport);
        pr = new PrintWriter(s.getOutputStream());
        in = new InputStreamReader(s.getInputStream());
        bf = new BufferedReader(in);
        //Create a server it will listen on
        ss = new ServerSocket(pport);
        //ls = new Socket("localhost", lport);

        System.out.println("Participant initialized: " + ss.getLocalPort());

    }

    public boolean join() throws IOException {
        System.out.println("JOIN Attempt");
        //boolean joined = false;
        pr.println("JOIN " + pport);
        pr.flush();

        String msg = null;
        try{
            msg = bf.readLine();
        }catch (Exception e){System.out.println("Can't read a line error "+ e);}
        if(msg != null && msg.equals(pport + " join accepted")){
            return true;
        }else{
            s.close();;
            return false;
        }
    }





    public static void main(String[] args) throws IOException{
        String[] defA = new String[6];
        defA[0] = "4998";
        defA[1] = "4997";
        defA[2] = "4999";
        defA[3] = "500";

        Participant participant = new Participant(args);
        boolean joined = participant.join();
        if (joined){
            System.out.println("Success");
        }else{
            System.out.println("Fail");
        }


//        Socket s = new Socket("localhost", 4999);
//        PrintWriter pr = new PrintWriter(s.getOutputStream());
//        pr.println("Client join request");
//        pr.flush();
//
//        InputStreamReader in = new InputStreamReader(s.getInputStream());
//        BufferedReader bf = new BufferedReader(in);
//
//        String str = bf.readLine();
//        System.out.println("Server: " + str);
    }


}
