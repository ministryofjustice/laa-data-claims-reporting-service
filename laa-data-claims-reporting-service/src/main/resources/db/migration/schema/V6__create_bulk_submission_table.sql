CREATE TABLE bulk_submission (
                                 id                      UUID NOT NULL,
                                 status                  TEXT NOT NULL,
                                 error_code              TEXT,
                                 error_description       TEXT,
                                 created_by_user_id      TEXT NOT NULL,
                                 created_on              TIMESTAMPTZ NOT NULL,
                                 updated_by_user_id      TEXT,
                                 updated_on              TIMESTAMPTZ,
                                 authorised_offices      TEXT,
                                 CONSTRAINT pk_bulk_submission PRIMARY KEY (id)
);
