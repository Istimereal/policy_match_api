package app.daos;

import app.entities.Question;
import app.exceptions.ApiException;
import app.exceptions.ApiExceptionCreate;
import app.exceptions.EntityNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import org.hibernate.exception.ConstraintViolationException;

import java.util.ArrayList;
import java.util.List;

public class QuestionDAO {
private static QuestionDAO instance;
private static EntityManagerFactory emf;

public QuestionDAO(){}

    public static QuestionDAO getInstance(EntityManagerFactory _emf){
        if(instance == null){
          instance = new QuestionDAO();
          emf = _emf;
        }
        return instance;
    }

    public List<Question> createQuestion(List<Question> questions){
    List<Question> newQuestiones = new ArrayList<>();
    try(EntityManager em = emf.createEntityManager()){
           em.getTransaction().begin();
           for(Question q : questions) {
               em.persist(q);
               newQuestiones.add(q);
           }
           em.getTransaction().commit();
       }
       catch(ConstraintViolationException cve){
           throw new ApiExceptionCreate(400, "Duplicate or invalid question detected", newQuestiones);
       }
       catch(PersistenceException pex){
        throw new ApiException(500, pex.getMessage());
       }
    return  newQuestiones;
    }

    public List<Question> getAllQuestions(){

    try(EntityManager em = emf.createEntityManager()){
        TypedQuery<Question> query = em.createQuery(("SELECT q FROM Question q"),Question.class);
        return query.getResultList();
        }
    catch(PersistenceException pex){
        throw new ApiException(500, pex.getMessage());
    }
    }

    public Question findQuestionById(int id){

    try (EntityManager em = emf.createEntityManager()){
        Question question = em.find(Question.class, id);
        if (question == null){
            throw new ApiException(404, "Question with id " + id + " does not exist");
        }
        return question;
    }
catch(Exception e){
        throw new ApiException(500, e.getMessage());
}
    }

    public Question updateQuestion(Question question){

   try(EntityManager em = emf.createEntityManager()){
       Question before = em.find(Question.class, question.getId());

       if(before == null){
           throw new ApiException(404, "Question not found");
       }
if(question.getHeader() != null){
    before.setHeader(question.getHeader());
}
if(question.getQuestionText() != null){
    before.setQuestionText(question.getQuestionText());
}
if (question.getSubject() != null){
    before.setSubject(question.getSubject());
}
       em.getTransaction().begin();
       em.merge(before);
       em.getTransaction().commit();
       return before;
   }
   catch(PersistenceException pex){
       throw new ApiException(500, pex.getMessage());
   }
    }

    public void deleteQuestion(int id){
    try(EntityManager em = emf.createEntityManager()){
        em.getTransaction().begin();
        Question question = em.find(Question.class, id);
        if (question == null){
            throw new ApiException(404, "Question with id " + id + " does not exist");
        }
        em.remove(question);
        em.getTransaction().commit();
    }
catch (PersistenceException pex){
        throw new ApiException(500, pex.getMessage());
}
    }
}


