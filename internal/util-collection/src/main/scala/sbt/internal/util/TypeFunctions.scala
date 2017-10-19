/*
 * sbt
 * Copyright 2011 - 2017, Lightbend, Inc.
 * Copyright 2008 - 2010, Mark Harrah
 * Licensed under BSD-3-Clause license (see LICENSE)
 */

package sbt.internal.util

trait TypeFunctions {
  type Id[X] = X
  sealed trait Const[A] { type Apply[B] = A }
  sealed trait ConstK[A] { type l[L[x]] = A }
  sealed trait Compose[A[_], B[_]] { type Apply[T] = A[B[T]] }
  sealed trait ∙[A[_], B[_]] { type l[T] = A[B[T]] }

  final val left = new (Id ~> Left[?, Nothing]) { def apply[T](t: T) = Left(t) }
  final val right = new (Id ~> Right[Nothing, ?]) { def apply[T](t: T) = Right(t) }
  final val some = new (Id ~> Some) { def apply[T](t: T) = Some(t) }
  final def idFun[T] = (t: T) => t
  final def const[A, B](b: B): A => B = _ => b
  final def idK[M[_]]: M ~> M = new (M ~> M) { def apply[T](m: M[T]): M[T] = m }

  def nestCon[M[_], N[_], G[_]](f: M ~> N): (M ∙ G)#l ~> (N ∙ G)#l =
    f.asInstanceOf[(M ∙ G)#l ~> (N ∙ G)#l] // implemented with a cast to avoid extra object+method call.  castless version:

  /* new ( (M ∙ G)#l ~> (N ∙ G)#l ) {
        def apply[T](mg: M[G[T]]): N[G[T]] = f(mg)
    } */

  type Endo[T] = T => T
  type ~>|[A[_], B[_]] = A ~> Compose[Option, B]#Apply
}

object TypeFunctions extends TypeFunctions

trait ~>[-A[_], +B[_]] { outer =>
  def apply[T](a: A[T]): B[T]
  // directly on ~> because of type inference limitations
  final def ∙[C[_]](g: C ~> A): C ~> B = new (C ~> B) { def apply[T](c: C[T]) = outer.apply(g(c)) }
  final def ∙[C, D](g: C => D)(implicit ev: D <:< A[D]): C => B[D] = i => apply(ev(g(i)))
  final def fn[T] = (t: A[T]) => apply[T](t)
}

object ~> {
  import TypeFunctions._
  val Id: Id ~> Id = new (Id ~> Id) { def apply[T](a: T): T = a }
  implicit def tcIdEquals: (Id ~> Id) = Id
}
