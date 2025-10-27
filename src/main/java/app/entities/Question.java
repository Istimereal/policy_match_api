package app.entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "question", uniqueConstraints = @UniqueConstraint(columnNames = {"header", "subject"}))
public class Question {

    @Id
    @Column(name = "question_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "header", nullable = false)
    private String header;

    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Column(name = "subject", nullable = false)
    private String subject;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserResponse> userResponses; // = new HashSet<>();

}
