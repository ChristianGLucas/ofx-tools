package nodes;

import axiom.AxiomContext;
import gen.Messages.AccountType;
import gen.Messages.ExtractAccountsResult;
import gen.Messages.OfxDocument;
import gen.Messages.RawOfxInput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ExtractAccountsTest {

    @Test
    public void listsTheCheckingAccountFromTheStatement() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.BANK_OFX_SGML).build());
        ExtractAccountsResult result = ExtractAccounts.extractAccounts(ax, doc);

        assertTrue(result.getError().getOk());
        assertEquals(1, result.getAccountsCount());
        assertEquals("121000358", result.getAccounts(0).getBankId());
        assertEquals("00000123456", result.getAccounts(0).getAccountId());
        assertEquals(AccountType.CHECKING, result.getAccounts(0).getAccountType());
    }

    @Test
    public void reportsCreditCardAccountTypeForCcStatement() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.CREDITCARD_OFX_SGML).build());
        ExtractAccountsResult result = ExtractAccounts.extractAccounts(ax, doc);

        assertEquals(1, result.getAccountsCount());
        assertEquals("4111111111111111", result.getAccounts(0).getAccountId());
        assertEquals(AccountType.CREDITCARD, result.getAccounts(0).getAccountType());
    }
}
