package app.daos;

import app.config.HibernateConfig;
import app.entities.Question;
import app.service.TestQuestionDaoPopulator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;

import java.awt.dnd.DragSourceAdapter;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuestionDAOTest {

    private EntityManagerFactory emf;
    private QuestionDAO dao;
    private List<Question> questions;
    private Question q1, q2;


    @BeforeAll
     void beforeAll() {
        emf = HibernateConfig.getEntityManagerFactoryForTest();
        dao = QuestionDAO.getInstance(emf);
    }

    @BeforeEach
    void setUp() {
        try (EntityManager em = emf.createEntityManager()) {

            em.getTransaction().begin();
            em.createNativeQuery("TRUNCATE TABLE question RESTART IDENTITY CASCADE")
                    .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            throw new RuntimeException("Failed to TRUNCATE table", e);
        }

        questions = TestQuestionDaoPopulator.addQuestions(emf);
        if (questions.size() == 2) {
            q1 = questions.get(0);
            q2 = questions.get(1);
        }
        else {
            throw new RuntimeException("Expected 2 questions from populator, got: " + questions.size());
        }
    }

    @AfterEach
    void tearDown() {
            if(emf != null || emf.isOpen()){
                emf.close();
            }
        }

  /*  @Test
    void getInstance() {
    }
*/
    @Test
    void createQuestion() {

        //Arrange
        Question newQuestion = Question.builder()
                .header("Working from home")
                .questionText("Should companies be required to offer employees the option to work remotely?")
                .subject("labor")
                .build();
List<Question>  arrangeQ = new ArrayList<>();
arrangeQ.add(newQuestion);
        //Act
        List<Question> created = dao.createQuestion(arrangeQ);

        //Assert
        assertNotNull(created);
        assertEquals(1, created.size());
        assertEquals("Working from home", created.get(0).getHeader());
    }

    @Test
    void getAllQuestions() {

        // Arrange DB is initialized in addQuestions method, with two questions

        //Act
        List<Question> allQuestions = dao.getAllQuestions();

        System.out.println("Length: " +  allQuestions.size());

        // Assert
        assertNotNull(allQuestions);
        assertThat(allQuestions, hasSize(2));
    }

    @Test
    void findQuestionById() {
    }

    @Test
    void updateQuestion() {
    }

    @Test
    void deleteQuestion() {
    }
}