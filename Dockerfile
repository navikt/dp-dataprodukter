FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-26@sha256:cdfa6aa5ea388b723e00804707ee8cfef3714a2ff3d7fd7cf817c53d3f601483

ENV TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-Dorg.apache.avro.SERIALIZABLE_PACKAGES=no.nav.dagpenger.dataprodukt", "-cp", "/app/lib/*", "no.nav.dagpenger.dataprodukter.MainKt"]
