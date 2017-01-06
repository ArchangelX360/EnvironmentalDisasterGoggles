FROM java:8

EXPOSE 3030

COPY ./install-fuseki.sh /usr/src/
COPY ./ontology-events.ttl /usr/src/
COPY ./docker-entrypoint.sh .
COPY ./shiro.ini /usr/src/

RUN /usr/src/install-fuseki.sh /usr/src/ontology-events.ttl

ENTRYPOINT ["/bin/bash", "docker-entrypoint.sh"]

