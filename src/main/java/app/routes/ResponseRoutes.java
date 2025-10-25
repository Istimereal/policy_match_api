package app.routes;

import app.controllers.ResponseController;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class ResponseRoutes {

    ResponseController responseController;

    public ResponseRoutes(ResponseController responseController) {
        this.responseController = responseController;
    }

    public EndpointGroup getRoutes() {

            return () -> {

                post(ctx -> responseController.userResponse(ctx));
                get(ctx -> responseController.getPolicyMatch(ctx));
            };

     /*   return () -> {
path("/response", () -> {
            post(ResponseController::userResponse);
            get(ResponseController::getPolicyMatch);
        });
    };  */
    }
}
