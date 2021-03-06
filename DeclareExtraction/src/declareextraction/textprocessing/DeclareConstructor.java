package declareextraction.textprocessing;

import java.util.Arrays;

import declareextraction.constructs.Action;
import declareextraction.constructs.ConstraintType;
import declareextraction.constructs.DeclareConstraint;
import declareextraction.constructs.DeclareModel;
import declareextraction.constructs.TextModel;
import declareextraction.utils.WordClasses;

public class DeclareConstructor {
	public DeclareModel convertToDeclareModel(TextModel textModel) {
		DeclareModel declareModel = new DeclareModel(textModel.getText());

		if (textModel.getInterrelations().isEmpty() && textModel.getActions().size() == 1) {
			DeclareConstraint constraint = transformToUnaryConstraint(textModel.getActions().get(0));
			declareModel.addConstraint(constraint);
			System.out.println("Constraint identified: " + constraint);
		}

		for (Interrelation rel : textModel.getInterrelations()) {
			DeclareConstraint constraint = transformRelationToConstraint(rel);
			if (constraint != null) {
				declareModel.addConstraint(constraint);
				System.out.println("Constraint identified: " + constraint);
			}
		}
		return declareModel;
	}

	private DeclareConstraint transformToUnaryConstraint(Action action) {
		if (action.isNegative()) {
			return new DeclareConstraint(ConstraintType.ABSENCE, action);
		} else {
			return new DeclareConstraint(ConstraintType.EXISTENCE, action);
		}
	}

	private DeclareConstraint transformRelationToConstraint(Interrelation rel) {
		// transform into meta constraint (init/end)
		if (isMetaAction(rel.getActionA()) || isMetaAction(rel.getActionB())) {
			return transformToMetaConstraint(rel);
		}

		// transform into binary constraint (response/precedence/succession)
		if (rel.getType() == Interrelation.RelationType.FOLLOWS) {
			return transformToBinaryConstraint(rel);
		}
		return null;
	}

	private DeclareConstraint transformToMetaConstraint(Interrelation rel) {
		System.out.println("Identified meta-relation: " + rel);
		// case: check if action A indicates process start
		if (WordClasses.isStartVerb(rel.getActionA().getVerb())) {
			ConstraintType constraintType = ConstraintType.INIT;
			return new DeclareConstraint(constraintType, rel.getActionB());
		}
		// case: check if action B indicates process start
		if (WordClasses.isStartVerb(rel.getActionB().getVerb())) {
			ConstraintType constraintType = ConstraintType.INIT;
			return  new DeclareConstraint(constraintType, rel.getActionA());
		}
		// case: check if action A indicates process end
		if (WordClasses.isEndVerb(rel.getActionA().getVerb())) {
			ConstraintType constraintType = ConstraintType.END;
			return new DeclareConstraint(constraintType, rel.getActionB());
		}
		// case: check if action B indicates process end
		if (WordClasses.isEndVerb(rel.getActionB().getVerb())) {
			ConstraintType constraintType = ConstraintType.END;
			return new DeclareConstraint(constraintType, rel.getActionA());
		}
		return null;
	}

	private DeclareConstraint transformToBinaryConstraint(Interrelation rel) {
		// determine binary constraint type
		ConstraintType constraintType = null;
		boolean aMand = WordClasses.isMandatory(rel.getActionA().getModal());
		boolean bMand = WordClasses.isMandatory(rel.getActionB().getModal());
		boolean bImm = rel.getActionB().isImmediate();
		if (!bMand) {
			constraintType = bImm ? ConstraintType.CHAINPRECEDENCE : ConstraintType.PRECEDENCE;
		} else if (aMand) {
			constraintType = bImm ? ConstraintType.CHAINSUCCESSION : ConstraintType.SUCCESSION;
		} else {
			constraintType = bImm ? ConstraintType.CHAINRESPONSE : ConstraintType.RESPONSE;
		}

		DeclareConstraint constraint = new DeclareConstraint(constraintType, rel.getActionA(), rel.getActionB());

		// check if constraint should be negated
		if (constraint.getActionB().isNegative()) {
			constraint.setNegative();
		}
		return constraint;
	}

	private boolean isMetaAction(Action action) {
		String joint = action.getObject().getText() + " " + action.getSubject().getText();

		for (String processObject : Arrays.asList(WordClasses.PROCESS_OBJECTS)) {
			if (joint.contains(processObject)) {
				return true;
			}
		}
		return false;
	}
}
