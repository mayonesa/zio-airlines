package io.scalac.zioairlines.adts

trait IncrementingKeyMap[A]:
  def add(a: A): IncrementingKeyMap[A]
  def updated(k: Int, a: A): IncrementingKeyMap[A]
  def nextKey: Int
  def get(k: Int): Option[A]

object IncrementingKeyMap:
  def empty[A]: IncrementingKeyMap[A] = new IncrementingKeyMapImpl(Map.empty, 0)
  
  private class IncrementingKeyMapImpl[A](map: Map[Int, A], latestKey: Int) extends IncrementingKeyMap[A]:
    override def add(a: A): IncrementingKeyMap[A] =
      val k = nextKey
      IncrementingKeyMapImpl(map + (k -> a), k)

    override def updated(k: Int, a: A): IncrementingKeyMap[A] = new IncrementingKeyMapImpl(map.updated(k, a), latestKey)

    override def nextKey: Int = latestKey + 1

    override def get(k: Int): Option[A] = map.get(k)
