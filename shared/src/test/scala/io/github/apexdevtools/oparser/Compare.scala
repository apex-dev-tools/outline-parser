/*
 * Copyright (c) 2022 FinancialForce.com, inc. All rights reserved.
 */

package io.github.apexdevtools.oparser

import io.github.apexdevtools.types.base.Location

object Compare {

  // Compare locations ignoring byte offsets (ANTLR doesn't calculate them for performance)
  private def locationsMatch(loc1: Option[Location], loc2: Option[Location]): Boolean = {
    (loc1, loc2) match {
      case (Some(l1), Some(l2)) =>
        l1.startLine == l2.startLine &&
        l1.startLineOffset == l2.startLineOffset &&
        l1.endLine == l2.endLine &&
        l1.endLineOffset == l2.endLineOffset
      case (None, None) => true
      case _            => false
    }
  }

  def compareClassTypeDeclarations(
    first: TestClassTypeDeclaration,
    second: TestClassTypeDeclaration
  ): Unit = {

    def innerClassTypeDeclarations(o: TestClassTypeDeclaration): Seq[TestClassTypeDeclaration] = {
      o.innerTypes
        .filter(_.isInstanceOf[TestClassTypeDeclaration])
        .map(_.asInstanceOf[TestClassTypeDeclaration])
    }

    def innerInterfaceTypeDeclarations(
      o: TestClassTypeDeclaration
    ): Seq[TestInterfaceTypeDeclaration] = {
      o.innerTypes
        .filter(_.isInstanceOf[TestInterfaceTypeDeclaration])
        .map(_.asInstanceOf[TestInterfaceTypeDeclaration])
    }

    def innerEnumTypeDeclarations(o: TestClassTypeDeclaration): Seq[TestEnumTypeDeclaration] = {
      o.innerTypes
        .filter(_.isInstanceOf[TestEnumTypeDeclaration])
        .map(_.asInstanceOf[TestEnumTypeDeclaration])
    }

    def compareInnerClasses(
      fst: Seq[TestClassTypeDeclaration],
      snd: Seq[TestClassTypeDeclaration]
    ): Unit = {

      fst.foreach(f => {
        val sOpt = snd.find(_.id == f.id)
        if (sOpt.isEmpty) {
          throw new Exception(s"Inner class not found ${f.id} in ${first.id}")
        }
        compareClassTypeDeclarations(f, sOpt.get)
      })
    }

    def compareInnerInterfaces(
      fst: Seq[TestInterfaceTypeDeclaration],
      snd: Seq[TestInterfaceTypeDeclaration]
    ): Unit = {

      fst.foreach(f => {
        val sOpt = snd.find(_.id == f.id)
        if (sOpt.isEmpty) {
          throw new Exception(s"Inner interface not found ${f.id} in ${first.id}")
        }
        compareInterfaceTypeDeclarations(f, sOpt.get)
      })
    }

    def compareInnerEnums(
      fst: Seq[TestEnumTypeDeclaration],
      snd: Seq[TestEnumTypeDeclaration]
    ): Unit = {

      fst.foreach(f => {
        val sOpt = snd.find(_.id == f.id)
        if (sOpt.isEmpty) {
          throw new Exception(s"Inner enum not found ${f.id} in ${first.id}")
        }
        compareEnumTypeDeclarations(f, sOpt.get)
      })
    }

    if (!(first._annotations sameElements second._annotations)) {
      throw new Exception(s"Different annotation ${first._annotations
          .mkString("Array(", ", ", ")")} != ${second._annotations.mkString("Array(", ", ", ")")}")
    }

    // Compare annotation locations (ignoring byte offsets)
    first._annotations.zip(second._annotations).foreach { case (a1, a2) =>
      if (!locationsMatch(a1.location, a2.location)) {
        throw new Exception(
          s"Different annotation location for '${a1.name}': ${a1.location} != ${a2.location}"
        )
      }
    }

    if (!(first._modifiers sameElements second._modifiers)) {
      throw new Exception(s"Different modifiers ${first._modifiers
          .mkString("Array(", ", ", ")")} != ${second._modifiers.mkString("Array(", ", ", ")")}")
    }

    // Compare modifier locations (ignoring byte offsets)
    first._modifiers.zip(second._modifiers).foreach { case (m1, m2) =>
      if (!locationsMatch(m1.location, m2.location)) {
        throw new Exception(
          s"Different modifier location for '${m1.text}': ${m1.location} != ${m2.location}"
        )
      }
    }

    if (first.id != second.id) {
      throw new Exception(s"Different or empty class id ${first.id} != ${second.id}")
    }

    if (first._extendsTypeRef != second._extendsTypeRef) {
      throw new Exception(
        s"Different extends ${first._extendsTypeRef} != ${second._extendsTypeRef}"
      )
    }

    if (first._implementsTypeList != second._implementsTypeList) {
      throw new Exception(
        s"Different implements ${first._implementsTypeList} != ${second._implementsTypeList}"
      )
    }

    if (first._initializers.length != second._initializers.length) {
      throw new Exception(
        s"Different initializers ${first._initializers} != ${second._initializers}"
      )
    }

    if (first._constructors != second._constructors) {
      throw new Exception(
        s"Different constructors ${first._constructors} != ${second._constructors}"
      )
    }

    if (first._methods != second._methods) {
      throw new Exception(s"Different methods ${first._methods} != ${second._methods}")
    }

    if (first._properties != second._properties) {
      throw new Exception(s"Different properties ${first._properties} != ${second._properties}")
    }

    if (first._fields != second._fields) {
      throw new Exception(s"Different fields ${first._fields} != ${second._fields}")
    }

    compareInnerClasses(innerClassTypeDeclarations(first), innerClassTypeDeclarations(second))
    compareInnerClasses(innerClassTypeDeclarations(second), innerClassTypeDeclarations(first))

    compareInnerInterfaces(
      innerInterfaceTypeDeclarations(first),
      innerInterfaceTypeDeclarations(second)
    )
    compareInnerInterfaces(
      innerInterfaceTypeDeclarations(second),
      innerInterfaceTypeDeclarations(first)
    )

    compareInnerEnums(innerEnumTypeDeclarations(first), innerEnumTypeDeclarations(second))
    compareInnerEnums(innerEnumTypeDeclarations(second), innerEnumTypeDeclarations(first))
  }

