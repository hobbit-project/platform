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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.stream.Stream;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.SecurityContext;
import javax.xml.bind.JAXBContext;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created on 28/10/2016.
 */
@ApplicationPath("rest")
public class Application extends ResourceConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    /**
     * see web.xml: there is cuurently no API to get these declared name
     * dynamically... {@code
     <security-role>
     <role-name>system-provider</role-name>
     </security-role>
     <security-role>
     <role-name>guest</role-name>
     </security-role>
     <security-role>
     <role-name>challenge-organiser</role-name>
     </security-role>}
     */

    public static final String[] ROLE_NAMES = new String[] { "benchmark-provider", "system-provider",
            "challenge-organiser", "guest" };

    public static final String PROPERTIES_FILE = "config.properties";
    private static Properties properties = new Properties();
    private volatile static Boolean isUsingDevDb = null;

    private Properties readProperties() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE);
        if (inputStream != null) {
            try {
                properties.load(inputStream);
            } catch (IOException e) {
                throw new RuntimeException("Cannot load " + PROPERTIES_FILE, e);
            }
        }
        return properties;
    }

    public Application() {
        System.setProperty(JAXBContext.JAXB_CONTEXT_FACTORY, "org.eclipse.persistence.jaxb.JAXBContextFactory");
        packages("de.usu.research.hobbit.gui.rest", "de.usu.research.hobbit.gui.rest.securedexamples");
        // enable security-annotations, see
        // https://jersey.java.net/documentation/latest/security.html#annotation-based-security
        register(RolesAllowedDynamicFeature.class);

        properties = readProperties();

        if (isUsingDevDb()) {
            LOGGER.info("Using in-memory database for GUI development");
        }
    }

    /**
     * @return true if in memory database should be used (for GUI development
     *         only)
     */
    public static boolean isUsingDevDb() {
        if (isUsingDevDb == null) {
            isUsingDevDb = Boolean.valueOf(properties.getProperty("useDevDb", "false"));
        }
        return isUsingDevDb;
    }

    public static String[] getRoleNames(SecurityContext sc) {
        Stream<String> stringStream = Stream.of(ROLE_NAMES);
        return stringStream.filter(x -> sc.isUserInRole(x)).toArray(String[]::new);
    }
}
