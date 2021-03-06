.PHONY: all test

all:
	mvn clean install -f org.metaborg.spg.sentence.shared/pom.xml
	mvn clean install -f org.metaborg.spg.sentence.sdf/pom.xml
	mvn clean install -f org.metaborg.spg.sentence.antlr/pom.xml
	mvn clean install -f org.metaborg.spg.sentence.antlr.eclipse.externaldeps/pom.xml
	mvn clean install -f pom.xml

test:
	mvn test -f org.metaborg.spg.sentence.sdf/pom.xml
	mvn test -f org.metaborg.spg.sentence.antlr/pom.xml
