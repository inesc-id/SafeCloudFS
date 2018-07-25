FROM debian:stretch-slim

ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64

RUN mkdir -p /usr/share/man/man1/ && \
 apt-get update && \
 apt-get -y install openjdk-8-jdk-headless && \
 apt-get -y install maven && \
 apt-get -y install git && \
 apt-get -y install libfuse-dev

RUN git clone https://github.com/inesc-id/SafeCloudFS.git /usr/src/safecloudfs

WORKDIR /usr/src/safecloudfs

COPY accounts.json /usr/src/safecloudfs/config/accounts.json

COPY safecloudfs.properties /usr/src/safecloudfs/config/safecloudfs.properties
 
RUN git clone https://github.com/inesc-id/DepSpacito.git

RUN cd DepSpacito && mvn clean && mvn install

RUN sh install.sh
