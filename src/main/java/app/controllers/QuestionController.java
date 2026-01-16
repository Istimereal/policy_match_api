package app.controllers;

import app.daos.QuestionDAO;
import app.dtos.QuestionDTO;
import app.entities.Question;
import app.exceptions.ApiExceptionCreate;
import app.service.ConverterQuestion;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.HttpStatus;
import jakarta.persistence.Converter;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.LoggerFactory;
import app.service.ConverterQuestion.*;

import io.javalin.http.Context;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.Keymap;

import static app.utils.ResponseUtil.disableCache;

public class QuestionController {
LocalDateTime timeStamp = LocalDateTime.now();
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
String formattedTime = timeStamp.format(formatter);

private static final Logger logger = LoggerFactory.getLogger("production");
    private static final Logger debugLogger = LoggerFactory.getLogger("debug");
    private final QuestionDAO questionDAO;

    public QuestionController(QuestionDAO questionDAO) {
        this.questionDAO = questionDAO;
    }

    public void createQuestions(Context ctx){
        List<Question> newQuestions = new ArrayList<Question>();
try {
    QuestionDTO newQuestiones = ctx.bodyAsClass(QuestionDTO.class);
    System.out.println("NyeSpørgsmål: " + newQuestiones);
   Question converted = ConverterQuestion.convertDTOToQuestion(newQuestiones);
   newQuestions.add(converted);
  List<Question> savedQuestions =  questionDAO.createQuestion(newQuestions);

  List<QuestionDTO> response = ConverterQuestion.convertToQuestionDTO(savedQuestions);
  QuestionDTO resp = response.get(0);

    ctx.status(HttpStatus.CREATED).json(resp);
    logger.info("Created questions successfully");
}
catch(BadRequestResponse br) {

    ctx.status(HttpStatus.BAD_REQUEST).
            json(Map.of("status", HttpStatus.BAD_REQUEST.getCode(),
                    "msg", "Invalid post, see documentation for correct form"));
}
catch(ApiExceptionCreate aec){
    int questionAmount;
    questionAmount = aec.getQuestions().size();
    ctx.status(HttpStatus.CONFLICT).json(Map.of("status", HttpStatus.CONFLICT.getCode(), "msg", "A question with header: " + aec.getQuestions().get(questionAmount - 1).getHeader() +
            "and subject: " + aec.getQuestions().get(questionAmount - 1).getSubject() + " already exists, adjust the question and try again"));
}
catch (PersistenceException pe){

    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
            "msg","Database problems, try agian later"));
    debugLogger.error(formattedTime, "Database problems", pe);
}
catch(Exception e) {
    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("status",HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
            "msg", "There was an unexpected problem with the server"));
    debugLogger.error(formattedTime, "Unexpected server problem", e);
}
    }

    public void updateQuestion(Context ctx) {
        int id;
        try {
            id = Integer.parseInt(ctx.pathParam("id"));

            if (id <= 0) {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of(
                        "status", HttpStatus.BAD_REQUEST.getCode(),
                        "msg", "Invalid id"
                ));
                return;
            }

            Question existing = questionDAO.findQuestionById(id);
            if (existing == null) {
                ctx.status(HttpStatus.NOT_FOUND).json(Map.of(
                        "status", HttpStatus.NOT_FOUND.getCode(),
                        "msg", "Question not found"
                ));
                return;
            }

            QuestionDTO questionDTO = ctx.bodyAsClass(QuestionDTO.class);

            if (questionDTO.getHeader() != null && questionDTO.getHeader().isEmpty()) {
                throw new BadRequestResponse("Header cannot be empty");
            }
            if (questionDTO.getSubject() == null || questionDTO.getSubject().isEmpty()) {
                throw new BadRequestResponse("Subject cannot be empty");
            }
            if (questionDTO.getQuestionText() != null && questionDTO.getQuestionText().isEmpty()) {
                throw new BadRequestResponse("Question text cannot be empty");
            }

            Question q = ConverterQuestion.convertDTOToQuestion(questionDTO);
            q.setId(id);

            Question updatedQuestion = questionDAO.updateQuestion(q);

            if (updatedQuestion != null) {
                QuestionDTO responseDTO = ConverterQuestion.convertQuestionToDTO(updatedQuestion);
                ctx.status(HttpStatus.OK).json(responseDTO);
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of(
                        "status", HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
                        "msg", "Question could not be updated"
                ));
            }

        } catch (NumberFormatException ne) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of(
                    "status", HttpStatus.BAD_REQUEST.getCode(),
                    "msg", "Invalid id format"
            ));
        } catch (BadRequestResponse bre) {
            String message = (bre.getMessage() == null || bre.getMessage().isBlank())
                    ? "question with id: " + ctx.pathParam("id") + " was not in valid JSON format. See API documentation for correct structure."
                    : bre.getMessage();

            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of(
                    "status", HttpStatus.BAD_REQUEST.getCode(),
                    "msg", message
            ));
        } catch (PersistenceException pe) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of(
                    "status", HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
                    "msg", "Database problems, try again later"
            ));
            debugLogger.error(formattedTime, "Database problems", pe);
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of(
                    "status", HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
                    "msg", "There was an unexpected problem with the server"
            ));
            debugLogger.error(formattedTime, "Unexpected server problem", e);
        }
    }

    /*
gammel update question med lille fejl
    public void updateQuestion(Context ctx) {
        int id = 0;
        String operationType = "initializing";
        Question question = null;
        try {
            id = Integer.parseInt(ctx.pathParam("id"));

            if (id > 0) {
                question = questionDAO.findQuestionById(id);
                if (question == null) {
                    ctx.status(HttpStatus.NOT_FOUND).json(Map.of("status", HttpStatus.NOT_FOUND.getCode(), "msg", "Question not found"));
                    return;
                }
            }
            QuestionDTO questionDTO = ctx.bodyAsClass(QuestionDTO.class);
            if (questionDTO.getHeader() != null && questionDTO.getHeader().isEmpty()) {
                throw new BadRequestResponse("Header cannot be empty");
            }
            if (questionDTO.getSubject() == null || questionDTO.getSubject().isEmpty()) {
                throw new BadRequestResponse("Subject cannot be empty");
            }
            if (questionDTO.getQuestionText() != null && questionDTO.getQuestionText().isEmpty()) {
                throw new BadRequestResponse("Question text cannot be empty");
            }
// mangler at lave QuestionDTO og returnere den med ctx
   Question updatedQuestion = questionDAO.updateQuestion(ConverterQuestion.convertDTOToQuestion(questionDTO));
            if(updatedQuestion != null){
QuestionDTO responseDTO = ConverterQuestion.convertQuestionToDTO(updatedQuestion);
                ctx.status(HttpStatus.OK).json(responseDTO);
            }
            else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .json(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
                                "msg", "Question could not be updated"));
            }

        } catch (NumberFormatException ne) {
            ctx.json(Map.of("status", HttpStatus.BAD_REQUEST.getCode(), "msg", "Invalid id format"));
        } catch (BadRequestResponse bre) {
            // 👇 skelner mellem JSON-formatfejl og logiske fejl
            String message;

            if (bre.getMessage() == null || bre.getMessage().isBlank()) {
                // A: JSON-formatfejl (bodyAsClass fejlede)
                message = "question with id: " + ctx.pathParam("id") +
                        " was not in valid JSON format. See API documentation for correct structure.";
            } else {
                message = bre.getMessage();
            }
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("status", HttpStatus.BAD_REQUEST.getCode(), "msg", message));

        } catch (PersistenceException pe) {
            ctx.json(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR.getCode(), "msg", "Database problems, try again later"));
            debugLogger.debug(formattedTime + "; Database error trying to: " + operationType, pe);
        }
        catch (Exception e) {
            ctx.json(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR.getCode(), "msg", "Unexpected error updating poem" + ctx.pathParam("id")));
            debugLogger.debug(formattedTime + "; Unexpected error trying to update poem:" + id + "OperationState: " + operationType, e);
        }
    }  */

    public void getAllQuestions(Context ctx){

try{
    disableCache(ctx);
    List<QuestionDTO> allQuestions = ConverterQuestion.convertToQuestionDTO(questionDAO.getAllQuestions());
    if(allQuestions.isEmpty()){

        ctx.status(HttpStatus.NOT_FOUND).json(Map.of("status", HttpStatus.NOT_FOUND.getCode(),
                "msg", "Questions not found"));
        logger.warn("No questions in Database");
    }
    else{
        ctx.status(HttpStatus.OK).json(allQuestions);

    }
}
catch (PersistenceException pe) {

    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
            "msg","Database problems, try agian later"));
    debugLogger.debug(formattedTime, "Error with database", pe);
}
 catch (Exception e) {

    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("status",
            HttpStatus.INTERNAL_SERVER_ERROR.getCode(), "msg",
            "There was an unexpected error with the server, try again later"));
 }
    }

    public void deleteQuestion(Context ctx){

        try {
            int id = Integer.parseInt(ctx.pathParam("id"));

            disableCache(ctx);
            if (id > 0) {
                questionDAO.deleteQuestion(id);
                ctx.status(HttpStatus.OK).json(Map.of("status", HttpStatus.OK.getCode(), "msg",
                        "Question deleted"));
            } else {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("status", HttpStatus.BAD_REQUEST.getCode(), "msg", "You need to type at id above 0"));
            }
        }
        catch (NumberFormatException ne) {
            ctx.json(Map.of("status", HttpStatus.BAD_REQUEST.getCode(), "msg",
                    "Invalid id format:" + ctx.pathParam("id")));
        }
        catch(PersistenceException pe) {

            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
                    "msg", "There was a problem with the database"));
            debugLogger.debug(formattedTime + "; Database problems", pe);
        }
        catch(Exception e) {

            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR.getCode(), "msg", "There was an unexpected error with the server"));
        }
    }
}
