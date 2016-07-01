package net.nemerosa.ontrack.repository;

import com.fasterxml.jackson.databind.JsonNode;
import net.nemerosa.ontrack.model.Ack;
import net.nemerosa.ontrack.model.structure.ID;
import net.nemerosa.ontrack.model.structure.ProjectEntity;
import net.nemerosa.ontrack.model.structure.ProjectEntityType;
import net.nemerosa.ontrack.repository.support.AbstractJdbcRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Predicate;

@Repository
public class PropertyJdbcRepository extends AbstractJdbcRepository implements PropertyRepository {

    @Autowired
    public PropertyJdbcRepository(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public TProperty loadProperty(String typeName, ProjectEntityType entityType, ID entityId) {
        return getFirstItem(
                String.format(
                        "SELECT * FROM PROPERTIES WHERE TYPE = :type AND %s = :entityId",
                        entityType.name()
                ),
                params("type", typeName).addValue("entityId", entityId.getValue()),
                (rs, rowNum) -> toProperty(rs)
        );
    }

    @Override
    public void saveProperty(String typeName, ProjectEntityType entityType, ID entityId, JsonNode data, String searchKey) {
        MapSqlParameterSource params = params("type", typeName).addValue("entityId", entityId.getValue());
        // Any previous value?
        Integer propertyId = getFirstItem(
                String.format(
                        "SELECT ID FROM PROPERTIES WHERE TYPE = :type AND %s = :entityId",
                        entityType.name()
                ),
                params,
                Integer.class
        );
        // Data parameters
        params
                .addValue("json", writeJson(data))
                .addValue("searchKey", searchKey);
        // Update
        if (propertyId != null) {
            getNamedParameterJdbcTemplate().update(
                    "UPDATE PROPERTIES SET JSON = CAST(:json AS JSONB), SEARCHKEY = :searchKey WHERE ID = :id",
                    params.addValue("id", propertyId)
            );
        }
        // Creation
        else {
            getNamedParameterJdbcTemplate().update(
                    String.format(
                            "INSERT INTO PROPERTIES(TYPE, %s, SEARCHKEY, JSON) " +
                                    "VALUES(:type, :entityId, :searchKey, CAST(:json AS JSONB))",
                            entityType.name()
                    ),
                    params
            );
        }
    }

    @Override
    public Ack deleteProperty(String typeName, ProjectEntityType entityType, ID entityId) {
        return Ack.one(
                getNamedParameterJdbcTemplate().update(
                        String.format(
                                "DELETE FROM PROPERTIES WHERE TYPE = :type AND %s = :entityId",
                                entityType.name()
                        ),
                        params("type", typeName).addValue("entityId", entityId.getValue())
                )
        );
    }

    @Override
    public Collection<ProjectEntity> searchByProperty(String typeName,
                                                      BiFunction<ProjectEntityType, ID, ProjectEntity> entityLoader,
                                                      Predicate<TProperty> predicate) {
        return getNamedParameterJdbcTemplate().execute(
                "SELECT * FROM PROPERTIES WHERE TYPE = :type ORDER BY ID DESC",
                params("type", typeName),
                (PreparedStatement ps) -> {
                    Collection<ProjectEntity> entities = new ArrayList<>();
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        TProperty t = toProperty(rs);
                        if (predicate.test(t)) {
                            entities.add(entityLoader.apply(t.getEntityType(), t.getEntityId()));
                        }
                    }
                    return entities;
                }
        );
    }

    private TProperty toProperty(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String searchKey = rs.getString("searchKey");
        String typeName = rs.getString("type");
        // Detects the entity
        ProjectEntityType entityType = null;
        ID entityId = null;
        for (ProjectEntityType candidate : ProjectEntityType.values()) {
            Integer candidateId = rs.getInt(candidate.name());
            if (!rs.wasNull()) {
                entityType = candidate;
                entityId = ID.of(candidateId);
            }
        }
        // Sanity check
        if (entityType == null || !ID.isDefined(entityId)) {
            throw new IllegalStateException(
                    String.format(
                            "Could not find any entity for property %s with key %s (id = %d)",
                            typeName,
                            searchKey,
                            id
                    )
            );
        }
        // OK
        return new TProperty(
                typeName,
                entityType,
                entityId,
                searchKey,
                readJson(rs, "json")
        );
    }
}
