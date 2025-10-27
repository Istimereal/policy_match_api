package app.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUserDTO {
    // bruges kun i response
    private String username;
    private String password; // bruges kun ved login/register
    private String email;
    private String city;
}
