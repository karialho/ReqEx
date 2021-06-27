package fi.improveit.req_ex;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import com.starbase.caliber.User;
import com.starbase.caliber.attribute.Attribute;
import com.starbase.caliber.attribute.AttributeValue;
import com.starbase.caliber.attribute.UDABooleanValue;
import com.starbase.caliber.attribute.UDADateValue;
import com.starbase.caliber.attribute.UDAFloatValue;
import com.starbase.caliber.attribute.UDAIntegerValue;
import com.starbase.caliber.attribute.UDAListValue;
import com.starbase.caliber.attribute.UDATextValue;
import com.starbase.caliber.server.RemoteServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class is a friendly wrapper for a custom attribute value

public class RValue {
	private static final Logger logger = LoggerFactory.getLogger(RValue.class.getName());
	private final AttributeValue v;

	public RValue(AttributeValue value) {
		v = value;
	}

	public Attribute getAttribute() throws RemoteServerException {
		return v.getAttribute();
	}

	public String getValue() throws RemoteServerException {
		if (v instanceof UDATextValue) {
			return ((UDATextValue) v).getValue();
		} else if (v instanceof UDABooleanValue) {
			return (((UDABooleanValue) v).getValue() ? "true" : "false");
		} else if (v instanceof UDAIntegerValue) {
			return Integer.toString(((UDAIntegerValue) v).getValue());
		} else if (v instanceof UDAFloatValue) {
			return Float.toString(((UDAFloatValue) v).getValue());
		} else if (v instanceof UDADateValue) {
			TimeZone tz = TimeZone.getTimeZone("UTC");
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
			df.setTimeZone(tz);
			return df.format(((UDADateValue) v).getValue());
		} else if (v instanceof UDAListValue) {
			Object sv = ((UDAListValue) v).getSelectedValue();
			logger.info("UDAListValue: {}", sv);
			if (sv == null)
				return null;
			else if (sv instanceof User)
				return ((User) sv).getFirstName() + " " + ((User) sv).getLastName();
			else
				return sv.toString();
		} else
			throw new RemoteServerException();
	}

	// Collect values from a multi-valued list attribute into a String table
	public String[] getValues() {
		assert(v instanceof UDAListValue);
		Object[] values = ((UDAListValue) v).getSelectedObjects();
		String[] s = new String[values.length];
		int i = 0;
		for (Object v : values) {
			if (v == null)
				s[i] = null;
			else if (v instanceof User)
				s[i] = ((User) v).getFirstName() + " " + ((User) v).getLastName();
			else
				s[i] = v.toString();
			logger.info("Added multivalued attribute value #{}: {}", i, s[i]);
			i++;
		}
		return s;
	}

}
