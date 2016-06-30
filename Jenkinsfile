stage 'Build'

// On a generic Docker slave
node('docker') {

    // Get some code from a GitHub repository
    checkout scm

    // Get the JDK
    // TODO Using a Docker image build means that we do not need a JDK installation any longer
    def javaHome = tool name: 'JDK8u74', type: 'hudson.model.JDK'

    // Environment
    def environment = [
            "JAVA_HOME=${javaHome}",
    ]

    // Run the Gradle build and builds the Docker image
    try {
        docker.build('nemerosa/ontrack-build', 'jenkins/build').inside {
            withEnv(environment) {
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
                    -Dorg.gradle.jvmargs="-Xmx3072m"
                    """
            }
        }
    } finally {
        // Archiving the tests
        step([$class: 'JUnitResultArchiver', testResults: '**/build/test-results/*.xml'])
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
