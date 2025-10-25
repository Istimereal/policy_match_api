package app.exceptions;

import app.entities.Question;

import java.util.List;

public class ApiExceptionCreate extends RuntimeException{
    private int code;
    private List<Question> questions;
    public ApiExceptionCreate(int code, String msg, List<Question> questions){
        super(msg);
        this.code = code;
        this.questions = questions;
    }

    public int getStatusCode(){
        return code;
    }

    public List<Question> getQuestions(){return questions;}
}
