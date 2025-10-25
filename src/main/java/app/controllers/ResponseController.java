package app.controllers;

import app.daos.UserResponseDAO;

import app.dtos.ChatGPTPolicyMatch;
import app.dtos.UserResponseDTO;
import app.entities.UserResponse;
import app.exceptions.ApiException;
import app.security.User;
import app.service.PolicyMatchPrompt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.errors.OpenAIException;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Array;
import java.security.Provider;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static app.service.ConverterUserResponse.convertToUserResponseDTOList;
import static app.service.ConverterUserResponse.convertToUserResponseList;

public class ResponseController {
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

    PolicyMatchPrompt policyMatchPrompt = new PolicyMatchPrompt();
    LocalDateTime timeStamp = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    String formattedTime = timeStamp.format(formatter);

    private static final Logger logger = LoggerFactory.getLogger("production");
    private static final Logger debugLogger = LoggerFactory.getLogger("debug");

    private final UserResponseDAO userResponseDAO;

    public ResponseController(UserResponseDAO userResponseDAO) {
        this.userResponseDAO = userResponseDAO;
    }

    public void userResponse(Context ctx) {

        try {
            int userId = ctx.attribute("userId");
            List<UserResponseDTO> allResponses = Arrays.asList(ctx.bodyAsClass(UserResponseDTO[].class));

            List<UserResponse> responseWithAll = convertToUserResponseList(allResponses, userId);
            userResponseDAO.createResponse(responseWithAll);

            ctx.status(HttpStatus.OK).json(Map.of("status", HttpStatus.OK, "msg",
                    "Your responses have been saved, go to GET /api/v1/responses to see your policy match"));
        } catch (BadRequestResponse br) {
            ctx.status(HttpStatus.BAD_REQUEST).
                    json(Map.of("status", HttpStatus.BAD_REQUEST.getCode(),
                            "msg", "Invalid post, see documentation for correct form"));
        } catch (PersistenceException pe) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
                    "msg", "Database problems, try again later"));
            debugLogger.error(formattedTime, "Database problems", pe);
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
                    "msg", "There was an unexpected problem with the server"));
            debugLogger.error(formattedTime, "There was an unexpected problem with the server", e);
        }
    }

    public void getPolicyMatch(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.attribute("userId").toString());

            List<UserResponse> userResponses = userResponseDAO.getAllResponse(id);
            if (userResponses.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND).json(Map.of("status", HttpStatus.NOT_FOUND.getCode(), "msg",
                        "You need to answer questions before you can get a policy match"));
            }
            List<UserResponseDTO> userResponseDTOs = convertToUserResponseDTOList(userResponses);
            ChatGPTPolicyMatch result = policyMatchPrompt.getPolicyMatch(userResponseDTOs);
            ctx.status(HttpStatus.OK).json(result);

        } catch (ApiException ae) {

            ctx.status(HttpStatus.NOT_FOUND).json(Map.of("status", HttpStatus.NOT_FOUND.getCode(),
                    "msg", "You need to answer questions before you can get a policy match"));
        } catch (PersistenceException pe) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
                    "msg", "Database problems, try again later"));
        } catch (OpenAIException oe) {
            ctx.status(HttpStatus.BAD_GATEWAY).json(Map.of(
                    "status", HttpStatus.BAD_GATEWAY.getCode(),
                    "msg", "ChatGPT didn´t reply, try agai"));
      debugLogger.error(formattedTime, "Intern error from chatGPT", oe);
        } catch (JsonProcessingException jpe) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of(
                    "status", HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
                    "msg", "ChatGPT didn´t reply, try again" + jpe.getMessage()));
            debugLogger.error(formattedTime, "There was an unexpected problem with the server", jpe);
        } catch (IOException ioe) {
            ctx.status(HttpStatus.SERVICE_UNAVAILABLE).json(Map.of(
                    "status", HttpStatus.SERVICE_UNAVAILABLE.getCode(),
                    "msg", "ChatGPT didn´t reply, try again"));
            debugLogger.error(formattedTime, "Connection error or networkproblems propmting chatGPT", ioe);
        }
        catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("stastus", HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
                    "msg", "An unexpected error happened with the server, try again"));
            debugLogger.error(formattedTime, "Unexpected error", e);
        }
        }
    }

