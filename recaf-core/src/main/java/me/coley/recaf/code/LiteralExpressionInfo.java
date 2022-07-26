package me.coley.recaf.code;

import com.github.javaparser.ast.expr.Expression;

public class LiteralExpressionInfo implements ItemInfo{
  
  private final Number value;
  private final Expression expression;
  
  public LiteralExpressionInfo(Number value, Expression expression) {
    this.value = value;
    this.expression = expression;
  }
  
  @Override
  public String getName() {
    return "expression";
  }
  
  public Number getValue() {
    return value;
  }
  
  public Expression getExpression() {
    return expression;
  }
}
