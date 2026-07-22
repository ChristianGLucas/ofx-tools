package nodes;

import axiom.AxiomContext;
import gen.Messages.ExtractCurrencyResult;
import gen.Messages.OfxDocument;
import gen.Messages.RawOfxInput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ExtractCurrencyTest {

    @Test
    public void reportsDeclaredCurrencyPerStatement() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.BANK_OFX_SGML).build());
        ExtractCurrencyResult result = ExtractCurrency.extractCurrency(ax, doc);

        assertEquals(1, result.getCurrenciesCount());
        assertEquals("BANK", result.getCurrencies(0).getStatementKind());
        assertEquals("USD", result.getCurrencies(0).getCurrency());
    }
}
