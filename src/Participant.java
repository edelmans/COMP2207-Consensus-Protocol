import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

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

    private ArrayList<Integer> otherParticipants = new ArrayList<>();
    private ArrayList<String> voteOptions = new ArrayList<>();

    private String chosenVote = null;

    private HashMap<Thread, Integer> participantConnections = new HashMap<>();

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

    public void getDetails() throws IOException{
        String msg;
        msg = bf.readLine();

        if(msg.contains("DETAILS")){
            String[] msgArr = msg.split(" ");
            for(int i = 1; i < msgArr.length; i++){
                otherParticipants.add(Integer.parseInt(msgArr[i]));
            }
            System.out.println("Other participants received: " + otherParticipants);
        }else{
            System.out.println("Unexpected message: " + msg);
        }
    }

    public void getOptions() throws IOException {
        String[] msgArr = bf.readLine().split(" ");

        if(msgArr[0].equals("VOTE_OPTIONS")){
            System.out.println("Getting VOTE_OPTIONS");
            for(int i = 1; i < msgArr.length; i++){
                voteOptions.add(msgArr[i]);
            }
            System.out.println("VOTE_OPTIONS " + voteOptions);
        }else{
            System.out.println("Unexpected message: " + msgArr);
        }
    }

    public void chooseVote(){
        System.out.println("\nPicking a random vote...");
        Random random = new Random();
        int voteNum = random.nextInt(voteOptions.size());
        chosenVote = voteOptions.get(voteNum);
        System.out.println("P" + pport + ": Chosen vote is: " + chosenVote);
    }

    public void connectToOthers() throws IOException {
        for(int i : otherParticipants) {
            ParticipantThread thread = new ParticipantThread(i);
            synchronized (participantConnections){
                participantConnections.put(thread, i);
            }
            thread.start();
        }
    }



//    public void castVote() throws IOException {
//        Socket tmpSocket;
//        PrintWriter tmpPr;
//        InputStreamReader tmpIn;
//        BufferedReader tmpBr;
//        for(int i : otherParticipants){
//            tmpSocket = new Socket("localhost", i);
//            tmpPr = new PrintWriter(tmpSocket.getOutputStream());
//            tmpIn = new InputStreamReader(tmpSocket.getInputStream());
//            tmpBr = new BufferedReader(tmpIn);
//
//            tmpPr.println("VOTE ");
//        }
//
//    }

    public class ParticipantThread extends Thread{
        Socket pSocket;
        PrintWriter out;
        InputStreamReader in;
        BufferedReader inBr;
        int otherPort;

        public ParticipantThread(int p1) throws IOException {
            otherPort = p1;
            pSocket = new Socket("localhost", otherPort);
            out = new PrintWriter(pSocket.getOutputStream());
            in = new InputStreamReader(pSocket.getInputStream());
            inBr = new BufferedReader(in);
        }

        public void run(){

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
            System.out.println("Successfully joined");
        }else{
            System.out.println("Failed at join request");
            return;
        }
        participant.getDetails();
        participant.getOptions();

        participant.chooseVote();

        participant.connectToOthers();
        //participant.castVote();


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
