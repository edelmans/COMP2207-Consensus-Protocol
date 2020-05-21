import com.sun.javafx.collections.MappingChange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Coordinator{
    private ServerSocket ss;
    private Socket s;
    private Socket ls;

    private int port, lport, parts, timeout;
    //<port> port coordinator is listening on
    //<lport> port that logger server is listening on
    //<parts> participants expected
    //<timeout> timeout in milliseconds
    //[<option>] set of options separated by spaces
    private Set<String> options;
    private int partsJoined = 0;
    private ArrayList<Integer> participants = new ArrayList<Integer>();

    private HashMap<Thread, Socket> joinedParticipants = new HashMap<>();

    public Coordinator(String[] args) throws IOException{
        if(args.length < 6){
            System.out.println("Insufficient arguments");
            return;
        }else{
            options = new HashSet<String>();
            for(int i = 4; i < args.length; i++){
                options.add(args[i]);
                System.out.println("Added option: " + args[i]);
            }
        }
        port = Integer.parseInt(args[0]);
        lport = Integer.parseInt(args[1]);
        parts = Integer.parseInt(args[2]);
        timeout = Integer.parseInt(args[3]);

        ss = new ServerSocket(port);

        System.out.println("Coordinator initialized.\nListening on " + port);


    }

    public void getParticipants() throws IOException {
        String str = null;
        while(partsJoined < parts){
            System.out.println(partsJoined + " participants joined");
            s = ss.accept();

            System.out.println("New client connected: " + s.getPort());


            CoordinatorThread thread = new CoordinatorThread(s);
            synchronized (joinedParticipants){
                joinedParticipants.put(thread, s);
            }
            thread.start();
        }
        System.out.println("All " + parts + " participants have joined:");
        for(int i : participants){
            System.out.println("Participant " + i);
        }
    }

    public void sendDetails() throws IOException{
        String str;
        synchronized (joinedParticipants){

        }

    }

    public class CoordinatorThread extends Thread{
        private PrintWriter pr;
        private InputStreamReader in;
        private BufferedReader bf;
        private Socket socket;
        private int pport;
        //private boolean joined = false;

        public CoordinatorThread(Socket s) throws IOException {
            socket = s;
            in = new InputStreamReader(socket.getInputStream());
            bf = new BufferedReader(in);

            pr = new PrintWriter(socket.getOutputStream());

        }

        public void run(){
            String str = null;
            try {
                str = bf.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(str != null && str.contains("JOIN")){
                String[] splitStr = str.split(" ");
                pport = Integer.parseInt(splitStr[1]);
                participants.add(pport);
                partsJoined++;
                pr.println(splitStr[1] + " join accepted");
                pr.flush();
                System.out.println("COORD: A new participant has joined - " + pport + " / " + socket.getPort()+"\n" +
                        "total participants: " + partsJoined);

            }

        }
    }

    public static void main(String[] args) throws IOException{
        String[] defA = new String[6];
        defA[0] = "4998";
        defA[1] = "4997";
        defA[2] = "2";
        defA[3] = "500";
        defA[4] = "A";
        defA[5] = "B";

        Coordinator coordinator = new Coordinator(defA);
        coordinator.getParticipants();
        coordinator.sendDetails();


//        ServerSocket ss = new ServerSocket(4999);
//        Socket s = ss.accept();
//
//        System.out.println("Client " + s.getPort() + " connected.");
//
//        InputStreamReader in = new InputStreamReader(s.getInputStream());
//        BufferedReader bf = new BufferedReader(in);
//
//        String str = bf.readLine();
//        System.out.println("Client " + s.getPort() + ": " + str);
//
//        PrintWriter pr = new PrintWriter(s.getOutputStream());
//        pr.println("ACK");
//        pr.flush();
    }

}










//import java.net.ServerSocket;
//import java.net.Socket;
//
//import java.io.BufferedReader;
//import java.io.PrintWriter;
//import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
//import java.io.IOException;
//
//import java.util.*;
//
//public class Coordinator {
//
//    private int port, lport, parts, timeout;
//
//    private ArrayList<Integer> participants = new ArrayList<>();
//    private int partJoined = 0;
//    private Set<String> options;
//
//    private Map<String, PrintWriter> map;
//
//
//    private ServerSocket ss;
//
//    private Coordinator(String[] args) {
//
//        if(args.length < 6){
//            System.out.println("Insufficient arguments");
//            return;
//        }
//        options = new HashSet<>();
//        for(int i = 4; i<args.length; i++){
//            options.add(args[i]);
//        }
//        //TEST
//        System.out.println(options + ": options");
//
//        //coordinator's port
//        port = Integer.parseInt(args[0]);
//        //logger server port
//        lport = Integer.parseInt(args[1]);
//        //number of participants expected
//        parts = Integer.parseInt(args[2]);
//        //timeout in milliseconds
//        timeout = Integer.parseInt(args[3]);
//
//        map = Collections.synchronizedMap(new HashMap<String,PrintWriter>(parts));
//
//    }
//
//    private class CoordinatorThread extends Thread{
//        private Socket partSocket;
//        private int pport;
//        private BufferedReader partIn;
//        private PrintWriter partOut;
//
//        CoordinatorThread(Socket client) throws IOException{
//            partSocket = client;
//            partIn = new BufferedReader(new InputStreamReader(partSocket.getInputStream()));
//            partOut = new PrintWriter(new OutputStreamWriter(partSocket.getOutputStream()));
//
//            partOut.println("Connection to Coordinator established");
//            partOut.flush();
//        }
//
//        public void run(){
//            try{
//                Tokenizer.Token token = null;
//                Tokenizer reqTokenizer = new Tokenizer();
//
//                token = reqTokenizer.getToken(partIn.readLine());
//                if(!(token instanceof Tokenizer.JoinToken)){
//                    partSocket.close();
//                    return;
//                }
//                //TODO - Fix this
//                if(!(join((pport = ((Tokenizer.JoinToken)token).name), partOut))){
//                    partSocket.close();
//                    return;
//                }
//                token = reqTokenizer.getToken(partIn.readLine());
//                while(!(token instanceof Tokenizer.OutcomeToken)) {
//                    if (token instanceof Tokenizer.VoteToken)
//                        vote(pport, ((Tokenizer.VoteToken) token).msg);
//                    token = reqTokenizer.getToken(partIn.readLine())
//                }
//                partSocket.close();
//            }catch (IOException e){System.err.println("Caught I/O Exception.");}
//        }
//    }
//
//
//    synchronized void vote(int port, String msg){
//        String txt = "VOTE " + port + " " + msg;
//        Iterator iter = map.values().iterator();
//        while(iter.hasNext()){
//            PrintWriter pw = (PrintWriter)iter.next();
//            pw.println(txt);
//            pw.flush();
//        }
//    }
//
//    synchronized boolean join(int name, PrintWriter out){
//        if(partJoined >= parts)
//            return false;
//    }
//
//
//
//}
////        if(args.length < 6){
////            System.out.println("Insufficient arguments");
////            return;
////        }
////        options = new HashSet<>();
////        for(int i = 4; i<args.length; i++){
////            options.add(args[i]);
////        }
////        //TEST
////        System.out.println(options + ": options");
////
////        //coordinator's port
////        port = Integer.parseInt(args[0]);
////        //logger server port
////        lport = Integer.parseInt(args[1]);
////        //number of participants expected
////        parts = Integer.parseInt(args[2]);
////        //timeout in milliseconds
////        timeout = Integer.parseInt(args[3]);
////
////        //Initialize coordinator
////        try{
////            ss = new ServerSocket(port);
////            System.out.println("Coordinator initialized\n" +
////                    "Port: "+ port + "\n" +
////                    "Participants: " + parts + "\n" +
////                    "Options: " + options + "\n" +
////                    "Timeout: " + timeout);
////        }catch (Exception e){System.out.println("Initialization error " + e);}
////    }
////
////    private void getConnections() throws IOException {
////        Socket client;
////
////        while(partConnected < parts){
////            client = ss.accept();
////            System.out.println("C" + port + ": New participant has connected");
////
////            //Thread creation
////            Thread thread = new CoordinatorThread(client);
////            synchronized ()
////        }
////    }
