package app.dtos;

public class QuestionAndResponseDTO {

    private String subject;

    private String questionTextPromt;

    private String responsePrompt;

    private String importancePrompt;

    public QuestionAndResponseDTO(QuestionDTO questionDTO, UserResponseDTO userResponseDTO) {

        this.subject = questionDTO.getSubject();
        this.questionTextPromt = questionDTO.getQuestionText();
        this.responsePrompt = userResponseDTO.getResponse().toString();
        this.importancePrompt = userResponseDTO.getImportance().toString();
    }
}
