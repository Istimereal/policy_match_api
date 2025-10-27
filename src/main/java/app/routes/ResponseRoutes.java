package app.routes;

import app.controllers.ResponseController;
import app.enums.Role;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class ResponseRoutes {

    ResponseController responseController;

    public ResponseRoutes(ResponseController responseController) {
        this.responseController = responseController;
    }

    public EndpointGroup getRoutes() {

            return () -> {
                post(ctx -> responseController.userResponse(ctx), Role.USER,  Role.ADMIN);
                get(ctx -> responseController.getPolicyMatch(ctx), Role.USER,  Role.ADMIN);
            };

     /*   return () -> {
path("/response", () -> {
            post(ResponseController::userResponse);
            get(ResponseController::getPolicyMatch);
        });
    };  */
    }
}
