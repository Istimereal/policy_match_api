package app.entities;

import app.security.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@NoArgsConstructor
@Table(name = "user_response")
public class UserResponse {

    @EmbeddedId
    private UserResponseId id;

    @ManyToOne
    @MapsId("user_Id")
    @JoinColumn(name = "user_Id")
    private User user;

    @ManyToOne
    @MapsId("response_Id")
    @JoinColumn(name = "response_Id")
    private Question question;

    @Column (name = "response")
private String response;

    @Column (name = "importance")
String importance;

}
