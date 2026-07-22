package nodes;

import axiom.AxiomContext;
import gen.Messages.InvestmentTransaction;
import gen.Messages.InvestmentTransactionList;
import gen.Messages.OfxDocument;
import gen.Messages.RawOfxInput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ExtractInvestmentTransactionsTest {

    @Test
    public void extractsBuyAndIncomeTransactionsWithFullMonetaryDetail() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.INVESTMENT_OFX_SGML).build());
        InvestmentTransactionList result = ExtractInvestmentTransactions.extractInvestmentTransactions(ax, doc);

        assertTrue(result.getError().getOk());
        assertEquals(2, result.getTransactionsCount());

        InvestmentTransaction buy = result.getTransactions(0);
        assertEquals("BUY_STOCK", buy.getType());
        assertEquals("INV0001", buy.getFitid());
        assertEquals("2025-07-05T00:00:00Z", buy.getTradeDateIso());
        assertEquals("2025-07-08T00:00:00Z", buy.getSettleDateIso());
        assertEquals("037833100", buy.getSecurityId());
        assertEquals("CUSIP", buy.getSecurityIdType());
        assertEquals(10.0, buy.getUnits(), 0.0001);
        assertEquals(150.25, buy.getUnitPrice(), 0.0001);
        assertEquals(9.99, buy.getCommission(), 0.0001);
        assertEquals(-1512.49, buy.getTotal(), 0.0001);

        InvestmentTransaction income = result.getTransactions(1);
        assertEquals("INCOME", income.getType());
        assertEquals("INV0002", income.getFitid());
        assertEquals("DIV", income.getIncomeType());
        assertEquals(25.50, income.getTotal(), 0.0001);
    }
}
