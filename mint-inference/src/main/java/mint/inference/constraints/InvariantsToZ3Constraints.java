package mint.inference.constraints;

import com.microsoft.z3.*;
import daikon.inv.Invariant;
import daikon.inv.binary.BinaryInvariant;
import daikon.inv.binary.twoScalar.*;
import daikon.inv.binary.twoString.TwoString;
import daikon.inv.unary.scalar.*;
import daikon.inv.unary.string.OneOfString;
import daikon.inv.unary.string.SingleString;
import mint.tracedata.types.BooleanVariableAssignment;
import mint.tracedata.types.DoubleVariableAssignment;
import mint.tracedata.types.StringVariableAssignment;
import mint.tracedata.types.VariableAssignment;

import java.util.HashMap;
import java.util.Map;

public class InvariantsToZ3Constraints {
	
	Map<String,Integer> miniHash;
	Map<String,ArithExpr> variables;
	Context ctx;
	BoolExpr current;
	
	public InvariantsToZ3Constraints() throws Z3Exception{
		HashMap<String, String> cfg = new HashMap<String, String>();
        cfg.put("model", "true");
        ctx = new Context(cfg);
        variables = new HashMap<String,ArithExpr>();
		miniHash = new HashMap<String,Integer>();
	}
	
	private void addConstraint(BoolExpr e) throws Z3Exception{
		if(current == null)
			current = e;
		else
			current = ctx.MkAnd(new BoolExpr[]{current,e});
	}
	
	private int hash(String s){
		if(miniHash.get(s) == null)
			miniHash.put(s, miniHash.size()+1);
		return miniHash.get(s);
	}
	
	
	public void addInvariant(Invariant i) throws Z3Exception{
		if(i instanceof TwoFloat)
			addBinaryFloatConstraint((TwoFloat)i);
		else if(i instanceof TwoScalar)
			addBinaryScalarConstraint((TwoScalar)i);
		else if(i instanceof TwoString)
			addBinaryStringConstraint((TwoString)i);
		else if(i instanceof SingleFloat)
			addUnaryConstraint((SingleFloat)i);
		else if(i instanceof SingleScalar)
			addUnaryConstraint((SingleScalar)i);
		else if(i instanceof SingleString)
			addUnaryConstraint((SingleString)i);
	}

	public void addBinaryFloatConstraint(TwoFloat inv) throws Z3Exception {
		ArithExpr var1 = getReal(inv.var1().java_name());
		ArithExpr var2 = getReal(inv.var2().java_name());
		addFunction(inv,var1,var2);
	}


	public void addBinaryScalarConstraint(TwoScalar inv) throws Z3Exception {
		ArithExpr var1 = getScalar(inv.var1().java_name());
		ArithExpr var2 = getScalar(inv.var2().java_name());
		addFunction(inv,var1,var2);
	}
	
	public void addBinaryStringConstraint(TwoString inv) throws Z3Exception {
		ArithExpr var1 = getScalar(inv.var1().java_name());
		ArithExpr var2 = getScalar(inv.var1().java_name());
		addFunction(inv,var1,var2);
	}
	
	public void addUnaryConstraint(SingleFloat inv) throws Z3Exception {
		ArithExpr var1 = getReal(inv.var().java_name());
		if(inv instanceof OneOfFloat){
			OneOfFloat finv = (OneOfFloat) inv;
			addFunction(finv,var1);
		}
		else if(inv instanceof UpperBoundFloat){
			UpperBoundFloat finv = (UpperBoundFloat) inv;
			addFunction(finv,var1);
		}
		else if(inv instanceof NonZeroFloat){
			NonZeroFloat finv = (NonZeroFloat) inv;
			addFunction(finv,var1);
		}
		else if(inv instanceof LowerBoundFloat){
			LowerBoundFloat finv = (LowerBoundFloat) inv;
			addFunction(finv,var1);
		}
	}


	private void addFunction(OneOfFloat inv, Expr var) throws Z3Exception {
		double[] elts = inv.getElts();
		Expr set = ctx.MkEmptySet(ctx.RealSort());
		for(int i = 0; i< inv.num_elts(); i++){
			Double d = elts[i];
			RatNum dub = ctx.MkReal(d.toString());
			ctx.MkSetAdd(set, dub);
		}
		addConstraint((BoolExpr) ctx.MkSetMembership(var, set));
		
	}
	
	private void addFunction(UpperBoundFloat inv, ArithExpr var) throws Z3Exception {
		Double max = inv.max();
		ArithExpr var1 = (ArithExpr) ctx.MkReal(max.toString());
		addConstraint(ctx.MkLe(var, var1));
		
	}
	
	private void addFunction(LowerBoundFloat inv, ArithExpr var) throws Z3Exception {
		Double max = inv.min();
		ArithExpr var1 = (ArithExpr) ctx.MkReal(max.toString());
		addConstraint(ctx.MkGe(var, var1));
	}
	
	private void addFunction(NonZeroFloat inv, ArithExpr var) throws Z3Exception {
		addConstraint(ctx.MkNot(ctx.MkEq(var, ctx.MkReal("0.0"))));
	}

	public void addUnaryConstraint(SingleScalar inv) throws Z3Exception {
		ArithExpr var1 = getScalar(inv.var().java_name());
		if(inv instanceof OneOfScalar){
			OneOfScalar finv = (OneOfScalar) inv;
			addFunction(finv,var1);
		}
		else if(inv instanceof UpperBound){
			UpperBound finv = (UpperBound) inv;
			addFunction(finv,var1);
		}
		else if(inv instanceof NonZero){
			NonZero finv = (NonZero) inv;
			addFunction(finv,var1);
		}
		else if(inv instanceof LowerBound){
			LowerBound finv = (LowerBound) inv;
			addFunction(finv,var1);
		}
	}
	
	


