package app;

import app.config.ApplicationConfig;
import app.config.HibernateConfig;
import app.service.Populator;

import jakarta.persistence.EntityManagerFactory;


public class Main {

    public static void main(String[] args) {
HibernateConfig.setIsTest(true);
      //  EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory("policymatch");
        EntityManagerFactory emfTest = HibernateConfig.getEntityManagerFactoryForTest();

        Populator pop = new Populator(emfTest);

        pop.createUsersAndRoles();

        pop.createQuestions();
        pop.createAnswers();
      //  pop.createRoles();

        ApplicationConfig.startServer(7073, emfTest);
    }
}
