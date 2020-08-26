node {
    checkout scm
    sh './mvnw -B -DskipTests clean package'
    docker.build("poc/tracing").push()
}