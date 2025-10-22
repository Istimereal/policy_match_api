package app.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@NoArgsConstructor
@Table(name = "question")
public class Question {

    @Id
    @Column(name = "questionId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "header", nullable = false)
    private String header;

    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Column(name = "subject", nullable = false)
    private String subject;

    @OneToMany(mappedBy = "question")
    private Set<UserResponse> userResponses; // = new HashSet<>();


}
