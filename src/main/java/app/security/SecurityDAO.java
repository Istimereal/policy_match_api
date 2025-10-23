package app.security;

import app.exceptions.ApiException;
import app.exceptions.EntityNotFoundException;
import app.exceptions.ValidationException;
import app.config.HibernateConfig;
import jakarta.persistence.*;
import dk.bugelhartmann.UserDTO;

import app.security.Role;

import java.util.List;
import java.util.stream.Collectors;

public class SecurityDAO implements ISecurityDAO{
    EntityManagerFactory emf;

    public SecurityDAO(EntityManagerFactory emf){
        this.emf = emf;
    }

    @Override
    public User getVerifiedUser(int id, String password) throws ValidationException {
        try(EntityManager em = emf.createEntityManager()){
            User foundUser = em.find(User.class, id);

            if(foundUser != null && foundUser.verifyPassword(password)) //foundUser.verifyPassword(password) == true
            {
                return foundUser;
            }
        }
        return null;
    }

    @Override
    public User createUser(User user) {
        try(EntityManager em = emf.createEntityManager()){

            TypedQuery<User> query = em.createQuery(
                    "SELECT u FROM User u WHERE u.username = :username", User.class
            );
            query.setParameter("username", user.getUsername());
            List<User> result = query.getResultList();
      if(!result.isEmpty()){
          throw new EntityExistsException("Username: " + user.getUsername() + " Is taken please select an other username");
      }
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
            return user;
        }
        catch(PersistenceException pe) {
            throw new ApiException(500, "Database error while creating user: " + pe.getMessage());
        }
    }

   /* @Override
    public User createUser(String username, String password) {
        try(EntityManager em = emf.createEntityManager()){
            User user = new User(username, password);
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
            return user;
        }
    }  */

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
    public User addUserRole(int id, String rolename) {

        try (EntityManager em = emf.createEntityManager()) {
            User foundUser = em.find(User.class, id);

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
