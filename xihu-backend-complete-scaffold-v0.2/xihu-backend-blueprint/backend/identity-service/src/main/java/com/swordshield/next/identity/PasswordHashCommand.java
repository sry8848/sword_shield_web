package com.swordshield.next.identity;

import java.io.Console;
import java.nio.CharBuffer;
import java.util.Arrays;

final class PasswordHashCommand {

    private PasswordHashCommand() {
    }

    /** 从交互式终端获取密码，输出一行哈希供管理员预置。 */
    static void run() {
        // 1. 从交互式终端读取密码，输入内容不会回显。
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("请在交互式终端中运行 --encode-password");
        }

        // 2. 输出编码后的哈希。
        System.out.println(encode(console.readPassword("请输入预置账号密码：")));
    }

    /** 将非空的终端密码编码为 Argon2id 字符串，使用后清除输入数组。 */
    static String encode(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("密码不能为空");
        }
        try {
            return IdentityPasswordConfiguration.identityPasswordEncoder().encode(CharBuffer.wrap(password));
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
