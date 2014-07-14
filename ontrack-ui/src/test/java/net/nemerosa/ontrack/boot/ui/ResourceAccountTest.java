package net.nemerosa.ontrack.boot.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.nemerosa.ontrack.model.security.Account;
import net.nemerosa.ontrack.model.security.ConnectedAccount;
import net.nemerosa.ontrack.model.security.SecurityRole;
import net.nemerosa.ontrack.model.structure.ID;
import net.nemerosa.ontrack.ui.resource.Resource;
import org.junit.Test;

import java.net.URI;

import static net.nemerosa.ontrack.json.JsonUtils.array;
import static net.nemerosa.ontrack.json.JsonUtils.object;
import static net.nemerosa.ontrack.test.TestUtils.assertJsonWrite;

public class ResourceAccountTest {

    @Test
    public void logged_to_json() throws JsonProcessingException {
        assertJsonWrite(
                object()
                        .with("_self", "urn:user")
                        .with("account", object()
                                .with("id", 1)
                                .with("name", "admin")
                                .with("fullName", "Administrator")
                                .with("email", "")
                                .with("role", "ADMINISTRATOR")
                                .with("accountGroup", (String) null)
                                .end())
                        .with("actions", array().end())
                        .with("logged", true)
                        .end(),
                Resource.of(
                        ConnectedAccount.of(
                                Account.of("admin", "Administrator", "", SecurityRole.ADMINISTRATOR)
                                        .withId(ID.of(1))
                                        .lock()
                        ),
                        URI.create("urn:user")
                )
        );
    }

    @Test
    public void not_logged_to_json() throws JsonProcessingException {
        assertJsonWrite(
                object()
                        .with("_self", "urn:user")
                        .with("account", (String) null)
                        .with("actions", array().end())
                        .with("logged", false)
                        .end(),
                Resource.of(
                        ConnectedAccount.none(),
                        URI.create("urn:user")
                )
        );
    }

}
