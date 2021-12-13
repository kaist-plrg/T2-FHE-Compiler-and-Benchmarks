package org.twc.terminator.t2dsl_compiler;

import org.twc.terminator.SymbolTable;
import org.twc.terminator.Var_t;
import org.twc.terminator.t2dsl_compiler.T2DSLsyntaxtree.*;

import java.util.ArrayList;
import java.util.List;

public class T2_2_HElib extends T2_Compiler {

  public T2_2_HElib(SymbolTable st) {
    super(st);
    this.st_.backend_types.put("EncInt", "Ctxt");
    this.st_.backend_types.put("EncInt[]", "vector<Ctxt>");
  }

  protected String new_ctxt_tmp() {
    tmp_cnt_++;
    String ctxt_tmp_ = "tmp_" + tmp_cnt_ + "_";
    append_idx(this.st_.backend_types.get("EncInt"));
    this.asm_.append(" ").append(ctxt_tmp_).append("(public_key);\n");
    return ctxt_tmp_;
  }

  protected void append_keygen() {
    append_idx("unsigned long p = 509, m = 14481, r = 1, bits = 120;\n");
    append_idx("unsigned long c = 2;\n");
    append_idx("Context context = helib::ContextBuilder<helib::BGV>()\n");
    append_idx("  .m(m).p(p).r(r).bits(bits).c(c).build();\n");
    append_idx("SecKey secret_key(context);\n");
    append_idx("secret_key.GenSecKey();\n");
    append_idx("addSome1DMatrices(secret_key);\n");
    append_idx("const PubKey& public_key = secret_key;\n");
    append_idx("const EncryptedArray& ea = context.getEA();\n");
    append_idx("long nslots = ea.size();\n\n");
    append_idx("Ptxt<helib::BGV> tmp(context);\n");
    append_idx("Ctxt tmp_(public_key);\n\n");
  }

  /**
   * f0 -> "int"
   * f1 -> "main"
   * f2 -> "("
   * f3 -> "void"
   * f4 -> ")"
   * f5 -> "{"
   * f6 -> ( VarDeclaration() )*
   * f7 -> ( Statement() )*
   * f8 -> "return"
   * f9 -> Expression()
   * f10 -> ";"
   * f11 -> "}"
   */
  public Var_t visit(MainClass n) throws Exception {
    append_idx("#include <iostream>\n\n");
    append_idx("#include <helib/helib.h>\n");
//    append_idx("#include \"../helper.hpp\"\n\n");
    append_idx("using namespace helib;\n");
    append_idx("using namespace std;\n\n");
    append_idx("int main(void) {\n");
    this.indent_ = 2;
    append_keygen();
    n.f6.accept(this);
    n.f7.accept(this);
    append_idx("return ");
    Var_t ret = n.f9.accept(this);
    this.asm_.append(ret.getName()).append(";\n}");
    return null;
  }

  /**
   * f0 -> Type()
   * f1 -> Identifier()
   * f2 -> ( VarDeclarationRest() )*
   * f3 -> ";"
   */
  public Var_t visit(VarDeclaration n) throws Exception {
    append_idx("");
    String type = n.f0.accept(this).getType();
    this.asm_.append(this.st_.backend_types.get(type));
    this.asm_.append(" ");
    Var_t id = n.f1.accept(this);
    this.asm_.append(id.getName());
    if (type.equals("EncInt")) {
      this.asm_.append("(public_key)");
    }
    if (n.f2.present()) {
      for (int i = 0; i < n.f2.size(); i++) {
        n.f2.nodes.get(i).accept(this);
        if (type.equals("EncInt")) {
          this.asm_.append("(public_key)");
        }
      }
    }
    this.asm_.append(";\n");
    return null;
  }

