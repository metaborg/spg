/*properties([
  pipelineTriggers([
    upstream(
      threshold: hudson.model.Result.SUCCESS,
      upstreamProjects: '/metaborg/spoofax-releng/master'
    )
  ])
])*/

node {
  stage 'Build and Test'
  checkout scm
  // Make sure generated files are removed (git-ignored files). Use "-fxd" to also remove untracked files, but note that this will also remove .repository forcing mvn to download all artifacts each build
  sh "git clean -fXd"
  withMaven(
    mavenLocalRepo: '.repository',
    mavenOpts: '-Xmx1024m -Xss16m'
  ){
    // Build pom-first projects first. Maven and Tycho dependencies in a single reactor build are not supported? (https://goo.gl/akexsK)
    sh "mvn -B -U clean install -f org.metaborg.spg.sentence.shared/pom.xml"
    sh "mvn -B -U clean install -f org.metaborg.spg.sentence/pom.xml"
    sh "mvn -B -U clean install -f org.metaborg.spg.sentence.antlr/pom.xml"
    sh "mvn -B -U clean install -f org.metaborg.spg.sentence.eclipse.externaldeps/pom.xml"

    // Build the rest
    sh "mvn -B -U clean install -f pom.xml -DforceContextQualifier=\$(date +%Y%m%d%H%M)"
  }
  archiveArtifacts artifacts: 'org.metaborg.spg.sentence.eclipse.site/target/site/', excludes: null, onlyIfSuccessful: true
}

