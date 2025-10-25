package app.entities;

import app.enums.Importance;
import app.enums.Response;
import app.security.User;
import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_response")
public class UserResponse {

    @EmbeddedId
    private UserResponseId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "response", nullable = false)
    private Response response;

    @Enumerated(EnumType.STRING)
    @Column(name = "importance", nullable = false)
    private Importance importance;

    @ManyToOne
    @MapsId("user_Id")
    @JoinColumn(name = "user_Id")
    private User user;

    @ManyToOne
    @MapsId("response_Id")
    @JoinColumn(name = "response_Id")
    private Question question;



}
