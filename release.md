# Release preparation

Short summary on how to perform a release...
This procedure is an extract of [this one](http://datumedge.blogspot.fr/2012/05/publishing-from-github-to-maven-central.html).

## One time: setup gpg

* gpg.exe must be in the path
* create your key

	gpg --gen-key
	
* List your keys:

	gpg --list-keys

The output should look like

	----------------------------------------------------
	pub   2048R/BCB94FEB 2015-03-27
	uid       [ultimate] Patrick Martin <pjlmartin@gmail.com>
	sub   2048R/D3A3E274 2015-03-27

BCB94FEB is is the key name in this example. you may have to specify 0xBCB94FEB...

* send the private key to public servers:

	gpg --keyserver hkp://pgp.mit.edu --send-key KEYNAME

## One time: setup GPG in your user maven settings

* Edit or create ~/.m2/settings.xml to include your credentials: 

	<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
	  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
	  <servers>
	    <server>
	      <id>sonatype-nexus-snapshots</id>
	      <username>myusername</username>
	      <password>mypassword</password>
	    </server>
	    <server>
	      <id>sonatype-nexus-staging</id>
	      <username>myusername</username>
	      <password>mypassword</password>
	    </server>
	  </servers>

	  <profiles>
	    <profile>
	      <id>sign</id>
	      <activation>
	        <activeByDefault>true</activeByDefault>
	      </activation>
	      <properties>
		    <gpg.keyname>myGPGuser</gpg.keyname>
	        <gpg.passphrase>mypassphrase</gpg.passphrase>
	      </properties>
	    </profile>
	  </profiles>
	</settings>


## Maven release preparation

Ensure first that everything is commited, and that the version in pom.xml is a SNAPSHOT.
Note: the following procedure only works from a clone of the central repository (not from a fork).

	mvn clean
	mvn -DpushChanges=false release:prepare
	
-DpushChanges=false is because we do not have ssh-agent running to store the git passphrase.

Then push the git tags and the changes to origin.

	git push
	git push origin <new tag>

## Maven release perform

	mvn release:perform
	
## Rollback a release

	mvn release:rollback
	git tag -d <tag>
	git push origin :refs/tags/<tag>
	 
## Sonatype staging

The files are now staged on [https://oss.sonatype.org/](https://oss.sonatype.org/).

* Access [https://oss.sonatype.org/](https://oss.sonatype.org/)
* Log in
* Click on "Staging Repositories"
* Select your repository
* Check the content and click on the "Close" button on top
* Wait for the email: "Nexus: Staging Completed"
* Go back on the "Staging Repositories"
* Select your repository
* Check and click on "Release"
