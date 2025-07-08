## Build stage ##
FROM gradle:8.5-jdk21-alpine as build

WORKDIR /usr/app/
COPY . .
RUN gradle clean build -x test --no-daemon

## Run stage ##
FROM eclipse-temurin:21-jre-alpine

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'
ENV TZ=Asia/Ho_Chi_Minh
ENV JAVA_OPTIONS="-Xms4G -Xmx4G"

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime

RUN apk add --no-cache \
    ca-certificates \
    freetype \
    fontconfig \
    && addgroup -g 1001 appgroup \
    && adduser -D -u 1001 -G appgroup appuser

WORKDIR /deployments

COPY --from=build /usr/app/build/libs/webrtc-0.0.1-SNAPSHOT.jar ./app.jar

RUN chown 1001:1001 /deployments/app.jar

EXPOSE 8989
USER 1001

ENTRYPOINT exec java $JAVA_OPTIONS -jar ./app.jar