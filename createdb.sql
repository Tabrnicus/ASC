-- Use rebuildDB.sh to rebuild db using this file

PRAGMA foreign_keys= ON;

CREATE TABLE servers
(
    sid         INTEGER PRIMARY KEY NOT NULL,
    description TEXT                NOT NULL,
    game        TEXT                NOT NULL,
    moniker     TEXT                NOT NULL,
    startfile   TEXT                NOT NULL,
    stopcommand TEXT                NOT NULL,
    warncommand TEXT                NOT NULL,
    port        INTEGER             NOT NULL,
    autostart   INTEGER             NOT NULL
);

CREATE TABLE eventType
(
    etid  INTEGER PRIMARY KEY,
    label TEXT
);

CREATE TABLE events
(
    eid   INTEGER PRIMARY KEY NOT NULL,
    sid   INTEGER             NOT NULL REFERENCES servers (sid),
    time  TIME                NOT NULL,
    etype INTEGER             NOT NULL REFERENCES eventType (etid),
    args  TEXT                NOT NULL DEFAULT '[]'
);

-- Event Type

INSERT INTO eventType
VALUES (0, 'EXECUTE');

INSERT INTO eventType
VALUES (1, 'START');

INSERT INTO eventType
VALUES (2, 'COMMAND');

INSERT INTO eventType
VALUES (3, 'STOP');

INSERT INTO eventType
VALUES (4, 'WARN');

-- Servers

INSERT INTO servers
VALUES (1,
        'Example 1',
        'game1',
        'server1',
        '/bin/bash',
        'stop',
        'There are $TIME minute(s) left',
        25565,
        1);

INSERT INTO servers
VALUES (2,
        'Example 2',
        'game1',
        'server2',
        '/bin/bash',
        'stop2',
        'There are $TIME minute(s) left',
        25566,
        1);

INSERT INTO servers
VALUES (3,
        'Example 3',
        'game2',
        'server1',
        '/bin/zsh',
        'exit',
        'There are $TIME minute(s) left',
        25585,
        1);

INSERT INTO servers
VALUES (4,
        'Example 1',
        'game2',
        'server2',
        '/bin/zsh',
        'exit2',
        'There are $TIME minute(s) left',
        25590,
        0);

-- Events

INSERT INTO events
VALUES (1,
        1,
        '09:15:00',
        1,
        '[]');

INSERT INTO events
VALUES (2,
        1,
        '09:20:00',
        2,
        '["someargument"]');

INSERT INTO events
VALUES (3,
        1,
        '17:00:00',
        3,
        '[]');

INSERT INTO events
VALUES (4,
        2,
        '16:00:00',
        4,
        '["30"]');

INSERT INTO events
VALUES (5,
        2,
        '10:15:00',
        1,
        '[]');

INSERT INTO events
VALUES (6,
        2,
        '10:20:00',
        2,
        '["someotherargument"]');

INSERT INTO events
VALUES (7,
        3,
        '03:00:00',
        3,
        '[]');

INSERT INTO events
VALUES (8,
        4,
        '03:00:15',
        0,
        '["/bin/bash", "-flag1", "-flag2"]');

.exit