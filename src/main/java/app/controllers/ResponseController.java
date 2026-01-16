package app.controllers;

import app.daos.QuestionDAO;
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
import dk.bugelhartmann.UserDTO;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
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
    private final QuestionDAO questionDAO;
    private final EntityManagerFactory emf;

    public ResponseController(QuestionDAO questionDAO, UserResponseDAO userResponseDAO, EntityManagerFactory _emf) {
        this.userResponseDAO = userResponseDAO;
        this.questionDAO = questionDAO;
        this.emf = _emf;
    }

    public void userResponse(Context ctx) {

        List<UserResponse> userResponsesWithUandQ = new ArrayList<>();

        try (EntityManager em = emf.createEntityManager()){

            UserDTO tokenUser = ctx.attribute("user");
            String username = tokenUser.getUsername();


            TypedQuery<User> query = em.createQuery(
                    "SELECT u FROM User u WHERE u.username = :username", User.class);
            query.setParameter("username", username);

            User user = query.getSingleResult();
            int userId = user.getId();

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
                    "Your responses have been saved, go to Policy Match Evaluation to see your evaluation"));
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
        try (EntityManager em = emf.createEntityManager()){
            UserDTO tokenUser = ctx.attribute("user");
            String username = tokenUser.getUsername();


            TypedQuery<User> query = em.createQuery(
                    "SELECT u FROM User u WHERE u.username = :username", User.class);
            query.setParameter("username", username);

            User user = query.getSingleResult();
            int userId = user.getId();

            List<UserResponse> userResponses = userResponseDAO.getAllResponse(userId);


            if (userResponses.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND).json(Map.of("status", HttpStatus.NOT_FOUND.getCode(), "msg",
                        "You need to answer questions before you can get a policy match"));
                return;
            }
            List<UserResponseDTO> userResponseDTOs = convertToUserResponseDTOList(userResponses);

            List<Question> questions = questionDAO.getAllQuestions(); // eller hvor du har den
            ChatGPTPolicyMatch result = policyMatchPrompt.getPolicyMatch(questions, userResponseDTOs);
            System.out.println("Resultat inden evaluering sendes til frontend: " + result);
            ctx.status(HttpStatus.OK).json(result);

        } catch (ApiException ae) {

            ctx.status(HttpStatus.NOT_FOUND).json(Map.of("status", HttpStatus.NOT_FOUND.getCode(),
                    "msg", "You need to answer questions before you can get a policy match"));
        } catch (PersistenceException pe) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
                    "msg", "Database problems, try again later"));
        }
        catch (OpenAIException oe) {
            // OpenAI SDK: 429 er typisk "quota/billing"
            String msg = oe.getMessage();

            if (msg != null && msg.contains("429")) {
                ctx.status(HttpStatus.TOO_MANY_REQUESTS).json(Map.of(
                        "status", HttpStatus.TOO_MANY_REQUESTS.getCode(),
                        "msg", "OpenAI quota/billing problem (429). Check API key + billing on platform.openai.com.",
                        "detail", msg
                ));
            } else {
                ctx.status(HttpStatus.BAD_GATEWAY).json(Map.of(
                        "status", HttpStatus.BAD_GATEWAY.getCode(),
                        "msg", "OpenAI call failed",
                        "detail", msg
                ));
            }
            debugLogger.error(formattedTime, "OpenAI error", oe);

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

        /* gammel AI catch blok
        catch (OpenAIException oe) {
            ctx.status(HttpStatus.BAD_GATEWAY).json(Map.of(
                    "status", HttpStatus.BAD_GATEWAY.getCode(),
                    "msg", "ChatGPT didn´t reply, try agai"));
      debugLogger.error(formattedTime, "Intern error from chatGPT", oe);
      */

    }

