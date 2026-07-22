package nodes;

import axiom.AxiomContext;
import gen.Messages.FilterByTypeInput;
import gen.Messages.OfxDocument;
import gen.Messages.RawOfxInput;
import gen.Messages.TransactionList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FilterTransactionsByTypeTest {

    @Test
    public void keepsOnlyMatchingTypeCaseInsensitively() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.BANK_OFX_SGML).build());
        TransactionList txs = ExtractTransactions.extractTransactions(ax, doc);

        TransactionList result = FilterTransactionsByType.filterTransactionsByType(ax,
                FilterByTypeInput.newBuilder().setTransactions(txs).setType("debit").build());

        assertEquals(1, result.getTransactionsCount());
        assertEquals("DEBIT", result.getTransactions(0).getType());
        assertEquals(-42.17, result.getTransactions(0).getAmount(), 0.0001);
    }

    @Test
    public void blankTypeMatchesNothing() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.BANK_OFX_SGML).build());
        TransactionList txs = ExtractTransactions.extractTransactions(ax, doc);

        TransactionList result = FilterTransactionsByType.filterTransactionsByType(ax,
                FilterByTypeInput.newBuilder().setTransactions(txs).setType("").build());

        assertEquals(0, result.getTransactionsCount());
    }
}
