package io.scalac.zioairlines.adts

type Index = Int

class OptionsMatrix[A] private (matrix: Vector[Vector[Option[A]]], nDefineds: Int):
  def isEmpty(i: Int, j: Int): Boolean = matrix(i)(j).isEmpty
  
  def set(i: Int, j: Int)(a: => A): OptionsMatrix[A] =
    OptionsMatrix(matrix.updated(i, matrix(i).updated(j, Some(a))), nDefineds + 1)

  def addIfEmpty(i: Int, j: Int)(a: => A): Option[OptionsMatrix[A]] =
    matrix(i)(j).fold(Some(set(i, j)(a))) { _ =>
      None
    }

  def empties: Set[(Index, Index)] =
    (for
      (column, i) <- matrix.zipWithIndex
      j           <- column.indices
    yield (i, j)).toSet

  def atCapacity: Boolean = nDefineds == matrix.size * matrix(0).size

object OptionsMatrix:
  def empty[A](nRows: Int, nCols: Int): OptionsMatrix[A] =
    val matrix = Vector.fill(nRows, nCols)(Option.empty[A])
    OptionsMatrix(matrix, 0)
