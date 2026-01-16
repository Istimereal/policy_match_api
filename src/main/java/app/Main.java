package app;

import app.config.ApplicationConfig;
import app.config.HibernateConfig;
import app.service.Populator;

import jakarta.persistence.EntityManagerFactory;


public class Main {

    public static void main(String[] args) {

        boolean deployed = "true".equalsIgnoreCase(System.getenv("DEPLOYED"));

        HibernateConfig.setIsTest(false);

        EntityManagerFactory emf = deployed
                ? HibernateConfig.getEntityManagerFactory(System.getenv("DB_NAME"))
                : HibernateConfig.getEntityManagerFactory("policymatch");

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7073"));

        Populator pop = new Populator(emf);
        pop.createUsersAndRoles();
        pop.createQuestions();
        pop.createAnswers();

        ApplicationConfig.startServer(port, emf);
    }
}
