package app.security;

import app.entities.UserResponse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mindrot.jbcrypt.BCrypt;

import app.security.Role;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@Entity
@NoArgsConstructor
@Table(name="users")
public class User implements ISecurityUser{
    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "city", nullable = false)
    private String city;

    @ManyToMany(mappedBy = "users")
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "users")
    private Set<UserResponse> userResponsesSet; // = new HashSet<>();

    public User(String username, String password){
        this.username = username;
        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
        this.password = hashed;
    }

    @Override
    public boolean verifyPassword(String pw) {
        return BCrypt.checkpw(pw, password);
    }

    @Override
    public void addRole(Role role) {
        roles.add(role);
        // her kaldes IKKE role.getUsers().add(this); (kun den ene side opdateres)
    }

    @Override
    public void removeRole(Role role) {
        this.roles.remove(role);

    }
}
