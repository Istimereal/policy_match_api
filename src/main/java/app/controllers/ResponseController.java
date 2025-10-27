package app.controllers;

import app.daos.UserResponseDAO;

import app.dtos.ChatGPTPolicyMatch;
import app.dtos.UserResponseDTO;
import app.entities.Question;
import app.entities.UserResponse;
import app.entities.UserResponseId;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Array;
import java.security.Provider;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static app.service.ConverterUserResponse.convertToUserResponseDTOList;
import static app.service.ConverterUserResponse.convertToUserResponseList;

public class ResponseController {

    PolicyMatchPrompt policyMatchPrompt = new PolicyMatchPrompt();
    LocalDateTime timeStamp = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    String formattedTime = timeStamp.format(formatter);

    private static final Logger logger = LoggerFactory.getLogger("production");
    private static final Logger debugLogger = LoggerFactory.getLogger("debug");

    private final UserResponseDAO userResponseDAO;
    private final EntityManagerFactory emf;

    public ResponseController(UserResponseDAO userResponseDAO, EntityManagerFactory _emf) {
        this.userResponseDAO = userResponseDAO;
        this.emf = _emf;
    }

    public void userResponse(Context ctx) {

        List<UserResponse> userResponsesWithUandQ = new ArrayList<>();

        try (EntityManager em = emf.createEntityManager()){
            int userId = ctx.attribute("userId");

            User user = em.find(User.class, userId);
            List<UserResponseDTO> allResponses = Arrays.asList(ctx.bodyAsClass(UserResponseDTO[].class));

            for (UserResponseDTO dto : allResponses) {
                Question q = em.find(Question.class, dto.getQuestionId());

                UserResponseId id = new UserResponseId(user.getId(), q.getId());

                UserResponse r = UserResponse.builder()
                        .id(id)
                        .user(user)
                        .question(q)
                        .response(dto.getResponse())
                        .importance(dto.getImportance())
                        .build();

                userResponsesWithUandQ.add(r);
            }

          //  List<UserResponse> responseWithAll = convertToUserResponseList(allResponses, userId);
            userResponseDAO.createResponse(userResponsesWithUandQ);

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

