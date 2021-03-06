package declareextraction.evaluation;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import declareextraction.constructs.Action;
import declareextraction.constructs.DeclareConstraint;
import declareextraction.constructs.DeclareModel;

public class Evaluator {
	
	DecimalFormat df = new DecimalFormat("####0.00");
	
	Map<Integer, Integer> tpm = new HashMap<Integer, Integer>();
	Map<Integer, Integer> fpm = new HashMap<Integer, Integer>();
	Map<Integer, Integer> fnm = new HashMap<Integer, Integer>();
	Map<Integer, Integer> tem = new HashMap<Integer, Integer>();
	Map<Integer, Integer> aem = new HashMap<Integer, Integer>();
	
	
	public Object[] evaluateCase(DeclareModel generated, DeclareModel gs) {
		int col = gs.getCollection();
		
		if (!tpm.containsKey(col)) {
			tpm.put(col, 0);
			fpm.put(col, 0);
			fnm.put(col, 0);
			tem.put(col, 0);
			aem.put(col, 0);
		}
		
		int mtp = 0;
		int mfp = 0;
		int mfn = 0;
		
		for (DeclareConstraint gsCon : gs.getConstraints()) {
			mfn = mfn + gsCon.size();
		}
		
		Map<DeclareConstraint, DeclareConstraint> matches = greedyMatching(generated, gs);
		
		for (DeclareConstraint genCon : matches.keySet()) {
			DeclareConstraint gsCon = matches.get(genCon);
			
			
			int tp = quantifyMatch(genCon, gsCon);
			int fp = (genCon.size() - tp);
			int fn;
			if (gsCon == null) {
				fn = 0;
			} else {
				fn = (gsCon.size() - tp);
			}
			
			mtp = mtp + tp;
			mfp = mfp + fp;
			mfn = mfn - tp;
			
			if (fp != 0 && fn != 0) {
				System.out.println("GS:    " + gsCon);
				if (genCon.getType() != gsCon.getType()) {
					tem.put(col, tem.get(col) + 1);
					System.out.println("TYPE ERROR. Found: " + genCon.getType() + ". GSType: " + gsCon.getType());
				}
				if (!equalActions(genCon.getActionA(), gsCon.getActionA())) {
					aem.put(col, aem.get(col) + 1);
				}
				if (!equalActions(genCon.getActionB(), gsCon.getActionB())) {
					aem.put(col, aem.get(col) + 1);
				}
			}
		}
		for (DeclareConstraint gsConstraint : gs.getConstraints()) {
			if (!matches.values().contains(gsConstraint)) {
				System.out.println("MISSED: " + gsConstraint);
			}
		}
		double prec;
		if (mtp + mfp == 0) {
			prec = 0;
		} else {
			prec = mtp * 1.0 / (mtp + mfp);
		}
		double rec = mtp * 1.0 / (mtp + mfn);
		System.out.println("Case precision: " + df.format(prec) + " recall: " + df.format(rec) + "\n");

		tpm.put(col, tpm.get(col) + mtp);
		fpm.put(col, fpm.get(col) + mfp);
		fnm.put(col, fnm.get(col) + mfn);
		
		return new Object[]{prec, rec, matches};
	}
	
	public void printCollectionResults(int col) {
		System.out.println("\nCollection: " + col);
		int ttp = tpm.get(col);
		int tfp = fpm.get(col);
		int tfn = fnm.get(col);
		
		double prec = ttp * 1.0 / (ttp + tfp);
		double rec = ttp * 1.0 / (ttp + tfn);
		
		System.out.println("Type errors: " + tem.get(col));
		System.out.println("Activity errors: " + aem.get(col));
		System.out.println("Overall precision: " + df.format(prec) + " recall: " + df.format(rec));
	}
	
	public void printOverallResults() {
		int ttp = 0;
		int tfp = 0;
		int tfn = 0;
		int typeErrors = 0;
		int actErrors = 0;
		
		for (int collection : tpm.keySet()) {
			printCollectionResults(collection);
			ttp += tpm.get(collection);
			tfp += fpm.get(collection);
			tfn += fnm.get(collection);
			typeErrors += tem.get(collection);
			actErrors += aem.get(collection);
		}

		double prec = ttp * 1.0 / (ttp + tfp);
		double rec = ttp * 1.0 / (ttp + tfn);
		
		System.out.println("\nType errors: " + typeErrors);
		System.out.println("Activity errors: " + actErrors);
		System.out.println("Overall precision: " + df.format(prec) + " recall: " + df.format(rec));
	}
	
	
	private Map<DeclareConstraint, DeclareConstraint> greedyMatching(DeclareModel genModel, DeclareModel gsModel) {
		Map<DeclareConstraint, DeclareConstraint> res = new HashMap<DeclareConstraint, DeclareConstraint>();
		
		for (int score = 4; score >= 0; score--) {
			for (DeclareConstraint genCon : genModel.getConstraints()) {
				for (DeclareConstraint gsCon : gsModel.getConstraints()) {
					if (quantifyMatch(genCon, gsCon) == score) {
						if (!res.keySet().contains(genCon) && !res.values().contains(gsCon)) {
							res.put(genCon, gsCon);
						}
					}
				}
			}
		}
		for (DeclareConstraint genCon : genModel.getConstraints()) {
			if (!res.keySet().contains(genCon)) {
				res.put(genCon, null);
			}
		}
		return res;
	}
	
	
	public int quantifyMatch(DeclareConstraint con1, DeclareConstraint con2) {
		if (con1 == null || con2 == null) {
			return 0;
		}
		int res = 0;
		if (con1.getType() != null && con1.getType() == con2.getType()) {
			res++;
		}
		if (equalActions(con1.getActionA(), con2.getActionA())) {
			res++;
		}
				
		if (con2.getActionB() != null & equalActions(con1.getActionB(), con2.getActionB())) {
			res++;
		}
		if (con2.isNegative() && con1.isNegative()) {
			res++;
			if (!(con1.getType() != null && con1.getType() == con2.getType())) {
				res++;
			}
		}
		return res;
	}
	
	private boolean equalActions(Action action1, Action action2) {
		if (action1 == null && action2 == null) { 
			return true;
		}
		if (action1 == null || action2 == null) {
			return false;
		}
		String gsVerb = action2.actionStr().split(" ")[0];
		
		return (action1.actionStr().toLowerCase().contains(gsVerb)) || (action2.actionStr().contains(action1.getVerb()));
	}
	
	
	
}
