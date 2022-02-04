package no.nav.dagpenger.data.inntekt

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.LocalDate
import java.util.Timer
import kotlin.concurrent.fixedRateTimer
import kotlin.random.Random.Default.nextDouble

private val logger = KotlinLogging.logger { }

internal class FakeInntektProducer(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic
) : RapidsConnection.StatusListener {
    init {
        rapidsConnection.register(this)
    }

    private val dataGenerator: Timer = fixedRateTimer(name = "fake-inntekt-producer", period = 5000) {
        if (!skalLageFalskeData()) return@fixedRateTimer
        val siste12Mnd = nextDouble(0.0, 500000.0)
        val siste36Mnd = nextDouble(siste12Mnd, 999999.0)

        Dagpengegrunnlag.newBuilder().apply {
            beregningsdato = LocalDate.now()
            gjeldendeGrunnbeloep = listOf(106399.0, 101351.0, 99858.0).random()
            grunnlag = listOf(
                Grunnlag(Grunnlagsperiode.Siste12mnd, siste12Mnd),
                Grunnlag(Grunnlagsperiode.Siste36mnd, siste36Mnd)
            )
            kontekst = Kontekst.Automatisering
        }.build().also { grunnlag ->
            logger.info { "Sender ut $grunnlag" }

            dataTopic.publiser(grunnlag)
        }
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        dataGenerator.cancel()
    }
}
