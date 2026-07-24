package nodes;

import axiom.AxiomContext;
import gen.Messages.OfxDocument;
import gen.Messages.OfxSyntax;
import gen.Messages.RawOfxInput;
import gen.Messages.Statement;
import gen.Messages.Transaction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ParseDocumentTest {

    @Test
    public void parsesSgmlBankStatementIntoNormalizedStructure() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.BANK_OFX_SGML).build());

        assertTrue(doc.getError().getOk());
        assertEquals(OfxSyntax.SGML, doc.getSyntax());
        assertEquals("102", doc.getVersion());

        assertEquals("0", doc.getSignOn().getStatusCode());
        assertEquals("INFO", doc.getSignOn().getStatusSeverity());
        assertEquals("2025-07-15T12:00:00Z", doc.getSignOn().getServerDateIso());
        assertEquals("First Test Bank", doc.getSignOn().getOrg());
        assertEquals("1001", doc.getSignOn().getFid());

        assertEquals(1, doc.getStatementsCount());
        Statement st = doc.getStatements(0);
        assertEquals("BANK", st.getStatementKind());
        assertEquals("121000358", st.getAccount().getBankId());
        assertEquals("00000123456", st.getAccount().getAccountId());
        assertEquals("USD", st.getCurrency());
        assertEquals("2025-07-01T00:00:00Z", st.getStartDateIso());
        assertEquals("2025-07-14T23:59:59Z", st.getEndDateIso());

        assertEquals(2, st.getTransactionsCount());
        Transaction t0 = st.getTransactions(0);
        assertEquals("DEBIT", t0.getType());
        assertEquals(-42.17, t0.getAmount(), 0.0001);
        assertEquals("2025070200001", t0.getFitid());
        assertEquals("COFFEE SHOP", t0.getName());
        assertEquals("Purchase", t0.getMemo());

        assertTrue(st.getLedgerBalance().getPresent());
        assertEquals(1957.83, st.getLedgerBalance().getAmount(), 0.0001);
        assertEquals("2025-07-14T23:59:59Z", st.getLedgerBalance().getAsOfDateIso());
    }

    @Test
    public void xmlAndSgmlSyntaxesProduceEquivalentTransactionData() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument sgmlDoc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.BANK_OFX_SGML).build());
        OfxDocument xmlDoc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.BANK_OFX_XML).build());

        assertTrue(sgmlDoc.getError().getOk());
        assertTrue(xmlDoc.getError().getOk());
        assertEquals(OfxSyntax.SGML, sgmlDoc.getSyntax());
        assertEquals(OfxSyntax.XML, xmlDoc.getSyntax());

        Transaction sgmlTx = sgmlDoc.getStatements(0).getTransactions(0);
        Transaction xmlTx = xmlDoc.getStatements(0).getTransactions(0);
        assertEquals(sgmlTx.getFitid(), xmlTx.getFitid());
        assertEquals(sgmlTx.getAmount(), xmlTx.getAmount(), 0.0001);
        assertEquals(sgmlTx.getDatePostedIso(), xmlTx.getDatePostedIso());
    }

    @Test
    public void emptyInputReturnsStructuredError() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText("").build());
        assertFalse(doc.getError().getOk());
        assertEquals("EMPTY_INPUT", doc.getError().getCode());
        assertEquals(0, doc.getStatementsCount());
    }

    @Test
    public void malformedInputReturnsStructuredErrorNotACrash() {
        AxiomContext ax = TestSupport.testContext();
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder()
                .setOfxText("OFXHEADER:100\nVERSION:102\n\n<OFX><SIGNONMSGSRSV1><SONRS>completely broken, no closing tags for aggregates")
                .build());
        assertFalse(doc.getError().getOk());
        assertEquals("MALFORMED", doc.getError().getCode());
    }

    @Test
    public void rejectsDoctypeDeclarationToPreventXxe() {
        AxiomContext ax = TestSupport.testContext();
        String xxePayload = "<?xml version=\"1.0\"?>\n" +
                "<?OFX OFXHEADER=\"200\" VERSION=\"203\" SECURITY=\"NONE\" OLDFILEUID=\"NONE\" NEWFILEUID=\"NONE\"?>\n" +
                "<!DOCTYPE OFX [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>\n" +
                "<OFX><SIGNONMSGSRSV1><SONRS><STATUS><CODE>0</CODE><SEVERITY>INFO</SEVERITY></STATUS>" +
                "<DTSERVER>20250715120000</DTSERVER><LANGUAGE>ENG</LANGUAGE></SONRS></SIGNONMSGSRSV1></OFX>";
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(xxePayload).build());
        assertFalse(doc.getError().getOk());
        assertEquals("UNSAFE_DOCTYPE", doc.getError().getCode());
        assertEquals(0, doc.getStatementsCount());
    }

    @Test
    public void largeInputDoesNotCrash() {
        AxiomContext ax = TestSupport.testContext();
        StringBuilder huge = new StringBuilder("OFXHEADER:100\nVERSION:102\n\n<OFX>");
        while (huge.length() <= 5_000_000) {
            huge.append("<PAD>x</PAD>");
        }
        huge.append("</OFX>");
        OfxDocument doc = ParseDocument.parseDocument(ax, RawOfxInput.newBuilder().setOfxText(huge.toString()).build());
        assertTrue(doc.getError().getOk());
    }
}
