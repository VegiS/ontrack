package net.nemerosa.ontrack.migration.postgresql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.Collections;

@Configuration
@SpringBootApplication
@ComponentScan("net.nemerosa.ontrack")
public class MigrationTool {

    private final Logger logger = LoggerFactory.getLogger(MigrationTool.class);

    @Autowired
    private MigrationProperties migrationProperties;

    @Autowired
    private FlywayProperties flywayProperties;

    @PostConstruct
    public void start() {
        flywayProperties.setLocations(Collections.singletonList("classpath:/ontrack/sql"));
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(MigrationTool.class);
        ConfigurableApplicationContext context = application.run(args);
        context.getBeansOfType(Migration.class).get("migration").run();
    }

    /**
     * PostgresQL database
     */
    @Bean
    @Primary
    @Qualifier("postgresql")
    public DataSource getDataSource() {
        return createDataSource(
                "Postgresql",
                "org.postgresql.Driver",
                migrationProperties.getPostgresql()
        );
    }

    /**
     * H2 database
     */
    @Bean
    @Qualifier("h2")
    public DataSource getH2DataSource() {
        return createDataSource(
                "H2",
                "org.h2.Driver",
                migrationProperties.getH2()
        );
    }

    private DataSource createDataSource(String name, String driver, MigrationProperties.DatabaseProperties databaseProperties) {
        logger.info("Using {} database at {}", name, databaseProperties.getUrl());
        org.apache.tomcat.jdbc.pool.DataSource pool = new org.apache.tomcat.jdbc.pool.DataSource();
        pool.setDriverClassName(driver);
        pool.setUrl(databaseProperties.getUrl());
        pool.setUsername(databaseProperties.getUsername());
        pool.setPassword(databaseProperties.getPassword());
        pool.setDefaultAutoCommit(false);
        pool.setInitialSize(10);
        pool.setMaxIdle(10);
        pool.setMaxActive(20);
        return pool;
    }

}
