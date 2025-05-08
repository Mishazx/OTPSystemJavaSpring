package ru.mishazx.otpsystemjavaspring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OtpSystemJavaSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(OtpSystemJavaSpringApplication.class, args);
    }

}
