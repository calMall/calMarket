<<<<<<< HEAD

=======
>>>>>>> BE
package com.example.calmall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
<<<<<<< HEAD

@SpringBootApplication
public class CalmallApplication {
=======
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot アプリケーションのエントリーポイント
 */
@SpringBootApplication
@EnableScheduling
public class CalmallApplication {

>>>>>>> BE
    public static void main(String[] args) {
        SpringApplication.run(CalmallApplication.class, args);
    }
}
