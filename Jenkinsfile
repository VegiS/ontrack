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
            step([$class: 'JUnitResultArchiver', testResults: '**/build/test-results/*.xml'])
        }
    }

    // TODO Ontrack build

    // Extracts the version from the file
    def versionInfo = readProperties file: 'build/version.properties'
    env.VERSION_DISPLAY = versionInfo.VERSION_DISPLAY
    echo "Version = ${env.VERSION_DISPLAY}"

}