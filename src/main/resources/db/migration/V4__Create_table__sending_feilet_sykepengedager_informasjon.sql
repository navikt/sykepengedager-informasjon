CREATE TABLE SENDING_FEILED_SYKEPENGEDAGER_INFORMASJON
(
    UUID                  UUID PRIMARY KEY,
    EVENT_ID              VARCHAR(40) NOT NULL,
    CREATED_AT            TIMESTAMP   NOT NULL,
    ERROR_MESSAGE         TEXT
);
