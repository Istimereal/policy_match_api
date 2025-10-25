package app.dtos;

import app.entities.UserResponse;
import app.enums.Importance;
import app.enums.Response;
import app.security.User;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
@ToString
public class UserResponseDTO {

    private int userId;
    private int questionId;
    private Response response;
    private Importance importance;

    public UserResponseDTO(UserResponse userResponse) {

        this.userId = userResponse.getId().getUserId();
        this.questionId = userResponse.getId().getQuestionId();
        this.response = userResponse.getResponse();
        this.importance = userResponse.getImportance();
    }
}
