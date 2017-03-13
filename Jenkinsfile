node{
    stage 'Build and Test'
    checkout scm
    // Remove generated files. Use "-fxd" to also remove untracked files, but note that this will also remove .repository forcing mvn to download all artifacts each build
    sh "git clean -fXd"
    
    withMaven(
        mavenLocalRepo: "${env.JENKINS_HOME}/m2repos/${env.EXECUTOR_NUMBER}",
        mavenOpts: "-Xmx1G -Xms1G -Xss16m",
        mavenSettingsConfig: "org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1430668968947"
    ){
        // Build org.metaborg.spg.{core,cmd}. On Jenkins, use publish instead of publish-m2 (see build.sbt)
        sh "sbt -Dsbt.log.noformat=true clean compile publish"

        // Build org.metaborg.spg.eclipse.externaldeps separately
        sh "mvn -f org.metaborg.spg.eclipse.externaldeps/pom.xml clean install"

        // Build org.metaborg.spg.{eclipse,eclipse.feature,eclipse.site}
        sh "mvn -f pom.xml clean install"
    }

    // Archive the update site
    archiveArtifacts artifacts: 'org.metaborg.spg.eclipse.site/target/site/', excludes: null, onlyIfSuccessful: true
}
