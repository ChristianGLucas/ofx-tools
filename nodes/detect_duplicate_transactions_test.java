package nodes;

import axiom.AxiomContext;
import gen.Messages.DetectDuplicatesResult;
import gen.Messages.Transaction;
import gen.Messages.TransactionList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DetectDuplicateTransactionsTest {

    @Test
    public void findsTransactionsSharingTheSameFitid() {
        AxiomContext ax = TestSupport.testContext();
        TransactionList txs = TransactionList.newBuilder()
                .addTransactions(Transaction.newBuilder().setFitid("DUPE1").setAmount(10).build())
                .addTransactions(Transaction.newBuilder().setFitid("UNIQUE1").setAmount(20).build())
                .addTransactions(Transaction.newBuilder().setFitid("DUPE1").setAmount(10).build())
                .build();

        DetectDuplicatesResult result = DetectDuplicateTransactions.detectDuplicateTransactions(ax, txs);

        assertTrue(result.getError().getOk());
        assertEquals(1, result.getDuplicateGroupsCount());
        assertEquals("DUPE1", result.getDuplicateGroups(0).getFitid());
        assertEquals(2, result.getDuplicateGroups(0).getCount());
        assertEquals(java.util.List.of(0, 2), result.getDuplicateGroups(0).getIndicesList());
        assertEquals(2, result.getDuplicateTransactionCount());
    }

    @Test
    public void blankFitidsAreNeverReportedAsDuplicatesOfEachOther() {
        AxiomContext ax = TestSupport.testContext();
        TransactionList txs = TransactionList.newBuilder()
                .addTransactions(Transaction.newBuilder().setFitid("").setAmount(10).build())
                .addTransactions(Transaction.newBuilder().setFitid("").setAmount(20).build())
                .build();

        DetectDuplicatesResult result = DetectDuplicateTransactions.detectDuplicateTransactions(ax, txs);

        assertEquals(0, result.getDuplicateGroupsCount());
        assertEquals(0, result.getDuplicateTransactionCount());
    }

    @Test
    public void noDuplicatesProducesEmptyResult() {
        AxiomContext ax = TestSupport.testContext();
        TransactionList txs = TransactionList.newBuilder()
                .addTransactions(Transaction.newBuilder().setFitid("A").build())
                .addTransactions(Transaction.newBuilder().setFitid("B").build())
                .build();

        DetectDuplicatesResult result = DetectDuplicateTransactions.detectDuplicateTransactions(ax, txs);
        assertEquals(0, result.getDuplicateGroupsCount());
    }
}
