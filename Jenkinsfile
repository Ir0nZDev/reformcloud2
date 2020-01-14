pipeline {
    agent any
    tools {
        jdk "1.8.0_222"
    }

    stages {
        stage('Clean') {
            steps {
                echo "Cleaning up...";
                sh 'mvn clean';
            }
        }

        stage('Build') {
            steps {
                echo "Building project...";
                sh 'mvn package';
            }
        }

        stage('Prepare zip') {
            steps {
                echo "Creating zip...";
                sh "rm -rf ReformCloud2.zip";
                sh "mkdir -p results";
                sh "cp -r .templates/* results/";
                sh "cp reformcloud2-runner/target/runner.jar results/runner.jar";
                zip archive: true, dir: 'results', glob: '', zipFile: 'ReformCloud2.zip';
                sh "rm -rf results/";
            }
        }

        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'ReformCloud2.zip'
                archiveArtifacts artifacts: 'reformcloud2-runner/target/runner.jar'
            }
        }
    }
}