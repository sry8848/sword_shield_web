CREATE TABLE identity_session (
    id CHAR(36) NOT NULL,
    account_id BIGINT NOT NULL,
    client_type VARCHAR(16) NOT NULL,
    credential_version INT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    last_used_at DATETIME(3) NOT NULL,
    idle_expires_at DATETIME(3) NOT NULL,
    absolute_expires_at DATETIME(3) NOT NULL,
    revoked_at DATETIME(3),
    PRIMARY KEY (id),
    CONSTRAINT fk_identity_session_account FOREIGN KEY (account_id)
        REFERENCES identity_account (id),
    CONSTRAINT chk_identity_session_client CHECK (client_type IN ('PORTAL', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;

CREATE TABLE identity_refresh_token (
    token_hash BINARY(32) NOT NULL,
    session_id CHAR(36) NOT NULL,
    issued_at DATETIME(3) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    PRIMARY KEY (token_hash),
    CONSTRAINT fk_identity_refresh_token_session FOREIGN KEY (session_id)
        REFERENCES identity_session (id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;
