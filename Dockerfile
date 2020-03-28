# docker build --network=host -t liquid .
# docker run -i -t --network=host --rm=true liquid /bin/bash
# docker run -i -t --network=host --rm=true liquid clojure -m liq.core

FROM clojure:openjdk-8-tools-deps

MAINTAINER Mogens Lund <salza@salza.dk>
RUN mkdir -p /workspace
WORKDIR /workspace

ADD . .
