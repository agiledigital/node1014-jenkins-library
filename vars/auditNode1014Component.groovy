/*
 * Toolform-compatible Jenkins 2 Pipeline build step for NodeJS 10.14 apps using the node1014 builder
 */

def call(Map config) {

  final yarn = { cmd ->
    ansiColor('xterm') {
      dir(config.baseDir) {
        sh "yarn ${cmd}"
      }
    }
  }
  
  container("node1014-builder") {

    stage('Audit Run Details') {
      echo "Project:   ${config.project}"
      echo "Component: ${config.component}"
      echo "BuildNumber: ${config.buildNumber}"
    }

    stage('Audit Production dependencies') {
      try {
      yarn "audit --frozen-lockfile --json --groups 'dependencies' > prod.audit"
     } catch (InterruptedException e) {
      // Build interupted
      currentBuild.result = "ABORTED"
      throw e
    } catch (e) {
      // If there was an exception thrown, the build failed
      currentBuild.result = "UNSTABLE"
    }
    }

    stage('Audit All dependencies') {
      try {
      yarn "audit --frozen-lockfile --groups 'devDependencies dependencies' --json > all.audit"
     } catch (InterruptedException e) {
      // Build interupted
      currentBuild.result = "ABORTED"
      throw e
    } catch (e) {
      // If there was an exception thrown, the build failed
      currentBuild.result = "UNSTABLE"
    }
    }

    stage('Show audit results') {
      try {
      yarn "audit --frozen-lockfile --groups 'dependencies' > audit.txt"
     } catch (InterruptedException e) {
      // Build interupted
      currentBuild.result = "ABORTED"
      throw e
    } catch (e) {
      // If there was an exception thrown, the build failed
      currentBuild.result = "UNSTABLE"
    }
    }

    stage('Archive to Jenkins') {
      dir(config.baseDir) {
        def tarName = "audits-node-${config.project}-${config.component}-${config.buildNumber}.tar.gz"
        sh "tar -czvf \"${tarName}\" all.audit prod.audit"
        archiveArtifacts tarName
        slackUploadFile filePath: "audit.txt", initialComment: "${config.component} audit summary", channel: config.slackThead.threadId
        slackUploadFile filePath: tarName, initialComment:  "${config.component} audit jsonl file:\"${tarName}\"", channel: config.slackThead.threadId
      }
    }

  }

}
