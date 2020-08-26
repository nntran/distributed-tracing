# ARG BUILD_IMAGE=maven:3.6-jdk-8-alpine
ARG BUILD_IMAGE=maven:3.6.3-jdk-11-slim
# ARG RUNTIME_IMAGE=openjdk:8-jre-alpine
ARG RUNTIME_IMAGE=openjdk:11-jre-stretch


# Step 1 : pull all maven dependencies
#############################################################################################
FROM ${BUILD_IMAGE} as dependencies
WORKDIR /app
COPY pom.xml .
# RUN mvn -e -B dependency:go-offline
RUN mvn -e -B dependency:resolve


# Step 2 : build SpringBoot app using maven
#############################################################################################
FROM dependencies as builder
COPY src ./src
RUN mvn -e -B -DskipTests=false clean package


# Step 3 : run Sonar analysis (optionnal)
#############################################################################################
FROM builder

ARG SONAR_ENABLED=false
ARG SONAR_URL=
ARG SONAR_ORGANIZATION=
ARG SONAR_USERNAME=
ARG SONAR_PASSWORD=
ARG SONAR_BRANCH=

# RUN if [ "$SONAR_ENABLED" = "true" ] ; \
#     then mvn -e -B sonar:sonar \
#     -Dsonar.host.url=${SONAR_URL} \
#     -Dsonar.organization=${SONAR_ORGANIZATION} \
#     -Dsonar.branch.name=${SONAR_BRANCH} \
#     -Dsonar.login=${SONAR_USERNAME} \
#     -Dsonar.password=${SONAR_PASSWORD}; \
#     fi

RUN if [ "$SONAR_ENABLED" = "true" ] ; \
    then mvn -e -B sonar:sonar \
    -Dsonar.host.url=${SONAR_URL}; \
    fi


# Step 4 : create docker image
#############################################################################################
FROM ${RUNTIME_IMAGE}

ARG IMAGE_VERSION=latest

WORKDIR /opt/app

COPY --from=builder /app/target/*.jar company.jar

# Expose port 8090 on the container
# Expose ports are only accessible to other containers on the same Docker network
EXPOSE 8090

# Run application when container is started
CMD ["java", "-jar", "company.jar"]

# Metadata
LABEL fr.ntdt.image.vendor="ntdt" \
    fr.ntdt.image.url="https://www.ntdt.fr/" \
    fr.ntdt.image.title="Company service" \
    fr.ntdt.image.description="Sample image for the PoC of Opentracing and Jaeger" \
    fr.ntdt.image.version="${IMAGE_VERSION}" \
    fr.ntdt.image.documentation="https://docs.ntdt.fr/poc/opentracing"