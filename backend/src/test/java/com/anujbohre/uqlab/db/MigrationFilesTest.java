package com.anujbohre.uqlab.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.anujbohre.uqlab.auth.AppUser;
import com.anujbohre.uqlab.job.SimulationJob;
import jakarta.persistence.Column;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * A cheap guard that needs no PostgreSQL: the Flyway script must mention every column the entities
 * declare. If someone adds a field to an entity and forgets the migration, this test fails.
 * (The full proof is starting the application with the "postgres" profile: Hibernate then
 * validates the real tables against the entities.)
 */
class MigrationFilesTest {

    private static final Path MIGRATIONS = Path.of("src", "main", "resources", "db", "migration");

    private static String read(String name) throws IOException {
        return Files.readString(MIGRATIONS.resolve(name), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
    }

    /** createdAt becomes created_at, the same naming rule Hibernate applies. */
    private static String snake(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private static void assertCoversEntity(String sql, String table, Class<?> entity) {
        assertThat(sql).contains("create table " + table);
        for (Field field : entity.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            assertThat(sql).as("column for %s.%s", entity.getSimpleName(), field.getName()).contains(snake(field.getName()));
        }
    }

    @Test
    void firstMigrationCreatesBothTablesWithEveryEntityColumn() throws IOException {
        String sql = read("V1__create_tables.sql");
        assertCoversEntity(sql, "app_user", AppUser.class);
        assertCoversEntity(sql, "simulation_job", SimulationJob.class);
    }

    @Test
    void usernameIsUniqueInTheDatabaseToo() throws IOException {
        assertThat(read("V1__create_tables.sql")).contains("unique (username)");
    }

    @Test
    void secondMigrationAddsTheOwnerIndexes() throws IOException {
        String sql = read("V2__job_indexes.sql");
        assertThat(sql).contains("create index").contains("owner_id");
    }

    @Test
    void stringColumnLengthsInTheScriptMatchTheEntities() throws IOException {
        String sql = read("V1__create_tables.sql");
        assertThat(sql).contains("varchar(" + length(AppUser.class, "username") + ")");
        assertThat(sql).contains("varchar(" + length(SimulationJob.class, "requestJson") + ")");
        assertThat(sql).contains("varchar(" + length(SimulationJob.class, "resultJson") + ")");
    }

    private static int length(Class<?> entity, String fieldName) {
        try {
            return entity.getDeclaredField(fieldName).getAnnotation(Column.class).length();
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
}
