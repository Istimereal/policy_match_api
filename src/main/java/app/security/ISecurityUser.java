package app.security;

import app.security.Role;
import java.util.Set;

public interface ISecurityUser {
    //    Set<String> getRolesAsStrings();
    boolean verifyPassword(String pw);
    void addRole(Role role);
    void removeRole(Role role);
}
