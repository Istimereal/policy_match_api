package app.security;

import app.dtos.AppUserDTO;
import app.service.ConverterUser;
import app.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.JOSEException;
import dk.bugelhartmann.TokenSecurity;
import dk.bugelhartmann.TokenVerificationException;
import dk.bugelhartmann.UserDTO;
import app.exceptions.ValidationException;
import app.exceptions.ApiException;
import app.exceptions.NotAuthorizedException;
import io.javalin.http.*;

//import dk.bugelhartmann.UserDTO;
import jakarta.persistence.EntityExistsException;

import java.text.ParseException;
import java.util.Map;
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

            try {
                User user = ctx.bodyAsClass(User.class);
                User verifiedUser = securityDAO.getVerifiedUser(user.getUsername(), user.getPassword());
                UserDTO verifiedUserDTO = ConverterUser.convertUserToUserDTO(verifiedUser);
                String token = createToken(verifiedUserDTO);
             /*   ObjectNode on = objectMapper
                        .createObjectNode()
                        .put("msg","Succesfull login for user: "+verified.getUsername());  */
                ctx.status(HttpStatus.OK).json(Map.of("username", verifiedUserDTO.getUsername(), "token", token));

            } catch(ValidationException ex){
                //     ObjectNode on = objectMapper.createObjectNode().put("msg","login failed. Wrong username or password");
                ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("status", HttpStatus.UNAUTHORIZED.getCode(), "msg", "login failed. Wrong username or password"));
                //     ctx.json(on).status(401);
            }
        };
    }
    /*
    public Handler login(){
        return (Context ctx) -> {

            try {
                User user = ctx.bodyAsClass(User.class);
                User verifiedUser = securityDAO.getVerifiedUser(user.getId(), user.getPassword());
                UserDTO verifiedUserDTO = ConverterUser.convertUserToUserDTO(verifiedUser);
                String token = createToken(verifiedUserDTO);

                ctx.status(HttpStatus.OK).json(Map.of("status", HttpStatus.OK.getCode(), "msg", "Succesfull login for user: "+verifiedUser.getUsername()));
                ctx.attribute("userId", verifiedUser.getId());

            } catch(ValidationException ex){
                ObjectNode on = objectMapper.createObjectNode().put("msg","login failed. Wrong username or password");
               ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("status", HttpStatus.UNAUTHORIZED.getCode(), "msg", "login failed. Wrong username or password"));
                ctx.json(on).status(401);
            }
        };
    }

*/
    public Handler register() {
        return (ctx) -> {
            ObjectNode returnObject = objectMapper.createObjectNode();
            try {
                AppUserDTO userInput = ctx.bodyAsClass(AppUserDTO.class);

                User newUser = ConverterUser.convertUserDTO(userInput);

                User created = securityDAO.createUser(newUser);

                String token = createToken(new UserDTO(created.getUsername(), Set.of("USER")));
                ctx.status(HttpStatus.CREATED).json(returnObject
                        .put("token", token)
                        .put("username", created.getUsername()));
            } catch (EntityExistsException e) {
                ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
                ctx.json(returnObject.put("msg", "User already exists"));
            }
        };
    }

    public void authenticate(Context ctx) {
        // This is a preflight request => no need for authentication
        try {
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
        catch (Exception e) {
            ctx.status(500);
        }
    }

    private UserDTO validateAndGetUserFromToken(Context ctx) throws Exception {
       try {
           String token = getToken(ctx);
           UserDTO verifiedTokenUser = verifyToken(token);
           if (verifiedTokenUser == null) {
               throw new UnauthorizedResponse("Invalid user or token"); // UnauthorizedResponse is javalin 6 specific but response is not json!
           }
           return verifiedTokenUser;
       }
       catch (Exception e) {
           e.printStackTrace();
           throw new Exception("Could Not validate", e);
       }
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


    public String createToken(dk.bugelhartmann.UserDTO user) throws Exception {
        try {
            String ISSUER;
            String TOKEN_EXPIRE_TIME;
            String SECRET_KEY;

            if (System.getenv("DEPLOYED") != null) {
                ISSUER = System.getenv("ISSUER");
                TOKEN_EXPIRE_TIME = System.getenv("TOKEN_EXPIRE_TIME");
                SECRET_KEY = System.getenv("SECRET_KEY");
            } else {
                ISSUER = "Thomas Hartmann";
                TOKEN_EXPIRE_TIME = "1800000";
                SECRET_KEY = Utils.getPropertyValue("SECRET_KEY", "config.properties");
            }

            return tokenSecurity.createToken(user, ISSUER, TOKEN_EXPIRE_TIME, SECRET_KEY);

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Could not create token", e);  // ✅ korrekt syntaks
        }
    }


  /*  @Override
    public String createToken(UserDTO user) throws Exception {
        String security = "";
        try {
            String ISSUER;
            String TOKEN_EXPIRE_TIME;
            String SECRET_KEY;

            if (System.getenv("DEPLOYED") != null) {
                ISSUER = System.getenv("ISSUER");
                TOKEN_EXPIRE_TIME = System.getenv("TOKEN_EXPIRE_TIME");
                SECRET_KEY = System.getenv("SECRET_KEY");
            } else {
                ISSUER = "Thomas Hartmann";
                TOKEN_EXPIRE_TIME = "1800000";
                SECRET_KEY = Utils.getPropertyValue("SECRET_KEY", "config.properties");
            }
             security = tokenSecurity.createToken(user, ISSUER, TOKEN_EXPIRE_TIME, SECRET_KEY);
       return security;

        } catch (Exception e) {

            e.printStackTrace();
        }
return security;
    }  */


    public UserDTO verifyToken(String token) throws Exception {
        boolean IS_DEPLOYED = (System.getenv("DEPLOYED") != null);
        String SECRET = IS_DEPLOYED
                ? System.getenv("SECRET_KEY")
                : Utils.getPropertyValue("SECRET_KEY", "config.properties");

        try {
            if (tokenSecurity.tokenIsValid(token, SECRET) && tokenSecurity.tokenNotExpired(token)) {
                return tokenSecurity.getUserWithRolesFromToken(token);
            } else {
                throw new NotAuthorizedException(403, "Token is not valid");
            }

        } catch (ParseException | NotAuthorizedException e) {
            e.printStackTrace();
            throw new Exception("Unauthorized. Could not verify token", e);

        } catch (TokenVerificationException tve) {
            throw new Exception("Unauthorized. Could not verify token", tve);
        }
    }


/*
    @Override
    public UserDTO verifyToken(String token) throws Exception {
        boolean IS_DEPLOYED = (System.getenv("DEPLOYED") != null);
        String SECRET = IS_DEPLOYED ? System.getenv("SECRET_KEY") : Utils.getPropertyValue("SECRET_KEY", "config.properties");
UserDTO tokenDTO =null;
        try {
            if (tokenSecurity.tokenIsValid(token, SECRET) && tokenSecurity.tokenNotExpired(token)) {
                tokenDTO = tokenSecurity.getUserWithRolesFromToken(token);
                return tokenDTO;
            } else {
                throw new NotAuthorizedException(403, "Token is not valid");
            }
        } catch (ParseException  | NotAuthorizedException e) {
            e.printStackTrace();
            throw new ApiException(HttpStatus.UNAUTHORIZED.getCode(), "Unauthorized. Could not verify token");
        }
        catch (TokenVerificationException tve){
            throw new ApiException(HttpStatus.UNAUTHORIZED.getCode(), "Unauthorized. Could not verify token");
        }
        catch (Exception e) {
           throw Exception();
        }
        return tokenDTO;
            }  */
}



