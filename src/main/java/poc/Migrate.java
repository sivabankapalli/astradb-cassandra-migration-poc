package poc;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Migrate {

  private static final String MIGRATIONS_DIR = "migrations/";
  private static final Pattern VERSIONED = Pattern.compile("^V(\\d+)__.*\\.cql$");

  public static void main(String[] args) throws Exception {
    String keyspace = mustGet("ASTRA_DB_KEYSPACE");
    String token = mustGet("ASTRA_DB_TOKEN");
    String scbPath = mustGet("ASTRA_SCB_PATH");

    try (CqlSession session = CqlSession.builder()
        .withCloudSecureConnectBundle(Path.of(scbPath))
        .withAuthCredentials("token", token)
        .withKeyspace(keyspace)
        .build()) {

      ensureSchemaMigrationsTable(session);

        List<String> all = listMigrationResources();
        Set<Integer> applied = loadAppliedVersions(session);

        for (String file : all) {
          int ver = versionOf(file);
          if (applied.contains(ver)) {
            System.out.println("Skipping already applied: " + file);
            continue;
          }

          String cql = readResource(MIGRATIONS_DIR + file);
          applyCql(session, cql);

          recordApplied(session, ver, file);
          System.out.println("Applied: " + file);
        }

      System.out.println("All migrations complete.");
    }
  }

  private static void ensureSchemaMigrationsTable(CqlSession session) {
    session.execute("""
      CREATE TABLE IF NOT EXISTS schema_migrations (
        version int PRIMARY KEY,
        description text,
        installed_on timestamp
      )
      """);
  }

  private static Set<Integer> loadAppliedVersions(CqlSession session) {
    Set<Integer> set = new HashSet<>();
    session.execute("SELECT version FROM schema_migrations")
        .forEach(r -> set.add(r.getInt("version")));
    return set;
  }

private static void recordApplied(CqlSession session, int version, String file) {
  session.execute(
      SimpleStatement.builder(
              "INSERT INTO schema_migrations (version, description, installed_on) VALUES (?, ?, ?)")
          .addPositionalValue(version)
          .addPositionalValue(file)
          .addPositionalValue(Instant.now())
          .build()
  );
}

  private static void applyCql(CqlSession session, String cql) {
    // Very simple splitter: executes statements separated by ';'
    // Good enough for PoC migrations.
    for (String stmt : cql.split(";")) {
      String trimmed = stmt.trim();
      if (trimmed.isEmpty()) continue;
      session.execute(trimmed);
    }
  }

  /*private static List<String> listMigrationResources() throws Exception {
    // Works because resources are on the classpath; in Maven exec, we can list via ClassLoader
    // Here, we hardcode for PoC simplicity; you can extend later.
    // Better: keep a manifest file migrations/index.txt
    return Arrays.asList(
      "V1__create_user.cql",
      "V2__add_index.cql"
    );
  }*/

  private static List<String> listMigrationResources() throws Exception {
  ClassLoader cl = Migrate.class.getClassLoader();
  URL dirUrl = cl.getResource(MIGRATIONS_DIR); // e.g. "migrations/"
  if (dirUrl == null) {
    throw new IllegalArgumentException("Missing migrations folder on classpath: " + MIGRATIONS_DIR +
        " (expected under src/main/resources/" + MIGRATIONS_DIR + ")");
  }

  String protocol = dirUrl.getProtocol();

  if ("file".equals(protocol)) {
    // Running from filesystem (e.g., target/classes/migrations)
    Path dirPath = Paths.get(dirUrl.toURI());
    try (Stream<Path> paths = Files.list(dirPath)) {
      return paths
          .filter(Files::isRegularFile)
          .map(p -> p.getFileName().toString())
          .filter(name -> VERSIONED.matcher(name).matches())
          .sorted(Comparator.comparingInt(Migrate::versionOf))
          .collect(Collectors.toList());
    }
  }

  if ("jar".equals(protocol)) {
    // Running from a jar
    URI jarUri = dirUrl.toURI(); // jar:file:/.../app.jar!/migrations/
    String s = jarUri.toString();
    String jarPath = s.substring("jar:".length(), s.indexOf("!")); // file:/.../app.jar

    try (JarFile jar = new JarFile(Paths.get(URI.create(jarPath)).toFile())) {
      List<String> files = new ArrayList<>();
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        JarEntry e = entries.nextElement();
        String name = e.getName(); // migrations/V1__x.cql
        if (e.isDirectory()) continue;
        if (!name.startsWith(MIGRATIONS_DIR)) continue;

        String fileNameOnly = name.substring(MIGRATIONS_DIR.length()); // V1__x.cql
        if (VERSIONED.matcher(fileNameOnly).matches()) {
          files.add(fileNameOnly);
        }
      }
      files.sort(Comparator.comparingInt(Migrate::versionOf));
      return files;
    }
  }

  throw new IllegalStateException("Unsupported classpath protocol for migrations folder: " + protocol);
}


  private static int versionOf(String filename) {
    Matcher m = VERSIONED.matcher(filename);
    if (!m.matches()) throw new IllegalArgumentException("Bad migration filename: " + filename);
    return Integer.parseInt(m.group(1));
  }

  private static String readResource(String path) throws Exception {
    try (var is = Migrate.class.getClassLoader().getResourceAsStream(path)) {
      if (is == null) throw new IllegalArgumentException("Missing resource: " + path);
      try (var br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append("\n");
        return sb.toString();
      }
    }
  }

  private static String mustGet(String name) {
    String v = System.getenv(name);
    if (v == null || v.isBlank()) throw new IllegalArgumentException("Missing env var: " + name);
    return v.trim();
  }
}
