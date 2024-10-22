# dp-dataprodukter

Denne appen lager dataprodukt på BigQuery. 

## Innløp
Den lager nye topic basert på rapiden `teamdagpenger.journalforing.v1`, og disse nye topicene blir til dataprodukter på bigquery ved hjelp av appen [dp-kafka-connect](https://github.com/navikt/dp-kafka-connect).

Dataproduktene er laget med hensikt å dekke etterlevelseskrav på statistikk og styringsinformasjon.

Dataproduktene ligger i bigquery datasettet `Dataprodukt`, og vi har foreløpig plan om følgende tabeller:

- Soknadsinnlop
- Utland
- Permittering
- Personoppslag
- Ettersending
