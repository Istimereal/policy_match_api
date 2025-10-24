package app.config;

import app.routes.SecurityRoutes;
import app.security.SecurityController;
import app.security.SecurityDAO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ApplicationConfig {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private static SecurityRoutes securityRoutes;
    private static Javalin app;

    // SLET ELLER LAD DEN VÆRE TOM, så den ikke registrerer routes i forkert rækkefølge
    public static void configuration(JavalinConfig config) { }

    public static Javalin startServer(int port, EntityManagerFactory emf) {
        // Lav controller/route-objekter FØR vi kalder Javalin.create
        SecurityDAO securityDAO = new SecurityDAO(emf);
        SecurityController securityController = new SecurityController(securityDAO);
        securityRoutes = new SecurityRoutes(securityController);

        // Opret app + context path + registrér EndpointGroups her
        app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.bundledPlugins.enableRouteOverview("/routes");
            config.router.contextPath = "/api/v1";
            // Registrér alle EndpointGroups her (INTET i configuration())
            config.router.apiBuilder(securityRoutes.getSecurityRoutes());
            config.router.apiBuilder(SecurityRoutes.getSecuredRoutes());
        });

        // Åbne endpoints
        app.get("/", ctx -> ctx.json(Map.of("status", "API is running ✅")));
        app.get("/auth/healthcheck", ctx -> ctx.result("OK"));

        // Security filters (kører før matched routes)
        app.beforeMatched(securityController::authenticate);
        app.beforeMatched(securityController::authorize);

        setCORS(app);
        setGeneralExceptionHandling(app);

        if (System.getenv("DEPLOYED") == null) {
            beforeFilter(app);
        }

        app.start(port);
        return app;
    }

    private static void setCORS(Javalin app) {
        app.before(ctx -> setCorsHeaders(ctx));
        app.options("/*", ctx -> setCorsHeaders(ctx));
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
            int statusCode = (e instanceof app.exceptions.ApiException apiEx) ? apiEx.getStatusCode() : 500;
            String message = (e instanceof app.exceptions.ApiException) ? e.getMessage() : "Internal server error";
            logger.error("An exception occurred", e);
            var on = jsonMapper.createObjectNode().put("status", statusCode).put("msg", message);
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
