import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Coordinator extends Thread{
    private int port, lport, parts, timeout;
    //<port> port coordinator is listening on
    //<lport> port that logger server is listening on
    //<parts> participants expected
    //<timeout> timeout in milliseconds
    //[<option>] set of options separated by spaces
    private Set<String> options = new HashSet<>();
    private ArrayList<Integer> participants = new ArrayList<Integer>();
    private HashMap<ServerThread, Integer> participantThreads = new HashMap<ServerThread, Integer>();
    private Map<Integer, PrintWriter> map = Collections.synchronizedMap(new HashMap<Integer, PrintWriter>(parts));
    private Set<Integer> outcomeParticipants = new HashSet<>(parts);
    private Set<String> outcomeVote = new HashSet<>();
    private int outcomesReceived = 0;
    public String finalVote;
    private boolean votingInProcess;

    private class ServerThread extends Thread{
        private Socket socket;
        private int pport;
        private BufferedReader br;
        private PrintWriter pr;

        ServerThread(Socket client) throws IOException{
            socket = client;
            br = new BufferedReader(new InputStreamReader(client.getInputStream()));
            pr = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
            pr.println("Coordinator welcomes you.");
            pr.flush();
        }

        public void run(){
            try{
                Tokenizer.Token token = null;
                Tokenizer tokenizer = new Tokenizer();
                token = tokenizer.getToken(br.readLine());
                if(!(token instanceof Tokenizer.JoinToken)){
                    socket.close();
                    return;
                }
                votingInProcess = true;
                String portStr = ((Tokenizer.JoinToken)token).name;
                pport = Integer.parseInt(portStr);
                if(!(joinCoordinator(pport, pr))){
                    socket.close();
                    return;
                }
                while(participants.size() < parts){
                    System.out.println("Waiting for participants");
                    Thread.sleep(1000);
                }


                sendDetails(participants);
                sendOptions(participants);

                token=tokenizer.getToken(br.readLine());
                if(!(token instanceof Tokenizer.OutcomeToken)){
                    System.out.println("Not an outcome socket");
                    socket.close();
                    return;
                }else {
                    System.out.println("Putting outcome vote from one participant");
                    ArrayList<Integer> tmpArr = ((Tokenizer.OutcomeToken) token).parts;
                    for (int i : tmpArr){
                        outcomeParticipants.add(i);
                    }
                    outcomeVote.add(((Tokenizer.OutcomeToken) token).vote);
                    outcomesReceived++;
                    System.out.println("Added " + ((Tokenizer.OutcomeToken) token).vote + " " + ((Tokenizer.OutcomeToken) token).parts);
                }

                while(outcomesReceived < parts){
                    System.out.println("Outcomes received " + outcomesReceived + "\nParticipants: " + parts);
                    Thread.sleep(1000);
                }
                socket.close();
                votingInProcess = false;
                checkOutcome();
                //this.interrupt();
            }catch (IOException e){System.err.println("Caught I/O Exception.");} catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    synchronized boolean checkOutcome() throws InterruptedException {
        while(!(outcomesReceived == parts)){
            Thread.sleep(1000);
        }
        if(!(outcomeVote.size() == 1)){
            System.out.println("Couldn't come up with a uniform vote\nOutcomeVote: " + outcomeVote);
            return false;
        }else if(!(outcomeParticipants.size() == parts)){
            System.out.println("Not enough participants have submitted a vote");
            return false;
        }

        finalVote = outcomeVote.toString();
        System.out.println("Everyone agrees on the outcome");
        return true;
    }

    synchronized void sendDetails(ArrayList<Integer> parts){
        for(Map.Entry<Integer,PrintWriter> entry : map.entrySet()){
            String msg = "DETAILS";
            for(int p:parts){
                if(p != entry.getKey())
                    msg+= " " + p;
            }
            PrintWriter pw = entry.getValue();
            pw.println(msg);
            pw.flush();
        }
    }

    synchronized void sendOptions(ArrayList<Integer> parts){
        for(Map.Entry<Integer,PrintWriter> entry : map.entrySet()){
            String msg = "VOTE_OPTIONS";
            for(String s : options){
                msg += " " + s;
            }
            PrintWriter pw = entry.getValue();
            pw.println(msg);
            pw.flush();
        }
    }

    boolean joinCoordinator(Integer port, PrintWriter out){
        if(participants.size() >= parts)
            return false;
        if(map.containsKey(port)){
            System.out.println("Coordinator: Participant already in.");
            return false;
        }
        System.out.println("ACK for " + port);
        out.println("ACK " + port);
        out.flush();
        try{
            map.put(port, out);
        }catch (NullPointerException e){
            return false;
        }
        participants.add(port);
        return true;
    }

    void startListening(String[] args) throws IOException{
        ServerSocket listener = new ServerSocket(Integer.parseInt(args[0]));
        votingInProcess = true;
        port = Integer.parseInt(args[0]);
        lport = Integer.parseInt(args[1]);
        parts = Integer.parseInt(args[2]);
        timeout = Integer.parseInt(args[3]);
        System.out.println("Adding options, arr length: " + args.length);
        for(int i = 4; i < args.length; i++){
            options.add(args[i]);
        }
        while (votingInProcess) {
            Socket client = listener.accept();
            ServerThread thread = new ServerThread(client);
            thread.start();
        }
        System.out.println("The world beyond the thread.start()");
    }

    public static void main(String[] args) throws IOException {


        if(args.length < 6){
            System.out.println("Usage: java Coordinator <port> <lport> <parts> <timeout> [<options>]");
            return;
        }
        Coordinator coordinator = new Coordinator();
        coordinator.startListening(args);




//    public Coordinator(String[] args) throws IOException{
//        if(args.length < 6){
//            System.out.println("Insufficient arguments");
//            return;
//        }else{
//            options = new HashSet<String>();
//            for(int i = 4; i < args.length; i++){
//                options.add(args[i]);
//                System.out.println("Added option: " + args[i]);
//            }
//        }
//        port = Integer.parseInt(args[0]);
//        lport = Integer.parseInt(args[1]);
//        parts = Integer.parseInt(args[2]);
//        timeout = Integer.parseInt(args[3]);
//
//        ss = new ServerSocket(port);
//
//        System.out.println("Coordinator initialized.\nListening on " + port);
//
//
//    }
//
//    public void handleConnection()throws IOException{
//        while(participants.size() < parts){
////                System.out.println("Current amount of participants joined: " + participants.size() + " out of " + parts);
//            s = ss.accept();
//            System.out.println("New client connection attempt from port " + s.getPort());
//
//            CoordinatorThread thread = new CoordinatorThread(s);
//            //System.out.println("Thread created");
//            synchronized (participantThreads){
//                participantThreads.put(thread, s.getPort());
//            }
//            //System.out.println("Starting thread");
//            thread.start();
//        }
//
//        System.out.println("All participants joined:");
//        int no = 1;
//        for(int i : participants){
//            System.out.println(no + ") " + i);
//            no++;
//        }
//    }
//
//    public void sendDetails(){
//        System.out.println("Sending details to participants");
//        synchronized (participantThreads){
//            Iterator iterator = participantThreads.entrySet().iterator();
//            while (iterator.hasNext()) {
//                Map.Entry element = (Map.Entry)iterator.next();
//                CoordinatorThread thread = (CoordinatorThread) element.getKey();
//                String str = "DETAILS ";
//                //System.out.println("Getting the message to send to " + thread.pport);
//                for(int i : participants){
//                    if(i != thread.pport){
//                        str += i + " ";
//                    }
//                }
//                //System.out.println("Sending details: \n"+ str);
//                thread.pr.println(str);
//                thread.pr.flush();
//
//            }
//        }
//    }
//
//    public void sendOptions(){
//        System.out.println("Sending vote options to participants");
//        synchronized (participantThreads){
//            Iterator iterator = participantThreads.entrySet().iterator();
//            while (iterator.hasNext()) {
//                Map.Entry element = (Map.Entry)iterator.next();
//                CoordinatorThread thread = (CoordinatorThread) element.getKey();
//                String str = "VOTE_OPTIONS";
//                //System.out.println("Getting the message to send to " + thread.pport);
//                for(String s : options){
//                    str += " " + s;
//                }
//                //System.out.println("Sending details: \n"+ str);
//                thread.pr.println(str);
//                thread.pr.flush();
//
//            }
//        }
//    }
//
//    public void waitForOutcome(){
//
//    }
//
//    public class CoordinatorThread extends Thread{
//        private PrintWriter pr;
//        private InputStreamReader in;
//        private BufferedReader bf;
//        private Socket socket;
//        private int pport;
//        //private boolean joined = false;
//
//        public CoordinatorThread(Socket s) throws IOException {
//            socket = s;
//            in = new InputStreamReader(socket.getInputStream());
//            bf = new BufferedReader(in);
//            pr = new PrintWriter(socket.getOutputStream());
//
//        }
//
////        public int getPport(){
////            return pport;
////        }
//
//        public void run(){
//            String str = null;
//            try {
//                str = bf.readLine();
//                //System.out.println("Str: " + str);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            if(str != null && str.contains("JOIN")){
////                System.out.println("Not null and contains JOIN");
//                String[] splitStr = str.split(" ");
//                pport = Integer.parseInt(splitStr[1]);
//                if(participants.size() < parts) {
//                    participants.add(pport);
//                }else{
//                    System.out.println("No more room for new participants");
//                    return;
//                }
//                pr.println(pport + " join accepted");
//                pr.flush();
//                System.out.println("COORD: A new participant has joined - " + pport + " / " + socket.getPort()+"\n" +
//                        "Total participants: " + participants.size() +"\n");
//            }
//
//        }
//    }


//    public void getParticipants() throws IOException {
//        String str = null;
//        while(partsJoined < parts){
//            System.out.println(partsJoined + " participants joined");
//            s = ss.accept();
//
//            System.out.println("New client connected: " + s.getPort());
//
//
//            CoordinatorThread thread = new CoordinatorThread(s);
//            synchronized (joinedParticipants){
//                joinedParticipants.put(thread, s);
//            }
//            thread.start();
//        }
//        System.out.println("All " + parts + " participants have joined:");
//        for(int i : participants){
//            System.out.println("Participant " + i);
//        }
//    }
//
//    public void sendDetails() throws IOException{
//        String str;
//        synchronized (joinedParticipants){
//
//        }
//
//    }


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