  /**
   * f0 -> Identifier()
   * f1 -> "="
   * f2 -> Expression()
   */
  public Var_t visit(AssignmentStatement n) throws Exception {
    Var_t lhs = n.f0.accept(this);
    String lhs_type = st_.findType(lhs);
    Var_t rhs = n.f2.accept(this);
    String rhs_type = st_.findType(rhs);
    String rhs_name = rhs.getName();
    if (lhs_type.equals("EncInt") && rhs_type.equals("int")) {
      // if EncInt <- int
      append_idx("tmp[0] = ");
      this.asm_.append(rhs_name);
      this.asm_.append(";\n");
      append_idx("public_key.Encrypt(");
      this.asm_.append(lhs.getName()).append(", tmp)");
      this.semicolon_ = true;
    } else if (lhs_type.equals("EncInt[]") && rhs_type.equals("int[]")) {
      // if EncInt[] <- int[]
      tmp_cnt_++;
      String tmp_i = "i_" + tmp_cnt_;
      append_idx(lhs.getName());
      this.asm_.append(".resize(").append(rhs_name).append(".size(), tmp_);\n");
      append_idx("for (size_t ");
      this.asm_.append(tmp_i).append(" = 0; ").append(tmp_i).append(" < ");
      this.asm_.append(rhs_name).append(".size(); ++").append(tmp_i);
      this.asm_.append(") {\n");
      this.indent_ += 2;
      append_idx("tmp[0] = ");
      this.asm_.append(rhs_name).append("[").append(tmp_i).append("];\n");
      append_idx("public_key.Encrypt(");
      this.asm_.append(lhs.getName()).append("[").append(tmp_i);
      this.asm_.append("], tmp);\n");
      this.indent_ -= 2;
      append_idx("}\n");
    } else if (lhs_type.equals(rhs_type)) {
      // if the destination has the same type as the source.
      append_idx(lhs.getName());
      if (rhs_name.startsWith("resize(")) {
        this.asm_.append(".");
      } else {
        this.asm_.append(" = ");
      }
      this.asm_.append(rhs_name);
      this.semicolon_ = true;
    } else {
      throw new Exception("Error assignment statement between different " +
              "types: " + lhs_type + ", " + rhs_type);
    }
    return null;
  }

  /**
   * f0 -> Identifier()
   * f1 -> "++"
   */
  public Var_t visit(IncrementAssignmentStatement n) throws Exception {
    Var_t id = n.f0.accept(this);
    String id_type = st_.findType(id);
    if (id_type.equals("EncInt")) {
      append_idx(id.getName());
      this.asm_.append(".addConstant(NTL::ZZX(1));\n");
    } else {
      append_idx(id.getName());
      this.asm_.append("++");
      this.semicolon_ = true;
    }
    return null;
  }

  /**
   * f0 -> Identifier()
   * f1 -> "--"
   */
  public Var_t visit(DecrementAssignmentStatement n) throws Exception {
    Var_t id = n.f0.accept(this);
    String id_type = st_.findType(id);
    if (id_type.equals("EncInt")) {
      append_idx(id.getName());
      this.asm_.append(".subConstant(NTL::ZZX(1));\n");
    } else {
      append_idx(id.getName());
      this.asm_.append("--");
      this.semicolon_ = true;
    }
    return null;
  }

  /**
   * f0 -> Identifier()
   * f1 -> CompoundOperator()
   * f2 -> Expression()
   */
  public Var_t visit(CompoundAssignmentStatement n) throws Exception {
    Var_t lhs = n.f0.accept(this);
    String op = n.f1.accept(this).getName();
    Var_t rhs = n.f2.accept(this);
    String lhs_type = st_.findType(lhs);
    String rhs_type = st_.findType(rhs);
    if (lhs_type.equals("int") && rhs_type.equals("int")) {
      append_idx(lhs.getName());
      this.asm_.append(" ").append(op).append(" ");
      this.asm_.append(rhs.getName());
    } else if (lhs_type.equals("EncInt") && rhs_type.equals("EncInt")) {
      append_idx(lhs.getName());
      switch (op) {
        case "+=":
          this.asm_.append(" += ").append(rhs.getName());
          break;
        case "*=":
          this.asm_.append(".multiplyBy(");
          this.asm_.append(rhs.getName()).append(")");
          break;
        case "-=":
          this.asm_.append(" -= ").append(rhs.getName());
          break;
        default:
          throw new Exception("Bad operand types: " + lhs_type + " " + op + " " + rhs_type);
      }
    } else if (lhs_type.equals("EncInt") && rhs_type.equals("int")) {
      append_idx(lhs.getName());
      switch (op) {
        case "+=":
          this.asm_.append(".addConstant(NTL::ZZX(").append(rhs.getName()).append("))");
          break;
        case "*=":
          this.asm_.append(".multByConstant(NTL::ZZX(").append(rhs.getName()).append("))");
          break;
        case "-=":
          this.asm_.append(".subConstant(NTL::ZZX(").append(rhs.getName()).append("))");
          break;
        default:
          throw new Exception("Bad operand types: " + lhs_type + " " + op + " " + rhs_type);
      }
    }
    this.semicolon_ = true;
    return null;
  }

