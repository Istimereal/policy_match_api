package app.service;

import app.dtos.QuestionDTO;
import app.entities.Question;

import java.util.List;

public class ConverterQuestion {

    public static QuestionDTO convertQuestionToDTO(Question question) {

      /*  if (question == null) {
            return null; // eller evt. kast en ApiException(400, "Question cannot be null")
        }  */
        QuestionDTO.QuestionDTOBuilder builder = QuestionDTO.builder()
                .header(question.getHeader())
                .questionText(question.getQuestionText())
                .subject(question.getSubject());
        if (question.getId() > 0) {
            builder.id(question.getId());
        }
        return builder.build();
    }

    public static Question convertDTOToQuestion(QuestionDTO questionDTO) {
        Question.QuestionBuilder builder = Question.builder()
                .header(questionDTO.getHeader())
                .questionText(questionDTO.getQuestionText())
                .subject(questionDTO.getSubject());
        if (questionDTO.getId() > 0) {
            builder.id(questionDTO.getId());
        }
        return builder.build();
    }

    public static List<QuestionDTO> convertToQuestionDTO(List<Question> questions) {

        return questions.stream()
                .map(ConverterQuestion::convertQuestionToDTO)
                .toList();
    }

    public static List<Question> convertDTOToQuestionList(List<QuestionDTO> questionDTOList) {
        return questionDTOList.stream()
                .map(ConverterQuestion::convertDTOToQuestion)
                .toList();
    }

}
