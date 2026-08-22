/*
 * Copyright (c) 2021 FinancialForce.com, inc. All rights reserved.
 */
package io.github.apexdevtools.oparser.testutil

import io.github.apexdevtools.apexparser.{ApexErrorListener, ApexParser, ApexParserFactory}
import io.github.apexdevtools.oparser._
import io.github.apexdevtools.oparser.testutil.AntlrOps._
import io.github.apexdevtools.types.base._
import io.github.apexdevtools.types.{ITypeDeclaration, base}
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.tree.TerminalNode

import scala.collection.immutable.ArraySeq
import scala.collection.mutable
import scala.jdk.CollectionConverters._

object AntlrParser {

  private final val voidTypeRef = Some(
    UnresolvedTypeRef(
      Array(new TypeNameSegment(LocatableIdToken("void", Location.default), TypeRef.emptyArraySeq)),
      0
    )
  )

  def parse(path: String, contents: Array[Byte]): Option[ITypeDeclaration] = {
    val stream        = CharStreams.fromString(new String(contents, "UTF-8"))
    val errorListener = new CollectingErrorListener
    val parserPair    = ApexParserFactory.createLexerAndParser(stream, errorListener)

    val cu = parserPair.getParser.compilationUnit()
    errorListener.firstError.foreach(msg => throw new Exception(msg))

    val td = cu.typeDeclaration()
    if (td == null) return None

    val cd = Option(td.classDeclaration())
    val id = Option(td.interfaceDeclaration())
    val ed = Option(td.enumDeclaration())
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
    LocatableIdToken(ctx.getText, ctx.location)
  }

  def toModifier(ctx: ApexParser.ModifierContext): Modifier = {
    val modifier = ctx.getText
    val len      = modifier.length
    // Add a space back in, it may not have been a single space but very likely it was
    if (len >= 7 && modifier.toLowerCase.endsWith("sharing")) {
      Modifier(
        modifier.substring(0, len - 7) + " " + modifier.substring(len - 7),
        Some(ctx.location)
      )
    } else
      Modifier(modifier, Some(ctx.location))
  }

  def antlrAnnotation(ctx: ApexParser.AnnotationContext): Annotation = {
    val hasParameters = ctx.LPAREN() != null
    val parameters =
      if (hasParameters)
        Some(
          Option(ctx.elementValue())
            .map(_.getText)
            .orElse(Option(ctx.elementValuePairs()).map(_.getText))
            .getOrElse("")
        )
      else None
    val parameterList = if (hasParameters) Some(antlrAnnotationParameters(ctx)) else None
    Annotation(ctx.id().getText, parameters, Some(ctx.location), parameterList)
  }

  /* Build the same parameter model the outline parser produces, so the two can be compared. A
   * comma is a token in the tree, its absence between two pairs means they were separated by
   * whitespace. */
  private def antlrAnnotationParameters(
    ctx: ApexParser.AnnotationContext
  ): ArraySeq[AnnotationParameter] = {
    Option(ctx.elementValue())
      .map(value =>
        ArraySeq(
          AnnotationParameter(
            None,
            value.getText,
            None,
            None,
            Some(value.location),
            Some(value.location)
          )
        )
      )
      .orElse(Option(ctx.elementValuePairs()).map(antlrElementValuePairs))
      .getOrElse(AnnotationParameter.emptyArraySeq)
  }

  private def antlrElementValuePairs(
    ctx: ApexParser.ElementValuePairsContext
  ): ArraySeq[AnnotationParameter] = {
    val parameters = ArraySeq.newBuilder[AnnotationParameter]
    var isFirst    = true
    var sawComma   = false

    ctx.children.asScala.foreach {
      case pair: ApexParser.ElementValuePairContext =>
        val separator =
          if (isFirst) None
          else if (sawComma) Some(AnnotationParameterSeparator.Comma)
          else Some(AnnotationParameterSeparator.Whitespace)
        val value = pair.elementValue()
        parameters += AnnotationParameter(
          Some(pair.id().getText),
          value.getText,
          separator,
          Some(pair.id().location),
          Some(value.location),
          Some(pair.location)
        )
        isFirst = false
        sawComma = false
      case node: TerminalNode if node.getSymbol.getType == ApexParser.COMMA =>
        sawComma = true
      case _ => ()
    }
    parameters.result()
  }

  def antlrTypeList(ctx: ApexParser.TypeListContext): ArraySeq[TypeRef] = {
    ArraySeq.from(ctx.typeRef().asScala.map(tr => antlrTypeRef(tr)))
  }

  def antlrTypeArguments(ctx: ApexParser.TypeArgumentsContext): ArraySeq[TypeRef] = {
    antlrTypeList(ctx.typeList())
  }

  def antlrTypeName(ctx: ApexParser.TypeNameContext): TypeNameSegment = {
    val typeArguments =
      Option(ctx.typeArguments())
        .map(ta => antlrTypeArguments(ta))
        .getOrElse(TypeRef.emptyArraySeq)
    Option(ctx.LIST(): TerminalNode)
      .map(l => new TypeNameSegment(LocatableIdToken(l.toString, Location.default), typeArguments))
      .getOrElse(
        Option(ctx.SET(): TerminalNode)
          .map(l =>
            new TypeNameSegment(LocatableIdToken(l.toString, Location.default), typeArguments)
          )
          .getOrElse(
            Option(ctx.MAP(): TerminalNode)
              .map(l =>
                new TypeNameSegment(LocatableIdToken(l.toString, Location.default), typeArguments)
              )
              .getOrElse(new TypeNameSegment(toId(ctx.id()), typeArguments))
          )
      )
  }

