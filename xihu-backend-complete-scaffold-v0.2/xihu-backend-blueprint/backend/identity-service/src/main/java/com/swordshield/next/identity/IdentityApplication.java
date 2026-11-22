package com.swordshield.next.identity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class IdentityApplication {
    /** 启动 Identity 服务，或根据终端参数从本地生成预置密码哈希。 */
    public static void main(String[] args) {
        if (args.length == 1 && "--encode-password".equals(args[0])) {
            PasswordHashCommand.run();
            return;
        }
        SpringApplication.run(IdentityApplication.class, args);
    }
}
