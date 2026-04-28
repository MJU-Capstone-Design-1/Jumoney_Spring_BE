package com.mju.Jumoney.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public abstract class BaseSoftDeleteEntity {

    @Column
    private LocalDateTime deleteAt;

    public void softDelete() {
        this.deleteAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deleteAt != null;
    }

}
