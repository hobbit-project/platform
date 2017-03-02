package de.usu.research.hobbit.gui.rest;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.collections.MapUtils;

@XmlType(name = "Datatype")
@XmlEnum
public enum Datatype {
  @XmlEnumValue(value = "xsd:string")STRING,
  @XmlEnumValue(value = "xsd:boolean")BOOLEAN,
  @XmlEnumValue(value = "xsd:decimal")DECIMAL,
  @XmlEnumValue(value = "xsd:integer")INTEGER,
  @XmlEnumValue(value = "xsd:unsignedInt")UNSIGNED_INT,
  @XmlEnumValue(value = "xsd:double")DOUBLE,
  @XmlEnumValue(value = "xsd:float")FLOAT,
  @XmlEnumValue(value = "xsd:dateTime")DATE_TIME;

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
        } catch (IllegalArgumentException | IllegalAccessException e) {
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
