/*
 * Copyright 2013-2015 Websudos, Limited.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Explicit consent must be obtained from the copyright owner, Outworkers Limited before any redistribution is made.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.websudos.phantom.macros
import scala.reflect.macros.blackbox

class MacroUtils(val c: blackbox.Context) {
  import c.universe._

  def fields(tpe: Type): Iterable[(Name, Type)] = {
    object CaseField {
      def unapply(arg: TermSymbol): Option[(Name, Type)] = {
        if (arg.isVal && arg.isCaseAccessor) {
          Some(newTermName(arg.name.toString.trim) -> arg.typeSignature)
        } else {
          None
        }
      }
    }

    tpe.declarations.collect { case CaseField(name, fType) => name -> fType }
  }

  def baseType(sym: Symbol): Set[Symbol] = {
    if (sym.isClass) {
      val clz = sym.asClass
      val base = clz.baseClasses.toSet - clz //`baseClasses` contains `a` itself
      val basebase = base.flatMap {
        case x: ClassSymbol => x.baseClasses.toSet - x
      }
      base -- basebase
    } else {
      c.abort(c.enclosingPosition, s"Expected a class, but the symbol was different")
    }
  }

  def filterMembers[T : WeakTypeTag, Filter : TypeTag](
    exclusions: Symbol => Option[Symbol] = { s: Symbol => Some(s) }
  ): Set[Symbol] = {
    val tpe = weakTypeOf[T].typeSymbol.typeSignature

    (for {
      baseClass <- tpe.baseClasses.reverse.flatMap(x => exclusions(x))
      symbol <- baseClass.typeSignature.members.sorted
      if symbol.typeSignature <:< typeOf[Filter]
    } yield symbol)(collection.breakOut)
  }


}
