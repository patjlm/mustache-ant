package com.github.patjlm.ant.mustache;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.filters.TokenFilter.ChainableReaderFilter;
import org.apache.tools.ant.types.RegularExpression;
import org.apache.tools.ant.util.regexp.Regexp;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

/**
 * Provides <a href="http://mustache.github.com/">Mustache</a> templating
 * services for Ant.
 *
 * <p>
 * See README.md for the basic usage within an Ant build script.
 */

public class MustacheFilter extends ChainableReaderFilter {

	/**
	 * Whether to use Ant project properties in the data model
	 */
	private Boolean projectProperties = true;

	/**
	 * Prefix that project properties should have in order to be included in the
	 * data model. By default, no prefix is set, so all project properties may
	 * be included.
	 */
	private String prefix = null;

	/**
	 * If prefix is not null, removePrefix defines if this prefix should be
	 * removed when inserting keys in the data model. Project properties names
	 * remain unchanged
	 */
	private Boolean removePrefix = false;

	/**
	 * the regular expression pattern used to parse boolean property names.
	 */
	private String booleanRegex = "^.+?$";
	// other example of boolean regex: "^is.+?"

	/**
	 * Whether to support list parsing in property names
	 */
	private Boolean supportLists = true;

	/**
	 * when list parsing is enabled, this defines the name of the id to be given
	 * to each element of the list
	 */
	private String listIdName = "__id__";

	/**
	 * the regular expression pattern used to parse list property names. This
	 * pattern should include three groups. The first group is the root key to
	 * access the list. The second group is the id of this item in the list. The
	 * third group is the sub-key to assign the value to.
	 */
	private String listRegex = "(.+?)\\.(\\d+)\\.(.+)";
	// other example of regex: (.+?)\[(\d+)\]\.(.+)

	/**
	 * A file name from which data model properties should be loaded from.
	 * Disabled by default.
	 */
	private String dataFile = null;

	// JMustache settings

	/**
	 * Default value to give to undefined and null keys. By default, undefined
	 * keys cause a failure. See
	 * {@link com.samskivert.mustache.Mustache.Compiler#defaultValue}.
	 */
	private String defaultValue = null;

	/**
	 * Whether sections should be strict. Default is false. See
	 * {@link com.samskivert.mustache.Mustache.Compiler#strictSections}.
	 */
	private boolean strictSections = false;

	/**
	 * Whether HTML output should be escaped. Default is false. See
	 * {@link com.samskivert.mustache.Mustache.Compiler#escapeHTML}.
	 */
	private boolean escapeHTML = false;

	public void setProjectProperties(Boolean projectProperties) {
		this.projectProperties = projectProperties;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void setRemovePrefix(Boolean removePrefix) {
		this.removePrefix = removePrefix;
	}

	public String getBooleanRegex() {
		return booleanRegex;
	}

	public void setBooleanRegex(String booleanRegex) {
		this.booleanRegex = booleanRegex;
	}

	public void setSupportLists(Boolean supportLists) {
		this.supportLists = supportLists;
	}

	public void setListIdName(String listIdName) {
		this.listIdName = listIdName;
	}

	public void setDataFile(String dataFile) {
		this.dataFile = dataFile;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public void setStrictSections(boolean strictSections) {
		this.strictSections = strictSections;
	}

	public void setEscapeHTML(boolean escapeHTML) {
		this.escapeHTML = escapeHTML;
	}

	public void setListRegex(String listRegex) {
		this.listRegex = listRegex;
	}

	/**
	 * The main method to implement the filter. Compiles the input text and
	 * returns the output according to the defined data model
	 */
	public String filter(String text) {
		getProject().log("Mustache Data: " + getData().toString(),
				Project.MSG_DEBUG);
		Template tmpl = Mustache.compiler().defaultValue(defaultValue)
				.strictSections(strictSections).escapeHTML(escapeHTML)
				.compile(text);
		return tmpl.execute(getData());
	}

	private MustacheData _data = null;

	/**
	 * gets the data model, building it from project properties and/or data file
	 * if not already done
	 * 
	 * @return the data model Map
	 */
	private MustacheData getData() {
		if (_data == null) {
			_data = new MustacheData(getProject(), booleanRegex, supportLists, listIdName, listRegex);
			addProjectProperties();
			addSrcFile();
		}
		return _data;
	}

	/**
	 * Add project properties in the data model
	 */
	private void addProjectProperties() {
		if (projectProperties) {
			addProperties(getProject().getProperties(), prefix, removePrefix);
		}
	}

	/**
	 * Add property file content to the data model.
	 */
	private void addSrcFile() {
		if (dataFile != null) {
			Properties props = new Properties();
			try {
				props.load(new FileInputStream(dataFile));
			} catch (IOException e) {
				throw new BuildException(e);
			}
			addProperties(props, null, false);
		}
	}

	/**
	 * Adds a set of properties to the datamodel
	 * 
	 * @param props
	 *            the properties to add
	 * @param prefix
	 *            the prefix that properties should have in order to be
	 *            considered. If null, all properties will be considered.
	 * @param removePrefix
	 *            whether the prefix should be removed in the data model key
	 *            name
	 */
	private void addProperties(Hashtable<?, ?> props, String prefix,
			Boolean removePrefix) {
		Iterator<?> it = props.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			if (prefix == null || key.startsWith(prefix)) {
				Object value = props.get(key);
				if (removePrefix && prefix != null) {
					key = key.substring(prefix.length());
				}
				_data.put(key, value);
			}
		}
	}
}
