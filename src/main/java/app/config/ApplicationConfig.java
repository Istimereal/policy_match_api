package app.config;

//import app.routes.RestRoutes;
//import app.service.Populator;
import app.routes.Routes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
//import app.controllers.Controller;
import app.exceptions.ApiException;

//import app.routes.Routes;
//import app.routes.Routes;
//import app.dao.;
import app.routes.SecurityRoutes;
import app.security.SecurityController;
import app.security.SecurityDAO;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import jakarta.persistence.EntityManagerFactory;
import io.javalin.http.Context;
import static io.javalin.apibuilder.ApiBuilder.path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


public class ApplicationConfig {
    private static Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);
    private static ObjectMapper jsonMapper = new ObjectMapper();
    private static ApplicationConfig appConfig;
    private static Routes routes;
    private static SecurityRoutes securityRoutes;
    private static Javalin app;

    public static void configuration(JavalinConfig config){
        config.showJavalinBanner = false;
        config.bundledPlugins.enableRouteOverview("/routes");
        config.router.contextPath = "/api/v1"; // Base path for all endpoints
      //  config.router.apiBuilder(routes.getRoutes());
        config.router.apiBuilder(securityRoutes.getSecurityRoutes());
        config.router.apiBuilder(securityRoutes.getSecuredRoutes());

    }
    public static Javalin startServer(int port, EntityManagerFactory emf){

        //  PoemDAO poemDAO = PoemDAO.getInstance(emf);
        //  PoemController poemController = new PoemController(poemDAO);
        // public static final . ChatGPT ville gerne have dette med
           SecurityDAO securityDAO = new SecurityDAO(emf);
          SecurityController securityController = new SecurityController(securityDAO);
// Er måske pladceret forkert
        //    Populator populator = new Populator(emf);
        //    PersonDAO personDAO = new PersonDAO(emf);
        //  PersonController personController = new PersonController();
        //  PersonEntityController personEntityController = new PersonEntityController(personDAO, populator);

        //   restRoutes = new RestRoutes(personController, personEntityController);

        //  PoemRoutes poemRoutes = new PoemRoutes(poemController);
          securityRoutes = new SecurityRoutes(securityController);
        //  routes = new Routes(poemRoutes);
        app = Javalin.create(ApplicationConfig::configuration);
        app.beforeMatched(ctx -> securityController.authenticate(ctx)); // check if there is a valid token in the header
        app.beforeMatched(ctx -> securityController.authorize(ctx)); // check if the user has the required role
// setCORS + setGeneralExceptionHandling skal måske være under app.start(port);
        setCORS(app);
        setGeneralExceptionHandling(app);
        //  beforeFilter(app);
        if (System.getenv("DEPLOYED") == null) {
            beforeFilter(app);
        }
        app.get("/", ctx -> ctx.json(Map.of("status", "API is running ✅")));
        app.get("/auth/healthcheck", ctx -> ctx.result("OK"));

        app.start(port);

        return app;
    }

    public static void stopServer(Javalin app){
        app.stop();
    }

    public static void setCORS(Javalin app) {
        app.before(ctx -> {
            setCorsHeaders(ctx);
        });
        app.options("/*", ctx -> { // Burde nok ikke være nødvendig?
            setCorsHeaders(ctx);
        });

    }

    private static void setCorsHeaders(Context ctx) {
        ctx.header("Access-Control-Allow-Origin", "*");
        ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        ctx.header("Access-Control-Allow-Credentials", "true");
    }

    /*    //     Adding below methods to ApplicationConfig, means that EVERY ROUTE will be checked for security roles. So open routes must have a role of ANYONE
        public ApplicationConfig checkSecurityRoles(Javalin app) {
            app.beforeMatched(ctx -> securityController.authenticate(ctx)); // check if there is a valid token in the header
            app.beforeMatched(ctx -> securityController.authorize(ctx)); // check if the user has the required role
            return appConfig;
        }  */
    public static void setGeneralExceptionHandling(Javalin app) {
        app.exception(Exception.class, (e, ctx) -> {
            int statusCode = (e instanceof ApiException apiEx)
                    ? apiEx.getStatusCode()
                    : 500;

            String message = (e instanceof ApiException)
                    ? e.getMessage()
                    : "Internal server error";

            logger.error("An exception occurred", e);

            ObjectNode on = jsonMapper
                    .createObjectNode()
                    .put("status", statusCode)
                    .put("msg", message);

            ctx.json(on);
            ctx.status(statusCode);
        });
    }

    public static void beforeFilter(Javalin app) {
        app.before(ctx -> {
            String pathInfo = ctx.req().getPathInfo();
            ctx.req().getHeaderNames().asIterator().forEachRemaining(el -> System.out.println(el));
        });
    }

}