node('docker') {

    checkout scm

    //==== BUILD ==============================

    stage 'Build'

    docker.build('nemerosa/ontrack-build', 'seed/docker').inside('--volume=/var/run/docker.sock:/var/run/docker.sock') {
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
    -PitJdbcWait=20 \\
    -PbowerOptions='--allow-root\' \\
    -Dorg.gradle.jvmargs=-Xmx1536m \\
    --info \\
    --stacktrace \\
    --profile \\
    --console plain
'''
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