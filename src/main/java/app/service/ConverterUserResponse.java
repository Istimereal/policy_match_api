package app.service;

import app.dtos.UserResponseDTO;
import app.entities.Question;
import app.entities.UserResponse;
import app.entities.UserResponseId;
import app.security.User;

import java.beans.PropertyEditorSupport;
import java.util.List;

public class ConverterUserResponse {

    public static UserResponse convertDTOToUserAndID(UserResponseDTO userResponseDTO, int userId, User user, Question question) {

       UserResponseId id = UserResponseId.builder()
               .userId(userId)
               .questionId(userResponseDTO.getQuestionId())
               .build();

        return new UserResponse().builder()
                .id(id)
                .user(user)
                .question(question)
                .response(userResponseDTO.getResponse())
                .importance(userResponseDTO.getImportance())
                .build();
    }

    public static UserResponseDTO convertUserResponseToDTONoId(UserResponse userResponse) {
 //.userId(userResponse.getId().getUserId())
return new UserResponseDTO().builder()
        .questionId(userResponse.getId().getQuestionId())
        .response(userResponse.getResponse())
        .importance(userResponse.getImportance())
        .build();
    }

public static List<UserResponseDTO> convertToUserResponseDTOList(List<UserResponse> userResponses) {

        return userResponses.stream()
                .map(ConverterUserResponse::convertUserResponseToDTONoId)
                .toList();
}

public static List<UserResponse> convertToUserResponseList(List<UserResponseDTO> userResponses, int userId, User user, Question question) {

        return userResponses.stream()
                .map(dto -> convertDTOToUserAndID(dto, userId, user, question))
                .toList();
}
}
