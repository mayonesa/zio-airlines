package io.scalac.zioairlines.adts

class OptionsMatrix[A] private (matrix: Vector[Vector[Option[A]]], nSomes: Int, capacity: Int):
  def isEmpty(i: Int, j: Int): Boolean = matrix(i)(j).isEmpty
  
  def set(i: Int, j: Int)(a: => A): OptionsMatrix[A] =
    OptionsMatrix(matrix.updated(i, matrix(i).updated(j, Some(a))), nSomes + 1, capacity)

  def addIfEmpty(i: Int, j: Int)(a: => A): Option[OptionsMatrix[A]] =
    matrix(i)(j).fold(Some(set(i, j)(a))) { _ =>
      None
    }

  def atCapacity: Boolean = nSomes == capacity

object OptionsMatrix:
  def empty[A](nRows: Int, nCols: Int): OptionsMatrix[A] =
    val matrix = Vector.fill(nRows, nCols)(Option.empty[A])
    OptionsMatrix(matrix, 0, nRows * nCols)
