package poc;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cognitor.cassandra.migration.Database;
import org.cognitor.cassandra.migration.MigrationRepository;
import org.cognitor.cassandra.migration.MigrationTask;

import java.nio.file.Path;

public final class RunMigrations {

  public static void main(String[] args) {
    String keyspace = mustGet("ASTRA_DB_KEYSPACE");
    String token = mustGet("ASTRA_DB_TOKEN");
    String scbPath = mustGet("ASTRA_SCB_PATH");

    // Important: per library docs, pass a dedicated session instance. :contentReference[oaicite:5]{index=5}
    try (CqlSession session = CqlSession.builder()
        .withCloudSecureConnectBundle(Path.of(scbPath))
        .withAuthCredentials("token", token)
        .withKeyspace(keyspace)
        .build()) {

      Database db = new Database(session, keyspace);
      MigrationTask task = new MigrationTask(db, new MigrationRepository()); // default: /cassandra/migration :contentReference[oaicite:6]{index=6}
      task.migrate();

      System.out.println("Cassandra migrations completed successfully.");
    }
  }

  private static String mustGet(String name) {
    String v = System.getenv(name);
    if (v == null || v.isBlank()) throw new IllegalArgumentException("Missing env var: " + name);
    return v.trim();
  }
}
