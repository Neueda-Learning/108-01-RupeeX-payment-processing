package com.rupeex.main.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DateTimeUtil Tests")
class DateTimeUtilTest {

    @Test
    @DisplayName("nowIst returns current time in IST timezone")
    void nowIst_ReturnsIstTime() {
        LocalDateTime before = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        LocalDateTime result = DateTimeUtil.nowIst();
        LocalDateTime after = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));

        assertThat(result).isAfterOrEqualTo(before);
        assertThat(result).isBeforeOrEqualTo(after);
    }

    @Test
    @DisplayName("IST constant is Asia/Kolkata")
    void istConstant_IsAsiaKolkata() {
        assertThat(DateTimeUtil.IST).isEqualTo(ZoneId.of("Asia/Kolkata"));
    }

    @Test
    @DisplayName("nowIst returns a non-null value")
    void nowIst_IsNotNull() {
        assertThat(DateTimeUtil.nowIst()).isNotNull();
    }

    @Test
    @DisplayName("nowIst returns LocalDateTime (no timezone info)")
    void nowIst_IsLocalDateTime() {
        LocalDateTime result = DateTimeUtil.nowIst();
        assertThat(result).isInstanceOf(LocalDateTime.class);
    }
}
