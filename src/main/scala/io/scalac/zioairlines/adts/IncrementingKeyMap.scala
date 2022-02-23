package io.scalac.zioairlines.adts

class IncrementingKeyMap[A] private (map: Map[Int, A], latestKey: Int):
  def add(a: A): IncrementingKeyMap[A] =
    val k = nextKey
    IncrementingKeyMap(map + (k -> a), k)
    
  def updated(k: Int, a: A): IncrementingKeyMap[A] = IncrementingKeyMap(map.updated(k, a), latestKey)

  def nextKey: Int = latestKey + 1

  def get(k: Int): Option[A] = map.get(k)

object IncrementingKeyMap:
  def empty[A]: IncrementingKeyMap[A] = IncrementingKeyMap(Map.empty, 0)