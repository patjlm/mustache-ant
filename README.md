mustache-ant
============

A Mustache implementation for ant scripts, based on JMustache. This is an [Ant](http://ant.apache.org/) filter to compute [Mustache](http://mustache.github.io/) templates based on [JMustache](https://github.com/samskivert/jmustache). Just as JMustache, one of the aim is to limit the number of dependencies: only the mustache-ant jar files needs to be downloaded in order to benefit from it.

It is to be used in a [Filterchain](http://ant.apache.org/manual/Types/filterchain.html) - [TokenFilter](http://ant.apache.org/manual/Types/filterchain.html#tokenfilter) using the [FileTokenizer](http://ant.apache.org/manual/Types/filterchain.html#filetokenizer).
This allows to process mustache templates in any ant task supporting filterchains, like copy, loadfile, concat, ... It also means several files can be processed at once. 

Installation
============

Download the mustache-ant jar from the
[Maven Central Repository](http://search.maven.org/remotecontent?filepath=com/github/patjlm/mustache-ant/1.0.0/mustache-ant-1.0.0.jar) or the [Sonatype release repository](https://oss.sonatype.org/content/repositories/releases/com/github/patjlm/mustache-ant/1.0.0/mustache-ant-1.0.0.jar),
store it somewhere accessible to your ant script (${mustache-ant.jar} below).

You can then define the mustache filter in your ant script as follows:

	<typedef resource="com/github/patjlm/ant/mustache/antlib.xml">
		<classpath>
			<path location="${mustache-ant.jar}" />
		</classpath>
	</typedef>

This will define a new ant filter called "mustache". Let's now see how to use it.

Usage
=====

This filter must be used inside a filetokenizer tokenfilter as it needs to parse the whole file at once:

	<filterchain>
		<tokenfilter>
			<filetokenizer />
			<mustache />
		</tokenfilter>
	</filterchain>

As this is a filter, it can be used in any Ant task supporting filterchains, like
* Concat
* Copy
* LoadFile
* LoadProperties
* LoadResource
* Move

Parameters
==========

All parameters are optional.

| Parameter         | Description                                                                   | Default            |
|-------------------|-------------------------------------------------------------------------------|--------------------|
| projectProperties | Boolean (true or false): should project properties be added to the data model | true               |
| prefix            | Only project properties starting with this prefix will be used                | No prefix used     |
| removePrefix      | Boolean: should we remove the prefix (if specified) from the property name?   | false              |
| booleanRegex      | Since v0.4. The regex pattern used to match boolean properties                | ^.+?$              |
| supportLists      | Boolean. Adds list support (see below)                                        | true               |
| listRegex         | The regex pattern to use to defined lists (see below)                         | (.+?)\.(\d+)\.(.+) |
| listIdName        | The name of the list id to be generated (see below)                           | \__id__            |
| dataFile          | A property file containing datamodel key and values                           | None               |
| defaultValue      | As JMustache defaultValue(), provides a default to non-defined keys | No default, fails on missing |
| strictSections    | As JMustache strictSections(), defines if section referring to a non-defined value should fail | false |
| escapeHTML        | As JMustache escapeHTML(), defines if outputed HTML should be escaped         | false              |
| partialPath       | Since v1.0.0. A path-like structure in which [partials](https://github.com/samskivert/jmustache#partials) (aka sub-templates) can be searched for. Non-directories path elements will be ignored | None. Partials not supported by default |
| emptyStringIsFalse| Since v1.1.0. Boolean (true or false): should empty string be treated as a false value, as in JavaScript mustache implementation. | false              |

PartialPath can also be defined as an [XML element](https://ant.apache.org/manual/using.html#path) inside the mustache type.
For example:

		<mustache>
			<partialPath>
				<pathelement location="${partials.dir}" />
			</partialPath>
		</mustache>


An example build script is available in the [test subfolder](https://github.com/patjlm/mustache-ant/tree/master/test) of this project.

Lists support
=============

Provided property names can be parsed to generate lists. The default Regexp pattern for such property is

	(.+?)\.(\d+)\.(.+)

This pattern means that any property containing a number between two dots would be translated into a list.
The list name is the first part.
The id in the list is the number. It can be accessed using the value of listIdName ("\__id__" by default).
The remaining part is then used as a key inside the list.

An example may help here. Consider the following properties:

	mylist.01.prop1 = value-1-1
	mylist.01.prop2 = value-1-2
	mylist.02.prop1 = value-2-1
	mylist.02.prop2 = value-2-2

And this template

	mylist = {{mylist}}
	{{#mylist}}
	{{__id__}}.prop1 = {{prop1}}
	{{__id__}}.prop2 = {{prop2}}
	{{/mylist}}

The output would be:

	mylist = [{prop2=value-1-2, prop1=value-1-1, __id__=01}, {prop2=value-2-2, prop1=value-2-1, __id__=02}]
	01.prop1 = value-1-1
	01.prop2 = value-1-2
	02.prop1 = value-2-1
	02.prop2 = value-2-2

Sub-lists are supported as well. For example, you could have the following properties:

	mylist1.1.mylist2.1.p1=1.1.1
	mylist1.1.mylist2.1.p2=1.1.2
	mylist1.1.mylist2.2.p1=1.2.1
	mylist1.1.mylist2.2.p2=1.2.2
	mylist1.2.mylist2.1.p1=2.1.1
	mylist1.2.mylist2.1.p2=2.1.2
	mylist1.2.mylist2.2.p1=2.2.1
	mylist1.2.mylist2.2.p2=2.2.2

and use them in the template

	{{#mylist1}}
		{{#mylist2}}
			{{p1}}-{{p2}}
		{{/mylist2}}
	{{/mylist1}}
	 
Note that you can override the default pattern. For example, you may prefer to use a notation with square brackets:

	listRegex="(.+?)\[(\d+)\]\.(.+)"

With such regex, the previous list would be written

	mylist[01].prop1 = value 01-1
	mylist[01].prop2 = value 01-2
	mylist[02].prop1 = value 02-1
	mylist[02].prop2 = value 02-2

Empty value
===========

The empty value is supported since V1.1.0 by setting the emptyStringIsFalse option to "true".

Consider the following properties:

	myproperty.enable = 

And this template

	{{#myproperty}}myproperty exists and should not be empty (value={{myproperty}}){{/myproperty}}
	{{^myproperty}}myproperty does not exist or is empty{{/myproperty}}
 
In case emptyStringIsFalse option is set to false (default value), the output will be:

	myproperty exists and should not be empty (value=)

In case emptyStringIsFalse option is set to true, the output will be:

	myproperty does not exist or is empty

Boolean values
==============

As the string "false" is not usually considered as actually False, a special treatment is needed for booleans.
Properties ending by a question mark are treated as Booleans by default, specifically to be used as tests inside the templates.

	mytrue? = true
	myfalse? = false

In the template:

	mytrue? = {{mytrue?}}
	{{#mytrue?}}
	mytrue is valid (not false nor empty list), showing this!
	{{/mytrue?}}
	{{^mytrue?}}
	mytrue is NOT valid (false or empty list), showing that!
	{{/mytrue?}}

	myfalse? = {{myfalse?}}
	{{#myfalse?}}
	myfalse is valid (not false nor empty list), showing this!
	{{/myfalse?}}
	{{^myfalse?}}
	myfalse is NOT valid (false or empty list), showing that!
	{{/myfalse?}}

Which outputs:

	mytrue? = true
	mytrue? is valid (not false nor empty list), showing this!
	myfalse? = false
	myfalse? is NOT valid (false or empty list), showing that!


You can override the default boolean key pattern by using the booleanRegex option.
For example, if you want to prefix all your boolean properties with "is", you could use this kind of pattern:

	booleanRegex="^is.+"

You can then use it in your template:

	{{#isThisTrue}}
	isThisTrue is valid (not false nor empty list), showing this!
	{{/isThisTrue}}

JSON values
===========

The JSON values are supported since V1.1.0.

Consider the following properties:

	myproperty@JSON = {"p1" : "a.1", "p2" : "a.2" }

And this template

	{{#myproperty}}myproperty exists, p1={{p1}}, p2={{p2}}{{/myproperty}}

Which outputs:

	myproperty exists, p1=a.1, p2=a.2

You can also use a JSON structure inside a list. The advantage is to reduce the number of properties.
Consider the following properties (simplification of example using list above):

	mylist.01@JSON = { "prop1" : "value-1-1", "prop2" : "value-1-2" }
	mylist.02@JSON = { "prop1" : "value-2-1", "prop2" : "value-2-2" }

The same template is the same as above:

	mylist = {{mylist}}
	{{#mylist}}
	{{__id__}}.prop1 = {{prop1}}
	{{__id__}}.prop2 = {{prop2}}
	{{/mylist}}

Which outputs:

	mylist = [{prop2=value-1-2, prop1=value-1-1, value={"prop1" : "value-1-1", "prop2" : "value-1-2" }, __id__=01}, {prop2=value-2-2, prop1=value-2-1, value={"prop1" : "value-2-1", "prop2" : "value-2-2" }, __id__=02}]
	01.prop1 = value-1-1
	01.prop2 = value-1-2
	02.prop1 = value-2-1
	02.prop2 = value-2-2

You can also have more complex JSON structure. Let's consider the following properties:

	mylist[01].value@JSON = { "msg" : "hello", "simple" : true, "ar" : "a1" }
	mylist[02].value@JSON = { "msg" : "world", "simple" : 20, "ar" : ["a1"] }
	mylist[03].value@JSON = { "msg" : "two words", "simple" : "true", "ar" : ["a1", "a2"] }
	mylist[04].value@JSON = { "msg" : "recursive json", "simple" : "false", 
														"ar" : [
																{ "sub-simple" : "false", "sub-ar" : ["a1", "a2"] }, 
																{ "sub-simple" : "true", "sub-ar" : ["b1", "b2"] }
														 ]
													}

The template will be the following:

	{{#mylist}}
		{ 
			"key" = "{{__id__}}",
			"msg" = "{{value.msg}}",
			"simple" = "{{value.simple}}",
			"ar" = "{{value.ar}}"
		},
	{{/mylist}}

Which outputs:

		{
			"key" = "01",
			"msg" = "hello",
			"simple" = "true",
			"ar" = "a1"},
		},
		{
			"key" = "02",
			"msg" = "world",
			"simple" = "20",
			"ar" = ["a1"]
		},
		{
			"key" = "03",
			"msg" = "two words",
			"simple" = "true",
			"ar" = ["a1", "a2"]
		},
		{
			"key" = "04",
			"msg" = "recursive json",
			"simple" = "false",
			"ar" = [
				{
					sub-simple = false,
					sub-ar = ["a1", "a2"]
				}, 
				{
					sub-simple = true,
					sub-ar = ["b1", "b2"]
				}
			]
		}

Note: using complex JSON with sub-levels, the double quotes disappear around the internal keys (sub-simple and sub-ar)... to be checked how to fix this

You can override the default JSON key pattern by using the jsonValueRegex option.
For example, if you want to suffix all your JSON properties with "!JSON", you could use this kind of pattern:

	booleanRegex="^(.+)(!JSON)$"

