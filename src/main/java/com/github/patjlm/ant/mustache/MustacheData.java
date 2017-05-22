package com.github.patjlm.ant.mustache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.RegularExpression;
import org.apache.tools.ant.util.regexp.Regexp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MustacheData extends HashMap<String, Object> {
	/**
	 * Whether to support list parsing in property names
	 */
	private Boolean supportLists;

	/**
	 * when list parsing is enabled, this defines the name of the id to be given
	 * to each element of the list
	 */
	private String listIdName;

	/**
	 * the regular expression pattern used to parse list property names. This
	 * pattern should include three groups. The first group is the root key to
	 * access the list. The second group is the id of this item in the list. The
	 * third group is the sub-key to assign the value to.
	 */
	private Regexp listRegexp;

	/**
	 * the regular expression pattern used to parse boolean property names.
	 */
	private Regexp booleanRegexp;

	/**
	 * the regular expression pattern used to parse property names having a JSON
	 * value.
	 */
	private Regexp jsonValueRegex;

	private Project project;

	public MustacheData(Project project, String booleanRegexPattern, Boolean supportLists, String listIdName, String listRegexPattern, String jsonValueRegexPattern) {
		this(project,
		     asAntRegexp(booleanRegexPattern, project),
		     supportLists,
		     listIdName,
		     asAntRegexp(listRegexPattern, project),
		     asAntRegexp(jsonValueRegexPattern, project));
	}

	public MustacheData(Project project, Regexp booleanRegexp, Boolean supportLists, String listIdName, Regexp listRegexp, Regexp jsonValueRegex) {
		super();
		this.project = project;
		this.booleanRegexp = booleanRegexp;
		this.supportLists = supportLists;
		this.listIdName = listIdName;
		this.listRegexp = listRegexp;
		this.jsonValueRegex = jsonValueRegex;
	}

	@Override
	public Object put(String key, Object value) {
		if (supportLists && listRegexp.matches(key)) {
			ListKeyParser parser = new ListKeyParser(key);
			addList(parser.rootKey, parser.id, parser.subKey, value);
		} else if (jsonValueRegex.matches(key)) {
			Vector<?> groups = jsonValueRegex.getGroups(key);
			key = (String) groups.get(1);
			addJsonMapValue(key, value);
		}

		return super.put(key, computeValue(key, value));
	}

	/**
	 * Adds a set of properties to the datamodel
	 *
	 * @param props
	 *                the properties to add
	 * @param prefix
	 *                the prefix that properties should have in order to be
	 *                considered. If null, all properties will be considered.
	 * @param removePrefix
	 *                whether the prefix should be removed in the data model key
	 *                name
	 */
	public void addProperties(Hashtable<?, ?> props, String prefix, Boolean removePrefix) {
		Iterator<?> it = props.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			if (prefix == null || key.startsWith(prefix)) {
				Object value = props.get(key);
				if (removePrefix && prefix != null) {
					key = key.substring(prefix.length());
				}

				put(key, value);
			}
		}
	}

	/**
	 * Adds or updates a list into the specified Map, creating the necessary
	 * List and Map objects. This method is recursive: it calls itself to add
	 * child elements to the data model
	 *
	 * @param data
	 *                the map to insert or update the list into
	 * @param rootKey
	 *                the list name
	 * @param id
	 *                the id of the element being inserted in the list
	 * @param subKey
	 *                the key of the value to put in the list element
	 * @param value
	 *                the value to put in the list element
	 */
	private void addList(String rootKey, String id, String subKey, Object value) {
		List<MustacheData> listContext = (List<MustacheData>) get(rootKey);
		if (listContext == null) {
			listContext = new ArrayList<MustacheData>();
			put(rootKey, listContext);
		}

		MustacheData foundData = null;
		for (MustacheData subData : listContext) {
			if (id.equals(subData.get(listIdName))) {
				foundData = subData;
				break;
			}
		}
		if (foundData == null) {
			foundData = new MustacheData(project, booleanRegexp, supportLists, listIdName, listRegexp, jsonValueRegex);
			foundData.put(listIdName, id);
			listContext.add(foundData);
			Collections.sort(listContext, new Comparator<MustacheData>() {
				@Override
				public int compare(MustacheData m1, MustacheData m2) {
					return ((String) m1.get(listIdName)).compareTo((String) m2.get(listIdName));
				}
			});
		}
		foundData.put(subKey, value);
	}

	/**
	 * Parses JSON value as Map and adds entries into the data model using the
	 * specified key as prefix.
	 *
	 * Note: for the moment the implementation only supports simple map value:
	 * <code>
	 * {
	   	"k1": "v1",
	   	"k2": "v2",
	   	"k3": "v3"
	   }
	   </code>
	 *
	 * @param parameterName
	 * @param value
	 */
	private void addJsonMapValue(String parameterName, Object jsonValue) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			Map<String, String> valueMap = objectMapper.readValue(jsonValue.toString(), new TypeReference<HashMap<String, String>>() {
			});

			for (Map.Entry<String, String> subKeyValue : valueMap.entrySet()) {
				put(subKeyValue.getKey(), subKeyValue.getValue());
			}
		} catch (IOException e) {
			throw new RuntimeException("Unable to parse Json value: " + jsonValue + ". Cause: " + e.getMessage(), e);
		}
	}

	/**
	 * get an Ant regexp from the given standard regex pattern
	 *
	 * @return the ant regexp
	 */
	private static Regexp asAntRegexp(String regexPattern, Project project) {
		RegularExpression regularExpression = new RegularExpression();
		regularExpression.setPattern(regexPattern);
		return regularExpression.getRegexp(project);
	}

	/**
	 * computes the value into a Boolean if needed
	 *
	 * @param key
	 *                the key to evaluate, which should match the boolean
	 *                pattern in order to be treated as a boolean
	 * @param value
	 *                the value to translate into a boolean if needed
	 * @return either a corresponding boolean or the value itself
	 */
	private Object computeValue(String key, Object value) {
		if (booleanRegexp.matches(key) && "false".equals(value)) {
			return false;
		}
		return value;
	}

	/**
	 * This class splits the key string provided to its constructor into the
	 * root key, the id and the sub key of a list item
	 *
	 * @author Patrick
	 *
	 */
	private class ListKeyParser {
		/**
		 * the list name
		 */
		public String rootKey = null;

		/**
		 * the id of the list element
		 */
		public String id = null;

		/**
		 * the key to put into the list element
		 */
		public String subKey = null;

		/**
		 * constructor: parses the key into root key, id and sub-key
		 *
		 * @param key
		 *                the key to parse
		 */
		public ListKeyParser(String key) {
			Vector<?> groups = listRegexp.getGroups(key);
			rootKey = (String) groups.get(1);
			id = (String) groups.get(2);
			subKey = (String) groups.get(3);
		}
	}

}
