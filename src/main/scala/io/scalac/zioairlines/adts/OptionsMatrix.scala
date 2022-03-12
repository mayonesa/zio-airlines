package io.scalac.zioairlines.adts

import io.scalac.zioairlines.adts.OptionsMatrix.setCell

type Index = Int

case class Coordinates(i: Index, j: Index)

class OptionsMatrix[A] private (matrix: Vector[Vector[Option[A]]], nDefineds: Int):
  def isEmptyAt(i: Index, j: Index): Boolean = matrix(i)(j).isEmpty
  
  def set(i: Index, j: Index)(a: => A): OptionsMatrix[A] =
    OptionsMatrix(setCell(i, j, matrix)(Some(a)), nDefineds + 1)

  def addIfEmpty(i: Int, j: Int)(a: => A): Option[OptionsMatrix[A]] =
    matrix(i)(j).fold(Some(set(i, j)(a))) { _ =>
      None
    }

  def emptyAt(coordinatesSet: Set[Coordinates]): OptionsMatrix[A] =
    OptionsMatrix(coordinatesSet.foldLeft(matrix) { (acc, coordinates) =>
      setCell(coordinates.i, coordinates.j, acc)(None)
    }, nDefineds - coordinatesSet.size)

  def mapOptions[B](f: Option[A] => B): Vector[Vector[B]] = matrix.map(_.map(f))

  def percentCapacity: Int = nDefineds / matrix.size * matrix.head.size * 100

  override def toString: String = matrix.toString

object OptionsMatrix:
  def empty[A](nRows: Int, nCols: Int): OptionsMatrix[A] =
    val matrix = Vector.fill(nRows, nCols)(Option.empty[A])
    OptionsMatrix(matrix, 0)

  private def setCell[A](i: Index, j: Index, matrix: Vector[Vector[Option[A]]])(cell: => Option[A]) =
    matrix.updated(i, matrix(i).updated(j, cell))
