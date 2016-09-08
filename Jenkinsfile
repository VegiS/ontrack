stage 'Build'

// On a generic Docker slave
node('docker') {

    // Get some code from a GitHub repository
    checkout scm

    // Run the Gradle build and builds the Docker image
    docker.build('nemerosa/ontrack-build', 'seed/docker').inside('--volume=/var/run/docker.sock:/var/run/docker.sock') {
        try {
            sh """./gradlew \\
                clean \\
                versionDisplay \\
                versionFile \\
                test \\
                integrationTest \\
                osPackages \\
                build \\
                --info \\
                --stacktrace \\
                --profile \\
                --console plain \\
                --no-daemon \\
                -PitHost=dockerhost \\
                -PbowerOptions='--allow-root' \\
                -Dorg.gradle.jvmargs="-Xmx3072m"
                """
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

    // TODO Local acceptance tests
    stage 'Local acceptance'

//    withEnv(environment) {
//        sh """./gradlew \\
//            ciAcceptanceTest \\
//            --info \\
//            --profile \\
//            --stacktrace
//            --console plain \\
//            --no-daemon \\
//            -Dorg.gradle.jvmargs="-Xmx3072m"
//            """
//    }
    // Archiving the tests
//    step([$class: 'JUnitResultArchiver', testResults: '*-tests.xml'])
    // TODO Ontrack validation

}

stage 'Docker'

stage 'Acceptance'

stage 'Publication'

stage 'Production'
