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
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.collections.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import de.usu.research.hobbit.gui.rabbitmq.GUIBackendException;
import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClient;
import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClientSingleton;
import de.usu.research.hobbit.gui.rest.beans.InfoBean;
import de.usu.research.hobbit.gui.rest.beans.KeycloakConfigBean;
import de.usu.research.hobbit.gui.rest.beans.SystemBean;
import de.usu.research.hobbit.gui.rest.beans.UserInfoBean;

@Path("internal")
public class InternalResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalResources.class);

    private static volatile KeycloakConfigBean cachedBean;
    private static Cache<String, UserInfoBean> userInfoCache = CacheBuilder.newBuilder().maximumSize(100)
            .expireAfterWrite(1, TimeUnit.HOURS).build();

    @Path("keycloak-config")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getKeycloakConfig(@Context ServletContext servletContext) {
        if (cachedBean != null) {
            return Response.ok(cachedBean).build();
        }

        try (InputStream is = servletContext.getResourceAsStream("/WEB-INF/jetty-web.xml")) {
            KeycloakConfigBean bean = cachedBean;
            if (bean == null) {
                bean = cachedBean = findKeycloakConfig(is);
                LOGGER.info("Keycloak configuration for webapp: " + bean);
            }
            if (bean == null)
                throw new GUIBackendException("Keycloak configuration not found");
            return Response.ok(bean).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(InfoBean.withMessage("Error on retrieving Keycloak configuration: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("user-info")
    @Produces(MediaType.APPLICATION_JSON)
    public Response userInfo(@Context SecurityContext sc) {
        UserInfoBean bean = getUserInfoBean(sc);
        LOGGER.info("User-info: " + bean);
        return Response.ok(bean).build();
    }

    public static UserInfoBean getUserInfoBean(SecurityContext sc) {
        Principal userPrincipal = sc.getUserPrincipal();
        String userPrincipalName = userPrincipal.getName();

        try {
            UserInfoBean result = userInfoCache.get(userPrincipalName, new Callable<UserInfoBean>() {
                @Override
                public UserInfoBean call() throws Exception {
                    UserInfoBean bean = new UserInfoBean();
                    String[] roleNames = Application.getRoleNames(sc);
                    bean.setRoles(Arrays.asList(roleNames));

                    // as Keycloak adapter is not on classpath, get information via
                    // reflection
                    // Security is a KeycloakSecurityContext
                    try {
                        Principal userPrincipal = sc.getUserPrincipal();
                        bean.setUserPrincipalName(userPrincipal.getName());
                        Method getKeycloakSecurityContext = userPrincipal.getClass()
                                .getMethod("getKeycloakSecurityContext");
                        Object ksc = getKeycloakSecurityContext.invoke(userPrincipal);
                        Method getToken = ksc.getClass().getMethod("getToken");
                        Object accessToken = getToken.invoke(ksc);

                        Method getPreferredUsername = accessToken.getClass().getMethod("getPreferredUsername");
                        Object preferredUsername = getPreferredUsername.invoke(accessToken);
                        bean.setPreferredUsername((String) preferredUsername);

                        Method getName = accessToken.getClass().getMethod("getName");
                        Object name = getName.invoke(accessToken);
                        bean.setName((String) name);

                        Method getEmail = accessToken.getClass().getMethod("getEmail");
                        Object email = getEmail.invoke(accessToken);
                        bean.setEmail((String) email);
                    } catch (Exception e) {
                        LOGGER.warn("Name/email fetch failed with: " + e);
                        LOGGER.debug("stacktrace", e);
                    }
                    return bean;
                }
            });
            return result;
        } catch (ExecutionException e) {
            LOGGER.warn("Exception: " + e);
            throw new RuntimeException(e);
        }
    }

    static KeycloakConfigBean findKeycloakConfig(InputStream is)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(is);
        Node node = doc.getDocumentElement();
        Element elem = walk(node);
        if (elem == null) {
            return null;
        }

        KeycloakConfigBean bean = new KeycloakConfigBean();
        Node child = elem.getFirstChild();
        String keycloakUrlUsedByJetty = null;
        while (child != null) {
            if (child instanceof Element && child.getNodeName().equals("Set")) {
                String name = child.getAttributes().getNamedItem("name").getNodeValue();
                String value = extractValue(child);
                switch (name) {
                case "realm":
                    bean.setRealm(value);
                    break;
                case "authServerUrl":
                    keycloakUrlUsedByJetty = value;
                    break;
                case "resource":
                    bean.setClientId(value.replace("REST", "GUI"));
                    break;
                }
            }

            child = child.getNextSibling();
        }
        if (System.getenv().containsKey("KEYCLOAK_AUTH_URL")) {
            bean.setUrl(System.getenv().get("KEYCLOAK_AUTH_URL"));
        } else {
            LOGGER.warn(
                    "Couldn't get the redirect URL which should be used for keycloak. Reusing direct keycloak URL used by jetty.");
            bean.setUrl(keycloakUrlUsedByJetty);
        }
        return bean;
    }

    private static String extractValue(Node child) {
        Node node = child.getFirstChild();
        StringBuilder sb = new StringBuilder();
        while (node != null) {
            if (node instanceof Element) {
                Element elem = (Element) node;
                String tag = elem.getNodeName();
                switch (tag) {
                case "Env": {
                    String name = elem.getAttribute("name");
                    String defValue = elem.getAttribute("default");
                    String value = System.getenv(name);
                    if (value == null) {
                        if (defValue != null)
                            sb.append(defValue);
                    } else {
                        sb.append(value);
                    }
                    break;
                }
                case "SystemProperty": {
                    String name = elem.getAttribute("name");
                    String defValue = elem.getAttribute("default");
                    String value = System.getProperty(name);
                    if (value == null) {
                        if (defValue != null)
                            sb.append(defValue);
                    } else {
                        sb.append(value);
                    }
                    break;
                }
                }
            } else {
                sb.append(node.getTextContent());
            }
            node = node.getNextSibling();
        }
        return sb.toString();
    }

    private static Element walk(Node node) {
        while (node != null) {
            if (node instanceof Element) {
                if (node.getNodeName().equals("New")) {
                    Node acls = node.getAttributes().getNamedItem("class");
                    if (acls != null && acls.getNodeValue()
                            .equals("org.keycloak.representations.adapters.config.AdapterConfig")) {
                        return (Element) node;
                    }
                }
                Node child = node.getFirstChild();
                Element elem = walk(child);
                if (elem != null) {
                    return elem;
                }
            }
            node = node.getNextSibling();
        }
        return null;
    }

    /**
     * Retrieves the set of system URIs that are visible for the given user.
     *
     * @param userInfo
     *            the bean describing the user for which the system URIs should be
     *            returned
     * @return the set of system URIs that are visible for the given user
     */
    @SuppressWarnings("unchecked")
    public static Set<String> getUserSystemIds(UserInfoBean userInfo) {
        String email = userInfo.getEmail();
        // If the user has no mail address (the user account has a wrong configuration
        // or it is the guest user account)
        if ((email == null) || (email.isEmpty())) {
            return SetUtils.EMPTY_SET;
        }
        List<SystemBean> userSystems = PlatformControllerClientSingleton.getInstance()
                .requestSystemsOfUser(userInfo.getEmail());
        // create set of user owned system ids
        String[] sysIds = userSystems.stream().map(s -> s.getId()).toArray(String[]::new);
        Set<String> userOwnedSystemIds = new HashSet<>(Arrays.asList(sysIds));
        LOGGER.info("userSystems={}", userOwnedSystemIds.toString());
        return userOwnedSystemIds;
    }

    /**
     * Retrieves the set of system URIs that are visible for the given user.
     *
     * @param sc
     *            the security context containing the user for which the system URIs
     *            should be returned
     * @return the set of system URIs that are visible for the given user
     */
    public static Set<String> getUserSystemIds(SecurityContext sc) {
        UserInfoBean userInfo = getUserInfoBean(sc);
        Set<String> userOwnedSystemIds = getUserSystemIds(userInfo);
        return userOwnedSystemIds;
    }

    public static List<SystemBean> getUserSystemBeans(SecurityContext sc) {
        UserInfoBean userInfo = InternalResources.getUserInfoBean(sc);
        PlatformControllerClient client = PlatformControllerClientSingleton.getInstance();
        if (client != null) {
            return client.requestSystemsOfUser(userInfo.getEmail());
        } else {
            LOGGER.error("Couldn't get platform controller client. Returning empty list.");
            return new ArrayList<>(0);
        }
    }
}
