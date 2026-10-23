package com.campus.lostfound;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lost & Found - Smart Campus Asset Recovery Platform.
 * Team "Four Founders", GL Bajaj Institute of Technology and Management.
 * Flow: Report -> Search -> Match -> Claim -> Verify -> Return.
 */
@SpringBootApplication
public class LostFoundApplication {
    public static void main(String[] args) {
        SpringApplication.run(LostFoundApplication.class, args);
    }
}
