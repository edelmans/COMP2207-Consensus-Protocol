import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.util.*;

public class Participant {
    private Socket s;
    private ServerSocket ss;
    //private Socket ls;
    private PrintWriter pr;
    private InputStreamReader in;
    private BufferedReader bf;
    private boolean connected = false;
    private int round = 1;
    private String outcomeVote = "";
    private Vote chosenVote;
    private int votesReceived = 0;

    private int cport, lport, pport, timeout;
    // <cport> port number that coordinator is listening on
    // <lport> port that logger server is listening on
    // <pport> port that this participant will listen on
    // <timeout> timeout in milliseconds
    private ArrayList<Integer> otherParts = new ArrayList<>();
    private Set<String> options = new HashSet<String>();


    private ArrayList<Thread> participantSendThreads = new ArrayList<>();
    private ArrayList<Thread> participantListenThreads = new ArrayList<>();

    private HashMap<Socket, PrintWriter> sendHashMap = new HashMap<>();

    public Participant(String[] args) throws IOException {
        cport = Integer.parseInt(args[0]);
        lport = Integer.parseInt(args[1]);
        pport = Integer.parseInt(args[2]);
        timeout = Integer.parseInt(args[3]);

        s = new Socket("localhost", cport);
        pr = new PrintWriter(s.getOutputStream(), true);
        in = new InputStreamReader(s.getInputStream());
        bf = new BufferedReader(in);
        //Create a server it will listen on
        ss = new ServerSocket(pport);
        //ls = new Socket("localhost", lport);

        System.out.println("Participant initialized: " + ss.getLocalPort());

    }

    void connectOthers(){
        System.out.println("P" + pport +": Starting the voting process." );
        //try to get it to listen to others, probably needs a ParticipantServer thread which will listen and accept any new connections
        //try to get it to cast vote probably through creating a new socket for each otherport and sending a message

        new Thread(this::getConnections).start();
        makeConnections();
        while(participantListenThreads.size() < otherParts.size() && sendHashMap.size() < otherParts.size()){
            System.out.println("Waiting for all connections to be made");
        }
        sendVote();


        while(votesReceived < otherParts.size()){
            System.out.println("Not enough votes received yet");
            try{
                Thread.sleep(5000);
            }catch (InterruptedException e){e.printStackTrace();}
        }
        votesReceived = 0;
        decideOutcome();



    }

    void decideOutcome(){
        String[] outcomeArr = outcomeVote.split(" ");
        System.out.println(outcomeVote + ": Deciding the outcome");
        ArrayList<Vote> votes = new ArrayList<>();
        for(int i = 1; i < outcomeArr.length; i++){
            votes.add(new Vote(Integer.parseInt(outcomeArr[i]), outcomeArr[i+1]));
            i++;
        }
        //Array of votes for now




    }



    void getConnections(){
        while(connected){
            try {
                Socket client = ss.accept();
                System.out.println("P" + pport + ": New participant connected.");
                ParticipantListenerThread thread = new ParticipantListenerThread(client);
                synchronized (participantListenThreads){
                    participantListenThreads.add(thread);
                    System.out.println("P" + pport + ": New participant added to list of threads.");
                }
                thread.start();
            }catch (IOException e){System.out.println("IOException at getConnections");}

        }
    }

    private class ParticipantListenerThread extends Thread{
        private Socket client;
        private BufferedReader in;
        private boolean running = true;
        private int clientPort;

        ParticipantListenerThread(Socket socket) throws IOException {
            client = socket;
            this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        }

