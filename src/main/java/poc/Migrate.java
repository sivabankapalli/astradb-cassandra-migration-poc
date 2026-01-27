package poc;

import com.datastax.oss.driver.api.core.CqlSession;
import com.hhandoko.cassandra.migration.CassandraMigration;

import java.nio.file.Path;

public class Migrate {
  public static void main(String[] args) {
    // Required env vars (set by GitHub Actions)
    String keyspace = mustGet("ASTRA_DB_KEYSPACE");
    String token = mustGet("ASTRA_DB_TOKEN");
    String scbPath = mustGet("ASTRA_SCB_PATH"); // absolute path to secure-connect.zip

    // DataStax driver cloud connection for Astra:
    // - withCloudSecureConnectBundle(...)
    // - withAuthCredentials("token", <token>)
    try (CqlSession session = CqlSession.builder()
        .withCloudSecureConnectBundle(Path.of(scbPath))
        .withAuthCredentials("token", token)
        .withKeyspace(keyspace)
        .build()) {

      // cassandra-migration runner (CQL migrations)
      CassandraMigration migration = new CassandraMigration(session);
      migration.migrate(); // applies pending V* migrations
      System.out.println("Migration completed.");
    }
  }

  private static String mustGet(String name) {
    String v = System.getenv(name);
    if (v == null || v.isBlank()) throw new IllegalArgumentException("Missing env var: " + name);
    return v;
  }
}
