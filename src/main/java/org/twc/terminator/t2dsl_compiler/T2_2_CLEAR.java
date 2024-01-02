package org.twc.terminator.t2dsl_compiler;

import org.twc.terminator.Main;
import org.twc.terminator.SymbolTable;
import org.twc.terminator.Var_t;
import org.twc.terminator.t2dsl_compiler.T2DSLsyntaxtree.*;

import java.util.ArrayList;
import java.util.List;

public class T2_2_CLEAR extends T2_Compiler {

  public T2_2_CLEAR(SymbolTable st, String config_file_path, int word_sz, int ring_dim) {
    super(st, config_file_path, word_sz, ring_dim);
    if (this.is_binary_) {
      throw new RuntimeException("TODO: binary");
    } else {
      this.st_.backend_types.put("EncInt", "vector<int>");
      this.st_.backend_types.put("EncInt[]", "vector<vector<int>>");
      this.st_.backend_types.put("EncDouble", "vector<double>");
      this.st_.backend_types.put("EncDouble[]", "vector<vector<double>>");
    }
  }

  protected String new_ctxt_tmp(String ret_type) {
    tmp_cnt_++;
    String ctxt_tmp_ = "tmp_" + tmp_cnt_ + "_";
    String type = (ret_type.equals("double")) ? "EncDouble" : "EncInt";
    append_idx(this.st_.backend_types.get(type));
    this.asm_.append(" ").append(ctxt_tmp_);
    this.asm_.append("(").append(this.ring_dim_).append(")");
    this.asm_.append(";\n");
    return ctxt_tmp_;
  }

  protected void append_keygen() {
  }

  protected void encrypt(String dst, String[] src_lst) {
    if (this.is_binary_) {
      throw new RuntimeException("TODO: binary");
    } else {
      if (src_lst.length != 1)
        throw new RuntimeException("encrypt: list length");
      append_idx("fill(" + dst + ".begin(), " + dst);
      this.asm_.append(".end(), ").append(src_lst[0]).append(");\n");
    }
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
      this.asm_.append("(").append(this.ring_dim_).append(")");
    }
    if (n.f2.present()) {
      for (int i = 0; i < n.f2.size(); i++) {
        n.f2.nodes.get(i).accept(this);
        if (type.equals("EncInt")) {
          this.asm_.append("(").append(this.ring_dim_).append(")");
        }
      }
    } else {
    }
    this.asm_.append(";\n");
    return null;
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
    append_idx("#include <iostream>\n");
    append_idx("#include <chrono>\n\n");
    append_idx("#include <vector>\n\n");
    if (this.st_.getScheme() == Main.ENC_TYPE.ENC_DOUBLE) {
      append_idx("#include <iomanip>\n\n");
    }
    append_idx(
        "#include \"../functional_units/functional_units.hpp\"\n\n");
    append_idx("using namespace std;\n\n");
    append_idx("int main(void) {\n");
    this.indent_ = 2;
    n.f6.accept(this);
    n.f7.accept(this);
    append_idx("return ");
    Var_t ret = n.f9.accept(this);
    this.asm_.append(ret.getName()).append(";\n}");
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
    if ((lhs_type.equals("EncInt") && rhs_type.equals("int")) ||
        (lhs_type.equals("EncDouble") && rhs_type.equals("double"))) {
      // if EncInt <- int
      encrypt(lhs.getName(), new String[] { rhs_name });
    } else if ((lhs_type.equals("EncInt[]") && rhs_type.equals("int[]")) ||
        (lhs_type.equals("EncDouble[]") && rhs_type.equals("double[]"))) {
      // if EncInt[] <- int[]
      append_idx(lhs.getName());
      this.asm_.append(".resize(").append(rhs_name).append(".size());\n");
      if (this.is_binary_) {
        throw new RuntimeException("TODO: binary");
      }
      append_idx("for (size_t " + this.tmp_i + " = 0; " + this.tmp_i + " < ");
      this.asm_.append(rhs_name).append(".size(); ++").append(this.tmp_i);
      this.asm_.append(") {\n");
      this.indent_ += 2;
      encrypt(lhs.getName() + "[" + this.tmp_i + "]",
          new String[] { rhs_name + "[" + this.tmp_i + "]" });
      this.indent_ -= 2;
      append_idx("}\n");
    } else if (lhs_type.equals(rhs_type)) {
      // if the destination has the same type as the source.
      if ((lhs_type.equals("EncInt") || lhs_type.equals("EncInt[]")) ||
          (lhs_type.equals("EncDouble") || lhs_type.equals("EncDouble[]"))) {
        if (rhs_name.startsWith("resize(")) {
          int rhs_new_size = 0;
          rhs_new_size = Integer.parseInt(rhs_name.substring(7, rhs_name.length() - 1));
          append_idx(lhs.getName() + ".resize(" + rhs_new_size + ");\n");
          // for (int i = 0; i < rhs_new_size; i++) {
          // append_idx(lhs.getName() + "[" + i + "].resize(word_sz);\n");
          // }
        } else {
          append_idx(lhs.getName() + " = " + rhs_name);
          this.semicolon_ = true;
        }
      } else {
        append_idx(lhs.getName());
        if (rhs_name.startsWith("resize(")) {
          this.asm_.append(".");
        } else {
          this.asm_.append(" = ");
        }
        this.asm_.append(rhs_name);
        this.semicolon_ = true;
      }
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
    append_idx(id.getName());
    this.asm_.append("++");
    this.semicolon_ = true;
    return null;
  }

