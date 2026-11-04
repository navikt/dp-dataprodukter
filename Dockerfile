FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-27@sha256:ee21878b378116b4efda82a9d453c74503a7c81c4d84f880d3704be62eec022e

ENV TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-Dorg.apache.avro.SERIALIZABLE_PACKAGES=no.nav.dagpenger.dataprodukt", "-cp", "/app/lib/*", "no.nav.dagpenger.dataprodukter.MainKt"]
