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

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.json.Json;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.persistence.jaxb.JAXBContextProperties;
import org.eclipse.persistence.oxm.json.JsonObjectBuilderResult;

import de.usu.research.hobbit.gui.rest.beans.BenchmarkBean;
import de.usu.research.hobbit.gui.rest.beans.BenchmarkListBean;
import de.usu.research.hobbit.gui.rest.beans.ChallengeBean;
import de.usu.research.hobbit.gui.rest.beans.ChallengeTaskBean;
import de.usu.research.hobbit.gui.rest.beans.ChallengesListBean;
import de.usu.research.hobbit.gui.rest.beans.ConfigurationParamBean;
import de.usu.research.hobbit.gui.rest.beans.ExperimentBean;
import de.usu.research.hobbit.gui.rest.beans.ExperimentsListBean;
import de.usu.research.hobbit.gui.rest.beans.SystemBean;

public class MarshallerUtil {
    public static final AtomicReference<JAXBContext> ref = new AtomicReference<>();

    public static String marshallObject(Object obj) throws JAXBException {
        JsonObjectBuilderResult result = new JsonObjectBuilderResult();
        getJc().createMarshaller().marshal(obj, result);

        Map<String, Object> jsonProperties = new HashMap<String, Object>();
        jsonProperties.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory writerFactory = Json.createWriterFactory(jsonProperties);
        StringWriter strWriter = new StringWriter();
        JsonWriter writer = writerFactory.createWriter(strWriter);
        writer.writeObject(result.getJsonObjectBuilder().build());
        writer.close();
        return strWriter.toString();
    }

    public static <T> JAXBElement<T> unmarshall(InputStream in, Class<T> cls) throws JAXBException {
        Unmarshaller un = getJc().createUnmarshaller();
        return un.unmarshal(new StreamSource(in), cls);
    }

    public static <T> JAXBElement<T> unmarshall(String json, Class<T> cls) throws JAXBException {
        return getJc().createUnmarshaller().unmarshal(new StreamSource(new StringReader(json)), cls);
    }

    private synchronized static JAXBContext getJc() {
        JAXBContext jc = ref.get();
        if (jc == null) {
            jc = createJAXBContext();
            ref.set(jc);
        }
        return jc;
    }

    private static JAXBContext createJAXBContext() {
        Map<String, Object> jaxbProperties = new HashMap<String, Object>();
        jaxbProperties.put(JAXBContextProperties.MEDIA_TYPE, "application/json");
        jaxbProperties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, false);
        JAXBContext jc;
        try {
            jc = JAXBContext.newInstance(
                    new Class[] { BenchmarkBean.class, SystemBean.class, ConfigurationParamBean.class, BenchmarkListBean.class,
                        ChallengesListBean.class, ChallengeBean.class, ChallengeTaskBean.class,
                        ExperimentBean.class, ExperimentsListBean.class}, jaxbProperties);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        return jc;
    }

    public static void main(String[] args) throws JAXBException {
        BenchmarkBean b1 = new BenchmarkBean("b1", "benchmark 1", "some description about benchmark 1");
        b1.setConfigurationParams(Arrays.asList(new ConfigurationParamBean("i1", Datatype.INTEGER)));
        String ss = marshallObject(b1);
        System.out.println(ss);
        InputStream in = MarshallerUtil.class.getResourceAsStream("/sample/benchmarks.json");
        JAXBElement<BenchmarkListBean> obj = unmarshall(in, BenchmarkListBean.class);
        BenchmarkListBean list = obj.getValue();
        System.out.println(list);

        InputStream in2 = MarshallerUtil.class.getResourceAsStream("/sample/challenges.json");
        JAXBElement<ChallengesListBean> obj2 = unmarshall(in2, ChallengesListBean.class);
        ChallengesListBean list2 = obj2.getValue();
        System.out.println(list2);
    }
}
