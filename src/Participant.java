import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Participant extends Thread {
    private Socket socket;
    private ServerSocket ss;
    //private Socket ls;
//    private PrintWriter pr;
//    private InputStreamReader in;
//    private BufferedReader bf;

    private int cport, lport, pport, timeout;
    // <cport> port number that coordinator is listening on
    // <lport> port that logger server is listening on
    // <pport> port that this participant will listen on
    // <timeout> timeout in milliseconds

    private ArrayList<Integer> otherParticipants = new ArrayList<>();
    private ArrayList<String> voteOptions = new ArrayList<>();

    private String chosenVote = null;

    private HashMap<Thread, Integer> participantConnections = new HashMap<>();
    private Map<Integer, PrintWriter> map = Collections.synchronizedMap(new HashMap<Integer, PrintWriter>());


    private class ClientThread extends Thread{
        private Socket client;
        private Integer pport;
        private BufferedReader br;
        private PrintWriter pr;

        ClientThread(Socket client, int port) throws IOException{
            this.client = client;
            br = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
            pr = new PrintWriter(new OutputStreamWriter(this.client.getOutputStream()));
            this.pport = port;
            System.out.println("Participant initialized");
        }

        public void run(){
            pr.println("JOIN " + pport);
            pr.flush();

            try{
                Tokenizer.Token token = null;
                Tokenizer tokenizer = new Tokenizer();
                String msg = br.readLine();
                System.out.println(msg);
                token = tokenizer.getToken(br.readLine());
                if(!(token instanceof Tokenizer.ACKToken)){
                    System.out.println("Expected ACKToken");
                    client.close();
                    return;
                }
                token = tokenizer.getToken(br.readLine());
                if(token instanceof Tokenizer.DetailToken){
                    connectParticipants(((Tokenizer.DetailToken)token).message);
                }else{
                    System.out.println("Expected DetailToken");
                    client.close();
                }
                token = tokenizer.getToken(br.readLine());
                if(token instanceof Tokenizer.VoteOptionToken){
                    chooseVote(((Tokenizer.VoteOptionToken)token).options);
                    beginVote();
                }else{
                    System.out.println("Expected VoteOptionToken");
                    client.close();
                }
                if(token instanceof Tokenizer.VoteToken){
                    collectVotes(((Tokenizer.VoteToken)token).message);
                    beginVote();
                }
                String outFinal = getOutcome();
                pr.println(outFinal);
                pr.flush();
                client.close();

            }catch (IOException e){
                System.err.println("Caught I/O Exception");
            }
        }
    }
    void connectParticipants(String participants){
        String[] partArr = participants.split(" ");
        System.out.println("Participants string:" + participants);
        for(int i = 1; i < partArr.length; i++){
            System.out.println("s:" + partArr[i]);
            otherParticipants.add(Integer.parseInt(partArr[i]));
        }

        System.out.println("P" + pport +": receiving other participants\n" + otherParticipants);
        System.out.println("Trying to connect participants");
    }
    void chooseVote(String options){
        System.out.println("Received vote options:" + options);
        ArrayList<String> tempArr = new ArrayList<String>(Arrays.asList(options.split(" ")));
        tempArr.remove(0);
        String optionArr[] = new String[tempArr.size()];
        optionArr = tempArr.toArray(optionArr);
        Random random = new Random();
        int optionNum = random.nextInt(optionArr.length);
        chosenVote = optionArr[optionNum];
        System.out.println("Chosen vote:" + chosenVote);

    }

    void beginVote(){
        System.out.println("Voting begins");
    }

    void collectVotes(String msg){
        System.out.println("Collecting votes");
    }

    String getOutcome(){
        String str = "";
        for(Integer i:otherParticipants){
            str += " " + i;
         }

        String msg = "OUTCOME " + chosenVote + str + " " + pport;
        System.out.println(msg);
        return msg;

    }

    void startConnection(String[] args) throws IOException{
        cport = Integer.parseInt(args[0]);
        lport = Integer.parseInt(args[1]);
        pport = Integer.parseInt(args[2]);
        timeout = Integer.parseInt(args[3]);
        socket = new Socket("localhost", cport);

        new ClientThread(socket, pport).start();
    }

    public static void main(String[] args) throws IOException{
        if(args.length < 4){
            System.out.println("Usage: java Participant <cport> <lport> <pport> <timeout>");
            return;
        }

        try {
            new Participant().startConnection(args);
        }catch (Exception e){System.err.println("Connection refused for one of the following reasons:" +
                "\n1. Voting already has started\n2. No Coordinator found on specified port");}

    }


}
