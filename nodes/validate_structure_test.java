package nodes;

import axiom.AxiomContext;
import gen.Messages.RawOfxInput;
import gen.Messages.ValidateStructureResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ValidateStructureTest {

    @Test
    public void validDocumentReportsNoIssues() {
        AxiomContext ax = TestSupport.testContext();
        ValidateStructureResult result = ValidateStructure.validateStructure(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.BANK_OFX_SGML).build());

        assertTrue(result.getError().getOk());
        assertTrue(result.getValid());
        assertTrue(result.getHasOfxRoot());
        assertTrue(result.getHasSignon());
        assertEquals(1, result.getStatementCount());
        assertEquals(0, result.getIssuesCount());
    }

    @Test
    public void malformedDocumentIsReportedAsInvalidNotAsATopLevelError() {
        AxiomContext ax = TestSupport.testContext();
        ValidateStructureResult result = ValidateStructure.validateStructure(ax, RawOfxInput.newBuilder()
                .setOfxText("this is not an ofx document at all").build());

        // The node COULD attempt validation (input passed the guard), it
        // just found the document invalid — so the top-level error stays
        // ok=true while valid=false and has_ofx_root=false.
        assertTrue(result.getError().getOk());
        assertFalse(result.getValid());
        assertFalse(result.getHasOfxRoot());
        assertFalse(result.getIssuesList().isEmpty());
    }

    @Test
    public void emptyInputIsAGuardLevelTopLevelError() {
        AxiomContext ax = TestSupport.testContext();
        ValidateStructureResult result = ValidateStructure.validateStructure(ax, RawOfxInput.newBuilder().setOfxText("").build());

        assertFalse(result.getError().getOk());
        assertEquals("EMPTY_INPUT", result.getError().getCode());
        assertFalse(result.getValid());
    }

    @Test
    public void missingStatementIsFlaggedAsAnIssue() {
        AxiomContext ax = TestSupport.testContext();
        String signOnOnly = "OFXHEADER:100\nDATA:OFXSGML\nVERSION:102\nSECURITY:NONE\nENCODING:USASCII\nCHARSET:1252\nCOMPRESSION:NONE\nOLDFILEUID:NONE\nNEWFILEUID:NONE\n\n" +
                "<OFX>\n<SIGNONMSGSRSV1>\n<SONRS>\n<STATUS>\n<CODE>0\n<SEVERITY>INFO\n</STATUS>\n<DTSERVER>20250715120000\n<LANGUAGE>ENG\n</SONRS>\n</SIGNONMSGSRSV1>\n</OFX>\n";
        ValidateStructureResult result = ValidateStructure.validateStructure(ax, RawOfxInput.newBuilder().setOfxText(signOnOnly).build());

        assertTrue(result.getError().getOk());
        assertFalse(result.getValid());
        assertTrue(result.getHasOfxRoot());
        assertTrue(result.getHasSignon());
        assertEquals(0, result.getStatementCount());
        assertFalse(result.getIssuesList().isEmpty());
    }
}
