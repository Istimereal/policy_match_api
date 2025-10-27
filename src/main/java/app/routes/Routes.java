package app.routes;

import app.routes.QuestionRoutes;
import app.routes.ResponseRoutes;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

public class Routes {

private final QuestionRoutes questionRoutes;
private final ResponseRoutes responseRoutes;
SecurityRoutes securityRoutes;

public Routes(QuestionRoutes questionRoutes, ResponseRoutes responseRoutes) {
    this.questionRoutes = questionRoutes;
    this.responseRoutes = responseRoutes;
}
public EndpointGroup getEndpoints() {

    return () -> {
        path("/questions", questionRoutes.getRoutes());
        path("/responses", responseRoutes.getRoutes());
    };
}

  /*  public EndpointGroup getRoutes(){

        return () -> {
            get("/", ctx -> ctx.result("Hello World"));
            path("/poem", poemRoutes.getRoutes());
        };
    }  */
}
