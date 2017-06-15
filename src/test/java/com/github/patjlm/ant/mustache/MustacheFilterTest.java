//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.github.patjlm.ant.mustache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.tools.ant.Project;
import org.junit.Test;

import com.samskivert.mustache.MustacheException;

/**
 * Various unit tests.
 */
public class MustacheFilterTest {

	@Test
	public void testSimpleVariable() {
		test(new MustacheFilter(), "{{foo}}", "bar", context("foo", "bar"));
	}

	@Test
	public void testPrefixRemoved() {
		test(getFilter("myprefix.", true), "{{foo}}", "bar", context("myprefix.foo", "bar", "foo", "IGNORED"));
	}

	@Test
	public void testBooleanTrue() {
		test(new MustacheFilter(), "{{#foo?}}foo? is True{{/foo?}}{{^foo?}}foo? is False{{/foo?}}", "foo? is True",
				context("foo?", "true"));
	}

	@Test
	public void testBooleanFalse() {
		test(new MustacheFilter(), "{{#foo?}}foo? is True{{/foo?}}{{^foo?}}foo? is False{{/foo?}}", "foo? is False",
				context("foo?", "false"));
	}

	@Test
	public void testBooleanAny() {
		test(new MustacheFilter(), "{{#foo?}}foo? is {{foo?}}{{/foo?}}{{^foo?}}foo? is False{{/foo?}}",
				"foo? is MyValue", context("foo?", "MyValue"));
	}

	@Test
	public void testBooleanPattern() {
		MustacheFilter m = new MustacheFilter();
		m.setBooleanRegex("^is.+?$");
		test(m, "{{#isFoo?}}isFoo? is True{{/isFoo?}}{{^isFoo?}}isFoo? is False{{/isFoo?}}", "isFoo? is False",
				context("isFoo?", "false"));
	}

	@Test
	public void testEmptyString() {
		String template = "{{#myproperty}}myproperty exists and should not be empty (value={{myproperty}}){{/myproperty}}\n{{^myproperty}}myproperty does not exist or is empty{{/myproperty}}";
		testEmptyString(false, false, template, "\nmyproperty does not exist or is empty", context());
		testEmptyString(false, true, template, "\nmyproperty does not exist or is empty", context());

		try {
			testEmptyString(true, false, template, null, context());
			fail("Expected MustacheException to be raised");
		} catch (MustacheException e) {
		}
		try {
			testEmptyString(true, true, template, null, context());
			fail("Expected MustacheException to be raised");
		} catch (MustacheException e) {
		}

		// The difference can be seen on this test case
		// if the property value is empty, it is considered as not set
		testEmptyString(false, false, template, "myproperty exists and should not be empty (value=)\n",
				context("myproperty", ""));
		testEmptyString(false, true, template, "\nmyproperty does not exist or is empty", context("myproperty", ""));
		testEmptyString(true, false, template, "myproperty exists and should not be empty (value=)\n",
				context("myproperty", ""));
		testEmptyString(true, true, template, "\nmyproperty does not exist or is empty", context("myproperty", ""));

		// This case should not happen with property files since values are
		// trimmed
		testEmptyString(false, false, template, "myproperty exists and should not be empty (value= )\n",
				context("myproperty", " "));
		testEmptyString(false, true, template, "myproperty exists and should not be empty (value= )\n",
				context("myproperty", " "));
		testEmptyString(true, false, template, "myproperty exists and should not be empty (value= )\n",
				context("myproperty", " "));
		testEmptyString(true, true, template, "myproperty exists and should not be empty (value= )\n",
				context("myproperty", " "));

		testEmptyString(false, false, template, "myproperty exists and should not be empty (value=1)\n",
				context("myproperty", "1"));
		testEmptyString(false, true, template, "myproperty exists and should not be empty (value=1)\n",
				context("myproperty", "1"));
		testEmptyString(true, false, template, "myproperty exists and should not be empty (value=1)\n",
				context("myproperty", "1"));
		testEmptyString(true, true, template, "myproperty exists and should not be empty (value=1)\n",
				context("myproperty", "1"));
	}

