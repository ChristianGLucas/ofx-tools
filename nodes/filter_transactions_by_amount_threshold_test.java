package nodes;

import axiom.AxiomContext;
import gen.Messages.FilterByAmountThresholdInput;
import gen.Messages.OfxDocument;
import gen.Messages.RawOfxInput;
import gen.Messages.TransactionList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FilterTransactionsByAmountThresholdTest {

    private TransactionList bankTransactions() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.BANK_OFX_SGML).build());
        return ExtractTransactions.extractTransactions(ax, doc);
    }

    @Test
    public void minBoundKeepsOnlyNonNegativeAmounts() {
        AxiomContext ax = TestSupport.testContext();
        TransactionList result = FilterTransactionsByAmountThreshold.filterTransactionsByAmountThreshold(ax,
                FilterByAmountThresholdInput.newBuilder().setTransactions(bankTransactions()).setHasMin(true).setMinAmount(0).build());

        assertEquals(1, result.getTransactionsCount());
        assertEquals(1500.00, result.getTransactions(0).getAmount(), 0.0001);
    }

    @Test
    public void maxBoundKeepsOnlyNonPositiveAmounts() {
        AxiomContext ax = TestSupport.testContext();
        TransactionList result = FilterTransactionsByAmountThreshold.filterTransactionsByAmountThreshold(ax,
                FilterByAmountThresholdInput.newBuilder().setTransactions(bankTransactions()).setHasMax(true).setMaxAmount(0).build());

        assertEquals(1, result.getTransactionsCount());
        assertEquals(-42.17, result.getTransactions(0).getAmount(), 0.0001);
    }

    @Test
    public void noBoundsReturnsEverything() {
        AxiomContext ax = TestSupport.testContext();
        TransactionList result = FilterTransactionsByAmountThreshold.filterTransactionsByAmountThreshold(ax,
                FilterByAmountThresholdInput.newBuilder().setTransactions(bankTransactions()).build());
        assertEquals(2, result.getTransactionsCount());
    }
}
