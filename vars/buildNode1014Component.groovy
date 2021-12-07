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

    stage('Install dependencies') {
      sh "git config --global url.\"https://\".insteadOf git://"
      yarn "install --frozen-lockfile"
    }

    stage('Test') {
      yarn 'test --ci --testResultsProcessor="jest-junit"'
      junit allowEmptyResults: true, testResults: testOutput
    }

  }

   container('node1014-builder') {
    stage('Build') {
      yarn "build"
    }
   }
  
  if(config.stage == 'dist') {

    container('node1014-builder') {

      stage('Package') {
        sh "mkdir -p ${artifactDir}"
        sh "mkdir -p ${artifactDir}/config"
        sh "mkdir -p ${artifactDir}/assets"

        yarn "install --frozen-lockfile --production --ignore-scripts --prefer-offline"
        

        stage('Inject configuration') {
          // TODO: Allow ${SETTINGS_CONTEXT} to be overriden
            // From https://stash.agiledigital.com.au/projects/MCP/repos/docker-builder/browse/builders/play2-multi-build/build.sh
          sh "cp *.conf \"${artifactDir}/config\""
        }

        // The build and dist folders may exisit depending on builder.
        // Copy them into the artifact if they exist. e.g. React uses build, NodeJS defualt is dist.
        if(fileExists("${config.baseDir}/dist")) {
          sh "mv ${config.baseDir}/dist/* ${artifactDir}/assets"
        }
        
        if(fileExists("${config.baseDir}/build")) {
          sh "mv ${config.baseDir}/build ${artifactDir}"
          sh "mv ${config.baseDir}/node_modules ${config.baseDir}/package.json ${artifactDir}"
        }
        
        if(fileExists("${config.baseDir}/serverless.yml")) {
          sh "mv ${config.baseDir}/serverless.yml ${artifactDir}"
          sh "mv ${config.baseDir}/node_modules ${config.baseDir}/package.json ${artifactDir}"
        }

        // The static folder and application specific config files 
        // should also be staged if they exist.
        if(fileExists("${config.baseDir}/static")) {
          sh "mv ${config.baseDir}/static ${artifactDir}"
          sh "mv ${config.baseDir}/node_modules ${config.baseDir}/package.json ${artifactDir}"
        }

        if(fileExists("${config.baseDir}/next.config.js")) {
          sh "mv ${config.baseDir}/next.config.js ${artifactDir}"
          sh "mv ${config.baseDir}/node_modules ${config.baseDir}/package.json ${artifactDir}"
        }
      }
    }

    stage('Archive to Jenkins') {
      def tarName = "${config.project}-${config.component}-${config.buildNumber}.tar.gz"
      sh "tar -czvf \"${tarName}\" -C \"${artifactDir}\" ."
      archiveArtifacts tarName
    }

  }

}
