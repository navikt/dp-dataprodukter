FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-24@sha256:902f4bb3c34aef336fad9c417fcbce476bd05aab120d1f585c94db4a99046659

ENV TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.dataprodukter.MainKt"]