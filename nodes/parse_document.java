package nodes;

import axiom.AxiomContext;
import gen.Messages.OfxDocument;
import gen.Messages.RawOfxInput;
import java.util.Map;

public class ParseDocument {

    /**
     * Parse a full OFX/QFX document — either legacy OFX 1.x SGML (unclosed
     * tags, HTTP-header-like preamble) or OFX 2.x XML — into the canonical
     * normalized OfxDocument structure: sign-on info plus every bank,
     * credit-card, and investment statement with its account, transactions,
     * and balances. Every other extraction/filter node in this package
     * consumes this envelope rather than re-parsing raw text.
     *
     * <p>Input is rejected outright if it contains a DOCTYPE declaration
     * (XXE hardening — OFX documents never legitimately need one). A
     * malformed document (including adversarially deep SGML nesting)
     * returns a structured error rather than crashing.
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input The raw OFX/QFX document text.
     */
    public static OfxDocument parseDocument(AxiomContext ax, RawOfxInput input) {
        ax.log().info("parseDocument handling", Map.of());
        return OfxSupport.buildOfxDocument(input.getOfxText());
    }
}
