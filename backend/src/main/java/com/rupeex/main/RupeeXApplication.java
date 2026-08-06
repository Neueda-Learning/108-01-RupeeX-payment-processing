package com.rupeex.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class RupeeXApplication {

    static {
        // RupeeX operates exclusively in Indian Standard Time. Setting the JVM
        // default timezone here (before Spring/Hibernate/Jackson initialize)
        // ensures every LocalDateTime.now() call, JDBC timestamp conversion,
        // and JSON serialization across the application is consistently IST,
        // regardless of the host/container's system timezone.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    public static void main(String[] args) {
        SpringApplication.run(RupeeXApplication.class, args);
    }

}
