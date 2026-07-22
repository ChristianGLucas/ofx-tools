package nodes;

import axiom.AxiomContext;
import gen.Messages.FormatInfo;
import gen.Messages.OfxSyntax;
import gen.Messages.RawOfxInput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DetectFormatTest {

    @Test
    public void detectsSgmlVersionFromHeader() {
        AxiomContext ax = TestSupport.testContext();
        FormatInfo result = DetectFormat.detectFormat(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.BANK_OFX_SGML).build());
        assertTrue(result.getError().getOk());
        assertEquals(OfxSyntax.SGML, result.getSyntax());
        assertEquals("102", result.getVersion());
    }

    @Test
    public void detectsXmlVersionFromProcessingInstruction() {
        AxiomContext ax = TestSupport.testContext();
        FormatInfo result = DetectFormat.detectFormat(ax, RawOfxInput.newBuilder().setOfxText(TestSupport.BANK_OFX_XML).build());
        assertTrue(result.getError().getOk());
        assertEquals(OfxSyntax.XML, result.getSyntax());
        assertEquals("203", result.getVersion());
    }

    @Test
    public void reportsStructuredErrorWhenNoOfxRootPresent() {
        AxiomContext ax = TestSupport.testContext();
        FormatInfo result = DetectFormat.detectFormat(ax, RawOfxInput.newBuilder().setOfxText("this is not an ofx document at all").build());
        assertFalse(result.getError().getOk());
        assertEquals("NO_ROOT_OFX", result.getError().getCode());
    }

    @Test
    public void doesNotParseBodySoSucceedsOnMalformedBody() {
        // Body is garbage after the root tag, but the header is well-formed —
        // detection must still succeed since it never parses the body.
        AxiomContext ax = TestSupport.testContext();
        String malformedBody = "OFXHEADER:100\nVERSION:102\n\n<OFX><THIS IS NOT VALID <<< \n";
        FormatInfo result = DetectFormat.detectFormat(ax, RawOfxInput.newBuilder().setOfxText(malformedBody).build());
        assertTrue(result.getError().getOk());
        assertEquals(OfxSyntax.SGML, result.getSyntax());
        assertEquals("102", result.getVersion());
    }
}
