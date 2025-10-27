package app.routes;

import app.controllers.QuestionController;
import app.enums.Role;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class QuestionRoutes {

    private final QuestionController questionController;

    public QuestionRoutes(QuestionController questionController) {
        this.questionController = questionController;
    }

    public EndpointGroup getRoutes(){

        return () -> {
            post( ctx -> questionController.createQuestions(ctx), Role.ADMIN);
            patch(ctx -> questionController.updateQuestion(ctx), Role.ADMIN);
            get(ctx -> questionController.getAllQuestions(ctx), Role.ANYONE);
            delete(ctx -> questionController.deleteQuestion(ctx), Role.ADMIN);
        };
    }
}
