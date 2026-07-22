package nodes;

import axiom.AxiomContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Shared test fixtures and a no-op AxiomContext, reused across every
 * *_test.java file in this package. Not a node itself.
 */
final class TestSupport {

    private TestSupport() {
    }

    static AxiomContext testContext() {
        return new TestContext();
    }

    static final class TestContext implements AxiomContext {
        public Logger log() {
            return new Logger() {
                public void debug(String m, Map<String, String> a) {}
                public void info(String m, Map<String, String> a)  {}
                public void warn(String m, Map<String, String> a)  {}
                public void error(String m, Map<String, String> a) {}
            };
        }
        public Secrets secrets() { return name -> Optional.empty(); }
        public String executionId() { return "test-execution-id"; }
        public String flowId() { return "test-flow-id"; }
        public String tenantId() { return "test-tenant-id"; }
        public Reflection reflection() {
            return () -> new FlowReflection() {
                public List<ReflectionNode> nodes() { return List.of(); }
                public List<ReflectionEdge> edges() { return List.of(); }
                public List<ReflectionEdge> loopEdges() { return List.of(); }
                public FlowPosition position() { return new FlowPosition(0, 0, Map.of(), List.of()); }
                public String graphId() { return ""; }
            };
        }
        public Mutation mutation() {
            return () -> new FlowMutation() {
                public int addNode(String pkg, String ver, CanvasPosition pos) { return 0; }
                public void addEdge(int src, int dst, EdgeCondition cond) {}
            };
        }
    }

    // ─────────────────────────── fixtures ───────────────────────────
    //
    // Every date/amount/id below is a hand-authored, hand-computed known
    // value (the independent oracle for the tests that assert against it):
    // checking statement DTSTART 2025-07-01T00:00:00Z .. DTEND
    // 2025-07-14T23:59:59Z, one DEBIT of -42.17 (FITID 2025070200001) and
    // one CREDIT of 1500.00 (FITID 2025070500002); ledger/available balance
    // both 1957.83 as-of 2025-07-14T23:59:59Z.

    static final String BANK_OFX_SGML =
            "OFXHEADER:100\r\n" +
            "DATA:OFXSGML\r\n" +
            "VERSION:102\r\n" +
            "SECURITY:NONE\r\n" +
            "ENCODING:USASCII\r\n" +
            "CHARSET:1252\r\n" +
            "COMPRESSION:NONE\r\n" +
            "OLDFILEUID:NONE\r\n" +
            "NEWFILEUID:NONE\r\n" +
            "\r\n" +
            "<OFX>\n" +
            "<SIGNONMSGSRSV1>\n" +
            "<SONRS>\n" +
            "<STATUS>\n" +
            "<CODE>0\n" +
            "<SEVERITY>INFO\n" +
            "</STATUS>\n" +
            "<DTSERVER>20250715120000\n" +
            "<LANGUAGE>ENG\n" +
            "<FI>\n" +
            "<ORG>First Test Bank\n" +
            "<FID>1001\n" +
            "</FI>\n" +
            "</SONRS>\n" +
            "</SIGNONMSGSRSV1>\n" +
            "<BANKMSGSRSV1>\n" +
            "<STMTTRNRS>\n" +
            "<TRNUID>1\n" +
            "<STATUS>\n" +
            "<CODE>0\n" +
            "<SEVERITY>INFO\n" +
            "</STATUS>\n" +
            "<STMTRS>\n" +
            "<CURDEF>USD\n" +
            "<BANKACCTFROM>\n" +
            "<BANKID>121000358\n" +
            "<ACCTID>00000123456\n" +
            "<ACCTTYPE>CHECKING\n" +
            "</BANKACCTFROM>\n" +
            "<BANKTRANLIST>\n" +
            "<DTSTART>20250701000000\n" +
            "<DTEND>20250714235959\n" +
            "<STMTTRN>\n" +
            "<TRNTYPE>DEBIT\n" +
            "<DTPOSTED>20250702120000\n" +
            "<TRNAMT>-42.17\n" +
            "<FITID>2025070200001\n" +
            "<NAME>COFFEE SHOP\n" +
            "<MEMO>Purchase\n" +
            "</STMTTRN>\n" +
            "<STMTTRN>\n" +
            "<TRNTYPE>CREDIT\n" +
            "<DTPOSTED>20250705090000\n" +
            "<TRNAMT>1500.00\n" +
            "<FITID>2025070500002\n" +
            "<NAME>PAYROLL DEPOSIT\n" +
            "</STMTTRN>\n" +
            "</BANKTRANLIST>\n" +
            "<LEDGERBAL>\n" +
            "<BALAMT>1957.83\n" +
            "<DTASOF>20250714235959\n" +
            "</LEDGERBAL>\n" +
            "<AVAILBAL>\n" +
            "<BALAMT>1957.83\n" +
            "<DTASOF>20250714235959\n" +
            "</AVAILBAL>\n" +
            "</STMTRS>\n" +
            "</STMTTRNRS>\n" +
            "</BANKMSGSRSV1>\n" +
            "</OFX>\n";

