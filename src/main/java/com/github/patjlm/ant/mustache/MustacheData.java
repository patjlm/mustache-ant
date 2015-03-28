package com.github.patjlm.ant.mustache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.RegularExpression;
import org.apache.tools.ant.util.regexp.Regexp;

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
	
	private Project project;

	public MustacheData(Project project, String booleanRegexPattern, Boolean supportLists, String listIdName, String listRegexPattern) {
		this(project, asAntRegexp(booleanRegexPattern, project), supportLists, listIdName, asAntRegexp(listRegexPattern, project));
	}

	public MustacheData(Project project, Regexp booleanRegexp, Boolean supportLists, String listIdName, Regexp listRegexp) {
		super();
		this.project = project;
		this.booleanRegexp = booleanRegexp;
		this.supportLists = supportLists;
		this.listIdName = listIdName;
		this.listRegexp = listRegexp;
	}

	@Override
	public Object put(String key, Object value) {
		if (supportLists && listRegexp.matches(key)) {
			ListKeyParser parser = new ListKeyParser(key);
			addList(parser.rootKey, parser.id, parser.subKey, value);
		}
		return super.put(key, computeValue(key, value));
	}

	/**
	 * Adds or updates a list into the specified Map, creating the necessary
	 * List and Map objects. This method is recursive: it calls itself to add
	 * child elements to the data model
	 * 
	 * @param data
	 *            the map to insert or update the list into
	 * @param rootKey
	 *            the list name
	 * @param id
	 *            the id of the element being inserted in the list
	 * @param subKey
	 *            the key of the value to put in the list element
	 * @param value
	 *            the value to put in the list element
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
			foundData = new MustacheData(project, booleanRegexp, supportLists, listIdName, listRegexp);
			foundData.put(listIdName, id);
			listContext.add(foundData);
			Collections.sort(listContext,
					new Comparator<MustacheData>() {
						public int compare(MustacheData m1, MustacheData m2) {
							return ((String) m1.get(listIdName)).compareTo((String) m2.get(listIdName));
						}
					});
		}
		foundData.put(subKey, value);
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
	 *            the key to evaluate, which should match the boolean pattern in
	 *            order to be treated as a boolean
	 * @param value
	 *            the value to translate into a boolean if needed
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
		 *            the key to parse
		 */
		public ListKeyParser(String key) {
			Vector<?> groups = listRegexp.getGroups(key);
			rootKey = (String) groups.get(1);
			id = (String) groups.get(2);
			subKey = (String) groups.get(3);
		}
	}

}