  /**
   * f0 -> Identifier()
   * f1 -> "--"
   */
  public Var_t visit(DecrementAssignmentStatement n) throws Exception {
    Var_t id = n.f0.accept(this);
    append_idx(id.getName());
    this.asm_.append("--");
    this.semicolon_ = true;
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
    append_idx(lhs.getName());
    this.asm_.append(" ").append(op).append(" ");
    this.asm_.append(rhs.getName());
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
    Var_t idx = n.f2.accept(this);
    String op = n.f4.accept(this).getName();
    Var_t rhs = n.f5.accept(this);
    append_idx(id.getName());
    this.asm_.append("[").append(idx.getName()).append("] ").append(op);
    this.asm_.append(" ").append(rhs.getName());
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
    Var_t idx = n.f2.accept(this);
    Var_t rhs = n.f5.accept(this);
    String id_type = st_.findType(id);
    String lhs_name = id.getName() + "[" + idx.getName() + "]";
    String rhs_type = st_.findType(rhs);
    if (id_type.startsWith("Enc") && id_type.endsWith("[]")) {
      if (id_type.startsWith(rhs_type)) {
        append_idx(lhs_name);
        this.asm_.append(" = ").append(rhs.getName());
      } else {
        encrypt(lhs_name, new String[] { rhs.getName() });
      }
    } else {
      append_idx(id.getName());
      this.asm_.append("[").append(idx.getName()).append("] = ");
      this.asm_.append(rhs.getName());
    }
    this.semicolon_ = true;
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
    Var_t exp = n.f3.accept(this);
    String id_type = st_.findType(id);
    append_idx(id.getName());
    this.asm_.append(" = { ");
    if (id_type.startsWith("Enc") && id_type.endsWith("[]")) {
      this.asm_.append("{ ").append(exp.getName()).append(" }");
      if (n.f4.present()) {
        for (int i = 0; i < n.f4.size(); i++) {
          this.asm_.append(", { ").append((n.f4.nodes.get(i).accept(this)).getName()).append(" }");
        }
      }
    } else {
      this.asm_.append(exp.getName());
      if (n.f4.present()) {
        for (int i = 0; i < n.f4.size(); i++) {
          this.asm_.append(", ").append((n.f4.nodes.get(i).accept(this)).getName());
        }
      }
    }
    this.asm_.append(" };\n");
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
    if (!id_type.equals("EncDouble[]") && !id_type.equals("EncInt[]"))
      throw new RuntimeException("BatchArrayAssignmentStatement: " + id_type);
    append_idx(id.getName());
    this.asm_.append("[").append(index.getName()).append("]").append(" = { ").append(exp.getName());
    if (n.f7.present()) {
      for (int i = 0; i < n.f7.size(); i++) {
        this.asm_.append(", ").append((n.f7.nodes.get(i).accept(this)).getName());
      }
    }
    this.asm_.append(" };\n");
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
    append_idx("cout << ");
    if (expr_type.toLowerCase().endsWith("double")) {
      this.asm_.append("fixed << setprecision(1) << ");
    }
    if (expr_type.startsWith("Enc")) {
      this.asm_.append(expr.getName()).append("[0]");
    } else {
      this.asm_.append(expr.getName());
    }
    this.asm_.append(" << endl");
    this.semicolon_ = true;
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
    if (!expr_type.equals("EncDouble") && !expr_type.equals("EncInt"))
      throw new RuntimeException("PrintBatchedStatement: expression type");
    Var_t size = n.f4.accept(this);
    String size_type = size.getType();
    if (size_type == null)
      size_type = st_.findType(size);
    if (!size_type.equals("int"))
      throw new RuntimeException("PrintBatchedStatement: size type");
    append_idx("for (int " + this.tmp_i + " = 0; ");
    this.asm_.append(this.tmp_i).append(" < ").append(size.getName());
    this.asm_.append("; ++").append(this.tmp_i).append(") {\n");
    append_idx("  cout << " + expr.getName() + "[" + this.tmp_i + "] << \" \";\n");
    append_idx("}\n");
    append_idx("cout << endl");
    this.semicolon_ = true;
    return null;
  }

