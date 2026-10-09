FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-27@sha256:6e4d3208e707cbcb20170ad57b97fb8825bfbf3f4d8d87528801b870f726c6a5

ENV TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-Dorg.apache.avro.SERIALIZABLE_PACKAGES=no.nav.dagpenger.dataprodukt", "-cp", "/app/lib/*", "no.nav.dagpenger.dataprodukter.MainKt"]
