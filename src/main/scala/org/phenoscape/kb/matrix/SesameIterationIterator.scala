package org.phenoscape.kb.matrix

import scala.language.implicitConversions

import info.aduna.iteration.Iteration

class SesameIterationIterator[A, B <: Exception](iteration: Iteration[A, B]) extends Iterator[A] {

  def hasNext: Boolean = iteration.hasNext

  def next(): A = iteration.next

}

object SesameIterationIterator {

  implicit def iterationToIterator[A, B <: Exception](iteration: Iteration[A, B]): Iterator[A] = new SesameIterationIterator(iteration)

}