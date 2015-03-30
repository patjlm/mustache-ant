package com.github.patjlm.ant.mustache;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;

import com.samskivert.mustache.Mustache;

public class PartialPath extends Path {

	public PartialPath(Project project) {
		super(project);
	}

	public PartialPath(Project project, String path) {
		super(project, path);
	}
	
	public Mustache.TemplateLoader getLoader() {
		return new Mustache.TemplateLoader() {
		    public Reader getTemplate (String name) {
		    	return findPartial(name);
		    }
		};
	}

	private Reader findPartial(String name) {
		Iterator it = iterator();
		while (it.hasNext()) {
			Resource resource = (Resource) it.next();
			if (resource.isDirectory()) {
				File partial = new File(((FileResource) resource).getFile(), name);
				if (partial.exists()) {
			    	try {
			    		return new FileReader(partial);
			    	} catch (Exception e) {
			    		throw new BuildException(e);
			    	}
				}
			}
		}
		throw new BuildException("Partial templaate " + name + " not found in path: " + toString());
	}

}
