package com.mju.Jumoney.domain.master.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "tags",
        indexes = {
                @Index(name = "idx_tag_name", columnList = "tag_name", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long id;

    @Column(name = "tag_name", nullable = false, unique = true, length = 50)
    private String tagName;

    public static Tag create(String tagName) {
        return Tag.builder()
                .tagName(tagName)
                .build();
    }
}
