def call() {
  pipeline {
    agent any

    stages {
      stage('Build') {
        when {
          branch 'master'
        }
        environment {
          GITHUB_CREDS = credentials('github')
        }
        steps {
          sh '''
            TAG=$(git rev-parse HEAD | cut -c 1-6)

            # Extract the image(/repo) name from the full Jenkins job name.
            IMAGE=$(echo $JOB_NAME  | cut -d '/' -f 2)

            echo $GITHUB_CREDS_PSW | docker login --username $GITHUB_CREDS_USR --password-stdin ghcr.io
            docker build -t ghcr.io/markvr/$IMAGE:$TAG .
            docker push ghcr.io/markvr/$IMAGE:$TAG
          '''
        }
      }
      stage('Deploy') {
        when {
          branch 'master'
        }
        environment {
          GITHUB_CREDS = credentials('github')
        }
        steps {
          sh  '''
            TAG=$(git rev-parse HEAD | cut -c 1-6)

            # Extract the image(/repo) name from the full Jenkins job name.
            IMAGE=$(echo $JOB_NAME  | cut -d '/' -f 2)

            # Image/repo name is hyphenated, but dsl filenames need to be underscored
            DSL_FILE=$(echo $IMAGE | sed s/-/_/g).dsl

            CONFIG_REPO=jenkins-dsl-config
            
            cd /tmp
            if [ -d $CONFIG_REPO ]; then
              cd $CONFIG_REPO
              git pull
            else
              git clone https://$GITHUB_CREDS_USR:$GITHUB_CREDS_PSW@github.com/markvr/$CONFIG_REPO
              cd $CONFIG_REPO
            fi
            sed -i -E  "s/$IMAGE:(.*)/$IMAGE:$TAG/" $DSL_FILE
            git commit -a -m "Updated $IMAGE to $TAG"
            git push
            '''
        }
      }
    }
  }
}