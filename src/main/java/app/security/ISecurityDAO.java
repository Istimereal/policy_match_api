package app.security;

import  app.security.Role;
import app.exceptions.ValidationException;

public interface ISecurityDAO {
    User getVerifiedUser(String username, String password) throws ValidationException; // used for login
    User createUser(User user); // used for register
    Role createRole(String roleName);
    User addUserRole(int id, String role);
}
