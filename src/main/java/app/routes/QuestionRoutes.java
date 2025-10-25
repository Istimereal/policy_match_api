package app.routes;

import app.controllers.QuestionController;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.post;

public class QuestionRoutes {

    private final QuestionController questionController;

    public QuestionRoutes(QuestionController questionController) {
        this.questionController = questionController;
    }

    public EndpointGroup getRoutes(){

        return () -> {
            post("/", ctx -> questionController.createQuestions(ctx));

        };
    }


}
