## Configuring SQL store for front50

#### MySQL:

```yaml
sql:
  enabled: true
  baseUrl: jdbc:mysql://localhost:3306
  connectionPools:
    default:
      jdbcUrl: ${sql.baseUrl}/front50?useSSL=false&serverTimezone=UTC
      user: 
      password:
  migration:
    jdbcUrl: ${sql.baseUrl}/front50?useSSL=false&serverTimezone=UTC
    user: 
    password:
```

#### PostgreSQL:
```yaml
sql:
  enabled: true
  baseUrl: jdbc:postgresql://localhost:5432
  connectionPools:
    default:
      jdbcUrl: ${sql.baseUrl}/front50?useSSL=false&serverTimezone=UTC
      dialect: POSTGRES
      user: 
      password:
  migration:
    jdbcUrl: ${sql.baseUrl}/front50?useSSL=false&serverTimezone=UTC
    user: 
    password:
```
