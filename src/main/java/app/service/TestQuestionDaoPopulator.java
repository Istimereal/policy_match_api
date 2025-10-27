package app.service;

import app.entities.Question;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

public class TestQuestionDaoPopulator {

    public static List<Question> addQuestions(EntityManagerFactory emf) {

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();


            Question q1 = Question.builder()
                    .header("Climate change responsibility")
                    .questionText("Do you think Denmark should take stronger action against climate change, even if it costs jobs in certain industries?")
                    .subject("environment")
                    .build();

            Question q2 = Question.builder()
                    .header("AI in education")
                    .questionText("Should schools allow students to use AI tools like ChatGPT during exams?")
                    .subject("technology")
                    .build();

            em.persist(q1);
            em.persist(q2);

            em.getTransaction().commit();


            List<Question> questions = new ArrayList<>();
            questions .add(q1);
            questions .add(q2);
            return questions ;
        }
    }
}
