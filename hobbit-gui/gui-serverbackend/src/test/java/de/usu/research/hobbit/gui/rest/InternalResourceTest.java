/**
 * This file is part of gui-serverbackend.
 *
 * gui-serverbackend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gui-serverbackend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with gui-serverbackend.  If not, see <http://www.gnu.org/licenses/>.
 */
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
