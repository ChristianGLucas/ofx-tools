package nodes;

import axiom.AxiomContext;
import gen.Messages.OfxDocument;
import gen.Messages.RawOfxInput;
import gen.Messages.TransactionList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ExtractTransactionsTest {

    @Test
    public void flattensBankTransactionsWithCorrectFields() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.BANK_OFX_SGML).build());
        TransactionList result = ExtractTransactions.extractTransactions(ax, doc);

        assertEquals(2, result.getTransactionsCount());
        assertEquals("2025070200001", result.getTransactions(0).getFitid());
        assertEquals("BANK", result.getTransactions(0).getStatementKind());
        assertEquals("00000123456", result.getTransactions(0).getAccountId());
        assertEquals(1500.00, result.getTransactions(1).getAmount(), 0.0001);
    }

    @Test
    public void excludesInvestmentTransactionsFromTheFlattenedList() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.INVESTMENT_OFX_SGML).build());
        TransactionList result = ExtractTransactions.extractTransactions(ax, doc);

        assertEquals(0, result.getTransactionsCount());
    }
}
