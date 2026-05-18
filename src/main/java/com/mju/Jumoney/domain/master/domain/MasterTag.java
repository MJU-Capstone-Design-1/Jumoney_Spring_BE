package com.mju.Jumoney.domain.master.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "master_tags",
        indexes = {
                @Index(name = "idx_master_tag_master_id", columnList = "master_id"),
                @Index(name = "idx_master_tag_tag_id", columnList = "tag_id"),
                @Index(name = "idx_master_tag_master_tag", columnList = "master_id, tag_id", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class MasterTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "master_tag_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_id", nullable = false)
    private Master master;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public static MasterTag create(Master master, Tag tag) {
        return MasterTag.builder()
                .master(master)
                .tag(tag)
                .build();
    }
}
