CREATE TABLE app_user
(
    user_id         SERIAL          NOT NULL,
    user_name       VARCHAR(20)     NOT NULL,
    email           VARCHAR(32)     NOT NULL,
    password        VARCHAR(256)    NOT NULL,
    name            VARCHAR(32)     NOT NULL,
    active          BOOLEAN         NOT NULL,
    PRIMARY KEY (user_id),
    UNIQUE (user_name),
    UNIQUE (email)
);

CREATE TABLE role
(
    role_id     SERIAL      NOT NULL,
    role        VARCHAR(32) NOT NULL,
    PRIMARY KEY (role_id)
);

CREATE TABLE app_user_role
(
    user_id     INT      NOT NULL,
    role_id     INT      NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user
        FOREIGN KEY (user_id)
            REFERENCES app_user (user_id),
    CONSTRAINT fk_app_user_role_role
            FOREIGN KEY (role_id)
                REFERENCES role (role_id)
);
