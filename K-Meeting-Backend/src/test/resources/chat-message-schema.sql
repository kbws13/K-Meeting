DROP TABLE IF EXISTS chatMessage;

CREATE TABLE chatMessage
(
    id               BIGINT PRIMARY KEY,
    meetingId        INT         NOT NULL,
    type             TINYINT     NOT NULL,
    content          VARCHAR(500),
    sendUserId       INT         NOT NULL,
    sendUserNickName VARCHAR(20),
    sendTime         BIGINT      NOT NULL,
    receiveType      TINYINT     NOT NULL,
    receiveUserId    INT,
    fileSize         BIGINT,
    fileName         VARCHAR(200),
    fileType         TINYINT,
    fileSuffix       VARCHAR(10),
    status           TINYINT
);

CREATE INDEX idx_chatMessage_meetingId_sendTime ON chatMessage (meetingId, sendTime);
CREATE INDEX idx_chatMessage_sendTime ON chatMessage (sendTime);