	private void testEmptyString(boolean strictSections, boolean emptyStringIsFalse, String template, String expected,
			Map<String, String> context) {
		MustacheFilter m = new MustacheFilter();
		m.setStrictSections(strictSections);
		m.setEmptyStringIsFalse(emptyStringIsFalse);
		test(m, template, expected, context);
	}

	@Test
	public void testList() {
		test(new MustacheFilter(), "{{#mylist}}{{__id__}}: {{p1}}-{{p2}}\n{{/mylist}}", "1: 1.1-1.2\n2: 2.1-2.2\n",
				context("mylist.1.p1", "1.1", "mylist.1.p2", "1.2", "mylist.2.p1", "2.1", "mylist.2.p2", "2.2"));
	}

	@Test
	public void testListUsingAlphanumericId() {
		MustacheFilter m = new MustacheFilter();
		m.setListRegex("(.+?)\\[([\\w\\.]+)\\]\\.(.+)");
		test(m, "{{#mylist}}{{__id__}}: {{p1}}-{{p2}}\n{{/mylist}}", "a.t: a.1-a.2\nb.u: b.1-b.2\n", context(
				"mylist[a.t].p1", "a.1", "mylist[a.t].p2", "a.2", "mylist[b.u].p1", "b.1", "mylist[b.u].p2", "b.2"));
	}

	@Test
	public void testJsonValue() {
		test(new MustacheFilter(), "{{#myproperty}}myproperty exists, p1={{p1}}, p2={{p2}}{{/myproperty}}",
				"myproperty exists, p1=a.1, p2=a.2",
				context("myproperty@JSON", "{\"p1\" : \"a.1\", \"p2\" : \"a.2\" }"));
	}

	@Test
	public void testListAndJsonValue() {
		test(new MustacheFilter(),
				"mylist={{mylist}}\n\n" + "Loop: {{#mylist}}{{__id__}}.prop1 = {{value.prop1}}; "
						+ "{{__id__}}.prop2 = {{value.prop2}}\n{{/mylist}}",
				"mylist=[{value={prop2=value-1-2, prop1=value-1-1}, __id__=01}, "
						+ "{value={prop2=value-2-2, prop1=value-2-1}, __id__=02}]\n\n"
						+ "Loop: 01.prop1 = value-1-1; 01.prop2 = value-1-2\n"
						+ "02.prop1 = value-2-1; 02.prop2 = value-2-2\n",
				context("mylist.01.value@JSON", "{\"prop1\" : \"value-1-1\", \"prop2\" : \"value-1-2\" }",
						"mylist.02.value@JSON", "{\"prop1\" : \"value-2-1\", \"prop2\" : \"value-2-2\" }"));
	}

	@Test
	public void testListAndJsonComplexValue() {
		MustacheFilter m = new MustacheFilter();
		m.setListRegex("(.+?)\\[([\\d\\.]+)\\]\\.(.+)");
		test(m, "{{#mylist}}" + "{" + "\"key\"=\"{{__id__}}\", " + "\"msg\"=\"{{value.msg}}\", "
				+ "\"simple\"=\"{{value.simple}}\", " + "\"ar\"=\"{{value.ar}}\"" + "}" +
				// add a comma and a space only if it's not the last item in the
				// list
				"{{^-last}}, {{/-last}}" + "{{/mylist}}",

				"{\"key\"=\"01\", \"msg\"=\"hello\", \"simple\"=\"true\", \"ar\"=\"a1\"}, "
						+ "{\"key\"=\"02\", \"msg\"=\"world\", \"simple\"=\"20\", \"ar\"=\"[\"a1\"]\"}, "
						+ "{\"key\"=\"03\", \"msg\"=\"two words\", \"simple\"=\"true\", \"ar\"=\"[\"a1\", \"a2\"]\"}, "

						// Note: using complex JSON with sub-levels, the double
						// quotes disappear around the internal keys (sub-simple
						// and sub-ar)... to be checked how to fix this
						+ "{\"key\"=\"04\", \"msg\"=\"recursive json\", \"simple\"=\"false\", \"ar\"=\"[{sub-simple=false, sub-ar=[\"a1\", \"a2\"]}, {sub-simple=true, sub-ar=[\"b1\", \"b2\"]}]\"}",

				context("mylist[01].value@JSON", "{\"msg\" : \"hello\",  \"simple\" : true, \"ar\" : \"a1\" }",
						"mylist[02].value@JSON", "{\"msg\" : \"world\",  \"simple\" : 20, \"ar\" : [\"a1\"] }",
						"mylist[03].value@JSON",
						"{\"msg\" : \"two words\", \"simple\" : \"true\", \"ar\" : [\"a1\", \"a2\"] }",
						"mylist[04].value@JSON",
						"{\"msg\" : \"recursive json\", \"simple\" : \"false\", \"ar\" : [{\"sub-simple\" :\"false\", \"sub-ar\" : [\"a1\", \"a2\"]}, {\"sub-simple\" :\"true\", \"sub-ar\" : [\"b1\", \"b2\"]}] }"

		));
	}

