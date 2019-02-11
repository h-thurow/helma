FROM openjdk:8 AS stage1

# update the packages database
RUN apt-get update
# install ant
RUN apt-get install -y ant

# copy Helma to the docker image
COPY . /opt/helma

# go to Helma's directory
WORKDIR /opt/helma
# build Helma
RUN ant jar

FROM openjdk:8 AS stage2

# copy Helma from stage1 to stage2
COPY --from=stage1 /opt/helma /opt/helma

# expose the web port
EXPOSE 8080

# export the apps, log, db and external libraries directory as volume
VOLUME ["/opt/helma/apps", "/opt/helma/log", "/opt/helma/db", "/opt/helma/lib/ext"]

# otpional Java options
ENV JAVA_OPTIONS="-server -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
# starts the HTTP server on the given port
# the HTTP server will not be started, if no port is given
ENV HTTP_PORT=""
# starts the XML-RPC server on the given port
# the XML-RPC server will not be started, if no port is given
ENV XMLRPC_PORT=""
# starts the AJP13 server on the given port
# the AJP13 server will not be started, if no port is given
ENV AJP13_PORT=""
# starts the RMI server on the given port
# the RMI server will not be started, if no port is given
ENV RMI_PORT=""

# run Helma
CMD ["/opt/helma/start.sh"]
