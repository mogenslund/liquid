# docker build -t liquid .
# docker run -i -t --rm=true liquid /bin/bash
# ./lein test
# ./lein run

FROM ubuntu

MAINTAINER Mogens Lund <salza@salza.dk>

RUN apt-get update

RUN apt-get install -y \
  curl \
  default-jre \
  net-tools

RUN mkdir -p /workspace
WORKDIR /workspace

RUN curl -O https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein

ENV LEIN_ROOT="true"

ADD . .

RUN ["chmod", "+x", "/workspace/lein"]
RUN ["/bin/bash", "/workspace/lein", "deps"]