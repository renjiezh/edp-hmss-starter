package src.test.edp

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.anysketch.SketchConfig
import org.wfanet.anysketch.SketchConfigKt
import org.wfanet.anysketch.SketchProtos
import org.wfanet.anysketch.crypto.ElGamalPublicKey
import org.wfanet.anysketch.crypto.EncryptSketchResponse
import org.wfanet.anysketch.crypto.SketchEncrypterAdapter
import org.wfanet.anysketch.crypto.encryptSketchRequest
import org.wfanet.anysketch.distribution
import org.wfanet.anysketch.exponentialDistribution
import org.wfanet.anysketch.oracleDistribution
import org.wfanet.anysketch.sketchConfig
import org.wfanet.anysketch.uniformDistribution
import org.wfanet.measurement.api.v2alpha.ListRequisitionsRequestKt
import org.wfanet.measurement.api.v2alpha.Measurement
import org.wfanet.measurement.api.v2alpha.MeasurementSpecKt
import org.wfanet.measurement.api.v2alpha.MeasurementSpecKt.vidSamplingInterval
import org.wfanet.measurement.api.v2alpha.PopulationSpecKt
import org.wfanet.measurement.api.v2alpha.Requisition
import org.wfanet.measurement.api.v2alpha.dataProvider
import org.wfanet.measurement.api.v2alpha.fulfillRequisitionRequest
import org.wfanet.measurement.api.v2alpha.listRequisitionsRequest
import org.wfanet.measurement.api.v2alpha.measurementSpec
import org.wfanet.measurement.api.v2alpha.populationSpec
import org.wfanet.measurement.eventdataprovider.shareshuffle.v2alpha.FrequencyVectorBuilder
import org.wfanet.measurement.eventdataprovider.shareshuffle.v2alpha.InMemoryVidIndexMap

@RunWith(JUnit4::class)
class EdpClientTest {

    @Test
    fun `excercise public api libraries`() {
        val dataProvider = dataProvider {
            name = "EDP1"
        }
        val listRequisitionsRequest = listRequisitionsRequest {
            listRequisitionsRequest {
                parent = "dataProviders/1"
                filter = ListRequisitionsRequestKt.filter {
                    states += Requisition.State.UNFULFILLED
                    measurementStates += Measurement.State.AWAITING_REQUISITION_FULFILLMENT
                }
            }
        }
        val fulfillRequisitionRequest = fulfillRequisitionRequest {
        }
    }

    @Test
    fun `excercise any-sketch libraries for LLv2 `() {
        val sketchConfig = sketchConfig {
            indexes +=
                    SketchConfigKt.indexSpec {
                        name = "Index"
                        distribution = distribution {
                            exponential = exponentialDistribution {
                                rate = 0.5
                                numValues = 100000
                            }
                        }
                    }
            values +=
                    SketchConfigKt.valueSpec {
                        name = "SamplingIndicator"
                        aggregator = SketchConfig.ValueSpec.Aggregator.UNIQUE
                        distribution = distribution {
                            uniform = uniformDistribution { numValues = 10000000 }
                        }
                    }

            values +=
                    SketchConfigKt.valueSpec {
                        name = "Frequency"
                        aggregator = SketchConfig.ValueSpec.Aggregator.SUM
                        distribution = distribution { oracle = oracleDistribution { key = "frequency" } }
                    }
        }
        val sketch = SketchProtos.toAnySketch(sketchConfig)

        // TODO: Verify encryptionAdaptor.
    }

    @Test
    fun `excercise hm shuffle libraries for HMSS`() {
        val populationSpec = populationSpec {
            subpopulations += PopulationSpecKt.subPopulation {
                vidRanges += PopulationSpecKt.vidRange {
                    // make the VIDs out of bounds of the frequency vector
                    startVid = 1
                    endVidInclusive = 101
                }
            }
        }

        val hmssVidIndexMap = InMemoryVidIndexMap.build(populationSpec)
        val measurementSpec = measurementSpec {
            reachAndFrequency = MeasurementSpecKt.reachAndFrequency {
                maximumFrequency = 5
            }
            vidSamplingInterval = vidSamplingInterval {
                start = 0.0f
                width = 0.5f
            }
        }
        val frequencyVectorBuilder =
                FrequencyVectorBuilder(hmssVidIndexMap.populationSpec, measurementSpec, strict = false)
        for (i in 1L .. 10L) {
            frequencyVectorBuilder.increment(hmssVidIndexMap[i])
        }
        val sampledFrequencyVector = frequencyVectorBuilder.build()

    }
}