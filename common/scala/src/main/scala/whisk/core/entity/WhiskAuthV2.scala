/*
 * Copyright 2015-2016 IBM Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package whisk.core.entity

import spray.json._
import scala.util.Try

/**
 * Represents a namespace for a subject as stored in the authentication
 * database. Each namespace has its own key which is used to determine
 * the {@ Identity} of the user calling.
 */
case class WhiskNamespace(name: EntityName, authkey: AuthKey)

object WhiskNamespace extends DefaultJsonProtocol {
    implicit val serdes = new RootJsonFormat[WhiskNamespace] {
        def write(w: WhiskNamespace) = JsObject(
            "name" -> w.name.toJson,
            "uuid" -> w.authkey.uuid.toJson,
            "key" -> w.authkey.key.toJson)

        def read(value: JsValue) = Try {
            value.asJsObject.getFields("name", "uuid", "key") match {
                case Seq(JsString(n), JsString(u), JsString(k)) =>
                    WhiskNamespace(EntityName(n), AuthKey(UUID(u), Secret(k)))
            }
        } getOrElse deserializationError("namespace record malformed")
    }
}

/**
 * Represents the new version of entries in the subjects database. No
 * top-level authkey is given but each subject has a set of namespaces,
 * which in turn have the keys.
 */
case class WhiskAuthV2(
    subject: Subject,
    namespaces: Set[WhiskNamespace])
    extends WhiskDocument {

    override def docid = DocId(subject())

    def toJson = JsObject(
        "subject" -> subject.toJson,
        "namespaces" -> namespaces.toJson)
}

object WhiskAuthV2 extends DefaultJsonProtocol {
    // Need to explicitly set field names since WhiskAuthV2 extends WhiskDocument
    // which defines more than the 2 "standard" fields
    implicit val serdes = jsonFormat(WhiskAuthV2.apply, "subject", "namespaces")
}
