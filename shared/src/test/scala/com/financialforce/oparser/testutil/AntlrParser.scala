/*
 * Copyright (c) 2021 FinancialForce.com, inc. All rights reserved.
 */
package com.financialforce.oparser.testutil

import com.financialforce.oparser._
import com.financialforce.types.base._
import com.financialforce.types.{ITypeDeclaration, base}
import com.nawforce.apexparser.ApexParser
import com.nawforce.runtime.parsers.{CodeParser, SourceData}
import com.nawforce.runtime.platform.Path
import com.financialforce.oparser.testutil.AntlrOps._
import com.nawforce.apexparser.ApexParser.ModifierContext

import scala.collection.immutable.ArraySeq
import scala.collection.mutable

object AntlrParser {

  private final val voidTypeRef = Some(
    UnresolvedTypeRef(
      Array(new TypeNameSegment(LocatableIdToken("void", Location.default), TypeRef.emptyArraySeq)),
      0
    )
  )

  def parse(path: String, contents: Array[Byte]): Option[ITypeDeclaration] = {

    // We re-use apex-ls CodeParser for JS/JVM portability
    val codeParser  = CodeParser(Path(path), SourceData(new String(contents)))
    val issuesAndCU = codeParser.parseClass()
    issuesAndCU.issues.headOption.map(issue => throw new Exception(issue.asString))

    val td = issuesAndCU.value.typeDeclaration()
    val cd = CodeParser.toScala(td.classDeclaration())
    val id = CodeParser.toScala(td.interfaceDeclaration())
    val ed = CodeParser.toScala(td.enumDeclaration())
    if (cd.nonEmpty) {
      val ctd                      = new TestClassTypeDeclaration(path, enclosing = null)
      val (annotations, modifiers) = splitAnnotationsAndModifiers(td.modifier())
      ctd.setAnnotations(annotations)
      ctd.setModifiers(modifiers)

      antlrClassTypeDeclaration(ctd, cd.get)
      return Some(ctd)
    } else if (id.nonEmpty) {
      val itd                      = new TestInterfaceTypeDeclaration(path, enclosing = null)
      val (annotations, modifiers) = splitAnnotationsAndModifiers(td.modifier())
      itd.setAnnotations(annotations)
      itd.setModifiers(modifiers)

      antlrInterfaceTypeDeclaration(itd, id.get)
      return Some(itd)
    } else if (ed.nonEmpty) {
      val etd                      = new TestEnumTypeDeclaration(path, enclosing = null)
      val (annotations, modifiers) = splitAnnotationsAndModifiers(td.modifier())
      etd.setAnnotations(annotations)
      etd.setModifiers(modifiers)

      antlrEnumTypeDeclaration(etd, ed.get)
      return Some(etd)
    }
    None
  }

  def toId(ctx: ApexParser.IdContext): LocatableIdToken = {
    LocatableIdToken(CodeParser.getText(ctx), ctx.location)
  }

  def toModifier(ctx: ApexParser.ModifierContext): Modifier = {
    val modifier = CodeParser.getText(ctx)
    val len      = modifier.length
    // Add a space back in, it may not have been a single space but very likely it was
    if (len >= 7 && modifier.toLowerCase.endsWith("sharing")) {
      Modifier(modifier.substring(0, len - 7) + " " + modifier.substring(len - 7))
    } else
      Modifier(modifier)
  }

  def antlrAnnotation(ctx: ApexParser.AnnotationContext): Annotation = {
    val qName = QualifiedName(
      CodeParser.toScala(ctx.qualifiedName().id()).map(id => toId(id)).toArray
    )
    val args = CodeParser
      .toScala(ctx.elementValue())
      .map(CodeParser.getText)
      .orElse(CodeParser.toScala(ctx.elementValuePairs()).map(CodeParser.getText))
      .orElse(if (CodeParser.getText(ctx).endsWith("()")) Some("") else None)
    Annotation(qName.toString, args)
  }

  def antlrTypeList(ctx: ApexParser.TypeListContext): ArraySeq[TypeRef] = {
    CodeParser
      .toScala(ctx.typeRef())
      .map(tr => antlrTypeRef(tr))
  }

  def antlrTypeArguments(ctx: ApexParser.TypeArgumentsContext): ArraySeq[TypeRef] = {
    antlrTypeList(ctx.typeList())
  }

  def antlrTypeName(ctx: ApexParser.TypeNameContext): TypeNameSegment = {
    val typeArguments =
      CodeParser
        .toScala(ctx.typeArguments())
        .map(ta => antlrTypeArguments(ta))
        .getOrElse(TypeRef.emptyArraySeq)
    CodeParser
      .toScala(ctx.LIST())
      .map(l => new TypeNameSegment(LocatableIdToken(l.toString, Location.default), typeArguments))
      .getOrElse(
        CodeParser
          .toScala(ctx.SET())
          .map(l =>
            new TypeNameSegment(LocatableIdToken(l.toString, Location.default), typeArguments)
          )
          .getOrElse(
            CodeParser
              .toScala(ctx.MAP())
              .map(l =>
                new TypeNameSegment(LocatableIdToken(l.toString, Location.default), typeArguments)
              )
              .getOrElse(new TypeNameSegment(toId(ctx.id()), typeArguments))
          )
      )
  }

