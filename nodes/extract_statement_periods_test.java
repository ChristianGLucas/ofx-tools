package nodes;

import axiom.AxiomContext;
import gen.Messages.ExtractStatementPeriodsResult;
import gen.Messages.OfxDocument;
import gen.Messages.RawOfxInput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ExtractStatementPeriodsTest {

    @Test
    public void reportsTheDeclaredStatementPeriodNotInferredFromTransactions() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.BANK_OFX_SGML).build());
        ExtractStatementPeriodsResult result = ExtractStatementPeriods.extractStatementPeriods(ax, doc);

        assertTrue(result.getError().getOk());
        assertEquals(1, result.getPeriodsCount());
        assertEquals("BANK", result.getPeriods(0).getStatementKind());
        assertEquals("00000123456", result.getPeriods(0).getAccountId());
        // Declared DTSTART/DTEND (2025-07-01 .. 2025-07-14T23:59:59Z), which
        // is a WIDER window than the earliest/latest transaction dates
        // (2025-07-02 .. 2025-07-05) — proving this reads the declared
        // period, not a derived one.
        assertEquals("2025-07-01T00:00:00Z", result.getPeriods(0).getStartDateIso());
        assertEquals("2025-07-14T23:59:59Z", result.getPeriods(0).getEndDateIso());
    }
}
