package net.nemerosa.ontrack.neo4j.migration;

import com.google.common.collect.ImmutableMap;
import net.nemerosa.ontrack.common.Time;
import net.nemerosa.ontrack.model.events.EventFactory;
import net.nemerosa.ontrack.model.events.EventType;
import net.nemerosa.ontrack.model.structure.Signature;
import net.nemerosa.ontrack.repository.AccountGroupRepository;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.ogm.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class Migration extends NamedParameterJdbcDaoSupport {

    public static final String MIGRATION_USER = "migration";

    private final Logger logger = LoggerFactory.getLogger(Migration.class);

    private final MigrationProperties migrationProperties;
    @Deprecated
    private final AccountGroupRepository accountGroupRepository;
    private final Neo4jOperations template;

    @Autowired
    public Migration(Neo4jOperations template, DataSource dataSource, MigrationProperties migrationProperties, AccountGroupRepository accountGroupRepository) {
        this.template = template;
        this.migrationProperties = migrationProperties;
        this.accountGroupRepository = accountGroupRepository;
        this.setDataSource(dataSource);
    }

    public void run() {
        logger.info("Starting migration...");
        long start = System.currentTimeMillis();
        // Deleting all nodes
        logger.info("Removing all nodes...");
        template.query("MATCH (n) DETACH DELETE n", Collections.emptyMap());
        // Migrating the projects
        logger.info("Migrating projects...");
        logger.info("Projects = {}", migrateProjects());
        // Migrating the branches
        logger.info("Migrating branches...");
        logger.info("Branches = {}", migrateBranches());
        // Migrating the promotion levels
        logger.info("Migrating promotion levels...");
        logger.info("Promotion levels = {}", migratePromotionLevels());
        // Migrating the validation stamps
        logger.info("Migrating validation stamps...");
        logger.info("Validation stamps = {}", migrateValidationStamps());
        // Migrating the builds
        logger.info("Migrating builds...");
        logger.info("Builds = {}", migrateBuilds());
        // Migrating the build links
        logger.info("Migrating build links...");
        logger.info("Build links = {}", migrateBuildLinks());
        // Migrating the promotion runs
        logger.info("Migrating promotion runs...");
        logger.info("Promotion runs = {}", migratePromotionRuns());
        // Migrating the validation runs
        logger.info("Migrating validation runs...");
        logger.info("Validation runs = {}", migrateValidationRuns());
        // Migrating ACL
        logger.info("Migrating ACL...");
        migrateACL();
        // TODO Entity data
        // TODO Configurations
        // TODO Properties
        // TODO Build filters
        // TODO Shared build filters
        // TODO Branch templates
        // TODO Events
        // TODO Predefined promotion levels
        // TODO Predefined validation stamps
        // TODO Project favourites
        // TODO Settings
        // TODO Storage
        // Creating the counters
        logger.info("Creating unique id generators...");
        createUniqueIdGenerators();
        // OK
        long end = System.currentTimeMillis();
        logger.info("End of migration ({} ms)", end - start);
    }

    private void createUniqueIdGenerators() {
        createUniqueIdGenerator("Project");
        createUniqueIdGenerator("Branch");
        createUniqueIdGenerator("PromotionLevel");
        createUniqueIdGenerator("ValidationStamp");
        createUniqueIdGenerator("Build");
        createUniqueIdGenerator("ValidationRun");
        // ----
        createUniqueIdGenerator("AccountGroup");
        createUniqueIdGenerator("Account");
    }

    private void createUniqueIdGenerator(String label) {
        Result query = template.query(String.format("MATCH (p:%s) RETURN MAX(p.id) as MAX", label),
                ImmutableMap.<String, Object>builder()
                        .put("label", label)
                        .build());
        int max = (Integer) query.queryResults().iterator().next().get("MAX");
        template.query(
                "CREATE (u:UniqueId {label: {label}, id: {id}})",
                ImmutableMap.<String, Object>builder()
                        .put("label", label)
                        .put("id", max)
                        .build()
        );
    }

    /**
     * ==========================
     * Structure
     * ==========================
     */

    @SuppressWarnings("RedundantCast")
    private int migrateProjects() {
        AtomicInteger count = new AtomicInteger();
        jdbc().query(
                "SELECT * FROM PROJECTS",
                (RowCallbackHandler) rs -> {
                    count.incrementAndGet();
                    int projectId = rs.getInt("ID");
                    Signature signature = getEventSignature("project", EventFactory.NEW_PROJECT, projectId);
                    template.query(
                            "CREATE (p:Project {id: {id}, name: {name}, description: {description}, disabled: {disabled}, createdAt: {createdAt}, createdBy: {createdBy}})",
                            ImmutableMap.<String, Object>builder()
                                    .put("id", projectId)
                                    .put("name", rs.getString("NAME"))
                                    .put("description", safeString(rs.getString("DESCRIPTION")))
                                    .put("disabled", rs.getBoolean("DISABLED"))
                                    .put("createdAt", Time.toJavaUtilDate(signature.getTime()))
                                    .put("createdBy", signature.getUser().getName())
                                    .build()
                    );
                }
        );
        return count.get();
    }

    @SuppressWarnings("RedundantCast")
    private int migrateBranches() {
        AtomicInteger count = new AtomicInteger();
        jdbc().query(
                "SELECT * FROM BRANCHES",
                (RowCallbackHandler) rs -> {
                    count.incrementAndGet();
                    int branchId = rs.getInt("ID");
                    int projectId = rs.getInt("PROJECTID");
                    Signature signature = getEventSignature("branch", EventFactory.NEW_BRANCH, branchId);
                    template.query(
                            "MATCH (p:Project {id: {projectId}}) " +
                                    "CREATE (b:Branch {id: {id}, name: {name}, description: {description}, createdAt: {createdAt}, createdBy: {createdBy}, disabled: {disabled}})" +
                                    "-[:BRANCH_OF]->(p)",
                            ImmutableMap.<String, Object>builder()
                                    .put("id", branchId)
                                    .put("projectId", projectId)
                                    .put("name", rs.getString("NAME"))
                                    .put("description", safeString(rs.getString("DESCRIPTION")))
                                    .put("createdAt", Time.toJavaUtilDate(signature.getTime()))
                                    .put("createdBy", signature.getUser().getName())
                                    .put("disabled", rs.getBoolean("DISABLED"))
                                    .build()
                    );
                }
        );
        return count.incrementAndGet();
    }

    @SuppressWarnings("RedundantCast")
    private int migratePromotionLevels() {
        AtomicInteger count = new AtomicInteger();
        jdbc().query(
                "SELECT * FROM PROMOTION_LEVELS",
                (RowCallbackHandler) rs -> {
                    count.incrementAndGet();
                    int promotionLevelId = rs.getInt("ID");
                    Signature signature = getEventSignature("promotion_level", EventFactory.NEW_PROMOTION_LEVEL, promotionLevelId);
                    template.query(
                            "MATCH (b:Branch {id: {branchId}}) " +
                                    "CREATE (pl:PromotionLevel {id: {id}, name: {name}, description: {description}, createdAt: {createdAt}, createdBy: {createdBy}, orderNb: {orderNb}})" +
                                    "-[:PROMOTION_LEVEL_OF]->(b)",
                            ImmutableMap.<String, Object>builder()
                                    .put("id", promotionLevelId)
                                    .put("branchId", rs.getInt("BRANCHID"))
                                    .put("name", rs.getString("NAME"))
                                    .put("description", safeString(rs.getString("DESCRIPTION")))
                                    .put("createdAt", Time.toJavaUtilDate(signature.getTime()))
                                    .put("createdBy", signature.getUser().getName())
                                    .put("orderNb", rs.getInt("ORDERNB"))
                                    // TODO Image type
                                    // TODO Image bytes - in a separate file
                                    .build()
                    );
                }
        );
        return count.get();
    }

    @SuppressWarnings("RedundantCast")
    private int migrateValidationStamps() {
        AtomicInteger count = new AtomicInteger();
        jdbc().query(
                "SELECT * FROM VALIDATION_STAMPS",
                (RowCallbackHandler) rs -> {
                    count.incrementAndGet();
                    int validationStampId = rs.getInt("ID");
                    Signature signature = getEventSignature("validation_stamp", EventFactory.NEW_VALIDATION_STAMP, validationStampId);
                    template.query(
                            "MATCH (b:Branch {id: {branchId}}) " +
                                    "CREATE (vs:ValidationStamp {id: {id}, name: {name}, description: {description}, createdAt: {createdAt}, createdBy: {createdBy}, orderNb: {orderNb}})" +
                                    "-[:VALIDATION_STAMP_OF]->(b)",
                            ImmutableMap.<String, Object>builder()
                                    .put("id", validationStampId)
                                    .put("branchId", rs.getInt("BRANCHID"))
                                    .put("name", rs.getString("NAME"))
                                    .put("description", safeString(rs.getString("DESCRIPTION")))
                                    .put("createdAt", Time.toJavaUtilDate(signature.getTime()))
                                    .put("createdBy", signature.getUser().getName())
                                    .put("orderNb", rs.getInt("ORDERNB"))
                                    // TODO Image type
                                    // TODO Image bytes - in a separate file
                                    .build()
                    );
                }
        );
        return count.get();
    }

    @SuppressWarnings("RedundantCast")
    private int migrateBuilds() {
        AtomicInteger count = new AtomicInteger();
        String limit = getLimit("builds", migrationProperties.getBuildCount());
        jdbc().query(
                String.format(
                        "SELECT * FROM BUILDS ORDER BY ID ASC %s",
                        limit
                ),
                (RowCallbackHandler) rs -> {
                    count.incrementAndGet();
                    template.query(
                            "MATCH (b:Branch {id: {branchId}}) " +
                                    "CREATE (r:Build {id: {id}, name: {name}, description: {description}, createdAt: {createdAt}, createdBy: {createdBy}})" +
                                    "-[:BUILD_OF]->(b)",
                            ImmutableMap.<String, Object>builder()
                                    .put("id", rs.getInt("ID"))
                                    .put("branchId", rs.getInt("BRANCHID"))
                                    .put("name", rs.getString("NAME"))
                                    .put("description", safeString(rs.getString("DESCRIPTION")))
                                    .put("createdAt", Time.toJavaUtilDate(Time.fromStorage(rs.getString("CREATION"))))
                                    .put("createdBy", rs.getString("CREATOR"))
                                    .build()
                    );
                }
        );
        return count.get();
    }

    @SuppressWarnings("RedundantCast")
    private int migrateBuildLinks() {
        // Build links
        AtomicInteger count = new AtomicInteger();
        jdbc().query(
                "SELECT * FROM BUILD_LINKS " + getLimit("build links", migrationProperties.getBuildLinkCount()),
                (RowCallbackHandler) rs -> {
                    count.incrementAndGet();
                    template.query(
                            "MATCH (a: Build {id: {sourceId}}), (b: Build {id: {targetId}}) " +
                                    "MERGE (a)-[:LINKED_TO]->(b)",
                            ImmutableMap.<String, Object>builder()
                                    .put("sourceId", rs.getInt("BUILDID"))
                                    .put("targetId", rs.getInt("TARGETBUILDID"))
                                    .build()
                    );
                });
        // Remove self links
        template.query(
                "MATCH (b:Build)-[r:LINKED_TO]->(b) DELETE r",
                Collections.emptyMap()
        );
        // OK
        return count.get();
    }

    @SuppressWarnings("RedundantCast")
    private int migratePromotionRuns() {
        AtomicInteger count = new AtomicInteger();
        jdbc().query(
                "SELECT * FROM PROMOTION_RUNS ORDER BY ID ASC " + getLimit("promotion runs", migrationProperties.getPromotionRunCount()),
                (RowCallbackHandler) rs -> {
                    count.incrementAndGet();
                    template.query(
                            "MATCH (b:Build {id: {buildId}}),(pl:PromotionLevel {id: {promotionLevelId}}) " +
                                    "CREATE (b)-[:PROMOTED_TO {createdAt: {createdAt}, createdBy: {createdAt}, description: {description}}]->(pl)",
                            ImmutableMap.<String, Object>builder()
                                    .put("buildId", rs.getInt("BUILDID"))
                                    .put("promotionLevelId", rs.getInt("PROMOTIONLEVELID"))
                                    .put("description", safeString(rs.getString("DESCRIPTION")))
                                    .put("createdAt", Time.toJavaUtilDate(Time.fromStorage(rs.getString("CREATION"))))
                                    .put("createdBy", rs.getString("CREATOR"))
                                    .build()
                    );
                }
        );
        return count.get();
    }

    @SuppressWarnings("RedundantCast")
    private int migrateValidationRuns() {
        AtomicInteger count = new AtomicInteger();
        jdbc().query(
                "SELECT * FROM VALIDATION_RUNS ORDER BY ID ASC " + getLimit("validation runs", migrationProperties.getValidationRunCount()),
                (RowCallbackHandler) rs -> {
                    count.incrementAndGet();
                    int runId = rs.getInt("ID");
                    // Validation run statuses
                    List<Map<String, Object>> statuses = getNamedParameterJdbcTemplate().queryForList(
                            "SELECT * FROM VALIDATION_RUN_STATUSES WHERE VALIDATIONRUNID = :runId ORDER BY ID ASC",
                            Collections.singletonMap("runId", runId)
                    );
                    // Initial status
                    Map<String, Object> initial = statuses.get(0);
                    // Validation run node
                    template.query(
                            "MATCH (b:Build {id: {buildId}}),(vs:ValidationStamp {id: {validationStampId}}) " +
                                    "CREATE " +
                                    "   (b)-[:HAS_VALIDATION]->(vr:ValidationRun {id:{validationRunId}, createdAt: {createdAt}, createdBy: {createdAt}})," +
                                    "   (vr)-[:VALIDATION_FOR]->(vs)",
                            ImmutableMap.<String, Object>builder()
                                    .put("buildId", rs.getInt("BUILDID"))
                                    .put("validationStampId", rs.getInt("VALIDATIONSTAMPID"))
                                    .put("validationRunId", runId)
                                    .put("createdAt", Time.toJavaUtilDate(Time.fromStorage((String) initial.get("CREATION"))))
                                    .put("createdBy", initial.get("CREATOR"))
                                    .build()
                    );
                    // Validation run statuses
                    for (Map<String, Object> status : statuses) {
                        template.query(
                                "MATCH (vr: ValidationRun {id: {validationRunId}}) " +
                                        "CREATE (vr)-[:HAS_STATUS]->(vrs:ValidationRunStatus {status: {status}, createdAt: {createdAt}, createdBy: {createdAt}, description: {description}})",
                                ImmutableMap.<String, Object>builder()
                                        .put("validationRunId", runId)
                                        .put("status", status.get("VALIDATIONRUNSTATUSID"))
                                        .put("description", safeString((String) status.get("DESCRIPTION")))
                                        .put("createdAt", Time.toJavaUtilDate(Time.fromStorage((String) status.get("CREATION"))))
                                        .put("createdBy", status.get("CREATOR"))
                                        .build()
                        );
                    }
                }
        );
        return count.get();
    }

    /**
     * ==========================
     * ACL
     * ==========================
     */

    private void migrateACL() {
        // Groups
        migrateGroups();
        // Accounts
        migrateAccounts();
        // Account groups
        migrateAccountGroupLinks();
        // Global permissions for groups
        migrateGlobalGroupPermissions();
        // Global permissions for accounts
        migrateGlobalPermissions();
        // Project permissions for groups
        migrateProjectGroupPermissions();
        // Project permissions for accounts
        migrateProjectPermissions();
        // LDAP mappings
        migrateGroupMappings();
    }

    private void migrateProjectGroupPermissions() {
        jdbc().query("SELECT * FROM GROUP_PROJECT_AUTHORIZATIONS", (RowCallbackHandler) rs ->
                template.query(
                        "MATCH (g: AccountGroup {id: {groupId}}), (p: Project {id: {id}}) " +
                                "MERGE (g)-[:HAS_ROLE {role: {role}}]->(p)",
                        ImmutableMap.<String, Object>builder()
                                .put("id", rs.getInt("PROJECT"))
                                .put("groupId", rs.getInt("ACCOUNTGROUP"))
                                .put("role", rs.getString("ROLE"))
                                .build()
                ));
    }

    private void migrateGlobalGroupPermissions() {
        jdbc().query("SELECT * FROM GROUP_GLOBAL_AUTHORIZATIONS", (RowCallbackHandler) rs ->
                template.query(
                        "MATCH (g: AccountGroup {id: {groupId}}) " +
                                "MERGE (r: GlobalRole {name: {role}}) " +
                                "MERGE (g)-[:HAS_ROLE]->(r)",
                        ImmutableMap.<String, Object>builder()
                                .put("groupId", rs.getInt("ACCOUNTGROUP"))
                                .put("role", rs.getString("ROLE"))
                                .build()
                ));
    }

    private void migrateGlobalPermissions() {
        jdbc().query("SELECT * FROM GLOBAL_AUTHORIZATIONS", (RowCallbackHandler) rs ->
                template.query(
                        "MATCH (a: Account {id: {accountId}}) " +
                                "MERGE (r: GlobalRole {name: {role}}) " +
                                "MERGE (a)-[:HAS_ROLE]->(r)",
                        ImmutableMap.<String, Object>builder()
                                .put("accountId", rs.getInt("ACCOUNT"))
                                .put("role", rs.getString("ROLE"))
                                .build()
                ));
    }

    private void migrateProjectPermissions() {
        jdbc().query("SELECT * FROM PROJECT_AUTHORIZATIONS", (RowCallbackHandler) rs ->
                template.query(
                        "MATCH (a: Account {id: {accountId}}), (p: Project {id: {id}}) " +
                                "MERGE (a)-[:HAS_ROLE {role: {role}}]->(p)",
                        ImmutableMap.<String, Object>builder()
                                .put("id", rs.getInt("PROJECT"))
                                .put("accountId", rs.getInt("ACCOUNT"))
                                .put("role", rs.getString("ROLE"))
                                .build()
                ));
    }

    private void migrateGroupMappings() {
        jdbc().query("SELECT * FROM ACCOUNT_GROUP_MAPPING", (RowCallbackHandler) rs -> template.query(
                String.format(
                        "MATCH (g: AccountGroup {id: {groupId}}) " +
                                "CREATE (m:%sMapping {name: {name}})-[:MAPS_TO]->(g)",
                        StringUtils.capitalize(rs.getString("MAPPING"))
                ),
                ImmutableMap.<String, Object>builder()
                        .put("groupId", rs.getInt("GROUPID"))
                        .put("name", rs.getString("SOURCE"))
                        .build()
        ));
    }

    private void migrateAccountGroupLinks() {
        jdbc().query("SELECT * FROM ACCOUNT_GROUP_LINK", (RowCallbackHandler) rs -> template.query(
                "MATCH (a: Account {id: {accountId}}), (g: AccountGroup {id: {groupId}}) " +
                        "CREATE (a)-[:BELONGS_TO]->(g)",
                ImmutableMap.<String, Object>builder()
                        .put("accountId", rs.getInt("ACCOUNT"))
                        .put("groupId", rs.getInt("ACCOUNTGROUP"))
                        .build()
        ));
    }

    private void migrateAccounts() {
        jdbc().query("SELECT * FROM ACCOUNTS", (RowCallbackHandler) rs -> template.query(
                "CREATE (a:Account {id: {id}, name: {name}, fullName: {fullName}, email: {email}, mode: {mode}, password: {password}, role: {role}, createdAt: {createdAt}, createdBy: {createdBy}})",
                ImmutableMap.<String, Object>builder()
                        .put("id", rs.getInt("ID"))
                        .put("name", rs.getString("NAME"))
                        .put("fullName", rs.getString("FULLNAME"))
                        .put("email", rs.getString("EMAIL"))
                        .put("mode", rs.getString("MODE"))
                        .put("password", rs.getString("PASSWORD"))
                        .put("role", rs.getString("ROLE"))
                        .put("createdAt", Time.toJavaUtilDate(Time.now()))
                        .put("createdBy", MIGRATION_USER)
                        .build()
        ));
    }

    private void migrateGroups() {
        accountGroupRepository.findAll().forEach(accountGroup ->
                template.query(
                        "CREATE (g:AccountGroup {id: {id}, name: {name}, description: {description}})",
                        ImmutableMap.<String, Object>builder()
                                .put("id", accountGroup.id())
                                .put("name", accountGroup.getName())
                                .put("description", accountGroup.getDescription())
                                .put("createdAt", Time.toJavaUtilDate(Time.now()))
                                .put("createdBy", MIGRATION_USER)
                                .build()
                )
        );
    }

    /**
     * ==========================
     * Utility methods
     * ==========================
     */

    private String getLimit(String name, int count) {
        String limit;
        if (count >= 0) {
            logger.warn("Limiting the number of {} to {}", name, count);
            limit = "LIMIT " + count;
        } else {
            limit = "";
        }
        return limit;
    }

    private Signature getEventSignature(String entity, EventType eventType, int entityId) {
        String eventUser;
        LocalDateTime eventTime;
        List<Map<String, Object>> results = getNamedParameterJdbcTemplate().queryForList(
                String.format(
                        "SELECT * FROM EVENTS WHERE %s = :entityId AND EVENT_TYPE = :type ORDER BY ID DESC LIMIT 1",
                        entity
                ),
                ImmutableMap.<String, Object>builder()
                        .put("entityId", entityId)
                        .put("type", eventType.getId())
                        .build()
        );
        if (results.isEmpty()) {
            eventUser = "import";
            eventTime = Time.now();
        } else {
            Map<String, Object> result = results.get(0);
            eventUser = (String) result.get("event_user");
            eventTime = Time.fromStorage((String) result.get("event_time"));
        }
        return Signature.of(eventTime, eventUser);
    }

    private JdbcOperations jdbc() {
        return getNamedParameterJdbcTemplate().getJdbcOperations();
    }

    private String safeString(String description) {
        return description == null ? "" : description;
    }

}
