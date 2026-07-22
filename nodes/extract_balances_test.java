package nodes;

import axiom.AxiomContext;
import gen.Messages.ExtractBalancesResult;
import gen.Messages.OfxDocument;
import gen.Messages.RawOfxInput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ExtractBalancesTest {

    @Test
    public void reportsLedgerAndAvailableBalanceWhenBothPresent() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.BANK_OFX_SGML).build());
        ExtractBalancesResult result = ExtractBalances.extractBalances(ax, doc);

        assertEquals(1, result.getBalancesCount());
        assertTrue(result.getBalances(0).getLedgerBalance().getPresent());
        assertEquals(1957.83, result.getBalances(0).getLedgerBalance().getAmount(), 0.0001);
        assertTrue(result.getBalances(0).getAvailableBalance().getPresent());
        assertEquals(1957.83, result.getBalances(0).getAvailableBalance().getAmount(), 0.0001);
    }

    @Test
    public void reportsAbsentAvailableBalanceDistinctFromZero() {
        AxiomContext ax = TestSupport.testContext();
        // CREDITCARD_OFX_SGML declares a LEDGERBAL but no AVAILBAL at all.
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.CREDITCARD_OFX_SGML).build());
        ExtractBalancesResult result = ExtractBalances.extractBalances(ax, doc);

        assertEquals(1, result.getBalancesCount());
        assertTrue(result.getBalances(0).getLedgerBalance().getPresent());
        assertEquals(-39.99, result.getBalances(0).getLedgerBalance().getAmount(), 0.0001);
        assertFalse(result.getBalances(0).getAvailableBalance().getPresent());
    }
}
