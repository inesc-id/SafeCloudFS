FROM maven:3.5.0-jdk-7

RUN git clone https://github.com/inesc-id/DepSpacito.git /usr/src/depspacito

WORKDIR /usr/src/depspacito

RUN rm config/hosts.config

RUN echo "0 $(hostname -I) 11000" > config/hosts.config

RUN mvn install:install-file -Dfile=lib/PVSS.jar -DgroupId=org.pvss -DartifactId=PVSS -Dversion=0.0.1 -Dpackaging=jar

RUN mvn install:install-file -Dfile=lib/SMaRt-eds.jar -DgroupId=org.bftsmart -DartifactId=SMaRt-eds -Dversion=2.0 -Dpackaging=jar

RUN mvn clean && mvn install
