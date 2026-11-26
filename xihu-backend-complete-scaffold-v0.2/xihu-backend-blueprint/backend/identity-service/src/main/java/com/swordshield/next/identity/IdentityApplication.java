package com.swordshield.next.identity;
import com.swordshield.next.common.UuidV7Ids;
import com.swordshield.next.common.web.ServletProblemAdvice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(ServletProblemAdvice.class)
public class IdentityApplication {
    /** 启动 Identity，或为离线账号预置生成密码哈希或 UUIDv7。 */
    public static void main(String[] args) {
        if (args.length == 1 && "--encode-password".equals(args[0])) {
            PasswordHashCommand.run();
            return;
        }
        if (args.length == 1 && "--generate-id".equals(args[0])) {
            System.out.println(UuidV7Ids.next());
            return;
        }
        SpringApplication.run(IdentityApplication.class, args);
    }
}
