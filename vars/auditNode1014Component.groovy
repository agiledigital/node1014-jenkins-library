/*
 * Toolform-compatible Jenkins 2 Pipeline build step for NodeJS 10.14 apps using the node1014 builder
 */

def call(Map config) {

  def artifactDir = "${config.project}-${config.component}-artifacts"
  def testOutput = "${config.project}-${config.component}-tests.xml"

  final yarn = { cmd ->
    ansiColor('xterm') {
      dir(config.baseDir) {
        sh "NODE_OPTIONS=--max-old-space-size=4096 JEST_JUNIT_OUTPUT=${testOutput} yarn ${cmd}"
      }
    }
  }
  
  container("node1014-builder") {

    stage('Build Details') {
      echo "Project:   ${config.project}"
      echo "Component: ${config.component}"
      echo "BuildNumber: ${config.buildNumber}"
    }

    stage('Audit Production dependencies') {
      yarn "audit --prod --frozen-lockfile --json > prod.audit"
    }

    stage('Audit All dependencies') {
      yarn "audit --frozen-lockfile --json > all.audit"
    }

    stage('Show audit results') {
      yarn "audit --frozen-lockfile --json"
    }

    stage('Archive to Jenkins') {
      def tarName = "audits-node-${config.project}-${config.component}-${config.buildNumber}.tar.gz"
      sh "tar -czvf \"${tarName}\" *.audit"
      archiveArtifacts tarName
    }

  }

}