  def antlrTypeRef(ctx: ApexParser.TypeRefContext): UnresolvedTypeRef = {
    val segments = new mutable.ArrayBuffer[TypeNameSegment]()
    CodeParser
      .toScala(ctx.typeName())
      .foreach(tn => {
        segments.append(antlrTypeName(tn))
      })

    base.UnresolvedTypeRef(
      segments.toArray,
      CodeParser.getText(ctx.arraySubscripts()).count(_ == ']')
    )
  }

  def antlrClassTypeDeclaration(
    ctd: TestClassTypeDeclaration,
    ctx: ApexParser.ClassDeclarationContext
  ): Unit = {
    ctd.setId(toId(ctx.id()))

    CodeParser.toScala(ctx.typeRef()).foreach(tr => ctd.setExtends(antlrTypeRef(tr)))
    CodeParser.toScala(ctx.typeList()).foreach(tl => ctd.setImplements(antlrTypeList(tl)))

    CodeParser.toScala(ctx.classBody().classBodyDeclaration()).foreach { c =>
      {
        CodeParser
          .toScala(c.memberDeclaration())
          .foreach(d => {
            val md                       = new MemberDeclaration
            val (annotations, modifiers) = splitAnnotationsAndModifiers(c.modifier())
            md.setAnnotations(annotations)
            md.setModifiers(modifiers)

            CodeParser
              .toScala(d.classDeclaration())
              .foreach(icd => {
                val innerClassDeclaration = new TestClassTypeDeclaration(ctd.path, ctd)
                innerClassDeclaration.setAnnotations(md.annotations)
                innerClassDeclaration.setModifiers(md.modifiers)
                ctd.appendInnerType(innerClassDeclaration)
                antlrClassTypeDeclaration(innerClassDeclaration, icd)
              })

            CodeParser
              .toScala(d.interfaceDeclaration())
              .foreach(iid => {
                val innerInterfaceDeclaration = new TestInterfaceTypeDeclaration(ctd.path, ctd)
                innerInterfaceDeclaration.setAnnotations(md.annotations)
                innerInterfaceDeclaration.setModifiers(md.modifiers)
                ctd.appendInnerType(innerInterfaceDeclaration)
                antlrInterfaceTypeDeclaration(innerInterfaceDeclaration, iid)
              })

            CodeParser
              .toScala(d.enumDeclaration())
              .foreach(ied => {
                val innerEnumDeclaration = new TestEnumTypeDeclaration(ctd.path, ctd)
                innerEnumDeclaration.setAnnotations(md.annotations)
                innerEnumDeclaration.setModifiers(md.modifiers)
                ctd.appendInnerType(innerEnumDeclaration)
                antlrEnumTypeDeclaration(innerEnumDeclaration, ied)
              })

            CodeParser
              .toScala(d.constructorDeclaration())
              .foreach(antlrConstructorDeclaration(ctd, md, _))
            CodeParser
              .toScala(d.methodDeclaration())
              .foreach(antlrMethodDeclaration(ctd, md, _))
            CodeParser
              .toScala(d.propertyDeclaration())
              .foreach(antlrPropertyDeclaration(ctd, md, _))
            CodeParser
              .toScala(d.fieldDeclaration())
              .foreach(antlrFieldDeclaration(ctd, md, _))
          })
        CodeParser
          .toScala(c.block())
          .foreach(_ => {
            ctd.appendInitializer(Initializer(Option(c.STATIC()).isDefined))
          })
      }
    }
  }

  def antlrInterfaceTypeDeclaration(
    itd: TestInterfaceTypeDeclaration,
    ctx: ApexParser.InterfaceDeclarationContext
  ): Unit = {
    itd.setId(toId(ctx.id()))

    CodeParser.toScala(ctx.typeList()).foreach(tl => itd.setImplements(antlrTypeList(tl)))

    CodeParser
      .toScala(
        ctx
          .interfaceBody()
          .interfaceMethodDeclaration()
      )
      .foreach(mctx => {
        val md                       = new MemberDeclaration
        val (annotations, modifiers) = splitAnnotationsAndModifiers(mctx.modifier())
        md.setAnnotations(annotations)
        md.setModifiers(modifiers)
        antlrMethodDeclaration(itd, md, mctx)
      })
  }

  def antlrEnumTypeDeclaration(
    etd: TestEnumTypeDeclaration,
    ctx: ApexParser.EnumDeclarationContext
  ): Unit = {
    etd.setId(toId(ctx.id()))

    CodeParser
      .toScala(
        ctx
          .enumConstants()
      )
      .map(c => CodeParser.toScala(c.id()))
      .getOrElse(ArraySeq())
      .foreach(ictx => {
        val id = toId(ictx)
        etd.appendField(FieldDeclaration(Array(), Array(Modifier("static")), etd, id))
      })
  }