  /**
   * f0 -> <REDUCE_NOISE>
   * f1 -> "("
   * f2 -> Expression()
   * f3 -> ")"
   */
  public Var_t visit(ReduceNoiseStatement n) throws Exception {
    Var_t expr = n.f2.accept(this);
    String expr_type = st_.findType(expr);
    if (!expr_type.startsWith("Enc"))
      throw new RuntimeException("ReduceNoiseStatement: expr type");
    return null;
  }

  /**
   * f0 -> <ROTATE_LEFT>
   * f1 -> "("
   * f2 -> Expression()
   * f3 -> ","
   * f4 -> Expression()
   * f5 -> ")"
   */
  public Var_t visit(RotateLeftStatement n) throws Exception {
    String ctxt = n.f2.accept(this).getName();
    String amnt = n.f4.accept(this).getName();
    append_idx("rotateLeft(" + ctxt + ", ");
    this.asm_.append(amnt).append(");\n");
    return null;
  }

  /**
   * f0 -> <ROTATE_RIGHT>
   * f1 -> "("
   * f2 -> Expression()
   * f3 -> ","
   * f4 -> Expression()
   * f5 -> ")"
   */
  public Var_t visit(RotateRightStatement n) throws Exception {
    String ctxt = n.f2.accept(this).getName();
    String amnt = n.f4.accept(this).getName();
    append_idx("rotateRight(" + ctxt + ", ");
    this.asm_.append(amnt).append(");\n");
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

    String ret_type = (lhs_type.toLowerCase().endsWith("double")
        || rhs_type.toLowerCase().endsWith("double")) ? "Double" : "Int";
    if (lhs_type.startsWith("Enc") || rhs_type.startsWith("Enc")) {
      ret_type = "Enc" + ret_type;
    } else {
      ret_type = ret_type.toLowerCase();
    }

    if ("&".equals(op) || "|".equals(op) || "^".equals(op) || "<<".equals(op) ||
        ">>".equals(op) || "+".equals(op) || "-".equals(op) || "*".equals(op) ||
        "/".equals(op) || "%".equals(op)) {
      return new Var_t(ret_type, lhs.getName() + op + rhs.getName());
    } else if ("==".equals(op) || "!=".equals(op) || "<".equals(op) ||
        "<=".equals(op) || ">".equals(op) || ">=".equals(op) ||
        "&&".equals(op) || "||".equals(op)) {
      return new Var_t("bool", lhs.getName() + op + rhs.getName());
    }
    throw new Exception("Bad operand types: " + lhs_type + " " + op + " " +
        rhs_type);
  }

  /**
   * f0 -> "~"
   * f1 -> PrimaryExpression()
   */
  public Var_t visit(BinNotExpression n) throws Exception {
    Var_t exp = n.f1.accept(this);
    String exp_type = st_.findType(exp);
    if (exp_type.equals("int")) {
      if (this.is_binary_) {
        return new Var_t("int", "~" + exp.getName());
      } else {
        return new Var_t("int", "plaintext_modulus + ~" + exp.getName());
      }
    } else if (exp_type.equals("EncInt")) {
      // TODO: ret_type
      String res_ = new_ctxt_tmp();
      if (this.is_binary_) {
        append_idx(res_ + " = not_bin(evaluator, batch_encoder, ");
        this.asm_.append(exp.getName()).append(", slots);\n");
      } else {
        append_idx("evaluator.negate(");
        this.asm_.append(exp.getName());
        this.asm_.append(", ").append(res_).append(");\n");
        append_idx("tmp = uint64_to_hex_string(1);\n");
        append_idx("evaluator.sub_plain_inplace(" + res_ + ", tmp);\n");
      }
      return new Var_t("EncInt", res_);
    }
    throw new Exception("Wrong type for ~: " + exp_type);
  }

  /**
   * f0 -> "("
   * f1 -> Expression()
   * f2 -> ")"
   * f3 -> "?"
   * f4 -> Expression()
   * f5 -> ":"
   * f6 -> Expression()
   */
  public Var_t visit(TernaryExpression n) throws Exception {
    throw new RuntimeException("TODO : TernaryExpression");
  }
}
