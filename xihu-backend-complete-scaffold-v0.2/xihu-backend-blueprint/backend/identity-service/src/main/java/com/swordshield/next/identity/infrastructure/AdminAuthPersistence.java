package com.swordshield.next.identity.infrastructure;

import java.util.Objects;
import java.util.Optional;

import com.swordshield.next.identity.application.AdminAuthentication;
import com.swordshield.next.identity.application.AdminAuthentication.AccountState;
import com.swordshield.next.identity.application.AdminAuthentication.AccountWithRoles;
import org.springframework.stereotype.Repository;

@Repository
class AdminAuthPersistence implements AdminAuthentication.Store {

    private final AdminAccountMapper accounts;

    AdminAuthPersistence(AdminAccountMapper accounts) {
        this.accounts = accounts;
    }

    /** 按手机号读取账号凭证及有效后台角色。 */
    @Override
    public Optional<AccountWithRoles> findByPhone(String phone) {
        var rows = accounts.selectLogin(phone);
        return rows.stream().findFirst().map(account -> new AccountWithRoles(account.id(), account.passwordHash(),
                account.enabled(), account.credentialVersion(),
                rows.stream().map(AdminAccountMapper.LoginRow::roleCode).filter(Objects::nonNull).toList()));
    }

    /** 按账号标识读取当前账号状态及有效后台角色。 */
    @Override
    public Optional<AccountState> findStateById(String id) {
        var rows = accounts.selectSession(id);
        return rows.stream().findFirst().map(account -> new AccountState(account.phone(), account.enabled(),
                account.credentialVersion(),
                rows.stream().map(AdminAccountMapper.SessionRow::roleCode).filter(Objects::nonNull).toList()));
    }
}
