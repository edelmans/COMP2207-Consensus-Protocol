import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Participant {
    private Socket s;
    private ServerSocket ss;
    //private Socket ls;
    private PrintWriter pr;
    private InputStreamReader in;
    private BufferedReader bf;
    private boolean connected = false;
    private boolean p2pComplete = false;

    private int cport, lport, pport, timeout;
    // <cport> port number that coordinator is listening on
    // <lport> port that logger server is listening on
    // <pport> port that this participant will listen on
    // <timeout> timeout in milliseconds
    private ArrayList<Integer> otherParts = new ArrayList<>();
    private Set<String> options = new HashSet<String>();
    private Vote chosenVote;

    private ArrayList<Thread> participantServerThreads = new ArrayList<>();
    private ArrayList<Thread> participantClientThreads = new ArrayList<>();

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
        while(connected){
            try{
                if(p2pComplete == false){
                    waitForConnections();
                }
            }
        }
    }

    void waitForConnections(){
        for(int p : otherParts){

        }
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
        System.out.println("P" + pport +": Chosen vote: " + chosenVote.getVote());
    }

    private class ParticipantClientThread extends Thread{
        private Socket client;
        private int serverPport;
        private BufferedReader in;
        private PrintWriter out;
        private boolean running = true;

        ParticipantClientThread(Socket client) throws IOException {


        }

        public void run(){

        }
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
        System.err.println("Ready to vote");



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