  /**
   * f0 -> Identifier()
   * f1 -> "["
   * f2 -> Expression()
   * f3 -> "]"
   * f4 -> CompoundOperator()
   * f5 -> Expression()
   */
  public Var_t visit(CompoundArrayAssignmentStatement n) throws Exception {
    Var_t id = n.f0.accept(this);
    String id_type = st_.findType(id);
    Var_t idx = n.f2.accept(this);
    String idx_type = st_.findType(idx);
    String op = n.f4.accept(this).getName();
    Var_t rhs = n.f5.accept(this);
    String rhs_type = st_.findType(rhs);
    switch (id_type) {
      case "int[]":
        append_idx(id.getName());
        this.asm_.append("[").append(idx.getName()).append("] ").append(op);
        this.asm_.append(" ").append(rhs.getName());
        break;
      case "EncInt[]":
        if (rhs_type.equals("EncInt")) {
          append_idx(id.getName());
          this.asm_.append("[").append(idx.getName()).append("] ");
          if (op.equals("+=") || op.equals("-=")) {
            this.asm_.append(op).append(" ").append(rhs.getName());
          } else if (op.equals("*=")) {
            this.asm_.append(".multiplyBy(");
            this.asm_.append(rhs.getName()).append(")");
          } else {
            throw new Exception("Error in compound array assignment");
          }
          break;
        } else if (rhs_type.equals("int")) {
          throw new Exception("Encrypt and move to temporary var.");
        }
      default:
        throw new Exception("error in array assignment");
    }
    this.semicolon_ = true;
    return null;
  }

  /**
   * f0 -> Identifier()
   * f1 -> "["
   * f2 -> Expression()
   * f3 -> "]"
   * f4 -> "="
   * f5 -> Expression()
   */
  public Var_t visit(ArrayAssignmentStatement n) throws Exception {
    Var_t id = n.f0.accept(this);
    String id_type = st_.findType(id);
    Var_t idx = n.f2.accept(this);
    String idx_type = st_.findType(idx);
    Var_t rhs = n.f5.accept(this);
    String rhs_type = st_.findType(rhs);
    switch (id_type) {
      case "int[]":
        append_idx(id.getName());
        this.asm_.append("[").append(idx.getName()).append("] = ");
        this.asm_.append(rhs.getName());
        break;
      case "EncInt[]":
        if (rhs_type.equals("EncInt")) {
          append_idx(id.getName());
          this.asm_.append("[").append(idx.getName()).append("] = ");
          this.asm_.append(rhs.getName()).append(";\n");
          break;
        } else if (rhs_type.equals("int")) {
          append_idx("tmp[0] = ");
          this.asm_.append(rhs.getName()).append(";\n");
          append_idx("public_key.Encrypt(");
          this.asm_.append(id.getName()).append("[").append(idx.getName()).append("], tmp)");
          break;
        }
      default:
        throw new Exception("error in array assignment");
    }
    return null;
  }

  /**
   * f0 -> Identifier()
   * f1 -> "="
   * f2 -> "{"
   * f3 -> Expression()
   * f4 -> ( BatchAssignmentStatementRest() )*
   * f5 -> "}"
   */
  public Var_t visit(BatchAssignmentStatement n) throws Exception {
    Var_t id = n.f0.accept(this);
    String id_type = st_.findType(id);
    Var_t exp = n.f3.accept(this);
    String exp_type = st_.findType(exp);
    switch (id_type) {
      case "int[]":
        append_idx(id.getName());
        this.asm_.append(" = { ").append(exp.getName());
        if (n.f4.present()) {
          for (int i = 0; i < n.f4.size(); i++) {
            this.asm_.append(", ").append((n.f4.nodes.get(i).accept(this)).getName());
          }
        }
        this.asm_.append(" };\n");
        break;
      case "EncInt":
        int cnt = 0;
        append_idx("tmp[0] = ");
        this.asm_.append(exp.getName()).append(";\n");
        if (n.f4.present()) {
          for (int i = 0; i < n.f4.size(); i++) {
            append_idx("tmp[");
            this.asm_.append(i + 1).append("] = ");
            this.asm_.append((n.f4.nodes.get(i).accept(this)).getName()).append(";\n");
          }
        }
        append_idx("public_key.Encrypt(");
        this.asm_.append(id.getName()).append(", tmp);\n");
        break;
      case "EncInt[]":
        String exp_var;
        if (exp_type.equals("int")) {
          exp_var = new_ctxt_tmp();
          append_idx("tmp[0] = " + exp.getName() + ";\n");
          append_idx("public_key.Encrypt(");
          this.asm_.append(exp_var).append(", tmp);\n");
        } else { // exp type is EncInt
          exp_var = exp.getName();
        }
        List<String> inits = new ArrayList<>();
        if (n.f4.present()) {
          for (int i = 0; i < n.f4.size(); i++) {
            String init = (n.f4.nodes.get(i).accept(this)).getName();
            if (exp_type.equals("int")) {
              String tmp_ = new_ctxt_tmp();
              append_idx("tmp[0] = " + init + ";\n");
              append_idx("public_key.Encrypt(");
              this.asm_.append(tmp_).append(", tmp);\n");
              inits.add(tmp_);
            } else { // exp type is EncInt
              inits.add(init);
            }
          }
        }
        append_idx(id.getName());
        this.asm_.append(" = { ").append(exp_var);
        for (String init : inits) {
          this.asm_.append(", ").append(init);
        }
        this.asm_.append(" };\n");
        break;
      default:
        throw new Exception("Bad operand types: " + id.getName() + " " + exp_type);
    }
    return null;
  }

