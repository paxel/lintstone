# prerequisites
https://dzone.com/articles/publish-your-artifacts-to-maven-central

keyserver there are deprecated so use one of those:
keyserver.ubuntu.com
keys.openpgp.org
pgp.mit.edu

# publish
mvn clean
mvn release:prepare
mvn release:perform
git push–tags
git push origin master
