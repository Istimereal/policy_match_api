package app.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatGPTPolicyMatch {

  /*  String party;

    int matchPercentage;

    String secondParty;

    String worstMatch;  */
  private String party;
    private String secondParty;
    private int matchPercentage;
    private String worstMatch;

    public ChatGPTPolicyMatch(String party, int matchPercentage, String secoundParty, String worstMAtch) {

        this.party = party;
        this.matchPercentage = matchPercentage;
        this.secondParty = secoundParty;
        this.worstMatch = worstMAtch;
    }
}
