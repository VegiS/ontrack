package net.nemerosa.ontrack.ui.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.nemerosa.ontrack.json.ObjectMapperFactory;
import net.nemerosa.ontrack.test.TestUtils;
import org.junit.Test;

import java.net.URI;

import static net.nemerosa.ontrack.json.JsonUtils.object;
import static org.junit.Assert.assertEquals;

public class ResourceTest {

    @Test
    public void resource_to_json() throws JsonProcessingException {
        Dummy info = new Dummy("1.0.0");
        Resource<Dummy> resource = Resource.of(info, URI.create("http://host/dummy")).with("connectors", URI.create("http://host/dummy/test"));
        ObjectMapper mapper = ObjectMapperFactory.create();
        JsonNode node = mapper.valueToTree(resource);
        TestUtils.assertJsonEquals(
                object()
                        .with("version", "1.0.0")
                        .with("href", "http://host/dummy")
                        .with("connectors", object()
                                .with("href", "http://host/dummy/test")
                                .end())
                        .end(),
                node
        );
    }

    @Test(expected = NullPointerException.class)
    public void resource_not_null() {
        Resource.<String>of(null, URI.create(""));
    }

    @Test
    public void container_first() {
        assertEquals(String.class, Resource.of("Test", URI.create("")).getType());
    }

}
