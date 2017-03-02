package de.usu.research.hobbit.gui.util;

import java.time.OffsetDateTime;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class OffsetDateTimeAdapter extends XmlAdapter<String, OffsetDateTime> {
	public OffsetDateTime unmarshal(String v) throws Exception {
		return OffsetDateTime.parse(v);
	}

	public String marshal(OffsetDateTime v) throws Exception {
		return v.toString();
	}
}