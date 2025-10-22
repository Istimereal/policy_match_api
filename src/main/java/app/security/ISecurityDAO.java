package app.security;

import  app.security.Role;
import app.exceptions.ValidationException;

public interface ISecurityDAO {
    User getVerifiedUser(String username, String password) throws ValidationException; // used for login
    User createUser(String username, String password); // used for register
    Role createRole(String roleName);
    User addUserRole(String username, String role);
}
