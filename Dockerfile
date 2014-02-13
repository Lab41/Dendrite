from boxcar/raring
# dev dockerfile
MAINTAINER Charlie Lewis <charliel@lab41.org>

ENV REFRESHED_AT 2014-02-02
RUN sed 's/main$/main universe/' -i /etc/apt/sources.list
RUN apt-get update
RUN apt-get upgrade -y

# Keep upstart from complaining
RUN dpkg-divert --local --rename --add /sbin/initctl
RUN ln -s /bin/true /sbin/initctl

RUN apt-get install -y git
RUN apt-get install -y openjdk-7-jdk
RUN apt-get install -y maven
RUN git clone https://github.com/Lab41/titan.git
RUN cd /titan; git checkout dendrite
RUN mvn install -f /titan/pom.xml -DskipTests
RUN git clone https://github.com/Lab41/faunus.git
RUN cd /faunus; git checkout dendrite
RUN mvn install -f /faunus/pom.xml -DskipTests
RUN apt-get install -y tomcat7
RUN chmod -R 777 /var/lib/tomcat7
ADD . /Dendrite
RUN mvn package -f /Dendrite/pom.xml
RUN cp /Dendrite/target/dendrite-0.1.0.BUILD-SNAPSHOT.war /var/lib/tomcat7/webapps/dendrite.war

EXPOSE 8080

CMD service tomcat7 start && tail -F /var/log/tomcat7/catalina.out
