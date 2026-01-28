# 📦 AstraDB Cassandra Migration PoC

This repository demonstrates how to manage **Cassandra / DataStax
AstraDB schema migrations** using the lightweight open-source tool\
👉 **Cassandra Migration Tool (`patka/cassandra-migration`)**

It provides: - Versioned CQL scripts for schema changes - Automated
migration execution via **GitHub Actions** - Built-in version tracking
(similar to Flyway/Liquibase) - Secure connection to AstraDB using
Secure Connect Bundle (SCB) and token authentication

------------------------------------------------------------------------

## ✨ Features

-   ✅ Cassandra-native migration tool (no JDBC, no Flyway/Liquibase)
-   ✅ Versioned CQL scripts (`001_*.cql`, `002_*.cql`, etc.)
-   ✅ Automatic migration state tracking in Cassandra
-   ✅ CI/CD ready with GitHub Actions
-   ✅ Secure AstraDB connectivity using SCB + token
-   ✅ Lightweight Java runner (no custom migration logic)
-   ✅ Audit trail via migration table + pipeline logs

------------------------------------------------------------------------

## 📂 Project Structure

``` text
astradb-cassandra-migration-poc/
│
├── .github/workflows/
│   └── migrate.yml
│
├── src/main/java/poc/
│   └── RunMigrations.java
│
├── src/main/resources/cassandra/migration/
│   ├── 001_create_user_table.cql
│   ├── 002_add_index.cql
│   └── 003_insert_seed_data.cql
│
├── pom.xml
├── README.md
└── LICENSE
```

------------------------------------------------------------------------

## 🧩 How It Works

1.  Migration scripts are stored in:

        src/main/resources/cassandra/migration

2.  Scripts must follow naming convention:

        <version>_<description>.cql

3.  On execution:

    -   Tool scans the migration folder
    -   Checks which versions were already applied
    -   Executes only new migrations
    -   Records applied versions in Cassandra

------------------------------------------------------------------------

## 🗄 Migration Tracking Table

Query in AstraDB CQL Console:

``` sql
USE app_dev_ks;
SELECT * FROM cassandra_migration;
```

------------------------------------------------------------------------

## 🔑 GitHub Secrets Required

  Secret Name         Description
  ------------------- ------------------------------
  ASTRA_DB_KEYSPACE   Target keyspace
  ASTRA_DB_TOKEN      Astra DB token
  ASTRA_SCB_BASE64    Base64 Secure Connect Bundle

------------------------------------------------------------------------

## ▶️ Run Locally

``` bash
export ASTRA_DB_KEYSPACE=app_dev_ks
export ASTRA_DB_TOKEN=your_token
export ASTRA_SCB_PATH=/path/to/secure-connect.zip

mvn clean package
mvn exec:java -Dexec.mainClass=poc.RunMigrations
```

------------------------------------------------------------------------

## 📄 License

MIT License

------------------------------------------------------------------------

## 🙌 Credits

-   Cassandra Migration Tool:
    https://github.com/patka/cassandra-migration
-   DataStax AstraDB:
    https://www.datastax.com/products/datastax-astra-db
