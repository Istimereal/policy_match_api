package app.service;

import app.config.HibernateConfig;
import app.security.Role;
import app.security.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDate;

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

            User admin = new User("admin", "admin123");
            User normalUser = new User("user1", "pass123");

            adminRole.addUser(admin);
            userRole.addUser(normalUser);

            em.persist(admin);
            em.persist(normalUser);

            em.getTransaction().commit();

            System.out.println("Users and roles created successfully!");
        }
    }
}

