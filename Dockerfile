FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-27@sha256:02c76531a4ac3c8d57885ecfea45d34babdd96b3537913e4860acef7fd155599

ENV TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-Dorg.apache.avro.SERIALIZABLE_PACKAGES=no.nav.dagpenger.dataprodukt", "-cp", "/app/lib/*", "no.nav.dagpenger.dataprodukter.MainKt"]
