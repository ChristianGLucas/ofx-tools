package nodes;

import axiom.AxiomContext;
import gen.Messages.ComputeTotalsResult;
import gen.Messages.OfxDocument;
import gen.Messages.RawOfxInput;
import gen.Messages.TransactionList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ComputeTransactionTotalsTest {

    @Test
    public void sumsCreditsAndDebitsSeparately() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.BANK_OFX_SGML).build());
        TransactionList txs = ExtractTransactions.extractTransactions(ax, doc);

        ComputeTotalsResult result = ComputeTransactionTotals.computeTransactionTotals(ax, txs);

        // Hand-computed oracle: one -42.17 debit, one +1500.00 credit.
        assertTrue(result.getError().getOk());
        assertEquals(1500.00, result.getSumCredits(), 0.0001);
        assertEquals(42.17, result.getSumDebits(), 0.0001);
        assertEquals(1457.83, result.getNet(), 0.0001);
        assertEquals(2, result.getTransactionCount());
    }

    @Test
    public void emptyListProducesZeroTotals() {
        AxiomContext ax = TestSupport.testContext();
        ComputeTotalsResult result = ComputeTransactionTotals.computeTransactionTotals(ax, TransactionList.newBuilder().build());
        assertEquals(0.0, result.getSumCredits(), 0.0001);
        assertEquals(0.0, result.getSumDebits(), 0.0001);
        assertEquals(0, result.getTransactionCount());
    }
}
