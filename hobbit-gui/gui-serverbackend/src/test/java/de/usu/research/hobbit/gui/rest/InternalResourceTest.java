package de.usu.research.hobbit.gui.rest;

import java.io.FileInputStream;
import java.io.InputStream;

import static org.junit.Assert.*;
import org.junit.Test;

import de.usu.research.hobbit.gui.rest.beans.KeycloakConfigBean;

public class InternalResourceTest {

  @Test
  public void testLoadConfig() throws Exception {
      try (InputStream is = new FileInputStream("src/main/webapp/WEB-INF/jetty-web.xml")) {
        KeycloakConfigBean bean = InternalResources.findKeycloakConfig(is);
        assertNotNull(bean);
        assertNotNull(bean.getClientId());
        assertNotNull(bean.getUrl());
        assertTrue(bean.getUrl().length() > 0);
        assertNotNull(bean.getRealm());
        assertEquals("Hobbit", bean.getRealm());
      }
  }

}
