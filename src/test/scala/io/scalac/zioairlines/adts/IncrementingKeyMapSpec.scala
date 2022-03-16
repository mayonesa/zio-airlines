package io.scalac.zioairlines.adts

import org.scalatest.flatspec.AnyFlatSpec

class IncrementingKeyMapSpec extends AnyFlatSpec:
  private val A = 'a'
  private val mapWithA = IncrementingKeyMap.empty.add(A)

  "add" should "add" in {
    assert(A === mapWithA.get(1).get)
  }
  "get" should "get" in {
    assert(mapWithA.get(1) === Some(A))
  }
  it should "not get" in {
    assert(mapWithA.get(2).isEmpty)
  }
  "update" should "update" in {
    val b = 'b'
    val k = 1
    assert(mapWithA.updated(k, b).get(k).get === b)
  }
  it should "not update" in {
    assertThrows[IndexOutOfBoundsException](mapWithA.updated(2, 'b'))
  }
  "nextKey" should "next-key" in {
    assert(mapWithA.nextKey === 2)
  }