	private void addFunction(OneOfScalar inv, Expr var) throws Z3Exception {
		long[] elts = inv.getElts();
		Expr set = ctx.MkEmptySet(ctx.IntSort());
		for(int i = 0; i< inv.num_elts(); i++){
			Long d = elts[i];
			IntNum dub = ctx.MkInt(d.toString());
			ctx.MkSetAdd(set, dub);
		}
		addConstraint((BoolExpr) ctx.MkSetMembership(var, set));
		
	}
	
	private void addFunction(UpperBound inv, ArithExpr var) throws Z3Exception {
		Long max = inv.max();
		ArithExpr var1 = (ArithExpr) ctx.MkInt(max.toString());
		addConstraint(ctx.MkLe(var, var1));
		
	}
	
	private void addFunction(LowerBound inv, ArithExpr var) throws Z3Exception {
		Long min = inv.min();
		ArithExpr var1 = (ArithExpr) ctx.MkInt(min.toString());
		addConstraint(ctx.MkLe(var, var1));
		
	}
	
	private void addFunction(NonZero inv, ArithExpr var) throws Z3Exception {
		addConstraint(ctx.MkNot(ctx.MkEq(var, ctx.MkInt("0"))));
		
	}
	
	public void addUnaryConstraint(SingleString inv) throws Z3Exception {
		Expr varint = getScalar(inv.var().java_name());
		if(inv instanceof OneOfString){
			OneOfString finv = (OneOfString) inv;
			addFunction(finv,varint);
		}
	}


	private void addFunction(OneOfString inv, Expr var) throws Z3Exception {
		String[] elts = inv.getElts();
		Expr set = ctx.MkEmptySet(ctx.IntSort());
		for(int i = 0; i< inv.num_elts(); i++){
			String s = elts[i];
			if(s == null)
				continue;
			Integer h = hash(s);
			IntNum dub = ctx.MkInt(h.toString());
			ctx.MkSetAdd(set, dub);
		}
		addConstraint((BoolExpr) ctx.MkSetMembership(var, set));
		
	}

	
	private void addFunction(BinaryInvariant inv, ArithExpr var1, ArithExpr var2) throws Z3Exception {
		BoolExpr expression = null;
		if(inv instanceof FloatEqual || inv instanceof IntEqual)
			expression = ctx.MkEq(var1, var2);
		else if(inv instanceof FloatGreaterEqual || inv instanceof IntGreaterEqual)
			expression = ctx.MkGe(var1, var2);
		else if(inv instanceof FloatGreaterThan || inv instanceof IntGreaterThan){
			expression = ctx.MkGt(var1, var2);
		}
		else if(inv instanceof FloatLessEqual || inv instanceof IntLessEqual){
			expression = ctx.MkLe(var1, var2);
		}
		else if(inv instanceof FloatLessThan || inv instanceof IntLessThan){
			expression = ctx.MkLt(var1, var2);
		}
		else if(inv instanceof FloatNonEqual || inv instanceof IntNonEqual){
			expression = ctx.MkNot(ctx.MkEq(var1, var2));
		}
		addConstraint(expression);
	}
	
	public void addVariableAssignment(String varName, Boolean i){
		//TODO
	}
	
	public void addVariableAssignment(String varName, Double i) throws Z3Exception{
		ArithExpr var = getScalar(varName);
		addConstraint(ctx.MkEq(var, ctx.MkReal(i.toString())));
	}
	
	public void addVariableAssignment(String varName, String i) throws Z3Exception{
		ArithExpr var = getScalar(varName);
		Integer h = hash(i);
		addConstraint(ctx.MkEq(var, ctx.MkInt(h.toString())));
	}
	
	public void addVariableAssignment(VariableAssignment<?> va) throws Z3Exception{
		if(va instanceof BooleanVariableAssignment){
			BooleanVariableAssignment b = (BooleanVariableAssignment)va;
			addVariableAssignment(va.getName(),b.getValue());
		}
		else if (va instanceof StringVariableAssignment){
			StringVariableAssignment s = (StringVariableAssignment)va;
			addVariableAssignment(va.getName(),s.getValue());
		}
		else if (va instanceof DoubleVariableAssignment){
			DoubleVariableAssignment d = (DoubleVariableAssignment)va;
			addVariableAssignment(va.getName(),d.getValue());
		}
	}
	
	public boolean solve() throws Z3Exception{
		Solver s = ctx.MkSolver();
		Status stat = s.Check();
		return stat == Status.SATISFIABLE;
	}
	
	private ArithExpr getScalar(String var1) throws Z3Exception {
		ArithExpr var = null;
		if(variables.containsKey(var1))
			var =  variables.get(var1);
		else{
			var = (ArithExpr) ctx.MkConst(ctx.MkSymbol(var1),ctx.MkIntSort());
			variables.put(var1, var);
		}
		return var;
	}
	
	private ArithExpr getReal(String var1) throws Z3Exception {
		ArithExpr var = null;
		if(variables.containsKey(var1))
			var =  variables.get(var1);
		else{
			var = (ArithExpr) ctx.MkConst(ctx.MkSymbol(var1),ctx.MkRealSort());
			variables.put(var1, var);
		}
		return var;
	}
	
}
