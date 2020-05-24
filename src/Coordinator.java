import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Coordinator {
    private ServerSocket ss;
    private Socket s;
    private Socket ls;

    private int port, lport, parts, timeout;
    //<port> port coordinator is listening on
    //<lport> port that logger server is listening on
    //<parts> participants expected
    //<timeout> timeout in milliseconds
    //[<option>] set of options separated by spaces
    private Set<String> options = new HashSet<String>();
    private int partsJoined = 0;
    private int partsConnected = 0;
    private ArrayList<Integer> participants = new ArrayList<Integer>();

    private HashMap<CoordinatorThread, Socket> joinedParticipants = new HashMap<>();
    private HashMap<Integer, String> incomingVotes = new HashMap<>();
    public Coordinator(String[] args) throws IOException {
        port = Integer.parseInt(args[0]);
        lport =  Integer.parseInt(args[1]);
        parts =  Integer.parseInt(args[2]);
        timeout =  Integer.parseInt(args[2]);
        for(int i = 4; i<args.length; i++){
            options.add(args[i]);
        }
        System.out.println("Coordinator initialized\n\n");
        System.out.println("Arguments assigned:\n Port: " + port +
                "\nLPort: " + lport +
                "\nParticipants expected: " + parts +
                "\nTimeout: " + timeout +
                "\nOptions: " + options +"\n");

    }

    void processJoin(CoordinatorThread part){
        participants.add(part.pport);
        partsJoined++;

        if(partsJoined == parts){
            System.out.println("C: All participants have joined, sending details and vote options");
            sendDetails();
            sendVoteOptions();
        }
    }

    void sendDetails(){
        synchronized (joinedParticipants){
            for(CoordinatorThread thread : joinedParticipants.keySet()){
                String partStr = "";
                for(int i: participants){
                    if(thread.pport != i)
                        partStr+= " " + i;
                }
                thread.send("DETAILS", partStr);
            }
        }
    }

    void sendVoteOptions(){
        String voteOpt = "";
        for(String s:options){
            voteOpt += " " + s;
        }
        synchronized (joinedParticipants){
            for(CoordinatorThread thread : joinedParticipants.keySet()){
                thread.send("VOTE_OPTIONS", voteOpt);
            }
        }
    }

    void removeParticipant(CoordinatorThread part){
        participants.remove((Integer) part.pport);
        partsConnected--;
        synchronized (joinedParticipants){
            if(joinedParticipants.keySet().contains(part)) {
                joinedParticipants.remove(part);
                partsJoined--;
            }
        }
        if(partsJoined == 0){
            System.out.println("C: No joined participants left");
        }
        if(partsConnected == 0){
            System.out.println("C: No participants connected");
        }
        try{
            part.client.close();
            part.in.close();
            part.out.close();
        }catch (IOException e){
            System.err.println("Error in removing the participant");
        }
    }

    private class CoordinatorThread extends Thread {
        private Socket client;
        private int pport;
        private BufferedReader in;
        private PrintWriter out;
        private boolean running = true;

        CoordinatorThread(Socket client) throws IOException {
            this.client = client;
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()), true);
        }

        public void run(){
            String msg;
            String[] msgArr;
            try{
                while(running){
                    msg = in.readLine();
                    if(msg == null){
                        System.out.println("C: Connection lost with " + pport);
                        removeParticipant(this);
                        return;
                    }
                    msgArr = msg.split(" ");
                    if(msgArr[0].equals("JOIN")){
                        pport = Integer.parseInt(msgArr[1]);
                        System.out.println("C: JOIN request from " + pport);
                        out.println("hello");
                        processJoin(this);
                    }else if(msgArr[0].equals("OUTCOME")){
                        System.out.println("C: Outcome from " + pport + ": " + msgArr[1]);
                        incomingVotes.put(pport, msgArr[1]);
                        break;
                    }else{
                        System.out.println("C: Unknown message: " + msg);
                    }
                }

            }catch (IOException e){
                System.err.println("Caught I/O Exception");
                removeParticipant(this);
            }
        }

        void send(String type, String msg){
            if(type.equals("DETAILS")){
                System.out.println("C: Sending details -" + msg +" to " + pport);
                out.println("DETAILS" + msg);
            }else if(type.equals("VOTE_OPTIONS")){
                System.out.println("C: Sending vote options -" + msg +" to " + pport);
                out.println("VOTE_OPTIONS" + msg);
            }
//            switch (type){
//                case "DETAILS":
//                    out.println("DETAILS" + msg);
//                case "VOTE_OPTIONS" :
//                    out.println("VOTE_OPTIONS" + msg);
//            }
        }

    }

    void startProcess() throws IOException{
        System.out.println("C: Waiting for connections");
        ServerSocket ss = new ServerSocket(port);
        while(partsConnected < parts){
            Socket client = ss.accept();
            System.out.println("C: New client connection established");
            CoordinatorThread thread = new CoordinatorThread(client);
            synchronized (joinedParticipants){
                joinedParticipants.put(thread, client);
                partsConnected++;
            }
            System.out.println("C: Starting thread");
            thread.start();
        }
        System.out.println("C: All participants have connected");
    }


    public static void main(String[] args) throws IOException{
        if(args.length < 6){
            System.out.println("Usage: java Coordinator <port> <lport> <parts> <timeout> [<option>]");
            return;
        }
        new Coordinator(args).startProcess();
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
