package app.dtos;

public class ChatGPTPolicyMatch {

    String party;

    int matchPercentage;

    String secondParty;

    String worstMatch;

    public ChatGPTPolicyMatch(String party, int matchPercentage, String secoundParty, String worstMAtch) {

        this.party = party;
        this.matchPercentage = matchPercentage;
        this.secondParty = secoundParty;
        this.worstMatch = worstMAtch;
    }
}
