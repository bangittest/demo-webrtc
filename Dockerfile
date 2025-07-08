## Build stage ##
FROM gradle:8.5-jdk21-alpine as build

WORKDIR /usr/app/

# Copy gradle files first for better caching
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle/ gradle/

# Copy source code
COPY src/ src/

# Build application
RUN gradle clean build -x test --no-daemon

## Run stage ##
FROM ghcr.io/graalvm/jdk-community:21.0.2-ol9

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'
ENV TZ=Asia/Ho_Chi_Minh
ENV JAVA_OPTIONS="-Xms4G -Xmx4G -XX:+UseZGC -XX:+ZGenerational"

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime

RUN microdnf update \
    && microdnf install telnet ca-certificates freetype fontconfig \
    && microdnf clean all \
    && mkdir /deployments \
    && chown 1001 /deployments \
    && chmod "g+rwX" /deployments \
    && chown 1001:root /deployments \
    && echo "securerandom.source=file:/dev/urandom" >> /usr/lib64/graalvm/graalvm-community-java21/lib/security/java.security

WORKDIR /deployments

# Copy only the jar file
COPY --from=build /usr/app/build/libs/webrtc-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 8989
USER 1001

ENTRYPOINT exec java $JAVA_OPTIONS -jar ./app.jar