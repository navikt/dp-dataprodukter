FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:11190d3b2721c0a6e42b5ee54ecd2e655a27a5c19480e78dc10497d979c38fec

ENV TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.dataprodukter.MainKt"]