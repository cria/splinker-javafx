CREATE TABLE IF NOT EXISTS ProxyConfiguration (
  proxy_username VARCHAR(50) NOT NULL,
  proxy_password VARCHAR(100) NOT NULL,
  proxy_port VARCHAR(10) NOT NULL,
  proxy_address VARCHAR(100) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS DbConfiguration (
  db_password VARCHAR(100) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS CentralServiceConfiguration (
  central_service_uri VARCHAR(100) NOT NULL,
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

INSERT INTO TransferConfiguration (rsync_port, rsync_server_destination) VALUES (10000, "bruno@34.68.143.184::meu_modulo");