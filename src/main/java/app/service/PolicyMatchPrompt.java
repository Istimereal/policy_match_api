package app.service;
import app.dtos.QuestionDTO;
import app.entities.Question;
import java.util.stream.Collectors;

import app.dtos.ChatGPTPolicyMatch;
import app.dtos.QuestionDTO;
import app.dtos.UserResponseDTO;
import app.entities.Question;
import app.exceptions.ApiException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.errors.OpenAIException;

import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputItem;

import java.io.IOException;
import java.util.List;

public class PolicyMatchPrompt {
    private final ObjectMapper mapper = new ObjectMapper();

    String chatPrompt = """
Du får et anonymiseret sæt holdnings-svar til politiske udsagn i Danmark.
Formålet er KUN at lave en neutral, deskriptiv analyse af hvilke folketingspartier et sådant svarmønster typisk ligger tættest på.
Det er ikke rådgivning og ikke en anbefaling til en person.

Input består af:
- spørgsmål (tekst) + kategori/overskrift
- svar: ENIG / NEUTRAL / UENIG
- vigtighed: HØJ / MEDIUM / LAV

Opgave:
1) Vurdér hvilket folketingsparti svarmønsteret ligger tættest på ("party")
2) Vurdér næst tætteste ("secondParty")
3) For det party der matcher bedst, skal du estimér en grov match-procent ("matchPercentage") baseret på overensstemmelse på tværs af udsagn, vægtet af vigtighed.
4) Vurdér hvilket parti svarmønsteret ligger længst fra ("worstMatch")

Krav:
- Returnér KUN valid JSON (ingen forklaring, ingen tekst før/efter).
- Hvis informationen ikke er nok til at vurdere partier, returnér stadig JSON men med tomme strings og matchPercentage = 0.

Returnformat:
{
  "party": "",
  "secondParty": "",
  "matchPercentage": 0,
  "worstMatch": ""
}
""";

    private String extractText(Response response) {
        StringBuilder sb = new StringBuilder();

        for (ResponseOutputItem item : response.output()) {
            var msgOpt = item.message();
            if (msgOpt.isEmpty()) continue;

            var msg = msgOpt.get();
            for (var c : msg.content()) {
                var outOpt = c.outputText(); // Optional<ResponseOutputText>
                if (outOpt.isPresent()) {
                    sb.append(outOpt.get().text());
                }
            }
        }
        return sb.toString();
    }

    // ✅ NY SIGNATUR: tager både spørgsmål + svar
    public ChatGPTPolicyMatch getPolicyMatch(List<Question> questions,
                                             List<UserResponseDTO> userResponseDTOs)
            throws IOException, OpenAIException, JsonProcessingException {

        OpenAIClient client = OpenAIOkHttpClient.fromEnv();


        List<QuestionDTO> questionDTOs = questions.stream()
                .map(QuestionDTO::new)
                .collect(Collectors.toList());

        String questionsJson = mapper.writeValueAsString(questionDTOs);
        String responsesJson = mapper.writeValueAsString(userResponseDTOs);

        String fullPrompt =
                chatPrompt
                        + "\n\nSpørgsmålene (JSON):\n" + questionsJson
                        + "\n\nBrugerens svar (JSON):\n" + responsesJson;

        ResponseCreateParams params = ResponseCreateParams.builder()
                .model("gpt-5")
                .input(fullPrompt)
                .build();

        Response response = client.responses().create(params);

        String text = extractText(response).trim();
        System.out.println("Text i getPolicymatch: " + text);

        if (!text.startsWith("{")) {
            throw new ApiException(502, "AI returned non-JSON output: " + text);
        }

        return mapper.readValue(text, ChatGPTPolicyMatch.class);
    }
}