  def antlrTypeRef(ctx: ApexParser.TypeRefContext): UnresolvedTypeRef = {
    val segments = new mutable.ArrayBuffer[TypeNameSegment]()
    ctx
      .typeName()
      .asScala
      .foreach(tn => {
        segments.append(antlrTypeName(tn))
      })

    base.UnresolvedTypeRef(
      segments.toArray,
      Option(ctx.arraySubscripts()).map(_.getText).getOrElse("").count(_ == ']')
    )
  }

  def antlrClassTypeDeclaration(
    ctd: TestClassTypeDeclaration,
    ctx: ApexParser.ClassDeclarationContext
  ): Unit = {
    ctd.setId(toId(ctx.id()))

    Option(ctx.typeRef()).foreach(tr => ctd.setExtends(antlrTypeRef(tr)))
    Option(ctx.typeList()).foreach(tl => ctd.setImplements(antlrTypeList(tl)))

    ctx.classBody().classBodyDeclaration().asScala.foreach { c =>
      {
        Option(c.memberDeclaration())
          .foreach(d => {
            val md                       = new MemberDeclaration
            val (annotations, modifiers) = splitAnnotationsAndModifiers(c.modifier())
            md.setAnnotations(annotations)
            md.setModifiers(modifiers)

            Option(d.classDeclaration())
              .foreach(icd => {
                val innerClassDeclaration = new TestClassTypeDeclaration(ctd.path, ctd)
                innerClassDeclaration.setAnnotations(md.annotations)
                innerClassDeclaration.setModifiers(md.modifiers)
                ctd.appendInnerType(innerClassDeclaration)
                antlrClassTypeDeclaration(innerClassDeclaration, icd)
              })

            Option(d.interfaceDeclaration())
              .foreach(iid => {
                val innerInterfaceDeclaration = new TestInterfaceTypeDeclaration(ctd.path, ctd)
                innerInterfaceDeclaration.setAnnotations(md.annotations)
                innerInterfaceDeclaration.setModifiers(md.modifiers)
                ctd.appendInnerType(innerInterfaceDeclaration)
                antlrInterfaceTypeDeclaration(innerInterfaceDeclaration, iid)
              })

            Option(d.enumDeclaration())
              .foreach(ied => {
                val innerEnumDeclaration = new TestEnumTypeDeclaration(ctd.path, ctd)
                innerEnumDeclaration.setAnnotations(md.annotations)
                innerEnumDeclaration.setModifiers(md.modifiers)
                ctd.appendInnerType(innerEnumDeclaration)
                antlrEnumTypeDeclaration(innerEnumDeclaration, ied)
              })

            Option(d.constructorDeclaration())
              .foreach(antlrConstructorDeclaration(ctd, md, _))
            Option(d.methodDeclaration())
              .foreach(antlrMethodDeclaration(ctd, md, _))
            Option(d.propertyDeclaration())
              .foreach(antlrPropertyDeclaration(ctd, md, _))
            Option(d.fieldDeclaration())
              .foreach(antlrFieldDeclaration(ctd, md, _))
          })
        Option(c.block())
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

    Option(ctx.typeList()).foreach(tl => itd.setImplements(antlrTypeList(tl)))

    ctx
      .interfaceBody()
      .interfaceMethodDeclaration()
      .asScala
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

    Option(ctx.enumConstants())
      .map(c => c.id().asScala.toSeq)
      .getOrElse(Seq.empty)
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

    val qName =
      QualifiedName(ctx.qualifiedName().id().asScala.map(toId).toArray)
    val formalParameterList =
      Option(
        ctx
          .formalParameters()
          .formalParameterList()
      )
        .map(fpl =>
          ArraySeq.unsafeWrapArray(
            fpl
              .formalParameter()
              .asScala
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
      Option(ctx.formalParameters().formalParameterList())
        .map(fpl =>
          ArraySeq.unsafeWrapArray(
            fpl
              .formalParameter()
              .asScala
              .map(antlrFormalParameter)
              .toArray
          )
        )
        .getOrElse(FormalParameter.emptyArraySeq)

    Option(ctx.typeRef()) match {
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
      Option(ctx.formalParameters().formalParameterList())
        .map(fpl =>
          ArraySeq.unsafeWrapArray(
            fpl
              .formalParameter()
              .asScala
              .map(antlrFormalParameter)
              .toArray
          )
        )
        .getOrElse(FormalParameter.emptyArraySeq)

    Option(ctx.typeRef()) match {
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

    ctx
      .variableDeclarators()
      .variableDeclarator()
      .asScala
      .foreach(v => {
        val id = toId(v.id())
        val field =
          FieldDeclaration(md.annotations, md.modifiers, md.typeRef.get, id)
        ctd.appendField(field)
      })
  }

  private def splitAnnotationsAndModifiers(
    context: java.util.List[ApexParser.ModifierContext]
  ): (Array[Annotation], Array[Modifier]) = {
    val ctxArray = context.asScala
    (
      ctxArray
        .flatMap(m => Option(m.annotation()))
        .map(m => antlrAnnotation(m))
        .toArray,
      ctxArray
        .filter(m => Option(m.annotation()).isEmpty)
        .map(toModifier)
        .toArray
    )
  }

  private class CollectingErrorListener extends ApexErrorListener {
    private val errors = mutable.ArrayBuffer.empty[String]
    override def apexSyntaxError(line: Int, column: Int, msg: String): Unit = {
      errors += s"line $line:$column $msg"
    }
    def firstError: Option[String] = errors.headOption
  }
}
