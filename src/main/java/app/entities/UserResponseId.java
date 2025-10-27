package app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseId implements Serializable {

    @Column(name = "user_id")
    private int userId;
    @Column(name = "question_id")
    private int questionId;

 @Override
 public boolean equals (Object o){
     if(this == o) return true;
     if (!(o instanceof UserResponseId that)) return false;
     return userId == that.userId &&
             questionId == that.questionId;
    /* if(o == null || getClass() != o.getClass()) return false;
     UserResponseId that = (UserResponseId) o;
     return Objects.equals(userId, that.userId) &&
     Objects.equals(questionId, that.questionId); */
 }

      @Override
    public int hashCode(){
          return Objects.hash(userId, questionId);
      }
}
