CREATE TABLE IF NOT EXISTS BasicConfiguration (
  token VARCHAR(50) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS CentralServiceConfiguration (
  central_service_url VARCHAR(100) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS TransferConfiguration (
  rsync_port INT NOT NULL,
  rsync_server_destination VARCHAR(500) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS ProxyConfiguration (
  proxy_username VARCHAR(50) NOT NULL,
  proxy_password VARCHAR(100) NOT NULL,
  proxy_port VARCHAR(10) NOT NULL,
  proxy_address VARCHAR(100) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS DataSourceConfiguration (
  datasource_filepath VARCHAR(50) NULL,
  datasource_type VARCHAR(50) NOT NULL,
  db_host VARCHAR(100) NULL,
  db_port VARCHAR(100) NULL,
  db_name VARCHAR(100) NULL,
  db_tablename VARCHAR(100) NULL,
  db_username VARCHAR(100) NULL,
  db_password VARCHAR(100) NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);

INSERT INTO TransferConfiguration (rsync_port, rsync_server_destination)
VALUES (10000, 'bruno@35.224.172.146');

INSERT INTO CentralServiceConfiguration (central_service_url)
VALUES ('https://specieslink.net/ws/1.0/splinker/');