package app.service;

import app.dtos.AppUserDTO;
import app.security.User;
import dk.bugelhartmann.UserDTO;

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

    public static UserDTO convertUserToUserDTO(User user) {

        UserDTO userDTO = new UserDTO(user.getUsername(), user.getPassword());

        return userDTO;
    }
}
