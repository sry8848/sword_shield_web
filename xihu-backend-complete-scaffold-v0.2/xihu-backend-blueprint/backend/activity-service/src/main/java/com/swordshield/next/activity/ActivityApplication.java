package com.swordshield.next.activity;
import com.swordshield.next.common.web.ServletProblemAdvice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(ServletProblemAdvice.class)
public class ActivityApplication {
    public static void main(String[] args) { SpringApplication.run(ActivityApplication.class,args); }
}
