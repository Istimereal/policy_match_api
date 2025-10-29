package app.service;

import app.dtos.AppUserDTO;
import app.security.User;
import dk.bugelhartmann.UserDTO;

import java.util.Set;
import java.util.stream.Collectors;

public class ConverterUser {

    public static User convertUserDTO(AppUserDTO appDTO) {
User user = new User(appDTO.getUsername(), appDTO.getPassword(),  appDTO.getEmail(), appDTO.getCity());

return user;
/*  return   User.builder()
                .username(appUserDTO.getUsername())
                .password(appUserDTO.getPassword())
                .email(appUserDTO.getEmail())
                .city(appUserDTO.getCity())
                .build();  */
    }


    public static dk.bugelhartmann.UserDTO convertUserToUserDTO(app.security.User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(app.security.Role::getRoleName)   // fx "user"
                .map(String::toUpperCase)              // "USER"
                .collect(Collectors.toSet());

        return new dk.bugelhartmann.UserDTO(user.getUsername(), roleNames);

    }

     /*public static UserDTO convertUserToUserDTO(User user) {
        UserDTO userDTO = new UserDTO(user.getUsername(), user.getPassword());

        return userDTO;
    }*/
}
