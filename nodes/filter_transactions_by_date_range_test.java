package nodes;

import axiom.AxiomContext;
import gen.Messages.FilterByDateRangeInput;
import gen.Messages.OfxDocument;
import gen.Messages.RawOfxInput;
import gen.Messages.TransactionList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FilterTransactionsByDateRangeTest {

    private TransactionList bankTransactions() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.BANK_OFX_SGML).build());
        return ExtractTransactions.extractTransactions(ax, doc);
    }

    @Test
    public void startBoundExcludesTransactionsBeforeIt() {
        // DEBIT posted 2025-07-02, CREDIT posted 2025-07-05.
        AxiomContext ax = TestSupport.testContext();
        TransactionList result = FilterTransactionsByDateRange.filterTransactionsByDateRange(ax,
                FilterByDateRangeInput.newBuilder().setTransactions(bankTransactions()).setStartDateIso("2025-07-03").build());

        assertEquals(1, result.getTransactionsCount());
        assertEquals("2025070500002", result.getTransactions(0).getFitid());
    }

    @Test
    public void dateOnlyEndBoundIsInclusiveOfTheWholeDay() {
        // A caller-supplied end bound of "2025-07-02" (date-only) must still
        // include the DEBIT posted at 2025-07-02T12:00:00Z — a naive
        // lexicographic string comparison against the full instant would
        // wrongly exclude it.
        AxiomContext ax = TestSupport.testContext();
        TransactionList result = FilterTransactionsByDateRange.filterTransactionsByDateRange(ax,
                FilterByDateRangeInput.newBuilder().setTransactions(bankTransactions()).setEndDateIso("2025-07-02").build());

        assertEquals(1, result.getTransactionsCount());
        assertEquals("2025070200001", result.getTransactions(0).getFitid());
    }

    @Test
    public void unparseableBoundIsIgnoredRatherThanCrashing() {
        AxiomContext ax = TestSupport.testContext();
        TransactionList result = FilterTransactionsByDateRange.filterTransactionsByDateRange(ax,
                FilterByDateRangeInput.newBuilder().setTransactions(bankTransactions()).setStartDateIso("not-a-date").build());

        // The unparseable bound is treated as absent, so both transactions pass through.
        assertEquals(2, result.getTransactionsCount());
    }

    @Test
    public void noBoundsReturnsEverythingUnchanged() {
        AxiomContext ax = TestSupport.testContext();
        TransactionList result = FilterTransactionsByDateRange.filterTransactionsByDateRange(ax,
                FilterByDateRangeInput.newBuilder().setTransactions(bankTransactions()).build());
        assertEquals(2, result.getTransactionsCount());
    }
}
