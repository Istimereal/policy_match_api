package app.security;

import jakarta.persistence.*;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "roles")
public class Role {

    @Id
    @Column(name = "rolename", nullable = false)
    private String roleName;

    public Role(String roleName) {
        this.roleName = roleName;
    }
    //@JoinColumn(name = "username")
    @ManyToMany
    @JoinTable(name = "role_user",
            joinColumns = @JoinColumn(name = "rolename", referencedColumnName = "rolename"),
            inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "user_id"))
    private Set<User> users = new HashSet<>();

    public void addUser(User user) {
        users.add(user);
    }
}
