/*
 * Copyright 2018 Fs2 Redis
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

package com.github.gvolpe.fs2redis.interpreter.streams

import cats.effect.{ Concurrent, Sync }
import cats.syntax.functor._
import com.github.gvolpe.fs2redis.algebra.RawStreaming
import com.github.gvolpe.fs2redis.streams._
import com.github.gvolpe.fs2redis.effect.JRFuture
import io.lettuce.core.XReadArgs.StreamOffset
import io.lettuce.core.api.StatefulRedisConnection

import scala.collection.JavaConverters._

private[streams] class Fs2RawStreaming[F[_]: Concurrent, K, V](
    val client: StatefulRedisConnection[K, V]
) extends RawStreaming[F, K, V] {

  override def xAdd(key: K, body: Map[K, V]): F[MessageId] =
    JRFuture {
      Sync[F].delay(client.async().xadd(key, body.asJava))
    }.map(MessageId.apply)

  override def xRead(streams: Set[StreamingOffset[K]]): F[List[StreamingMessageWithId[K, V]]] = {
    val offsets = streams.map(s => StreamOffset.from(s.key, s.offset)).toSeq
    JRFuture {
      Sync[F].delay(client.async().xread(offsets: _*))
    }.map { list =>
      list.asScala.toList.map { msg =>
        StreamingMessageWithId[K, V](MessageId(msg.getId), msg.getStream, msg.getBody.asScala.toMap)
      }
    }
  }

}
