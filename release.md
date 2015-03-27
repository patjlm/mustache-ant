Short summary on how to perform a release...

# One timer: setup gpg

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

# Maven release preparation

Ensure first that evrything is commited, and that the version in pom.xml is a SNAPSHOT

	mvn clean
	mvn -DpushChanges=false release:prepare
	
-DpushChanges=false is because we do not have ssh-agent runnig to store the git passphrase.

Then push the git tags an the changes to origin.

# Maven release perform

	 mvn release:release
	 
# Sonatype staging

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
