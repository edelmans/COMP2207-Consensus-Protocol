import javax.xml.soap.Detail;
import java.util.ArrayList;
import java.util.StringTokenizer;

class Tokenizer {
    Tokenizer() {;}

    Token getToken(String req){
        StringTokenizer sTokenizer = new StringTokenizer(req);
        if(!(sTokenizer.hasMoreTokens()))
            return null;
        String firstToken = sTokenizer.nextToken();
        if(firstToken.equals("JOIN")){
            if(sTokenizer.hasMoreTokens())
                return new JoinToken(req, sTokenizer.nextToken());
            else
                return null;
        }
        if(firstToken.equals("OUTCOME")) {
            String vote = "";
            ArrayList<Integer> parts = new ArrayList<>();
            vote += sTokenizer.nextToken();
            while(sTokenizer.hasMoreTokens())
                parts.add(Integer.parseInt(sTokenizer.nextToken()));
            return new OutcomeToken(req, vote, parts);
        }
        if(firstToken.equals("DETAILS")){
            String msg = "";
            while(sTokenizer.hasMoreTokens())
                msg += " " + sTokenizer.nextToken();
            return new DetailToken(req, msg);
        }
        if(firstToken.equals("VOTE_OPTIONS")){
            String msg = "";
            while(sTokenizer.hasMoreTokens())
                msg += " " + sTokenizer.nextToken();
            return new VoteOptionToken(req, msg);
        }
        if(firstToken.equals("ACK")){
            if(sTokenizer.hasMoreTokens())
                return new ACKToken(req, sTokenizer.nextToken());
            else
                return null;
        }
        if(firstToken.equals("VOTE")){
            String msg = "";
            while(sTokenizer.hasMoreTokens())
                msg += " " + sTokenizer.nextToken();
            return new VoteToken(req, msg);
        }
//        if(firstToken.equals("DETAILS")){
//            String msg = "";
//            while (sTokenizer.hasMoreTokens())
//                msg += " " + sTokenizer.nextToken();
//            return new DetailsToken(req, name, )
//        }
        return null;
    }

    abstract class Token{
        String req;
    }

    class VoteToken extends Token{
        String message;
        VoteToken(String req, String msg){
            this.req = req;
            this.message = msg;
        }
    }

    class JoinToken extends Token{
        String name;
        JoinToken(String req, String name){
            this.req = req;
            this.name = name;
        }
    }
    class OutcomeToken extends Token{
        String vote;
        ArrayList<Integer> parts;
        OutcomeToken(String req, String vote, ArrayList<Integer> parts) {
            this.req = req;
            this.vote = vote;
            this.parts = parts;
        }
    }
    class DetailToken extends Token{
        String message;
        DetailToken(String req, String msg){ this.req = req; message = msg;}
    }
    class VoteOptionToken extends Token{
        String options;
        VoteOptionToken(String req, String opt){this.req = req; options = opt;}
    }
    class ACKToken extends Token{
        Integer port;
        ACKToken(String req, String port){this.req = req; this.port = Integer.parseInt(port);}
    }
}


