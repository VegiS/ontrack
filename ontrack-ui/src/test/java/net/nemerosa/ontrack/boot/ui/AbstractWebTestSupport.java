package net.nemerosa.ontrack.boot.ui;

import net.nemerosa.ontrack.boot.Application;
import net.nemerosa.ontrack.it.AbstractServiceTestSupport;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = Application.class)
public abstract class AbstractWebTestSupport extends AbstractServiceTestSupport {

}
