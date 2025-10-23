package app.dtos;

import app.entities.Question;
import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
@ToString
public class QuestionDTO {

    private int  id;
    private String header;
    private String questionText;
    private String subject;

    public QuestionDTO(Question question) {
        this.id = question.getId();
        this.header = question.getHeader();
        this.questionText = question.getQuestionText();
        this.subject = question.getSubject();
    }

}
