package app.daos;

import app.entities.UserResponse;
import app.exceptions.ApiException;
import app.security.User;
import jakarta.persistence.*;

import java.util.List;

public class UserResponseDAO {
    private static UserResponseDAO instance;
    private static EntityManagerFactory emf;


  public UserResponseDAO() {
    }

    public static UserResponseDAO getInstance(EntityManagerFactory _emf) {
        if (instance == null) {
            instance = new UserResponseDAO();
            emf = _emf;
        }
        return instance;
    }

    public void createResponse(List<UserResponse> response) {

        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<UserResponse> query = em.createQuery(
                    "SELECT ur FROM UserResponse ur WHERE ur.user = :user AND ur.question = :question",
                    UserResponse.class
            );
            em.getTransaction().begin();
            for(UserResponse ur : response) {
                query.setParameter("user", ur.getUser());
                query.setParameter("question", ur.getQuestion());

                if (query.getResultList().isEmpty()) {
                    em.persist(ur);
                }
            }
            em.getTransaction().commit();
        }
        catch (PersistenceException pe) {
            throw new ApiException(500, pe.getMessage());
        }
    }

    public List<UserResponse> getAllResponse(int userId) {

        try (EntityManager em = emf.createEntityManager()) {

            TypedQuery<UserResponse> query = em.createQuery("SELECT ur FROM UserResponse ur WHERE ur.user.id = :userId", UserResponse.class);

            query.setParameter("userId", userId);
            List<UserResponse> userResponses = query.getResultList();
            if (userResponses.isEmpty()) {
                throw new ApiException(404, "No questions has been answered by user: " + userId);
            }
            return userResponses;
        }
        catch (PersistenceException pe) {
            throw new PersistenceException();
        }
    }
}
