package de.usu.research.hobbit.gui.rest;

import java.io.FileInputStream;
import java.io.InputStream;

import static org.junit.Assert.*;
import org.junit.Test;

public class InternalResourceTest {

  @Test
  public void testLoadConfig() throws Exception {
      try (InputStream is = new FileInputStream("src/main/webapp/WEB-INF/jetty-web.xml")) {
        KeycloakConfigBean bean = InternalResources.findKeycloakConfig(is);
        assertNotNull(bean);
        assertNotNull(bean.clientId);
        assertNotNull(bean.url);
        assertTrue(bean.url.length() > 0);
        assertNotNull(bean.realm);
        assertEquals("Hobbit", bean.realm);
      }
  }

}