  /**
   * f0 -> Identifier()
   * f1 -> "["
   * f2 -> Expression()
   * f3 -> "]"
   * f4 -> "="
   * f5 -> "{"
   * f6 -> Expression()
   * f7 -> ( BatchAssignmentStatementRest() )*
   * f8 -> "}"
   */
  public Var_t visit(BatchArrayAssignmentStatement n) throws Exception {
    Var_t id = n.f0.accept(this);
    Var_t index = n.f2.accept(this);
    Var_t exp = n.f6.accept(this);
    String id_type = st_.findType(id);
    assert(id_type.equals("EncInt[]"));
    String index_type = st_.findType(index);
    tmp_cnt_++;
    append_idx("tmp[0] = ");
    this.asm_.append(exp.getName()).append(";\n");
    if (n.f7.present()) {
      for (int i = 0; i < n.f7.size(); i++) {
        append_idx("tmp[");
        this.asm_.append(i + 1).append("] = ");
        this.asm_.append((n.f7.nodes.get(i).accept(this)).getName()).append(";\n");
      }
    }
    append_idx("public_key.Encrypt(");
    this.asm_.append(id.getName()).append("[").append(index.getName()).append("], tmp);\n");
    return null;
  }

  /**
   * f0 -> "print"
   * f1 -> "("
   * f2 -> Expression()
   * f3 -> ")"
   */
  public Var_t visit(PrintStatement n) throws Exception {
    Var_t expr = n.f2.accept(this);
    String expr_type = st_.findType(expr);
    switch (expr_type) {
      case "int":
        append_idx("cout << ");
        this.asm_.append(expr.getName());
        this.asm_.append(" << endl;\n");
        break;
      case "EncInt":
        append_idx("secret_key.Decrypt(tmp, ");
        this.asm_.append(expr.getName()).append(");\n");
        append_idx("cout << \"dec(");
        this.asm_.append(expr.getName()).append(") = \" << tmp << endl");
        break;
      default:
        throw new Exception("Bad type for print statement");
    }
    return null;
  }

  /**
   * f0 -> "print_batched"
   * f1 -> "("
   * f2 -> Expression()
   * f3 -> ","
   * f4 -> Expression()
   * f5 -> ")"
   */
  public Var_t visit(PrintBatchedStatement n) throws Exception {
    Var_t expr = n.f2.accept(this);
    String expr_type = st_.findType(expr);
    assert(expr_type.equals("EncInt"));
    Var_t size = n.f4.accept(this);
    String size_type = st_.findType(expr);
    assert(size_type.equals("int"));
    append_idx("secret_key.Decrypt(tmp, ");
    this.asm_.append(expr.getName()).append(");\n");
    append_idx("for (int i = 0; i < ");
    this.asm_.append(size.getName()).append("; ++i) {\n");
    append_idx("  cout << tmp[i] << \"\\t\";\n");
    append_idx("}\n");
    append_idx("cout << endl;\n");
    return null;
  }

  /**
   * f0 -> <REDUCE_NOISE>
   * f1 -> "("
   * f2 -> Expression()
   * f3 -> ")"
   */
  public Var_t visit(ReduceNoiseStatement n) throws Exception {
    // Calls mod switch automatically.
    return null;
  }

