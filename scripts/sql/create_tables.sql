CREATE TABLE IF NOT EXISTS LocalConfiguration (
  db_password VARCHAR(100) NOT NULL,
  central_service_uri VARCHAR(100) NOT NULL,
  central_service_url VARCHAR(100) NOT NULL,
  proxy_username VARCHAR(50) NOT NULL,
  proxy_password VARCHAR(100) NOT NULL,
  proxy_port VARCHAR(10) NOT NULL,
  proxy_address VARCHAR(100) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
)
