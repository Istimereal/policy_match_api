package app.security;

import app.exceptions.EntityNotFoundException;
import app.exceptions.ValidationException;
import app.config.HibernateConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import dk.bugelhartmann.UserDTO;

import app.security.Role;

import java.util.stream.Collectors;

public class SecurityDAO implements ISecurityDAO{
    EntityManagerFactory emf;

    public SecurityDAO(EntityManagerFactory emf){
        this.emf = emf;
    }

    @Override
    public User getVerifiedUser(String username, String password) throws ValidationException {
        try(EntityManager em = emf.createEntityManager()){
            User foundUser = em.find(User.class, username);

            if(foundUser != null && foundUser.verifyPassword(password)) //foundUser.verifyPassword(password) == true
            {
                return foundUser;
            }
        }
        return null;
    }

    @Override
    public User createUser(String username, String password) {
        try(EntityManager em = emf.createEntityManager()){
            User user = new User(username, password);
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
            return user;
        }
    }

    @Override
    public Role createRole(String roleName) {

        Role role = new Role(roleName);
        try (EntityManager em = emf.createEntityManager()) {

            em.getTransaction().begin();
            em.persist(role);
            em.getTransaction().commit();
            return role;
        }
    }
    @Override
    public User addUserRole(String username, String rolename) {

        try (EntityManager em = emf.createEntityManager()) {
            User foundUser = em.find(User.class, username);

            Role foundRole = em.find(Role.class, rolename);
            if (foundUser == null || foundRole == null) {
                throw new IllegalArgumentException("User and role not found");
            }
            foundUser.addRole(foundRole);
            em.getTransaction().begin();
            em.merge(foundUser);
            em.getTransaction().commit();
            return foundUser;
        }
    }

}
