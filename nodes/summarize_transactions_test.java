package nodes;

import axiom.AxiomContext;
import gen.Messages.OfxDocument;
import gen.Messages.RawOfxInput;
import gen.Messages.SummarizeTransactionsResult;
import gen.Messages.TransactionList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SummarizeTransactionsTest {

    @Test
    public void summarizesCountAndTotalsByType() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.BANK_OFX_SGML).build());
        TransactionList txs = ExtractTransactions.extractTransactions(ax, doc);

        SummarizeTransactionsResult result = SummarizeTransactions.summarizeTransactions(ax, txs);

        assertTrue(result.getError().getOk());
        assertEquals(2, result.getCount());
        assertEquals(1500.00, result.getTotalIn(), 0.0001);
        assertEquals(42.17, result.getTotalOut(), 0.0001);
        assertEquals(1457.83, result.getNet(), 0.0001);

        assertEquals(2, result.getCountByTypeCount());
        assertEquals("DEBIT", result.getCountByType(0).getType());
        assertEquals(1, result.getCountByType(0).getCount());
        assertEquals("CREDIT", result.getCountByType(1).getType());
        assertEquals(1, result.getCountByType(1).getCount());
    }
}
