package app.config;

import app.controllers.QuestionController;
import app.controllers.ResponseController;
import app.daos.QuestionDAO;
import app.daos.UserResponseDAO;
import app.exceptions.ApiException;
import app.routes.QuestionRoutes;
import app.routes.ResponseRoutes;
import app.routes.Routes;
import app.routes.SecurityRoutes;
import app.security.SecurityController;
import app.security.SecurityDAO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ApplicationConfig {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    public static Javalin startServer(int port, EntityManagerFactory emf) {

        // Init security + routes

        QuestionDAO questionDAO = QuestionDAO.getInstance(emf);
        UserResponseDAO userResponseDAO = UserResponseDAO.getInstance(emf);

        QuestionController questionController = new QuestionController(questionDAO);
        ResponseController responseController = new ResponseController(userResponseDAO, emf);

        SecurityDAO securityDAO = new SecurityDAO(emf);
        SecurityController securityController = new SecurityController(securityDAO);
        SecurityRoutes securityRoutes = new SecurityRoutes(securityController);

        QuestionRoutes questionRoutes = new QuestionRoutes(questionController);
        ResponseRoutes responseRoutes = new ResponseRoutes(responseController);

        Routes routes = new Routes(questionRoutes, responseRoutes);

        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.bundledPlugins.enableRouteOverview("/routes");
            config.router.contextPath = "/api/v1";

            // 👇 BEGGE linjer her skal være der
            config.router.apiBuilder(routes.getEndpoints());
            config.router.apiBuilder(securityRoutes.getSecurityRoutes());
        });
        //     Routes routes = new Routes(questionRoutes, responseRoutes, LoginRoute loginRoute, SecurityRoutes securityRoutes);
     /*   Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.bundledPlugins.enableRouteOverview("/routes"); // nice debug
            config.router.contextPath = "/api/v1";

            config.router.apiBuilder(routes.getEndpoints());
            config.router.apiBuilder(securityRoutes.getSecurityRoutes());


        });  */

                   /* config.router.apiBuilder(() -> {
                        routes.getEndpoints();
                        securityRoutes.getSecurityRoutes();
                    });
                });

         /*   // 👇 ÉN samlet apiBuilder med ALLE routes
            config.router.apiBuilder(() -> {


                    }

           /*     // ---------- AUTH ----------
                path("/auth", () -> {
                    post("/login",    securityController.login(),    Role.ANYONE);
                    post("/register", securityController.register(), Role.ANYONE);
                });

                // ---------- QUESTIONS ----------
                path("/questions", () -> {
                    // alle kan se spørgsmål
                    get("/", ctx -> questionController.getAllQuestions(ctx), Role.ANYONE);

                    // admin kan oprette nyt spørgsmål (POST /api/v1/questions)
                    post("/", ctx -> questionController.createQuestions(ctx), Role.ADMIN);

                    // admin kan opdatere et spørgsmål (PATCH /api/v1/questions/{id})
                    patch("/{id}", ctx -> questionController.updateQuestion(ctx), Role.ADMIN);

                    // admin kan slette et spørgsmål (DELETE /api/v1/questions/{id})
                    delete("/{id}", ctx -> questionController.deleteQuestion(ctx), Role.ADMIN);
                });

                // ---------- RESPONSES ----------
                path("/responses", () -> {
                    // bruger gemmer sine svar
                    post("/", ctx -> responseController.userResponse(ctx), Role.USER);

                    // bruger får sin policy match ud fra sine svar
                    get("/", ctx -> responseController.getPolicyMatch(ctx), Role.USER);
                });

                // lille ping endpoint
                get("/", ctx -> ctx.json(Map.of("status", "API is running ✅")), Role.ANYONE);
            });
        });  */


  /*      // Opret app og registrér routes herinde
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.bundledPlugins.enableRouteOverview("/routes");
            config.router.contextPath = "/api/v1";
            config.router.apiBuilder(() -> {
                routes.getEndpoints();
                // Registrér EndpointGroups direkte i routeren
                securityRoutes.getSecurityRoutes();

        });  */


        // Åbne endpoints (ingen auth)
        app.get("/", ctx -> ctx.json(Map.of("status", "API is running ✅")));

        // Security filters (kører før matched routes)
        app.beforeMatched(securityController::authenticate);
        app.beforeMatched(securityController::authorize);

        // CORS + exception handling
        setCORS(app);
        setGeneralExceptionHandling(app);

        // Dev logging
        if (System.getenv("DEPLOYED") == null) {
            beforeFilter(app);
        }

        app.start(port);
        return app;
    }

    private static void setCORS(Javalin app) {
        app.before(ApplicationConfig::setCorsHeaders);
        app.options("/*", ApplicationConfig::setCorsHeaders);
    }

    private static void setCorsHeaders(io.javalin.http.Context ctx) {
        ctx.header("Access-Control-Allow-Origin", "*");
        ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        ctx.header("Access-Control-Allow-Credentials", "true");
    }
    //ændringer
    private static void setGeneralExceptionHandling(Javalin app) {
        app.exception(Exception.class, (e, ctx) -> {
            int statusCode = (e instanceof ApiException apiEx) ? apiEx.getStatusCode() : 500;
            String message = (e instanceof ApiException) ? e.getMessage() : "Internal server error";

            logger.error("An exception occurred", e);

            ObjectNode on = jsonMapper.createObjectNode()
                    .put("status", statusCode)
                    .put("msg", message);

            ctx.json(on);
            ctx.status(statusCode);
        });
    }
    private static void beforeFilter(Javalin app) {
        app.before(ctx -> {
            // Debug-request headers, valgfrit
            ctx.req().getHeaderNames().asIterator().forEachRemaining(System.out::println);
        });
    }
}
