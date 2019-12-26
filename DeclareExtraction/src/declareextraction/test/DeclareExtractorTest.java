package declareextraction.test;

import declareextraction.constructs.*;
import declareextraction.textprocessing.DeclareConstructor;
import declareextraction.textprocessing.TextParser;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeclareExtractorTest {
    private static TextParser textParser;
    private static DeclareConstructor declareConstructor;

    @BeforeAll
    public static void setUp() {
        textParser = new TextParser();
        declareConstructor = new DeclareConstructor();
    }

    @Test
    public void singleConstraintEncoding() {
        String text = "The process starts when the customer arrives.";
        TextModel textModel = textParser.parseConstraintString(text);
        DeclareModel dm = declareConstructor.convertToDeclareModel(textModel);
        assertEquals(1, dm.getConstraints().size());

        DeclareConstraint dc = new DeclareConstraint(ConstraintType.INIT, new Action("the customer arrive"));
        DeclareConstraint dcEncoded = dm.getConstraints().get(0);
        assertEquals(dc.toRuMString(), dcEncoded.toRuMString());
    }

    @Test
    public void singleConstraintNegative() {
        String text = "the customer does not enter after the invoice is not paid";
        DeclareConstraint dc1 = getOnlyConstraintFromSentence(text);

        String text2 = "if the invoice is not paid the customer does not enter";
        DeclareConstraint dc2 = getOnlyConstraintFromSentence(text2);

        assertEquals(dc1.toRuMString(), dc2.toRuMString());

        DeclareConstraint dc3 = new DeclareConstraint(ConstraintType.PRECEDENCE, new Action("not pay the invoice"), new Action("the customer enter"), true);
        dc3.getActionB().setNegative(true);
        assertEquals(dc1.toRuMString(), dc3.toRuMString());
    }

    @Test
    public void RuMPrintConstraint() {
        String text = "immediately after the invoice is not paid the customer does not nicely immediately nicely enter immediately";
        DeclareConstraint dc = getOnlyConstraintFromSentence(text);
        System.out.println(dc.toRuMString());
    }

    @Test
    public void constraintTypesCheck() {
        Map<String, ConstraintType> sentenceToConstraint = new HashMap<>();
        sentenceToConstraint.put("process starts with making an approval", ConstraintType.INIT);
        sentenceToConstraint.put("case ends with removing instances", ConstraintType.END);
        //sentenceToConstraint.put("invoice is made", ConstraintType.EXISTENCE);
        //sentenceToConstraint.put("customer is not served", ConstraintType.ABSENCE);
        sentenceToConstraint.put("once the results are clear, the employee is notified", ConstraintType.PRECEDENCE);
        sentenceToConstraint.put("if something happens, we will react", ConstraintType.RESPONSE);
        sentenceToConstraint.put("if a meeting must be arranged, it must be held", ConstraintType.SUCCESSION);
        sentenceToConstraint.put("the results are displayed immediately, after the notification is received", ConstraintType.CHAINPRECEDENCE);
        sentenceToConstraint.put("if something happens, we will react instantly", ConstraintType.CHAINRESPONSE);
        sentenceToConstraint.put("if a meeting must be arranged, it must directly be held", ConstraintType.CHAINSUCCESSION);

        for (Map.Entry<String, ConstraintType> entry: sentenceToConstraint.entrySet()) {
            DeclareConstraint dc = getOnlyConstraintFromSentence(entry.getKey());
            System.out.println(dc.toRuMString() + '\n');
            assertEquals(entry.getValue(), dc.getType());
        }
    }

    @Test
    public void chainConstraintFail() {
        //is not recognized as chain constraint since the clause is not next to the verb (StanfordParsing logic); only one clause is saved per action
        //starting actionA with "immediately after" or "as soon" not possible either
        String text = "if the invoice is not paid the customer does not immediately nicely enter";
        DeclareConstraint dc = getOnlyConstraintFromSentence(text);

        assertEquals(ConstraintType.CHAINPRECEDENCE, dc.getType());
    }

    private DeclareConstraint getOnlyConstraintFromSentence(String text) {
        DeclareModel declareModel = declareConstructor.convertToDeclareModel(textParser.parseConstraintString(text));
        assertEquals(1, declareModel.getConstraints().size());
        return declareModel.getConstraints().get(0);
    }
}