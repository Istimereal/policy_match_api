package app.entities;

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
    private int userId;
    private int questionId;

 @Override
 public boolean equals (Object o){
     if(this == o) return true;
     if(o == null || getClass() != o.getClass()) return false;
     UserResponseId that = (UserResponseId) o;
     return Objects.equals(userId, that.userId) &&
     Objects.equals(questionId, that.questionId);
 }

      @Override
    public int hashCode(){
          return Objects.hash(userId, questionId);
      }
}
