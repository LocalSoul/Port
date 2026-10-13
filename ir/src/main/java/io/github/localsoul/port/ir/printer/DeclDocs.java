package io.github.localsoul.port.ir.printer;

import io.github.localsoul.port.ir.ast.AttributeUse;
import io.github.localsoul.port.ir.ast.ClassDecl;
import io.github.localsoul.port.ir.ast.CtorDecl;
import io.github.localsoul.port.ir.ast.EnumDecl;
import io.github.localsoul.port.ir.ast.EnumMember;
import io.github.localsoul.port.ir.ast.EventDecl;
import io.github.localsoul.port.ir.ast.FieldDecl;
import io.github.localsoul.port.ir.ast.InterfaceDecl;
import io.github.localsoul.port.ir.ast.IrHeader;
import io.github.localsoul.port.ir.ast.MemberDecl;
import io.github.localsoul.port.ir.ast.MethodDecl;
import io.github.localsoul.port.ir.ast.Modifier;
import io.github.localsoul.port.ir.ast.NodeAttrs;
import io.github.localsoul.port.ir.ast.Param;
import io.github.localsoul.port.ir.ast.ParamMode;
import io.github.localsoul.port.ir.ast.PropDecl;
import io.github.localsoul.port.ir.ast.StructDecl;
import io.github.localsoul.port.ir.ast.TypeAttrs;
import io.github.localsoul.port.ir.ast.TypeDecl;
import io.github.localsoul.port.ir.atom.TypeRef;
import java.util.List;

/** Übersetzt Dateikopf, Typen und Mitglieder in {@link Doc}. */
final class DeclDocs {

  private DeclDocs() {}

  static Doc header(IrHeader header) {
    return GroupBuilder.named("ir")
        .keywordAtom("version", Integer.toString(header.version()))
        .keywordString("assembly", header.assembly())
        .keywordString("sha256", header.sha256())
        .attrs(header.attrs())
        .build();
  }

  static Doc type(TypeDecl type) {
    return switch (type) {
      case ClassDecl c -> classLike("class", c.name(), c.typeAttrs(), c.members(), c.attrs());
      case StructDecl s -> classLike("struct", s.name(), s.typeAttrs(), s.members(), s.attrs());
      case InterfaceDecl i ->
          classLike("interface", i.name(), i.typeAttrs(), i.members(), i.attrs());
      case EnumDecl e -> enumDecl(e);
    };
  }

  private static Doc classLike(
      String head, String name, TypeAttrs typeAttrs, List<MemberDecl> members, NodeAttrs attrs) {
    GroupBuilder builder = GroupBuilder.named(head).atom(name);
    if (typeAttrs.baseType() != null) {
      builder.keywordAtom("base", typeAttrs.baseType().toString());
    }
    builder.keyword("implements", typeList(typeAttrs.implementedTypes()));
    builder.keyword("generics", nameList(typeAttrs.generics()));
    builder.keyword("mods", modifiers(typeAttrs.modifiers()));
    builder.keyword("attrs", attributes(typeAttrs.attributes()));
    builder.bodyStartsHere();
    List<MemberDecl> sorted = CanonicalOrder.members(name, members);
    for (MemberDecl member : sorted) {
      builder.doc(member(member));
    }
    return builder.attrs(attrs).forceBreak(!members.isEmpty()).build();
  }

  private static Doc enumDecl(EnumDecl decl) {
    GroupBuilder builder =
        GroupBuilder.named("enum")
            .atom(decl.name())
            .keywordAtom("underlying", decl.underlyingType().toString())
            .bodyStartsHere();
    for (EnumMember member : CanonicalOrder.enumMembers(decl.members())) {
      Doc memberDoc =
          GroupBuilder.named(member.name())
              .atom(member.value().text())
              .attrs(member.attrs())
              .build();
      builder.doc(memberDoc);
    }
    return builder.attrs(decl.attrs()).forceBreak(!decl.members().isEmpty()).build();
  }

  static Doc member(MemberDecl member) {
    return switch (member) {
      case FieldDecl f -> field(f);
      case PropDecl p -> prop(p);
      case MethodDecl m -> method(m);
      case CtorDecl c -> ctor(c);
      case EventDecl e ->
          GroupBuilder.named("event")
              .atom(e.name())
              .atom(e.type().toString())
              .keyword("mods", modifiers(e.modifiers()))
              .attrs(e.attrs())
              .build();
    };
  }

