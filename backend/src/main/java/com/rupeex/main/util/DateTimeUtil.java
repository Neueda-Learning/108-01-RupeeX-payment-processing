package com.rupeex.main.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Central helper for Indian Standard Time (IST) date/time handling.
 *
 * RupeeX operates exclusively in IST (Asia/Kolkata, UTC+05:30) — every
 * timestamp persisted or displayed by the platform (payment creation,
 * scheduling, audit trails, notifications) must be derived from this
 * utility instead of the JVM/server-local {@code LocalDateTime.now()} so
 * behavior is identical regardless of the host machine's configured
 * timezone.
 */
public final class DateTimeUtil {

    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private DateTimeUtil() {
    }

    /** Current wall-clock time in IST. */
    public static LocalDateTime nowIst() {
        return LocalDateTime.now(IST);
    }
}
