package org.example.motoadvisor.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class SchemaInitializationTests {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    SchemaInitializationTests(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void requiredSchemaTablesExist() {
        assertEquals(1, tableExists("search_history"));
        assertEquals(1, tableExists("agent_logs"));
    }

    private Integer tableExists(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sqlite_master WHERE type='table' AND name=?",
                Integer.class,
                table
        );
    }
}

