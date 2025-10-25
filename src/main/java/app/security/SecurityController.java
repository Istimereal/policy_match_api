package app.security;

import app.exceptions.EntityNotFoundException;
import app.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.bugelhartmann.TokenSecurity;
import dk.bugelhartmann.TokenVerificationException;
import dk.bugelhartmann.UserDTO;
import app.exceptions.ValidationException;
import app.exceptions.ApiException;
import app.exceptions.NotAuthorizedException;
import app.config.HibernateConfig;
import app.utils.Utils;
import io.javalin.http.*;

import dk.bugelhartmann.UserDTO;
import io.javalin.http.*;

import java.security.PrivateKey;
import java.text.ParseException;
import java.util.Set;
import java.util.stream.Collectors;

public class SecurityController {

    private final SecurityDAO securityDAO;
    ObjectMapper objectMapper = new Utils().getObjectMapper();
    TokenSecurity tokenSecurity = new TokenSecurity();

    public SecurityController(SecurityDAO securityDAO) {
        this.securityDAO = securityDAO;
    }

    public Handler login(){
        return (Context ctx) -> {
            User user = ctx.bodyAsClass(User.class);
            try {
                User verified = securityDAO.getVerifiedUser(user.getId(), user.getPassword());
                ObjectNode on = objectMapper
                        .createObjectNode()
                        .put("msg","Succesfull login for user: "+verified.getUsername());
                ctx.json(on).status(200);
                ctx.attribute("userId", verified.getId());

            } catch(ValidationException ex){
                ObjectNode on = objectMapper.createObjectNode().put("msg","login failed. Wrong username or password");
                ctx.json(on).status(401);
            }
        };
    }

    public void authenticate(Context ctx) {
        // This is a preflight request => no need for authentication
        if (ctx.method().toString().equals("OPTIONS")) {
            ctx.status(200);
            return;
        }
        // If the endpoint is not protected with roles or is open to ANYONE role, then skip
        Set<String> allowedRoles = ctx.routeRoles().stream().map(role -> role.toString().toUpperCase()).collect(Collectors.toSet());
        if (isOpenEndpoint(allowedRoles))
            return;

        // If there is no token we do not allow entry
        UserDTO verifiedTokenUser = validateAndGetUserFromToken(ctx);
        ctx.attribute("user", verifiedTokenUser); // -> ctx.attribute("user") in ApplicationConfig beforeMatched filter
    }

    private UserDTO validateAndGetUserFromToken(Context ctx) {
        String token = getToken(ctx);
        UserDTO verifiedTokenUser = verifyToken(token);
        if (verifiedTokenUser == null) {
            throw new UnauthorizedResponse("Invalid user or token"); // UnauthorizedResponse is javalin 6 specific but response is not json!
        }
        return verifiedTokenUser;
    }

    private static String getToken(Context ctx) {
        String header = ctx.header("Authorization");
        if (header == null) {
            throw new UnauthorizedResponse("Authorization header is missing"); // UnauthorizedResponse is javalin 6 specific but response is not json!
        }

        // If the Authorization Header was malformed, then no entry
        String token = header.split(" ")[1];
        if (token == null) {
            throw new UnauthorizedResponse("Authorization header is malformed"); // UnauthorizedResponse is javalin 6 specific but response is not json!
        }
        return token;
    }

    private UserDTO verifyToken(String token) {
        boolean IS_DEPLOYED = (System.getenv("DEPLOYED") != null);
        String SECRET = IS_DEPLOYED ? System.getenv("SECRET_KEY") : Utils.getPropertyValue("SECRET_KEY", "config.properties");

        try {
            if (tokenSecurity.tokenIsValid(token, SECRET) && tokenSecurity.tokenNotExpired(token)) {
                return tokenSecurity.getUserWithRolesFromToken(token);
            } else {
                throw new NotAuthorizedException(403, "Token is not valid");
            }
        } catch (ParseException | NotAuthorizedException | TokenVerificationException e) {
            // logger.error("Could not create token", e);
            throw new ApiException(HttpStatus.UNAUTHORIZED.getCode(), "Unauthorized. Could not verify token");
        }
    }

    private boolean isOpenEndpoint(Set<String> allowedRoles) {
        // If the endpoint is not protected with any roles:
        if (allowedRoles.isEmpty())
            return true;

        // 1. Get permitted roles and Check if the endpoint is open to all with the ANYONE role
        if (allowedRoles.contains("ANYONE")) {
            return true;
        }
        return false;
    }

    public void authorize(Context ctx) {
        Set<String> allowedRoles = ctx.routeRoles()
                .stream()
                .map(role -> role.toString().toUpperCase())
                .collect(Collectors.toSet());

        // 1. Check if the endpoint is open to all (either by not having any roles or having the ANYONE role set
        if (isOpenEndpoint(allowedRoles))
            return;
        // 2. Get user and ensure it is not null
        UserDTO user = ctx.attribute("user");
        if (user == null) {
            throw new ForbiddenResponse("No user was added from the token");
        }
        // 3. See if any role matches
        if (!userHasAllowedRole(user, allowedRoles))
            throw new ForbiddenResponse("User was not authorized with roles: " + user.getRoles() + ". Needed roles are: " + allowedRoles);
    }

    private static boolean userHasAllowedRole(UserDTO user, Set<String> allowedRoles) {
        return user.getRoles().stream()
                .anyMatch(role -> allowedRoles.contains(role.toUpperCase()));
    }




}