  def compareInterfaceTypeDeclarations(
    first: TestInterfaceTypeDeclaration,
    second: TestInterfaceTypeDeclaration
  ): Unit = {

    if (!(first.annotations sameElements second.annotations)) {
      throw new Exception(s"Different annotation ${first.annotations
          .mkString("Array(", ", ", ")")} != ${second.annotations.mkString("Array(", ", ", ")")}")
    }

    // Compare annotation locations (ignoring byte offsets)
    first.annotations.zip(second.annotations).foreach { case (a1, a2) =>
      if (!locationsMatch(a1.location, a2.location)) {
        throw new Exception(
          s"Different annotation location for '${a1.name}': ${a1.location} != ${a2.location}"
        )
      }
    }

    if (!(first.modifiers sameElements second.modifiers)) {
      throw new Exception(s"Different modifiers ${first.modifiers
          .mkString("Array(", ", ", ")")} != ${second.modifiers.mkString("Array(", ", ", ")")}")
    }

    // Compare modifier locations (ignoring byte offsets)
    first.modifiers.zip(second.modifiers).foreach { case (m1, m2) =>
      if (!locationsMatch(m1.location, m2.location)) {
        throw new Exception(
          s"Different modifier location for '${m1.text}': ${m1.location} != ${m2.location}"
        )
      }
    }

    if (first.id != second.id) {
      throw new Exception(s"Different or empty interface id ${first.id} != ${second.id}")
    }

    if (first._implementsTypeList != second._implementsTypeList) {
      throw new Exception(
        s"Different extends ${first._implementsTypeList} != ${second._implementsTypeList}"
      )
    }

    if (first.methods != second.methods) {
      throw new Exception(s"Different methods ${first.methods} != ${second.methods}")
    }
  }

  def compareEnumTypeDeclarations(
    first: TestEnumTypeDeclaration,
    second: TestEnumTypeDeclaration
  ): Unit = {

    if (!(first.annotations sameElements second.annotations)) {
      throw new Exception(s"Different annotation ${first.annotations
          .mkString("Array(", ", ", ")")} != ${second.annotations.mkString("Array(", ", ", ")")}")
    }

    // Compare annotation locations (ignoring byte offsets)
    first.annotations.zip(second.annotations).foreach { case (a1, a2) =>
      if (!locationsMatch(a1.location, a2.location)) {
        throw new Exception(
          s"Different annotation location for '${a1.name}': ${a1.location} != ${a2.location}"
        )
      }
    }

    if (!(first.modifiers sameElements second.modifiers)) {
      throw new Exception(s"Different modifiers ${first.modifiers
          .mkString("Array(", ", ", ")")} != ${second.modifiers.mkString("Array(", ", ", ")")}")
    }

    // Compare modifier locations (ignoring byte offsets)
    first.modifiers.zip(second.modifiers).foreach { case (m1, m2) =>
      if (!locationsMatch(m1.location, m2.location)) {
        throw new Exception(
          s"Different modifier location for '${m1.text}': ${m1.location} != ${m2.location}"
        )
      }
    }

    if (first.id != second.id) {
      throw new Exception(s"Different or empty enum id ${first.id} != ${second.id}")
    }

    if (first.fields != second.fields) {
      throw new Exception(s"Different constants ${first.fields} != ${second.fields}")
    }
  }
}