    // Same document, same values, as OFX 2.x XML — used to prove both
    // syntaxes parse to the same normalized result.
    static final String BANK_OFX_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<?OFX OFXHEADER=\"200\" VERSION=\"203\" SECURITY=\"NONE\" OLDFILEUID=\"NONE\" NEWFILEUID=\"NONE\"?>\n" +
            "<OFX>\n" +
            "  <SIGNONMSGSRSV1>\n" +
            "    <SONRS>\n" +
            "      <STATUS><CODE>0</CODE><SEVERITY>INFO</SEVERITY></STATUS>\n" +
            "      <DTSERVER>20250715120000</DTSERVER>\n" +
            "      <LANGUAGE>ENG</LANGUAGE>\n" +
            "      <FI><ORG>First Test Bank</ORG><FID>1001</FID></FI>\n" +
            "    </SONRS>\n" +
            "  </SIGNONMSGSRSV1>\n" +
            "  <BANKMSGSRSV1>\n" +
            "    <STMTTRNRS>\n" +
            "      <TRNUID>1</TRNUID>\n" +
            "      <STATUS><CODE>0</CODE><SEVERITY>INFO</SEVERITY></STATUS>\n" +
            "      <STMTRS>\n" +
            "        <CURDEF>USD</CURDEF>\n" +
            "        <BANKACCTFROM><BANKID>121000358</BANKID><ACCTID>00000123456</ACCTID><ACCTTYPE>CHECKING</ACCTTYPE></BANKACCTFROM>\n" +
            "        <BANKTRANLIST>\n" +
            "          <DTSTART>20250701000000</DTSTART>\n" +
            "          <DTEND>20250714235959</DTEND>\n" +
            "          <STMTTRN>\n" +
            "            <TRNTYPE>DEBIT</TRNTYPE>\n" +
            "            <DTPOSTED>20250702120000</DTPOSTED>\n" +
            "            <TRNAMT>-42.17</TRNAMT>\n" +
            "            <FITID>2025070200001</FITID>\n" +
            "            <NAME>COFFEE SHOP</NAME>\n" +
            "            <MEMO>Purchase</MEMO>\n" +
            "          </STMTTRN>\n" +
            "          <STMTTRN>\n" +
            "            <TRNTYPE>CREDIT</TRNTYPE>\n" +
            "            <DTPOSTED>20250705090000</DTPOSTED>\n" +
            "            <TRNAMT>1500.00</TRNAMT>\n" +
            "            <FITID>2025070500002</FITID>\n" +
            "            <NAME>PAYROLL DEPOSIT</NAME>\n" +
            "          </STMTTRN>\n" +
            "        </BANKTRANLIST>\n" +
            "        <LEDGERBAL><BALAMT>1957.83</BALAMT><DTASOF>20250714235959</DTASOF></LEDGERBAL>\n" +
            "        <AVAILBAL><BALAMT>1957.83</BALAMT><DTASOF>20250714235959</DTASOF></AVAILBAL>\n" +
            "      </STMTRS>\n" +
            "    </STMTTRNRS>\n" +
            "  </BANKMSGSRSV1>\n" +
            "</OFX>\n";

    // Credit-card statement: two transactions (-89.99 DEBIT, 50.00 CREDIT),
    // ledger balance -39.99, deliberately NO AVAILBAL (tests
    // Balance.present=false for an absent balance).
    static final String CREDITCARD_OFX_SGML =
            "OFXHEADER:100\r\n" +
            "DATA:OFXSGML\r\n" +
            "VERSION:102\r\n" +
            "SECURITY:NONE\r\n" +
            "ENCODING:USASCII\r\n" +
            "CHARSET:1252\r\n" +
            "COMPRESSION:NONE\r\n" +
            "OLDFILEUID:NONE\r\n" +
            "NEWFILEUID:NONE\r\n" +
            "\r\n" +
            "<OFX>\n" +
            "<SIGNONMSGSRSV1>\n" +
            "<SONRS>\n" +
            "<STATUS>\n" +
            "<CODE>0\n" +
            "<SEVERITY>INFO\n" +
            "</STATUS>\n" +
            "<DTSERVER>20250715120000\n" +
            "<LANGUAGE>ENG\n" +
            "</SONRS>\n" +
            "</SIGNONMSGSRSV1>\n" +
            "<CREDITCARDMSGSRSV1>\n" +
            "<CCSTMTTRNRS>\n" +
            "<TRNUID>1\n" +
            "<STATUS>\n" +
            "<CODE>0\n" +
            "<SEVERITY>INFO\n" +
            "</STATUS>\n" +
            "<CCSTMTRS>\n" +
            "<CURDEF>USD\n" +
            "<CCACCTFROM>\n" +
            "<ACCTID>4111111111111111\n" +
            "</CCACCTFROM>\n" +
            "<BANKTRANLIST>\n" +
            "<DTSTART>20250601000000\n" +
            "<DTEND>20250630235959\n" +
            "<STMTTRN>\n" +
            "<TRNTYPE>DEBIT\n" +
            "<DTPOSTED>20250610100000\n" +
            "<TRNAMT>-89.99\n" +
            "<FITID>CC0001\n" +
            "<NAME>ONLINE STORE\n" +
            "</STMTTRN>\n" +
            "<STMTTRN>\n" +
            "<TRNTYPE>CREDIT\n" +
            "<DTPOSTED>20250615100000\n" +
            "<TRNAMT>50.00\n" +
            "<FITID>CC0002\n" +
            "<NAME>PAYMENT RECEIVED\n" +
            "</STMTTRN>\n" +
            "</BANKTRANLIST>\n" +
            "<LEDGERBAL>\n" +
            "<BALAMT>-39.99\n" +
            "<DTASOF>20250630235959\n" +
            "</LEDGERBAL>\n" +
            "</CCSTMTRS>\n" +
            "</CCSTMTTRNRS>\n" +
            "</CREDITCARDMSGSRSV1>\n" +
            "</OFX>\n";

