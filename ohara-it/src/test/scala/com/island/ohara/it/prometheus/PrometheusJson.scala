/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.it.prometheus
import com.island.ohara.client.Enum
import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, JsValue, RootJsonFormat}

/**
  * Prometheus json object
  */
object PrometheusJson {

  //Targets Object
  final case class Targets(status: String, data: Data)
  final case class Data(activeTargets: Seq[TargetSeq], droppedTargets: Seq[TargetSeq])

  abstract sealed class Health(val name: String)

  object Health extends Enum[Health] {
    case object UP extends Health("up")

    case object Down extends Health("down")

    case object UnKnown extends Health("unknown")
  }

  implicit val STATE_JSON_FORMAT: RootJsonFormat[Health] = new RootJsonFormat[Health] {
    override def write(obj: Health): JsValue = JsString(obj.name)
    override def read(json: JsValue): Health =
      Health.forName(json.convertTo[String])
  }

  final case class TargetSeq(discoveredLabels: DiscoveredLabels,
                             labels: Labels,
                             scrapeUrl: String,
                             lastError: String,
                             lastScrape: String,
                             health: Health)
  final case class Labels(instance: String, job: String)
  final case class DiscoveredLabels(__address__ : String, __metrics_path__ : String, __scheme__ : String, job: String)

  implicit val DISCOVEREDLABELS_FORMAT: RootJsonFormat[DiscoveredLabels] = jsonFormat4(DiscoveredLabels)
  implicit val LABELS_FORMAT: RootJsonFormat[Labels] = jsonFormat2(Labels)
  implicit val TARGETSEQ_FORMAT: RootJsonFormat[TargetSeq] = jsonFormat6(TargetSeq)
  implicit val DATA_FORMAT: RootJsonFormat[Data] = jsonFormat2(Data)
  implicit val TARGET_FORMAT: RootJsonFormat[Targets] = jsonFormat2(Targets)

  //Config Object
  final case class Config(status: String, data: ConfigData)
  final case class ConfigData(yaml: String)

  implicit val CONFIGDATA_FORMAT: RootJsonFormat[ConfigData] = jsonFormat1(ConfigData)
  implicit val CONFIG_FORMAT: RootJsonFormat[Config] = jsonFormat2(Config)

}
