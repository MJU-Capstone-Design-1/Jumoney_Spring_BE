package com.mju.Jumoney.domain.sector.domain;

import com.mju.Jumoney.domain.sector.enums.SectorType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sectors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sector_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private SectorType sectorName;

    // ========== 정적 팩토리 메서드 ==========

    public static Sector create(SectorType sectorName) {
        return Sector.builder()
                .sectorName(sectorName)
                .build();
    }
}
