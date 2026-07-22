package nodes;

import axiom.AxiomContext;
import gen.Messages.ExtractCreditCardStatementsResult;
import gen.Messages.OfxDocument;
import gen.Messages.RawOfxInput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ExtractCreditCardStatementsTest {

    @Test
    public void extractsCreditCardStatementDetail() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.CREDITCARD_OFX_SGML).build());
        ExtractCreditCardStatementsResult result = ExtractCreditCardStatements.extractCreditCardStatements(ax, doc);

        assertTrue(result.getError().getOk());
        assertEquals(1, result.getStatementsCount());
        assertEquals("4111111111111111", result.getStatements(0).getAccount().getAccountId());
        assertEquals("USD", result.getStatements(0).getCurrency());
        assertEquals(2, result.getStatements(0).getTransactionsCount());
        assertEquals(-39.99, result.getStatements(0).getLedgerBalance().getAmount(), 0.0001);
        assertFalse(result.getStatements(0).getAvailableBalance().getPresent());
    }

    @Test
    public void excludesNonCreditCardStatements() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.BANK_OFX_SGML).build());
        ExtractCreditCardStatementsResult result = ExtractCreditCardStatements.extractCreditCardStatements(ax, doc);
        assertEquals(0, result.getStatementsCount());
    }
}
