package app.service;

import app.config.HibernateConfig;
import app.dtos.UserResponseDTO;
import app.entities.Question;
import app.entities.UserResponse;
import app.entities.UserResponseId;

import app.security.Role;
import app.security.User;
import com.openai.models.responses.Response;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDate;

import static app.enums.Importance.HIGH;
import static app.enums.Response.AGREE;


/**
 * Purpose: To populate the database with users and roles
 * Author: Thomas Hartmann
 */
public class Populator {
    // method to create users and roles before each test
//    public void createUsersAndRoles(EntityManagerFactory emf) {
//        try (EntityManager em = emf.createEntityManager()) {
//            em.getTransaction().begin();
//            em.createQuery("DELETE FROM User u").executeUpdate();
//            em.createQuery("DELETE FROM Role r").executeUpdate();
//            User user = new User("user", "user123");
//            User admin = new User("admin", "admin123");
//            User superUser = new User("super", "super123");
//            Role userRole = new Role("user");
//            Role adminRole = new Role("admin");
//            user.addRole(userRole);
//            admin.addRole(adminRole);
//            superUser.addRole(userRole);
//            superUser.addRole(adminRole);
//            em.persist(user);
//            em.persist(admin);
//            em.persist(superUser);
//            em.persist(userRole);
//            em.persist(adminRole);
//            em.getTransaction().commit();
//        }
//    }
//

    private final EntityManagerFactory emf;
    public Populator(EntityManagerFactory emf){ this.emf = emf; }

    public void createUsersAndRoles() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Ryd relationstabel først (ellers FK-fejl)
            em.createNativeQuery("DELETE FROM role_user").executeUpdate();
            // Slet Users før Roles (ellers FK-fejl)
            em.createQuery("DELETE FROM Role").executeUpdate();
            em.createQuery("DELETE FROM User").executeUpdate();

            Role adminRole = new Role("admin");
            Role userRole = new Role("user");
            em.persist(adminRole);
            em.persist(userRole);
User admin = new User("admin","admin123","myMail@ad.dk","Lyngby");

            User normalUser = new User("user1","pass123","myMail@user.dk","Lyngby");

            adminRole.addUser(admin);
            userRole.addUser(normalUser);

            em.persist(admin);
            em.persist(normalUser);

            em.getTransaction().commit();

            System.out.println("Users and roles created successfully!");
        }
    }

    public void createQuestions() {
        try (EntityManager em = emf.createEntityManager()) {

            em.getTransaction().begin();
            em.createNativeQuery("DELETE FROM user_response").executeUpdate();
            em.createNativeQuery("DELETE FROM question").executeUpdate();

            Question q1 = Question.builder()
                    .header("Climate change responsibility")
                    .questionText("Do you think Denmark should take stronger action against climate change, even if it costs jobs in certain industries?")
                    .subject( "environment")
                    .build();

            Question q2 = Question.builder()
                    .header("AI in education")
                    .questionText("Should schools allow students to use AI tools like ChatGPT during exams?")
                    .subject("technology")
                    .build();

            em.persist(q1);
            em.persist(q2);
            em.getTransaction().commit();

            System.out.println("Questions created successfully!");
        }
    }

    public void createAnswers(){
     /* // ikke
        UserResponseDTO r1 = UserResponseDTO.builder()
                .userId(2)
                .questionId(1)
                .response(AGREE)
                .importance(HIGH)
                .build();

       UserResponse response = ConverterUserResponse.convertDTOToUserAndID(r1, 2);   */

        try(EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            em.createNativeQuery("DELETE FROM user_response").executeUpdate();

            User user = em.find(User.class, 2);
            Question question = em.find(Question.class, 1);
            UserResponseId id = UserResponseId.builder().userId(user.getId()).questionId(question.getId()).build();

            UserResponse response = UserResponse.builder()
                    .id(id)
                    .user(user)
                    .question(question)
                    .response(AGREE)
                    .importance(HIGH)
                    .build();

            System.out.println("user? " + (response.getUser() != null));
            System.out.println("question? " + (response.getQuestion() != null));
            System.out.println("id? " + (response.getId() != null ? response.getId() : "null"));

            em.persist(response);
            em.getTransaction().commit();

            System.out.println("UserResponse created successfully!");
        }
    }
}

