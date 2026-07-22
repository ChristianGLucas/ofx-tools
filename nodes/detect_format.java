package nodes;

import axiom.AxiomContext;
import gen.Messages.FormatInfo;
import gen.Messages.RawOfxInput;
import java.util.Map;

public class DetectFormat {

    /**
     * Detect an OFX/QFX document's declared version (e.g. "102" for OFX
     * 1.0.2, "203" for OFX 2.0.3) and syntax (SGML for OFX 1.x, XML for OFX
     * 2.x) from its header only — a lightweight text scan that never
     * attempts to parse the document body, so it succeeds even when the
     * body itself is malformed. Returns a structured error when no root
     * &lt;OFX&gt; element can be found at all.
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input The raw OFX/QFX document text.
     */
    public static FormatInfo detectFormat(AxiomContext ax, RawOfxInput input) {
        ax.log().info("detectFormat handling", Map.of());
        return OfxSupport.detectFormat(input.getOfxText());
    }
}
