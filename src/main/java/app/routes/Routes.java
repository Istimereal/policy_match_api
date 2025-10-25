package app.routes;

import app.routes.QuestionRoutes;
import app.routes.ResponseRoutes;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

public class Routes {

private final QuestionRoutes questionRoutes;
private final ResponseRoutes responseRoutes;

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

 //   private final PoemRoutes poemRoutes;

 /*   public Routes(PoemRoutes poemRoutes) {
        this.poemRoutes = poemRoutes;
    } */

  /*  public EndpointGroup getRoutes(){

        return () -> {
            get("/", ctx -> ctx.result("Hello World"));
            path("/poem", poemRoutes.getRoutes());
        };
    }  */
}
