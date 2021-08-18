# amd64/ or arm64v8/
ARG ARCH=
FROM ${ARCH}debian:10-slim

# get curl
RUN apt-get update \
 && apt-get upgrade -y \
 && apt-get install -y \
      curl \
      build-essential \
      zlib1g-dev \
 && apt-get autoremove \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /data

ARG GRAALVM_VERSION=21.2.0
ARG GVM_JAVA_VERSION=java11
# amd64 or aarch64
ARG GVM_PLATFORM=aarch64

# Install JAVA / GraalVM
# see here: https://github.com/graalvm/graalvm-ce-builds/releases/
RUN curl -LO https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-${GRAALVM_VERSION}/graalvm-ce-${GVM_JAVA_VERSION}-linux-${GVM_PLATFORM}-${GRAALVM_VERSION}.tar.gz \
 && tar xzf graalvm-ce-${GVM_JAVA_VERSION}-linux-${GVM_PLATFORM}-${GRAALVM_VERSION}.tar.gz

ENV PATH=${PATH}:/data/graalvm-ce-${GVM_JAVA_VERSION}-${GRAALVM_VERSION}/bin \
    JAVA_HOME=/data/graalvm-ce-${GVM_JAVA_VERSION}-${GRAALVM_VERSION}

# Install native-image
RUN gu install native-image

WORKDIR /app

COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle/ gradle/

RUN ./gradlew --no-daemon tasks
