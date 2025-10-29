package app.security;

import app.dtos.AppUserDTO;
import app.exceptions.NotAuthorizedException;
import app.exceptions.ValidationException;
import app.service.ConverterUser;
import app.utils.Utils;
import ch.qos.logback.core.subst.Token;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.bugelhartmann.TokenSecurity;
import dk.bugelhartmann.TokenVerificationException;
import dk.bugelhartmann.UserDTO;
import io.javalin.http.*;
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
                System.out.println("login 1");
                User user = ctx.bodyAsClass(User.class);
                System.out.println("login 1.1");
                User verifiedUser = securityDAO.getVerifiedUser(user.getUsername(), user.getPassword());
                System.out.println("login 1.2");
                UserDTO verifiedUserDTO = ConverterUser.convertUserToUserDTO(verifiedUser);

                System.out.println("login 1.3 verifiedUserDTO username: " + verifiedUserDTO.getUsername());
                System.out.println("login 1.3 verifiedUserDTO passsword: " + verifiedUserDTO.getPassword());
                System.out.println("login 1.3 verifiedUserDTO roles: " + verifiedUserDTO.getRoles());


                System.out.println("login 1.3");
                String token = createToken(verifiedUserDTO);
                System.out.println("login 1.4");
             /*   ObjectNode on = objectMapper
                        .createObjectNode()
                        .put("msg","Succesfull login for user: "+verified.getUsername());  */
                System.out.println("login 1.5");
                ctx.status(HttpStatus.OK).json(Map.of("username", verifiedUserDTO.getUsername(), "token", token));
                System.out.println("login 1.6");
            } catch(ValidationException ex){
                //     ObjectNode on = objectMapper.createObjectNode().put("msg","login failed. Wrong username or password");
                System.out.println("login validation exception 1");
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
                String password = userInput.getPassword();
                String username = userInput.getUsername();

                User newUser = ConverterUser.convertUserDTO(userInput);

                User created = securityDAO.createUser(newUser);

                securityDAO.addUserRole(created.getId(), "user");

                User verifiedDTO = securityDAO.getVerifiedUser(username, password);

                System.out.println("Created Username: verified " +verifiedDTO.getUsername());
                System.out.println("Created Password: verified " +verifiedDTO.getPassword());
                System.out.println("Created Roles: verified " +verifiedDTO.getRoles());

             UserDTO createdDTO = ConverterUser.convertUserToUserDTO(verifiedDTO);

                System.out.println("2created UserDTO" + createdDTO);
                String token = createToken(createdDTO);
             //   String token = createToken(new UserDTO(username, Set.of("USER")));

                System.out.println("Token: " + token);
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

            System.out.println("Creating Token 1");

            if (System.getenv("DEPLOYED") != null) {
                ISSUER = System.getenv("ISSUER");
                TOKEN_EXPIRE_TIME = System.getenv("TOKEN_EXPIRE_TIME");
                SECRET_KEY = System.getenv("SECRET_KEY");

                System.out.println("Creating Token 1: deployed");
            } else {

                System.out.println("createToken developer: 1 a.");
                ISSUER = "Thomas Hartmann";
                TOKEN_EXPIRE_TIME = "1800000";
                SECRET_KEY = Utils.getPropertyValue("SECRET_KEY", "config.properties");
                System.out.println("createToken developer: 1 B.");
            }
            System.out.println("Creating Token 2");
            String token = tokenSecurity.createToken(user, ISSUER, TOKEN_EXPIRE_TIME, SECRET_KEY);

            System.out.println("Created token: " + token);
            return token;

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Creating Token 1 c error");
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
        System.out.println("Login Verifying Token 1");
        try {
            if (tokenSecurity.tokenIsValid(token, SECRET) && tokenSecurity.tokenNotExpired(token)) {
                System.out.println("Login Verified Token 2");
                return tokenSecurity.getUserWithRolesFromToken(token);
            } else {
                System.out.println("login verifyToken : Not authorized ");
                throw new NotAuthorizedException(403, "Token is not valid");
            }

        } catch (ParseException | NotAuthorizedException e) {
            e.printStackTrace();
            System.out.println("login verifyToken : PerseException ");
            throw new Exception("Unauthorized. Could not verify token", e);

        } catch (TokenVerificationException tve) {
            System.out.println("login verifyToken : TokenVerificationException last ");
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



