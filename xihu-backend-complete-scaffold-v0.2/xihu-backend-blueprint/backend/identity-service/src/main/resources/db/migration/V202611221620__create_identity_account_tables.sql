CREATE TABLE identity_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    phone VARCHAR(11) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL,
    credential_version INT NOT NULL DEFAULT 1,
    permission_version INT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_identity_account_phone UNIQUE (phone),
    CONSTRAINT chk_identity_account_phone CHECK (REGEXP_LIKE(phone, '^1[3-9][0-9]{9}\\z', 'c')),
    CONSTRAINT chk_identity_account_password_hash CHECK (password_hash LIKE '$argon2id$_%'),
    CONSTRAINT chk_identity_account_status CHECK (status IN ('ENABLED', 'DISABLED')),
    CONSTRAINT chk_identity_account_credential_version CHECK (credential_version >= 1),
    CONSTRAINT chk_identity_account_permission_version CHECK (permission_version >= 1)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;

CREATE TABLE identity_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_identity_role_code UNIQUE (code),
    CONSTRAINT chk_identity_role_code CHECK (REGEXP_LIKE(code, '^[A-Z][A-Z0-9_]*\\z', 'c'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;

CREATE TABLE identity_account_role (
    account_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (account_id, role_id),
    INDEX idx_identity_account_role_role_id (role_id),
    CONSTRAINT fk_identity_account_role_account FOREIGN KEY (account_id)
        REFERENCES identity_account (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_identity_account_role_role FOREIGN KEY (role_id)
        REFERENCES identity_role (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;

INSERT INTO identity_role (code) VALUES ('SUPER_ADMIN');