  /**
   * f0 -> PrimaryExpression()
   * f1 -> BinOperator()
   * f2 -> PrimaryExpression()
   */
  public Var_t visit(BinaryExpression n) throws Exception {
    Var_t lhs = n.f0.accept(this);
    String op = n.f1.accept(this).getName();
    Var_t rhs = n.f2.accept(this);
    String lhs_type = st_.findType(lhs);
    String rhs_type = st_.findType(rhs);
    if (lhs_type.equals("int") && rhs_type.equals("int")) {
      if ("&".equals(op) || "|".equals(op) || "^".equals(op) || "<<".equals(op) ||
          ">>".equals(op) || "+".equals(op) || "-".equals(op) || "*".equals(op) ||
          "/".equals(op) || "%".equals(op)
      ) {
        return new Var_t("int", lhs.getName() + op + rhs.getName());
      } else if ("==".equals(op) || "!=".equals(op) || "<".equals(op) ||
                 "<=".equals(op) || ">".equals(op) || ">=".equals(op) ||
                 "&&".equals(op) || "||".equals(op)) {
        return new Var_t("bool", lhs.getName() + op + rhs.getName());
      }
    } else if (lhs_type.equals("int") && rhs_type.equals("EncInt")) {
      String res_ = new_ctxt_tmp();
      switch (op) {
        case "+":
          append_idx(res_);
          this.asm_.append(" = ").append(rhs.getName()).append(";\n");
          append_idx(res_);
          this.asm_.append(".addConstant(NTL::ZZX(").append(lhs.getName()).append("));\n");
          break;
        case "*":
          append_idx(res_);
          this.asm_.append(" = ").append(rhs.getName()).append(";\n");
          append_idx(res_);
          this.asm_.append(".multByConstant(NTL::ZZX(").append(lhs.getName()).append("));\n");
          break;
        case "-":
          append_idx("tmp[0] = ");
          this.asm_.append(lhs.getName()).append(";\n");
          append_idx("public_key.Encrypt(");
          this.asm_.append(res_).append(", tmp);\n");
          append_idx(res_);
          this.asm_.append(" -= ").append(rhs.getName()).append(";\n");
          break;
        case "==":
        case "<":
        case "<=":
          throw new RuntimeException("Not yet supported");
        default:
          throw new Exception("Bad operand types: " + lhs_type + " " + op + " " + rhs_type);
      }
      return new Var_t("EncInt", res_);
    } else if (lhs_type.equals("EncInt") && rhs_type.equals("int")) {
      String res_ = new_ctxt_tmp();
      append_idx(res_);
      this.asm_.append(" = ").append(lhs.getName()).append(";\n");
      append_idx(res_);
      switch (op) {
        case "+":
          this.asm_.append(".addConstant(NTL::ZZX(").append(rhs.getName()).append("));\n");
          break;
        case "*":
          this.asm_.append(".multByConstant(NTL::ZZX(").append(rhs.getName()).append("));\n");
          break;
        case "-":
          this.asm_.append(".subConstant(NTL::ZZX(").append(rhs.getName()).append("));\n");
          break;
        case "==":
        case "<":
        case "<=":
          throw new RuntimeException("Not yet supported");
        default:
          throw new Exception("Bad operand types: " + lhs_type + " " + op + " " + rhs_type);
      }
      return new Var_t("EncInt", res_);
    } else if (lhs_type.equals("EncInt") && rhs_type.equals("EncInt")) {
      String res_ = new_ctxt_tmp();
      append_idx(res_);
      this.asm_.append(" = ").append(lhs.getName()).append(";\n");
      append_idx(res_);
      switch (op) {
        case "*":
        case "+":
        case "-":
          this.asm_.append(" ").append(op).append("= ");
          this.asm_.append(rhs.getName()).append(";\n");
          break;
        case "==":
        case "<":
        case "<=":
          throw new RuntimeException("Not yet supported");
        default:
          throw new Exception("Bad operand types: " + lhs_type + " " + op + " " + rhs_type);
      }
      return new Var_t("EncInt", res_);
    }
    throw new Exception("Bad operand types: " + lhs_type + " " + op + " " + rhs_type);
  }

  /**
   * f0 -> "new"
   * f1 -> "EncInt"
   * f2 -> "["
   * f3 -> Expression()
   * f4 -> "]"
   */
  public Var_t visit(EncryptedArrayAllocationExpression n) throws Exception {
    String size = n.f3.accept(this).getName();
    return new Var_t("EncInt[]", "resize(" + size + ", tmp_)");
  }

}