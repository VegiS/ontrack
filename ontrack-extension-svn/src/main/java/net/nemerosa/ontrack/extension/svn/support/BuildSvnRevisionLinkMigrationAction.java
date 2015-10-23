package net.nemerosa.ontrack.extension.svn.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.nemerosa.ontrack.extension.scm.support.TagPattern;
import net.nemerosa.ontrack.extension.svn.property.SVNBranchConfigurationPropertyType;
import net.nemerosa.ontrack.json.ObjectMapperFactory;
import net.nemerosa.ontrack.model.structure.ServiceConfiguration;
import net.nemerosa.ontrack.model.support.DBMigrationAction;
import net.nemerosa.ontrack.model.support.NoConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Migration of {@link net.nemerosa.ontrack.extension.svn.property.SVNBranchConfigurationProperty}
 * to the use of {@link net.nemerosa.ontrack.extension.svn.model.BuildSvnRevisionLink}.
 */
@Component
public class BuildSvnRevisionLinkMigrationAction implements DBMigrationAction {

    private final ObjectMapper objectMapper = ObjectMapperFactory.create();

    private final RevisionSvnRevisionLink revisionLink;
    private final TagNamePatternSvnRevisionLink tagPatternLink;

    public BuildSvnRevisionLinkMigrationAction(RevisionSvnRevisionLink revisionLink, TagNamePatternSvnRevisionLink tagPatternLink) {
        this.revisionLink = revisionLink;
        this.tagPatternLink = tagPatternLink;
    }

    @Override
    public int getPatch() {
        return 23;
    }

    @Override
    public void migrate(Connection connection) throws Exception {
        // For all Svn branch configurations
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM CONFIGURATIONS WHERE TYPE = ?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) {
            ps.setString(1, SVNBranchConfigurationPropertyType.class.getName());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Configuration as JSON
                    String json = rs.getString("CONTENT");
                    // Parses the configuration as JSON
                    ObjectNode node = (ObjectNode) objectMapper.readTree(json);
                    // Migrates the node
                    migrate(node);
                    // Updating
                    rs.updateString("CONTENT", objectMapper.writeValueAsString(node));
                    rs.updateRow();
                }
            }
        }
    }

    protected void migrate(ObjectNode node) {
        // Gets the build path & branch path
        String buildPath = node.get("buildPath").asText();
        // Removes the build path property
        node.remove("buildPath");
        // Converts to a service configuration
        ConfiguredBuildSvnRevisionLink<?> configuredBuildSvnRevisionLink = toBuildSvnRevisionLinkConfiguration(
                buildPath
        );
        // Gets the configuration representation
        ServiceConfiguration serviceConfiguration = configuredBuildSvnRevisionLink.toServiceConfiguration();
        // As json...
        node.put("buildRevisionLink", (ObjectNode) objectMapper.valueToTree(serviceConfiguration));
    }

    protected ConfiguredBuildSvnRevisionLink<?> toBuildSvnRevisionLinkConfiguration(String buildPath) {
        // Revision based
        if (SVNUtils.isPathRevision(buildPath)) {
            return new ConfiguredBuildSvnRevisionLink<>(
                    revisionLink,
                    NoConfig.INSTANCE
            );
        }
        // Looking for the {build} expression
        Pattern pattern = Pattern.compile(SVNUtils.BUILD_PLACEHOLDER_PATTERN);
        Matcher matcher = pattern.matcher(buildPath);
        if (matcher.find()) {
            String expression = matcher.group(1);
            if ("build".equals(expression)) {
                return TagNameSvnRevisionLink.DEFAULT;
            } else if (StringUtils.startsWith(expression, "build:")) {
                String buildExpression = StringUtils.substringAfter(expression, "build:");
                return new ConfiguredBuildSvnRevisionLink<>(
                        tagPatternLink,
                        new TagPattern(buildExpression)
                );
            }
        }
        // Default
        return TagNameSvnRevisionLink.DEFAULT;
    }

    @Override
    public String getDisplayName() {
        return "Migration of SVN branch configurations to the use of revision links";
    }
}