  def antlrConstructorDeclaration(
    ctd: TestClassTypeDeclaration,
    md: MemberDeclaration,
    ctx: ApexParser.ConstructorDeclarationContext
  ): Unit = {

    val qName = QualifiedName(CodeParser.toScala(ctx.qualifiedName().id()).map(toId).toArray)
    val formalParameterList =
      CodeParser
        .toScala(
          ctx
            .formalParameters()
            .formalParameterList()
        )
        .map(fpl =>
          ArraySeq.unsafeWrapArray(
            CodeParser
              .toScala(
                fpl
                  .formalParameter()
              )
              .map(antlrFormalParameter)
              .toArray
          )
        )
        .getOrElse(FormalParameter.emptyArraySeq)

    val constructor =
      ConstructorDeclaration(md.annotations, md.modifiers, qName, formalParameterList)

    ctd.appendConstructor(constructor)
  }

  def antlrMethodDeclaration(
    res: TestTypeDeclaration,
    md: MemberDeclaration,
    ctx: ApexParser.MethodDeclarationContext
  ): Unit = {

    val id = toId(ctx.id())

    val formalParameterList =
      CodeParser
        .toScala(ctx.formalParameters().formalParameterList())
        .map(fpl =>
          ArraySeq.unsafeWrapArray(
            CodeParser
              .toScala(
                fpl
                  .formalParameter()
              )
              .map(antlrFormalParameter)
              .toArray
          )
        )
        .getOrElse(FormalParameter.emptyArraySeq)

    CodeParser.toScala(ctx.typeRef()) match {
      case Some(tr) => md.add(antlrTypeRef(tr))
      case None     => md.typeRef = voidTypeRef
    }

    val method =
      MethodDeclaration(md.annotations, md.modifiers, md.typeRef, id, formalParameterList)

    res.appendMethod(method)
  }

  def antlrMethodDeclaration(
    res: TestTypeDeclaration,
    md: MemberDeclaration,
    ctx: ApexParser.InterfaceMethodDeclarationContext
  ): Unit = {

    val id = toId(ctx.id())

    val formalParameterList =
      CodeParser
        .toScala(ctx.formalParameters().formalParameterList())
        .map(fpl =>
          ArraySeq.unsafeWrapArray(
            CodeParser
              .toScala(
                fpl
                  .formalParameter()
              )
              .map(antlrFormalParameter)
              .toArray
          )
        )
        .getOrElse(FormalParameter.emptyArraySeq)

    CodeParser.toScala(ctx.typeRef()) match {
      case Some(tr) => md.add(antlrTypeRef(tr))
      case None     => md.typeRef = voidTypeRef
    }

    val method =
      MethodDeclaration(md.annotations, md.modifiers, md.typeRef, id, formalParameterList)

    res.appendMethod(method)
  }

  def antlrFormalParameter(ctx: ApexParser.FormalParameterContext): FormalParameter = {
    val (annotations, modifiers) = splitAnnotationsAndModifiers(ctx.modifier())
    FormalParameter(annotations, modifiers, antlrTypeRef(ctx.typeRef()), toId(ctx.id()))
  }

  def antlrPropertyDeclaration(
    ctd: TestClassTypeDeclaration,
    md: MemberDeclaration,
    ctx: ApexParser.PropertyDeclarationContext
  ): Unit = {

    val id = toId(ctx.id())
    md.add(antlrTypeRef(ctx.typeRef()))

    val property = PropertyDeclaration(md.annotations, md.modifiers, md.typeRef.get, Array(), id)
    ctd.appendProperty(property)
  }

  def antlrFieldDeclaration(
    ctd: TestClassTypeDeclaration,
    md: MemberDeclaration,
    ctx: ApexParser.FieldDeclarationContext
  ): Unit = {
    md.add(antlrTypeRef(ctx.typeRef()))

    CodeParser
      .toScala(ctx.variableDeclarators().variableDeclarator())
      .foreach(v => {
        val id = toId(v.id())
        val field =
          FieldDeclaration(md.annotations, md.modifiers, md.typeRef.get, id)
        ctd.appendField(field)
      })
  }

  private def splitAnnotationsAndModifiers(
    context: AntlrCollection[ApexParser.ModifierContext]
  ): (Array[Annotation], Array[Modifier]) = {
    val ctxArray: ArraySeq[ModifierContext] = CodeParser.toScala(context)
    (
      ctxArray
        .flatMap(m => CodeParser.toScala(m.annotation()))
        .map(m => antlrAnnotation(m))
        .toArray,
      ctxArray
        .filter(m => CodeParser.toScala(m.annotation()).isEmpty)
        .map(toModifier)
        .toArray
    )
  }
}
