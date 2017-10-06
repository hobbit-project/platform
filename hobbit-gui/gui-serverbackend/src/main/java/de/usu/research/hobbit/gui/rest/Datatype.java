/**
 * This file is part of gui-serverbackend.
 * <p>
 * gui-serverbackend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * gui-serverbackend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with gui-serverbackend.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.usu.research.hobbit.gui.rest;

import org.apache.commons.collections.MapUtils;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@XmlType(name = "Datatype")
@XmlEnum
public enum Datatype {
    @XmlEnumValue(value = "xsd:string") STRING,
    @XmlEnumValue(value = "xsd:boolean") BOOLEAN,
    @XmlEnumValue(value = "xsd:decimal") DECIMAL,
    @XmlEnumValue(value = "xsd:integer") INTEGER,
    @XmlEnumValue(value = "xsd:unsignedInt") UNSIGNED_INT,
    @XmlEnumValue(value = "xsd:double") DOUBLE,
    @XmlEnumValue(value = "xsd:float") FLOAT,
    @XmlEnumValue(value = "xsd:dateTime") DATE_TIME;

    private static AtomicReference<Map<String, Datatype>> refMap = new AtomicReference<>();
    private static AtomicReference<Map<Datatype, String>> refMap2 = new AtomicReference<>();

    public static Datatype getDatatype(String uri) {
        getMapFromRef();
        return refMap.get().get(uri);
    }


    public static String getValue(Datatype datatype) {
        getMapFromRef();
        return refMap2.get().get(datatype);
    }

    @SuppressWarnings("unchecked")
    private static void getMapFromRef() {
        Map<String, Datatype> map = refMap.get();
        if (map == null) {
            map = new HashMap<>();
            for (Field f : Datatype.class.getDeclaredFields()) {
                XmlEnumValue annot = f.getAnnotation(XmlEnumValue.class);
                try {
                    Object obj = f.get(null);
                    if (obj instanceof Datatype) {
                        Datatype d = (Datatype) obj;
                        String name = annot.value();
                        map.put(name, d);
                    }
                }
                catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            refMap.set(map);
            Map<Datatype, String> map2 = MapUtils.invertMap(map);
            refMap2.set(map2);
        }
    }

    public static void main(String[] args) {
        System.out.println(Datatype.getDatatype("xsd:decimal"));
    }
}
