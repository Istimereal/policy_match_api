package app;

import app.config.ApplicationConfig;
import app.config.HibernateConfig;
import app.service.Populator;

import jakarta.persistence.EntityManagerFactory;


public class Main {

    public static void main(String[] args) {

        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory("policymatch");

        Populator pop = new Populator(emf);

        pop.createUsersAndRoles();

        pop.createQuestions();
        pop.createAnswers();

        ApplicationConfig.startServer(7075, emf);
    }
}