        public void run(){
            String msg;
            String[] msgArr;
            try{
                while(running){
                    msg = in.readLine();
                    if(msg == null){
                        System.out.println("P" + pport + ": Connection lost with " + clientPort);
                        client.close();
                        in.close();
                        synchronized (participantListenThreads){
                            participantListenThreads.remove(this);
                        }
                        if(participantListenThreads.size() == 0){
                            System.out.println("P" + pport + ": No more participants");
                            System.exit(1);
                        }
                        running = false;
                        return;
                    }
                    msgArr = msg.split(" ");
                    if(msgArr[0].equals("VOTE")){
                        clientPort = Integer.parseInt(msgArr[1]);
                        System.out.println("P" + pport + ": VOTE from " + clientPort);
                        System.out.println("P" + pport + ": Registering vote");

                        synchronized (outcomeVote) {
                            outcomeVote += " " + clientPort + " " + msgArr[2];
                            System.out.println("New outcomeVote: " + outcomeVote);
                        }
                        votesReceived++;
                    }else{
                        System.out.println("P" + pport + ": Unknown message: " + msg);
                    }

                }

            }catch (IOException e){
                System.err.println("Caught I/O Exception");
            }
        }
    }

    void makeConnections(){
        for(int i : otherParts){
            try{
                Socket sendSocket = new Socket("localhost", i);
                PrintWriter sendOut = new PrintWriter(new OutputStreamWriter(sendSocket.getOutputStream()),true);
                synchronized (sendHashMap){
                    System.out.println("P" + pport +": Connection made with " + i + " and added to sendHashMap");
                    sendHashMap.put(sendSocket, sendOut);
                }
            }catch (IOException e){System.out.println("P" + pport +": Can't make a connection with another participant " + e);}
        }
    }

    void sendVote(){
//        String vote = "VOTE " + pport + " " + chosenVote;
        synchronized (sendHashMap) {
            for (HashMap.Entry<Socket, PrintWriter> entry : sendHashMap.entrySet()) {
                entry.getValue().println(outcomeVote);
            }
        }
        System.out.println("Votes sent to all the other participants");
        round++;
    }


    void waitforDetails(){
        try {
            String msg;
            String[] msgArr;
            while (!(otherParts.size() > 0)){
                msgArr = bf.readLine().split(" ");
                if(msgArr[0].equals("DETAILS")){
                    for(int i = 1; i < msgArr.length; i++){
                        otherParts.add(Integer.parseInt(msgArr[i]));
                    }
                    System.out.println("P" + pport +": Other participants received: " + otherParts);
                }else{
                    System.err.println("P" + pport +": Unexpected message error");
                    System.err.println("P" + pport +": "+ msgArr);
                    System.exit(1);
                }


            }
        }catch (IOException e){
            System.err.println("IOException at waitForDetails: " + e);
        }
    }

    void waitforOptions(){
        try {
            String msg;
            String[] msgArr;
            while (!(options.size() > 0)){
                msgArr = bf.readLine().split(" ");
                if(msgArr[0].equals("VOTE_OPTIONS")){
                    for(int i = 1; i < msgArr.length; i++){
                        options.add(msgArr[i]);
                    }
                    System.out.println("P" + pport +": Options received: " + options);
                }else{
                    System.err.println("P" + pport +": Unexpected message error");
                    System.err.println("P" + pport +": "+ msgArr);
                    System.exit(1);
                }

            }
        }catch (IOException e){
            System.err.println("IOException at waitForDetails: " + e);
        }
    }

    void chooseVote(){
        Random rand = new Random();
        String[] array = options.toArray(new String[options.size()]);


        int i = rand.nextInt(array.length);
        chosenVote = new Vote(pport, array[i]);
        outcomeVote = "VOTE " + pport + " " + chosenVote.getVote();
        System.out.println("P" + pport +": Chosen vote: " + chosenVote.getVote());
    }


    public static void main(String[] args) throws IOException{
        if(args.length < 4){
            System.out.println("Usage: java Participant <cport> <lport> <pport> <timeout>");
            return;
        }

        Participant participant = new Participant(args);
        participant.pr.println("JOIN " + participant.pport);
        System.out.println("P: Requesting JOIN with " + participant.cport);
        String in = participant.bf.readLine();
        if(in.equals("hello")){
            participant.connected =true;
            System.out.println("P: Joined coordinator on port " + participant.cport);
        }
        participant.waitforDetails();
        participant.waitforOptions();
        participant.chooseVote();

        participant.connectOthers();



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
