def withXvfb(def ctx, String script) {
    ctx.sh """\
#!/bin/bash

mkdir -p xvfb-\${EXECUTOR_NUMBER}-\${BUILD_NUMBER}
let 'NUM = EXECUTOR_NUMBER + 1'
echo "Display number: \${NUM}"
nohup /usr/bin/Xvfb :\${NUM} -screen 0 1024x768x24 -fbdir xvfb-\${EXECUTOR_NUMBER}-\${BUILD_NUMBER} & > xvfb.pid

# Make sure to stop Xvfb at the end
trap "kill -KILL `cat xvfb.pid`" EXIT

export DISPLAY=":\${NUM}"

${script}

# Exit normally in all cases
# Evaluation is done by test reporting
exit 0
"""
}

node('docker') {

    checkout scm

    //==== BUILD ==============================

    stage 'Build'

    sh '''\
      HOSTIP=`ip -4 addr show docker0 | grep 'inet ' | awk '{print $2}' | awk -F '/' '{print $1}'`
      echo HOSTIP=${HOSTIP}
      echo HOSTIP=${HOSTIP} > host.properties
      '''

    def props = readProperties(file: 'host.properties')
    String hostIP = props.HOSTIP
    echo "Host IP = ${hostIP}"

    docker.build('nemerosa/ontrack-build', 'seed/docker').inside("--volume=/var/run/docker.sock:/var/run/docker.sock --add-host dockerhost:${hostIP}") {
        try {
            sh '''\
#!/bin/bash
./gradlew \\
    clean \\
    versionDisplay \\
    versionFile \\
    test \\
    integrationTest \\
    dockerLatest \\
    osPackages \\
    build \\
    -PitJdbcHost=dockerhost \\
    -PitJdbcWait=60 \\
    -PbowerOptions='--allow-root\' \\
    -Dorg.gradle.jvmargs=-Xmx1536m \\
    --info \\
    --stacktrace \\
    --profile \\
    --console plain
'''
            // Reads the version properties
            props = readProperties(file: 'build/version.properties')
            env.VERSION = props.VERSION_DISPLAY
            currentBuild.description = env.VERSION
            // Archives the delivery archive
            stash name: 'ontrack-archives',
                  includes: 'build/distributions/ontrack-*-delivery.zip,build/distributions/ontrack*.deb,build/distributions/ontrack*.rpm'
        } finally {
            // Archiving the tests
            junit allowEmptyResults: false, testResults: '**/build/test-results/*.xml'
        }
    }

    // TODO Ontrack build

    stage 'Local acceptance'

    docker.build('nemerosa/ontrack-build', 'seed/docker').inside("--volume=/var/run/docker.sock:/var/run/docker.sock --add-host dockerhost:${hostIP}") {

        unstash 'ontrack-archives'
        unzip test: true, zipFile: "build/distributions/ontrack-${env.VERSION}-delivery.zip"

        withXvfb delegate,  """\
./gradlew \\
    ciAcceptanceTest \\
    -PacceptanceJar=build/distributions/ontrack-acceptance-${env.VERSION}.jar \\
    -PciHost=dockerhost \\
    -Dorg.gradle.jvmargs=-Xmx1536m \\
    --info \\
    --profile \\
    --console plain \\
    --stacktrace
"""

        junit allowEmptyResults: false, testResults: '*-tests.xml'

    }

//    stage 'Docker publication'
//
//    parallel digitalocean: {
//        stage 'Digital Ocean acceptance'
//    }, centos6: {
//        stage 'CentOS 6 acceptance'
//    }, centos7: {
//        stage 'CentOS 7 acceptance'
//    }, debian {
//        stage 'Debian acceptance'
//    }, failFast: true
//
//    stage 'Publication'
//
//    stage 'Production'
//
//    stage 'Production tests'

}