  private static Doc field(FieldDecl field) {
    GroupBuilder builder =
        GroupBuilder.named("field")
            .atom(field.name())
            .atom(field.type().toString())
            .keyword("mods", modifiers(field.modifiers()))
            .keyword("attrs", attributes(field.attributes()));
    if (field.initializer() != null) {
      builder.keyword("init", ExprDocs.of(field.initializer()));
    }
    return builder.attrs(field.attrs()).build();
  }

  private static Doc prop(PropDecl prop) {
    GroupBuilder builder =
        GroupBuilder.named("prop")
            .atom(prop.name())
            .atom(prop.type().toString())
            .keyword("mods", modifiers(prop.modifiers()));
    if (prop.getter() != null) {
      builder.keyword("get", StmtDocs.of(prop.getter()));
    }
    if (prop.setter() != null) {
      builder.keyword("set", StmtDocs.of(prop.setter()));
    }
    return builder.attrs(prop.attrs()).build();
  }

  private static Doc method(MethodDecl method) {
    GroupBuilder builder =
        GroupBuilder.named("method")
            .atom(method.name())
            .atom(method.returnType().toString())
            .doc(params(method.parameters()))
            .keyword("mods", modifiers(method.modifiers()))
            .keyword("generics", nameList(method.generics()));
    if (method.body() == null) {
      builder.flag("abstract");
    } else {
      builder.doc(StmtDocs.of(method.body()));
    }
    return builder.attrs(method.attrs()).build();
  }

  private static Doc ctor(CtorDecl ctor) {
    GroupBuilder builder =
        GroupBuilder.named("ctor")
            .doc(params(ctor.parameters()))
            .keyword("mods", modifiers(ctor.modifiers()));
    if (ctor.baseCallArguments() != null) {
      Doc arguments = GroupBuilder.headless().docs(ExprDocs.of(ctor.baseCallArguments())).build();
      builder.keyword("base-call", arguments);
    }
    return builder.doc(StmtDocs.of(ctor.body())).attrs(ctor.attrs()).build();
  }

  /** {@code ( {(name type [:ref|:out|:in] [:default expr])} )}. */
  static Doc params(List<Param> params) {
    GroupBuilder list = GroupBuilder.headless();
    for (Param param : params) {
      GroupBuilder builder = GroupBuilder.named(param.name()).atom(param.type().toString());
      if (param.mode() != ParamMode.VALUE) {
        builder.flag(param.mode().spelling());
      }
      if (param.defaultValue() != null) {
        builder.keyword("default", ExprDocs.of(param.defaultValue()));
      }
      list.doc(builder.attrs(param.attrs()).build());
    }
    return list.build();
  }

  /** {@code :mods}, {@code null} bei leerer Liste (Keyword entfällt). */
  private static Doc modifiers(List<Modifier> modifiers) {
    if (modifiers.isEmpty()) {
      return null;
    }
    GroupBuilder list = GroupBuilder.headless();
    for (Modifier modifier : modifiers) {
      list.atom(modifier.spelling());
    }
    return list.build();
  }

  /** {@code :attrs}, {@code null} bei leerer Liste. */
  private static Doc attributes(List<AttributeUse> attributes) {
    if (attributes.isEmpty()) {
      return null;
    }
    GroupBuilder list = GroupBuilder.headless();
    for (AttributeUse attribute : attributes) {
      Doc doc =
          GroupBuilder.named(attribute.type())
              .docs(ExprDocs.of(attribute.arguments()))
              .attrs(attribute.attrs())
              .build();
      list.doc(doc);
    }
    return list.build();
  }

  /** Typliste, {@code null} bei leerer Liste. */
  private static Doc typeList(List<TypeRef> types) {
    if (types.isEmpty()) {
      return null;
    }
    GroupBuilder list = GroupBuilder.headless();
    for (TypeRef type : types) {
      list.atom(type.toString());
    }
    return list.build();
  }

  /** Namensliste, {@code null} bei leerer Liste. */
  private static Doc nameList(List<String> names) {
    if (names.isEmpty()) {
      return null;
    }
    GroupBuilder list = GroupBuilder.headless();
    for (String name : names) {
      list.atom(name);
    }
    return list.build();
  }
}