    // Investment statement: one BUYSTOCK (10 units @ 150.25, commission
    // 9.99, total -1512.49) and one INCOME/DIV of 25.50.
    static final String INVESTMENT_OFX_SGML =
            "OFXHEADER:100\r\n" +
            "DATA:OFXSGML\r\n" +
            "VERSION:102\r\n" +
            "SECURITY:NONE\r\n" +
            "ENCODING:USASCII\r\n" +
            "CHARSET:1252\r\n" +
            "COMPRESSION:NONE\r\n" +
            "OLDFILEUID:NONE\r\n" +
            "NEWFILEUID:NONE\r\n" +
            "\r\n" +
            "<OFX>\n" +
            "<SIGNONMSGSRSV1>\n" +
            "<SONRS>\n" +
            "<STATUS>\n" +
            "<CODE>0\n" +
            "<SEVERITY>INFO\n" +
            "</STATUS>\n" +
            "<DTSERVER>20250715120000\n" +
            "<LANGUAGE>ENG\n" +
            "</SONRS>\n" +
            "</SIGNONMSGSRSV1>\n" +
            "<INVSTMTMSGSRSV1>\n" +
            "<INVSTMTTRNRS>\n" +
            "<TRNUID>1\n" +
            "<STATUS>\n" +
            "<CODE>0\n" +
            "<SEVERITY>INFO\n" +
            "</STATUS>\n" +
            "<INVSTMTRS>\n" +
            "<DTASOF>20250715120000\n" +
            "<CURDEF>USD\n" +
            "<INVACCTFROM>\n" +
            "<BROKERID>testbroker.com\n" +
            "<ACCTID>INV00042\n" +
            "</INVACCTFROM>\n" +
            "<INVTRANLIST>\n" +
            "<DTSTART>20250701000000\n" +
            "<DTEND>20250714235959\n" +
            "<BUYSTOCK>\n" +
            "<INVBUY>\n" +
            "<INVTRAN>\n" +
            "<FITID>INV0001\n" +
            "<DTTRADE>20250705000000\n" +
            "<DTSETTLE>20250708000000\n" +
            "</INVTRAN>\n" +
            "<SECID>\n" +
            "<UNIQUEID>037833100\n" +
            "<UNIQUEIDTYPE>CUSIP\n" +
            "</SECID>\n" +
            "<UNITS>10\n" +
            "<UNITPRICE>150.25\n" +
            "<COMMISSION>9.99\n" +
            "<TOTAL>-1512.49\n" +
            "<SUBACCTSEC>CASH\n" +
            "<SUBACCTFUND>CASH\n" +
            "</INVBUY>\n" +
            "<BUYTYPE>BUY\n" +
            "</BUYSTOCK>\n" +
            "<INCOME>\n" +
            "<INVTRAN>\n" +
            "<FITID>INV0002\n" +
            "<DTTRADE>20250710000000\n" +
            "</INVTRAN>\n" +
            "<SECID>\n" +
            "<UNIQUEID>037833100\n" +
            "<UNIQUEIDTYPE>CUSIP\n" +
            "</SECID>\n" +
            "<INCOMETYPE>DIV\n" +
            "<TOTAL>25.50\n" +
            "<SUBACCTSEC>CASH\n" +
            "<SUBACCTFUND>CASH\n" +
            "</INCOME>\n" +
            "</INVTRANLIST>\n" +
            "</INVSTMTRS>\n" +
            "</INVSTMTTRNRS>\n" +
            "</INVSTMTMSGSRSV1>\n" +
            "</OFX>\n";
}