	@Test
	public void testListUsingAlphanumericIdAndJsonValue() {
		MustacheFilter m = new MustacheFilter();
		m.setListRegex("(.+?)\\[(\\w+)\\]\\.(.+)");
		test(m, "p1={{p1}}\np2={{p2}}\n{{#mylist}}{{__id__}}: {{value.p1}}-{{value.p2}}\n{{/mylist}}",
				"p1=default.p1\np2=default.p2\na1: a.1-a.2\nb1: b.1-b.2\n",
				context("p1", "default.p1", "p2", "default.p2", "mylist[a1].value@JSON",
						"{\"p1\":\"a.1\", \"p2\":\"a.2\"}", "mylist[b1].value@JSON",
						"{\"p1\":\"b.1\", \"p2\":\"b.2\"}"));
	}

	@Test
	public void testImbricatedList() {
		test(new MustacheFilter(), "{{#mylist1}}{{#mylist2}}{{__id__}}: {{p1}}-{{p2}}\n{{/mylist2}}{{/mylist1}}",
				"1: 1.1.1-1.1.2\n2: 1.2.1-1.2.2\n1: 2.1.1-2.1.2\n2: 2.2.1-2.2.2\n",
				context("mylist1.1.mylist2.1.p1", "1.1.1", "mylist1.1.mylist2.1.p2", "1.1.2", "mylist1.1.mylist2.2.p1",
						"1.2.1", "mylist1.1.mylist2.2.p2", "1.2.2", "mylist1.2.mylist2.1.p1", "2.1.1",
						"mylist1.2.mylist2.1.p2", "2.1.2", "mylist1.2.mylist2.2.p1", "2.2.1", "mylist1.2.mylist2.2.p2",
						"2.2.2"));
	}

	protected MustacheFilter getFilter(String prefix, Boolean removePrefix) {
		MustacheFilter m = new MustacheFilter();
		if (prefix != null) {
			m.setPrefix(prefix);
		}
		if (removePrefix != null) {
			m.setRemovePrefix(removePrefix);
		}
		return m;
	}

	protected void test(MustacheFilter mustache, String template, String expected, Map<String, String> context) {
		Project project = new Project();
		project.setProperty("ant.regexp.regexpimpl", "org.apache.tools.ant.util.regexp.Jdk14RegexpRegexp");
		if (context != null) {
			for (Entry<String, String> entry : context.entrySet()) {
				project.setProperty(entry.getKey(), entry.getValue());
			}
		}
		mustache.setProject(project);
		String output = mustache.filter(template);
		check(expected, output);
	}

	protected void check(String expected, String output) {
		assertEquals(uncrlf(expected), uncrlf(output));
	}

	protected static String uncrlf(String text) {
		return text == null ? null : text.replace("\r", "\\r").replace("\n", "\\n");
	}

	protected Map<String, String> context(String... data) {
		Map<String, String> ctx = new HashMap<String, String>();
		for (int ii = 0; ii < data.length; ii += 2) {
			ctx.put(data[ii], data[ii + 1]);
		}
		return ctx;
	}
}
