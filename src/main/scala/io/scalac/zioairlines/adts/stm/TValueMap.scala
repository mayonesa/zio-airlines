package io.scalac.zioairlines.adts.stm

import zio.stm.{STM, TRef, USTM}

import java.util.NoSuchElementException

class TValueMap[K, V] private (map: Map[K, TRef[V]]):
  def updateWith(k: K)(f: Option[V] => Option[V]): STM[NoSuchElementException, Unit] =
    map.get(k).fold(STM.fail(NoSuchElementException(s"`$k` does not")))

object TValueMap:
  def make[K, V](kvs: (K, V)*): USTM[TValueMap[K, V]] = kvs.toMap.view.mapValues(TRef.make).map(TValueMap(_))