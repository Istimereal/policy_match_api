package app.service;

import app.dtos.ChatGPTPolicyMatch;
import app.dtos.UserResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.errors.OpenAIException;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

import java.io.IOException;
import java.util.List;

public class PolicyMatchPrompt {
    String chatPrompt = """
[Her har du en række spørgsmål på fra en bruger. Det er meningen at du skal evaluere brugerens politiske orientering ud fra svarene. 
Der er en kategori, med en tilhørende overskrift og spørgsmål. Brugeren er enten enig, neutral eller uenig i forslaget fra spørgsmålet. 
Derud over har de vægtet spørgsmålets betydning for deres politiske overbevisning. Så de enten synes emne og problematik/spørgsmålet er vigtigt, 
af lav betydning for deres orientering, eller har betydning for deres orientering, men er ikke meget afgørende (MEDIUM). Jeg ønsker at du ud fra 
danske partier som stiller op til folketinget, vurder hvilket parti, der matcher helheden af deres svar og vægtning for hvert enkelt spørgsmål 
(party). Dernæst ønskes det næst bedste match (secoundParty). Samt for det bedste match hvor mange procent de er enige med den gennerelle 
holdning i partiet. Altså deres svar opholdt mod hvordan du vurderer partiet ville svare og vægte spørgsmålene, hvis man kunne spørge hele 
det bedst matchende parti på en gang og få partigruppens samlede svar(matchPercentage). Til sidst ønsker jeg at du vurderer hvilket svar 
der matcher brugerens holdning dårligst (worstMatch). \s]
Returnér resultatet som JSON:
{
  "party": "",
  "secondParty": "",
  "matchPercentage": 0,
  "worstMatch": ""
}
""";

    public ChatGPTPolicyMatch getPolicyMatch(List<UserResponseDTO> userResponseDTOs)   throws IOException, OpenAIException, JsonProcessingException {
        ChatGPTPolicyMatch chatGPTPolicyMatch = null;

        OpenAIClient client = OpenAIOkHttpClient.fromEnv();

        ResponseCreateParams params = ResponseCreateParams.builder()
                .input(chatPrompt)
                .model("gpt-5")
                .build();

        Response response = client.responses().create(params);

        return chatGPTPolicyMatch;
    }
}
