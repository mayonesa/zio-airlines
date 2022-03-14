package io.scalac.zioairlines.adts

trait IncrementingKeyMap[A]:
  def add(a: A): IncrementingKeyMap[A]
  def updated(k: Int, a: A): IncrementingKeyMap[A]
  def nextKey: Int
  def get(k: Int): Option[A]

object IncrementingKeyMap:
  def empty[A]: IncrementingKeyMap[A] = new IncrementingKeyMapImpl(Vector.empty[A])

  protected[IncrementingKeyMap] def toIndex(k: Int): Int = k - 1

  private class IncrementingKeyMapImpl[A](map: Vector[A]) extends IncrementingKeyMap[A]:
    override def add(a: A): IncrementingKeyMap[A] = IncrementingKeyMapImpl(map :+ a)

    override def updated(k: Int, a: A): IncrementingKeyMap[A] = new IncrementingKeyMapImpl(map.updated(toIndex(k), a))

    override def nextKey: Int = map.length + 1

    override def get(k: Int): Option[A] = if k > 0 && toIndex(k) < map.size then Some(map(toIndex(k))) else None
