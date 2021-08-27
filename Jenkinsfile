@Library('shared-library@no-tests-more-properties') _

pipeline {
  agent none

  options {
    disableConcurrentBuilds()
  }
  environment {
    PREFIX = "protocol"
    CREDENTIALS_ID = "nexus-ci"
    UNION_STACK = "protocol-union"
  }

  stages {
    stage('test') {
      agent any
      steps {
         sh 'mvn clean test -U'
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'
          step([ $class: 'JacocoPublisher', execPattern: '**/target/jacoco-aggregate.exec' ])
        }
      }
    }
    stage('package and publish') {
      agent any
      when {
        anyOf { branch 'master'; branch 'release/*' }
        beforeInput true
      }
      steps {
        sh 'mvn clean package -DskipTests'

        script {
          env.BRANCH_NAME = "${env.GIT_BRANCH}"
          env.IMAGE_TAG = "${env.BRANCH_NAME.replace('release/', '')}-${env.BUILD_NUMBER}"
          env.VERSION = "${env.IMAGE_TAG}"
        }
        dir("api") {
        	deployToMaven(env.CREDENTIALS_ID)
        }
        publishDockerImages(env.PREFIX, env.CREDENTIALS_ID, env.IMAGE_TAG)
      }
    }
    stage("deploy to dev") {
      agent any
      when {
        allOf {
          anyOf { branch 'master'}
          expression {
            input message: "Deploy to dev?"
            return true
          }
        }
        beforeAgent true
      }
      environment {
        APPLICATION_ENVIRONMENT = 'dev'
      }
      steps {
        deployStack(env.APPLICATION_ENVIRONMENT, env.UNION_STACK, env.PREFIX, env.IMAGE_TAG)
      }
    }
    stage("deploy to e2e") {
      agent any
      when {
        allOf {
          anyOf { branch 'master'; branch 'release/*' }
          expression {
            input message: "Deploy to e2e?"
            return true
          }
        }
        beforeAgent true
      }
      environment {
        APPLICATION_ENVIRONMENT = 'e2e'
      }
      steps {
        deployStack(env.APPLICATION_ENVIRONMENT, env.UNION_STACK, env.PREFIX, env.IMAGE_TAG)
      }
    }
    stage("deploy to staging") {
      agent any
      when {
        allOf {
          anyOf { branch 'release/*' }
          expression {
            input message: "Deploy to staging?"
            return true
          }
        }
        beforeAgent true
      }
      environment {
        APPLICATION_ENVIRONMENT = 'staging'
      }
      steps {
        deployStack(env.APPLICATION_ENVIRONMENT, env.UNION_STACK, env.PREFIX, env.IMAGE_TAG)
      }
    }
    stage("deploy to prod") {
      agent any
      when {
        allOf {
          anyOf { branch 'release/*' }
          expression {
            input message: "Deploy to prod?"
            return true
          }
        }
        beforeAgent true
      }
      environment {
        APPLICATION_ENVIRONMENT = 'prod'
      }
      steps {
        deployStack(env.APPLICATION_ENVIRONMENT, env.UNION_STACK, env.PREFIX, env.IMAGE_TAG)
      }
    }
  }
  post {
    always {
      node("") {
        cleanWs()
      }
    }
  }
}
